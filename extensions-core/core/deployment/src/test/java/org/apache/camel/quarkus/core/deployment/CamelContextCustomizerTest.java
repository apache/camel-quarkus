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
package org.apache.camel.quarkus.core.deployment;

import java.util.function.Consumer;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextCustomizerBuildItem;
import org.apache.camel.spi.CamelContextCustomizer;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CamelContextCustomizerTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .addBuildChainCustomizer(buildCustomizer())
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(RestConfigurationCustomizer.class));

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        String methodName = "execute";
                        BytecodeRecorderImpl bri = new BytecodeRecorderImpl(true, getClass().getSimpleName(), methodName,
                                Integer.toString(methodName.hashCode()), true, s -> null);
                        RuntimeValue<CamelContextCustomizer> value = bri
                                .newInstance(RestConfigurationCustomizer.class.getName());
                        context.produce(new CamelContextCustomizerBuildItem(value));
                        context.produce(new StaticBytecodeRecorderBuildItem(bri));
                    }
                }).produces(CamelContextCustomizerBuildItem.class).produces(StaticBytecodeRecorderBuildItem.class).build();
            }
        };
    }

    @Inject
    CamelContext camelContext;

    @Test
    public void testRestConfiguration() {
        assertThat(camelContext.getRestConfiguration().getApiContextPath()).isEqualTo("/example");
    }
}
