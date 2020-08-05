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
package org.apache.camel.quarkus.component.smallrye.reactive.messaging.deployment;

import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.smallrye.reactive.messaging.camel.CamelConnector;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

class SmallRyeReactiveMessagingProcessor {

    private static final DotName CAMEL_CONNECTOR_DOTNAME = DotName.createSimple(CamelConnector.class.getName());

    private static final String FEATURE = "camel-smallrye-reactive-messaging";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void overrideSmallRyeReactiveMessagingConfiguration(BuildProducer<AnnotationsTransformerBuildItem> transformers) {
        // Veto setup & configuration logic that is already handled by the camel-quarkus-reactive-streams extension
        transformers.produce(new AnnotationsTransformerBuildItem(context -> {
            if (context.isField()) {
                FieldInfo fieldInfo = context.getTarget().asField();
                ClassInfo classInfo = fieldInfo.declaringClass();

                // Make CamelReactiveStreamsService injectable from producers configured in the reactive-streams extension
                if (classInfo.name().equals(CAMEL_CONNECTOR_DOTNAME) && fieldInfo.name().equals("reactive")) {
                    AnnotationInstance injectAnnotation = getAnnotationInstance(DotNames.INJECT, fieldInfo);
                    context.transform().add(injectAnnotation).done();
                }
            }

            if (context.isMethod()) {
                MethodInfo methodInfo = context.getTarget().asMethod();
                ClassInfo classInfo = methodInfo.declaringClass();

                if (classInfo.name().equals(CAMEL_CONNECTOR_DOTNAME)) {
                    // Disable CamelReactiveStreamsService producer since the reactive-streams extension handles this
                    if (methodInfo.name().equals("getCamelReactive")) {
                        AnnotationInstance producesAnnotation = getAnnotationInstance(DotNames.PRODUCES, methodInfo);
                        context.transform()
                                .remove(annotationInstance -> annotationInstance.target().equals(producesAnnotation.target()))
                                .done();
                    }

                    // Remove @PostConstruct from the init method as the configuration logic is handled by the reactive-streams extension
                    if (methodInfo.name().equals("init")) {
                        AnnotationInstance postConstructAnnotation = getAnnotationInstance(DotNames.POST_CONSTRUCT, methodInfo);
                        context.transform()
                                .remove(methodAnnotation -> methodAnnotation.target().equals(postConstructAnnotation.target()))
                                .done();
                    }
                }
            }
        }));
    }

    private AnnotationInstance getAnnotationInstance(DotName dotName, AnnotationTarget target) {
        return AnnotationInstance.create(dotName, target, new AnnotationValue[0]);
    }
}
