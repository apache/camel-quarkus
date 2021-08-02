/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.grpc.deployment;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

import javax.enterprise.context.Dependent;

import io.grpc.BindableService;
import io.grpc.stub.AbstractAsyncStub;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractFutureStub;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.StreamObserver;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import org.apache.camel.component.grpc.GrpcComponent;
import org.apache.camel.component.grpc.server.GrpcMethodHandler;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.grpc.runtime.CamelGrpcRecorder;
import org.apache.camel.quarkus.grpc.runtime.CamelQuarkusBindableService;
import org.apache.camel.quarkus.grpc.runtime.QuarkusBindableServiceFactory;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

class GrpcProcessor {

    private static final DotName BINDABLE_SERVICE_DOT_NAME = DotName.createSimple(BindableService.class.getName());
    private static final DotName[] STUB_CLASS_DOT_NAMES = new DotName[] {
            DotName.createSimple(AbstractAsyncStub.class.getName()),
            DotName.createSimple(AbstractBlockingStub.class.getName()),
            DotName.createSimple(AbstractFutureStub.class.getName())
    };
    private static final String FEATURE = "camel-grpc";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndexBuildItem) {

        IndexView index = combinedIndexBuildItem.getIndex();
        for (DotName dotName : STUB_CLASS_DOT_NAMES) {
            index.getAllKnownSubclasses(dotName)
                    .stream()
                    .map(classInfo -> new ReflectiveClassBuildItem(true, false, classInfo.name().toString()))
                    .forEach(reflectiveClass::produce);
        }
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, AbstractStub.class.getName()));
    }

    @BuildStep
    void quarkusBindableServiceFactoryBean(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(QuarkusBindableServiceFactory.class));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    CamelBeanBuildItem createGrpcComponent(CamelGrpcRecorder recorder) {
        return new CamelBeanBuildItem(
                "grpc",
                GrpcComponent.class.getName(),
                recorder.createGrpcComponent());
    }

    @BuildStep
    void createBindableServiceBeans(
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            CombinedIndexBuildItem combinedIndexBuildItem) {

        IndexView index = combinedIndexBuildItem.getIndex();
        Collection<ClassInfo> bindableServiceImpls = index.getAllKnownImplementors(BINDABLE_SERVICE_DOT_NAME);

        // Generate implementation classes from any abstract gRPC BindableService implementations included in the application archive
        // Override the various sync and async methods so that requests can be intercepted and delegated to Camel routing
        // This mimics similar logic in DefaultBindableServiceFactory that uses Javassist ProxyFactory & MethodHandler
        for (ClassInfo service : bindableServiceImpls) {
            if (!Modifier.isAbstract(service.flags())) {
                continue;
            }
            if (service.name().withoutPackagePrefix().startsWith("Mutiny")) {
                /* The generate-code goal of quarkus-maven-plugin generates also Mutiny service that we do not use
                 * Not skipping it here results in randomly registering the Mutiny one or the right one.
                 * In case the Mutiny service one is registered, the client throws something like
                 * io.grpc.StatusRuntimeException: UNIMPLEMENTED */
                continue;
            }

            String superClassName = service.name().toString();
            String generatedClassName = superClassName + "QuarkusMethodHandler";

            // Register the service classes for reflection
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, service.name().toString()));
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, service.enclosingClass().toString()));
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, false, generatedClassName));

            try (ClassCreator classCreator = ClassCreator.builder()
                    .classOutput(new GeneratedBeanGizmoAdaptor(generatedBean))
                    .className(generatedClassName)
                    .superClass(superClassName)
                    .interfaces(CamelQuarkusBindableService.class)
                    .build()) {

                classCreator.addAnnotation(Dependent.class);

                FieldCreator serverMethodHandler = classCreator
                        .getFieldCreator("methodHandler", GrpcMethodHandler.class.getName())
                        .setModifiers(Modifier.PRIVATE);

                // Create constructor
                try (MethodCreator initMethod = classCreator.getMethodCreator("<init>", void.class)) {
                    initMethod.setModifiers(Modifier.PUBLIC);
                    initMethod.invokeSpecialMethod(MethodDescriptor.ofMethod(superClassName, "<init>", void.class),
                            initMethod.getThis());
                    initMethod.returnValue(null);
                }

                // Create setMethodHandler override
                try (MethodCreator setMethodHandlerMethod = classCreator.getMethodCreator("setMethodHandler", void.class,
                        GrpcMethodHandler.class)) {
                    setMethodHandlerMethod.setModifiers(Modifier.PUBLIC);

                    ResultHandle self = setMethodHandlerMethod.getThis();
                    ResultHandle methodHandlerInstance = setMethodHandlerMethod.getMethodParam(0);

                    setMethodHandlerMethod.writeInstanceField(serverMethodHandler.getFieldDescriptor(), self,
                            methodHandlerInstance);
                    setMethodHandlerMethod.returnValue(null);
                }

                // Override service methods that the gRPC component is interested in
                // E.g methods with one or two parameters where one is of type StreamObserver
                List<MethodInfo> methods = service.methods();
                for (MethodInfo method : methods) {
                    if (isCandidateServiceMethod(method)) {
                        String[] params = method.parameters()
                                .stream()
                                .map(type -> type.name().toString())
                                .toArray(String[]::new);

                        ClassInfo classInfo = index
                                .getClassByName(DotName.createSimple(GrpcMethodHandler.class.getName()));

                        String returnType = method.returnType().name().toString();
                        try (MethodCreator methodCreator = classCreator.getMethodCreator(method.name(), returnType, params)) {
                            method.exceptions()
                                    .stream()
                                    .map(type -> type.name().toString())
                                    .forEach(methodCreator::addException);

                            if (method.parameters().size() == 1) {
                                ResultHandle returnValue = generateGrpcDelegateMethod(classInfo, serverMethodHandler,
                                        methodCreator,
                                        method, "handleForConsumerStrategy");
                                methodCreator.returnValue(returnValue);
                            } else if (method.parameters().size() == 2) {
                                generateGrpcDelegateMethod(classInfo, serverMethodHandler, methodCreator, method,
                                        "handle");
                                methodCreator.returnValue(null);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isCandidateServiceMethod(MethodInfo method) {
        List<Type> parameters = method.parameters();
        if (parameters.size() == 1) {
            return parameters.get(0).name().toString().equals(StreamObserver.class.getName());
        } else if (parameters.size() == 2) {
            return parameters.get(1).name().toString().equals(StreamObserver.class.getName());
        }
        return false;
    }

    private ResultHandle generateGrpcDelegateMethod(ClassInfo classInfo, FieldCreator fieldCreator, MethodCreator methodCreator,
            MethodInfo sourceMethod, String targetMethod) {

        MethodInfo method = classInfo.methods()
                .stream()
                .filter(methodInfo -> methodInfo.name().equals(targetMethod))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Unable to find target method " + targetMethod + " on GrpcServerMethodHandler"));

        ResultHandle methodNameParam = methodCreator.load(sourceMethod.name());
        ResultHandle[] methodParams;
        if (sourceMethod.parameters().size() == 1) {
            methodParams = new ResultHandle[] { methodCreator.getMethodParam(0), methodNameParam };
        } else {
            methodParams = new ResultHandle[] { methodCreator.getMethodParam(0), methodCreator.getMethodParam(1),
                    methodNameParam };
        }

        ResultHandle resultHandle = methodCreator.readInstanceField(fieldCreator.getFieldDescriptor(), methodCreator.getThis());
        return methodCreator.invokeVirtualMethod(method, resultHandle, methodParams);
    }
}
