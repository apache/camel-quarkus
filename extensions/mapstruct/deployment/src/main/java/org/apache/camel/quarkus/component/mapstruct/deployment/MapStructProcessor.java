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
package org.apache.camel.quarkus.component.mapstruct.deployment;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.RuntimeValue;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.Exchange;
import org.apache.camel.component.mapstruct.MapstructComponent;
import org.apache.camel.quarkus.component.mapstruct.ConversionMethodInfo;
import org.apache.camel.quarkus.component.mapstruct.MapStructRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelTypeConverterRegistryBuildItem;
import org.apache.camel.support.SimpleTypeConverter.ConversionMethod;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ReflectionHelper;
import org.apache.camel.util.StringHelper;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.mapstruct.Mapper;

class MapStructProcessor {

    private static final String FEATURE = "camel-mapstruct";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    MapStructMapperPackagesBuildItem getMapperPackages(CombinedIndexBuildItem combinedIndex) {
        final Set<String> mapperPackages = new HashSet<>();

        Optional<String> mapperPackageName = ConfigProvider.getConfig()
                .getOptionalValue("camel.component.mapstruct.mapper-package-name", String.class);

        if (mapperPackageName.isPresent()) {
            String packages = StringUtils.deleteWhitespace(mapperPackageName.get());
            mapperPackages.addAll(Arrays.asList(packages.split(",")));
        } else {
            // Fallback on auto discovery
            combinedIndex.getIndex()
                    .getAnnotations(Mapper.class)
                    .stream()
                    .map(AnnotationInstance::target)
                    .map(AnnotationTarget::asClass)
                    .map(ClassInfo::name)
                    .map(DotName::packagePrefix)
                    .forEach(mapperPackages::add);
        }

        return new MapStructMapperPackagesBuildItem(mapperPackages);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem mapStructComponentBean(
            MapStructMapperPackagesBuildItem mapperPackages,
            ConversionMethodInfoRuntimeValuesBuildItem conversionMethodInfos,
            MapStructRecorder recorder) {
        return new CamelBeanBuildItem("mapstruct", MapstructComponent.class.getName(),
                recorder.createMapStructComponent(mapperPackages.getMapperPackages(),
                        conversionMethodInfos.getConversionMethodInfoRuntimeValues()));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void generateMapStructTypeConverters(
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            BuildProducer<GeneratedClassBuildItem> generatedClass,
            BuildProducer<UnremovableBeanBuildItem> unremovableBean,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ConversionMethodInfoRuntimeValuesBuildItem> conversionMethodInfos,
            CombinedIndexBuildItem combinedIndex,
            MapStructMapperPackagesBuildItem mapperPackages,
            MapStructRecorder recorder) {

        // The logic that follows mimics dynamic TypeConverter logic in DefaultMapStructFinder.discoverMappings
        Set<String> packages = mapperPackages.getMapperPackages();
        AtomicInteger methodCount = new AtomicInteger();
        Map<String, RuntimeValue<ConversionMethodInfo>> conversionMethods = new HashMap<>();
        IndexView index = combinedIndex.getIndex();

        // Find implementations of Mapper annotated interfaces or abstract classes
        index.getAnnotations(Mapper.class)
                .stream()
                .map(AnnotationInstance::target)
                .map(AnnotationTarget::asClass)
                .filter(classInfo -> packages.contains(classInfo.name().packagePrefix()))
                .filter(classInfo -> classInfo.isInterface() || Modifier.isAbstract(classInfo.flags()))
                .flatMap(classInfo -> Stream.concat(index.getAllKnownImplementors(classInfo.name()).stream(),
                        index.getAllKnownSubclasses(classInfo.name()).stream()))
                .forEach(classInfo -> {
                    AtomicReference<RuntimeValue<?>> mapperRuntimeValue = new AtomicReference<>();
                    String mapperClassName = classInfo.name().toString();
                    String mapperDefinitionClassName = getMapperDefinitionClassName(classInfo);
                    if (ObjectHelper.isEmpty(mapperDefinitionClassName)) {
                        return;
                    }

                    // Check if there's a static instance field defined for the Mapper
                    ClassInfo mapperDefinitionClassInfo = index.getClassByName(mapperDefinitionClassName);
                    Optional<FieldInfo> mapperInstanceField = mapperDefinitionClassInfo
                            .fields()
                            .stream()
                            .filter(fieldInfo -> Modifier.isStatic(fieldInfo.flags()))
                            .filter(fieldInfo -> fieldInfo.type().name().toString().equals(mapperDefinitionClassName))
                            .findFirst();

                    // Check of the Mapper is a CDI bean with one of the supported MapStruct annotations
                    boolean mapperBeanExists = classInfo.hasDeclaredAnnotation(ApplicationScoped.class)
                            || classInfo.hasDeclaredAnnotation(Named.class);
                    if (mapperInstanceField.isEmpty()) {
                        if (mapperBeanExists) {
                            unremovableBean
                                    .produce(new UnremovableBeanBuildItem(beanInfo -> beanInfo.hasType(classInfo.name())));
                        } else {
                            // Create the Mapper ourselves
                            mapperRuntimeValue.set(recorder.createMapper(mapperClassName));
                        }
                    }

                    /*
                     * Generate SimpleTypeConverter.ConversionMethod implementations for each candidate Mapper method.
                     *
                     * ReflectionHelper is used to resolve the mapper methods for simplicity, compared to Jandex where
                     * we potentially have to iterate over the type hierarchy (E.g for multiple interfaces,
                     * interface / class inheritance etc).
                     *
                     * public final class FooConversionMethod implements ConversionMethod {
                     *    private final FooMapperImpl mapper;
                     *
                     *    // Generated only if a Mapper instance is a CDI bean
                     *    public FooConversionMethod() {
                     *    }
                     *
                     *    // Generated only if a Mapper instance was declared on the Mapper interface
                     *    public FooConversionMethod() {
                     *        this(CarMapper.INSTANCE);
                     *    }
                     *
                     *    public FooConversionMethod(FooMapperImpl mapper) {
                     *        this.mapper = mapper;
                     *    }
                     *
                     *    @Override
                     *    public Object doConvert(Class<?> type, Exchange exchange, Object value) throws Exception {
                     *        return mapper.stringToInt(value);
                     *    }
                     * }
                     */
                    ReflectionHelper.doWithMethods(resolveClass(mapperDefinitionClassName), method -> {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (method.getParameterCount() != 1) {
                            return;
                        }

                        Class<?> fromType = parameterTypes[0];
                        Class<?> toType = method.getReturnType();
                        if (toType.isPrimitive()) {
                            return;
                        }

                        String conversionMethodClassName = String.format("%s.%s%dConversionMethod",
                                classInfo.name().packagePrefixName().toString(),
                                StringHelper.capitalize(method.getName()),
                                methodCount.incrementAndGet());

                        ClassOutput output = mapperBeanExists ? new GeneratedBeanGizmoAdaptor(generatedBean)
                                : new GeneratedClassGizmoAdaptor(generatedClass, true);

                        try (ClassCreator classCreator = ClassCreator.builder()
                                .className(conversionMethodClassName)
                                .classOutput(output)
                                .setFinal(true)
                                .interfaces(ConversionMethod.class)
                                .superClass(Object.class.getName())
                                .build()) {

                            // Take advantage of CDI and use injection to get the Mapper instance
                            if (mapperBeanExists) {
                                classCreator.addAnnotation(Singleton.class);
                                unremovableBean.produce(new UnremovableBeanBuildItem(
                                        beanInfo -> beanInfo.hasType(DotName.createSimple(conversionMethodClassName))));
                            }

                            FieldCreator mapperField = classCreator.getFieldCreator("mapper", mapperClassName)
                                    .setModifiers(Modifier.PRIVATE | Modifier.FINAL);

                            if (mapperInstanceField.isPresent() || mapperBeanExists) {
                                // Create a no-args constructor
                                try (MethodCreator initMethod = classCreator.getMethodCreator("<init>", void.class)) {
                                    initMethod.setModifiers(Modifier.PUBLIC);
                                    if (mapperBeanExists) {
                                        initMethod.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class),
                                                initMethod.getThis());
                                    } else {
                                        // If we don't have CDI injection, get the Mapper instance from the Mapper interface
                                        FieldInfo fieldInfo = mapperInstanceField.get();
                                        initMethod.invokeSpecialMethod(
                                                MethodDescriptor.ofConstructor(conversionMethodClassName,
                                                        mapperClassName),
                                                initMethod.getThis(),
                                                initMethod.readStaticField(FieldDescriptor.of(mapperClassName,
                                                        fieldInfo.name(), fieldInfo.type().toString())));
                                    }
                                    initMethod.returnNull();
                                }
                            }

                            try (MethodCreator initMethod = classCreator.getMethodCreator("<init>", void.class,
                                    mapperClassName)) {
                                initMethod.setModifiers(Modifier.PUBLIC);
                                if (mapperBeanExists) {
                                    initMethod.addAnnotation(Inject.class);
                                }
                                initMethod.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class),
                                        initMethod.getThis());
                                initMethod.writeInstanceField(mapperField.getFieldDescriptor(), initMethod.getThis(),
                                        initMethod.getMethodParam(0));
                                initMethod.returnNull();
                            }

                            // doConvert implementation
                            try (MethodCreator doConvertMethod = classCreator.getMethodCreator("doConvert",
                                    Object.class, Class.class, Exchange.class, Object.class)) {
                                doConvertMethod.setModifiers(Modifier.PUBLIC);

                                MethodDescriptor mapperMethod = MethodDescriptor.ofMethod(mapperClassName,
                                        method.getName(), toType.getName(), fromType.getName());

                                ResultHandle mapper = doConvertMethod
                                        .readInstanceField(mapperField.getFieldDescriptor(), doConvertMethod.getThis());

                                // Invoke the target method on the Mapper with the 'value' method arg from convertTo
                                ResultHandle mapperResult = doConvertMethod.invokeVirtualMethod(mapperMethod,
                                        mapper, doConvertMethod.getMethodParam(2));

                                doConvertMethod.returnValue(mapperResult);
                            }
                        }

                        // Register the 'to' type for reflection (See MapstructEndpoint.doBuild())
                        reflectiveClass.produce(ReflectiveClassBuildItem.builder(toType).build());

                        // Instantiate the generated ConversionMethod
                        String key = String.format("%s:%s", fromType.getName(), toType.getName());
                        conversionMethods.computeIfAbsent(key,
                                conversionMethodsKey -> recorder.createConversionMethodInfo(fromType, toType,
                                        mapperBeanExists,
                                        mapperRuntimeValue.get(), conversionMethodClassName));
                    });
                });

        conversionMethodInfos
                .produce(new ConversionMethodInfoRuntimeValuesBuildItem(new HashSet<>(conversionMethods.values())));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void registerTypeConverters(
            BeanContainerBuildItem beanContainer,
            CamelTypeConverterRegistryBuildItem typeConverterRegistry,
            ConversionMethodInfoRuntimeValuesBuildItem conversionMethodInfos,
            MapStructRecorder recorder) {
        // Register the TypeConverter leveraging the generated ConversionMethod
        recorder.registerMapStructTypeConverters(typeConverterRegistry.getRegistry(),
                conversionMethodInfos.getConversionMethodInfoRuntimeValues(), beanContainer.getValue());
    }

    @BuildStep
    void registerMapperServiceProviders(
            BuildProducer<ServiceProviderBuildItem> serviceProvider,
            CombinedIndexBuildItem combinedIndex,
            MapStructMapperPackagesBuildItem mapperPackages) {
        // If a Mapper definition uses a custom implementationName then they may get loaded via the ServiceLoader
        Set<String> packages = mapperPackages.getMapperPackages();
        combinedIndex.getIndex()
                .getAnnotations(Mapper.class)
                .stream()
                .filter(annotationInstance -> packages.contains(annotationInstance.target().asClass().name().packagePrefix()))
                .forEach(annotation -> {
                    AnnotationValue value = annotation.value("implementationName");
                    if (value != null) {
                        DotName name = annotation.target().asClass().name();
                        AnnotationValue implementationPackage = annotation.value("implementationPackage");
                        String packageName = implementationPackage != null ? implementationPackage.asString()
                                : name.packagePrefix();
                        serviceProvider
                                .produce(new ServiceProviderBuildItem(name.toString(), packageName + "." + value.asString()));
                    }
                });
    }

    /**
     * Gets the name of the Mapper interface or abstract class
     */
    private String getMapperDefinitionClassName(ClassInfo mapStructMapperImpl) {
        List<DotName> interfaces = mapStructMapperImpl.interfaceNames();
        if (interfaces.isEmpty()) {
            String superClassName = mapStructMapperImpl.superClassType().name().toString();
            if (!superClassName.equals(Object.class.getName())) {
                return superClassName;
            }
            return null;
        }
        return interfaces.get(0).toString();
    }

    private Class<?> resolveClass(String className) {
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
