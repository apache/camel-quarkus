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
package org.apache.camel.quarkus.test.internal;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.builder.ConsumeFlag;
import io.quarkus.builder.ConsumeFlags;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.deployment.builditem.TestProfileBuildItem;
import io.quarkus.test.junit.buildchain.TestBuildChainCustomizerProducer;
import org.apache.camel.quarkus.core.deployment.spi.RoutesBuilderClassExcludeBuildItem;
import org.apache.camel.quarkus.test.CamelQuarkusTest;
import org.apache.camel.quarkus.test.CamelTestHelper;
import org.jboss.jandex.Index;

/**
 * Adds the build steps needed by {@link CamelQuarkusTest} to the build of test applications.
 */
public class CamelQuarkusTestBuildChainCustomizerProducer implements TestBuildChainCustomizerProducer {

    @Override
    public Consumer<BuildChainBuilder> produce(Index testClassesIndex) {
        return new Consumer<>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        CamelQuarkusTestInspector.Result result = CamelQuarkusTestInspector.inspect(testClassesIndex,
                                QuarkusClassLoader::isClassPresentAtRuntime);
                        result.assertValid();

                        if (!result.isEmpty()) {
                            // Quarkus only detects composed @QuarkusTest annotations declared in the test classes
                            context.produce(new TestAnnotationBuildItem(CamelQuarkusTest.class.getName()));
                        }
                        for (String pattern : result.routesBuilderExcludes()) {
                            context.produce(RoutesBuilderClassExcludeBuildItem.ofPathPattern(pattern));
                        }

                        TestProfileBuildItem testProfile = context.consume(TestProfileBuildItem.class);
                        if (testProfile != null) {
                            String isolatedTestClass = result.isolatedTestClasses()
                                    .get(testProfile.getTestProfileClassName());
                            if (isolatedTestClass != null) {
                                context.produce(new GeneratedResourceBuildItem(
                                        CamelQuarkusTestLifecycleHook.ISOLATED_TEST_CLASS_RESOURCE,
                                        isolatedTestClass.getBytes(StandardCharsets.UTF_8)));
                            }
                        }
                    }

                    @Override
                    public String toString() {
                        return CamelQuarkusTestBuildChainCustomizerProducer.class.getSimpleName() + "#inspectTestClasses";
                    }
                })
                        .consumes(TestProfileBuildItem.class, ConsumeFlags.of(ConsumeFlag.OPTIONAL))
                        .produces(TestAnnotationBuildItem.class)
                        .produces(RoutesBuilderClassExcludeBuildItem.class)
                        .produces(GeneratedResourceBuildItem.class)
                        .build();

                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(AdditionalBeanBuildItem.unremovableOf(CamelTestHelper.class));
                    }

                    @Override
                    public String toString() {
                        return CamelQuarkusTestBuildChainCustomizerProducer.class.getSimpleName() + "#camelTestHelper";
                    }
                })
                        .produces(AdditionalBeanBuildItem.class)
                        .build();
            }
        };
    }
}
