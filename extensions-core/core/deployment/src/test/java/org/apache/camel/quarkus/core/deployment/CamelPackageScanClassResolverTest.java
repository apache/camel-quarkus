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

import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.quarkus.core.deployment.spi.CamelPackageScanClassBuildItem;
import org.apache.camel.spi.PackageScanClassResolver;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CamelPackageScanClassResolverTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new CamelPackageScanClassBuildItem(Cat.class.getName()));
                            context.produce(new CamelPackageScanClassBuildItem(Dog.class.getName()));
                            context.produce(new CamelPackageScanClassBuildItem(Mushroom.class.getName()));
                        }
                    })
                            .produces(CamelPackageScanClassBuildItem.class)
                            .build();
                }
            })
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    CamelContext context;

    @Test
    public void findImplementations() {
        ExtendedCamelContext ecc = context.adapt(ExtendedCamelContext.class);
        PackageScanClassResolver resolver = ecc.getPackageScanClassResolver();
        Set<Class<?>> classes = resolver.findImplementations(Animal.class, Animal.class.getPackageName());
        assertNotNull(classes);
        assertEquals(2, classes.size());
        assertTrue(classes.contains(Cat.class));
        assertTrue(classes.contains(Dog.class));
    }

    @Test
    public void findByFilter() {
        ExtendedCamelContext ecc = context.adapt(ExtendedCamelContext.class);
        PackageScanClassResolver resolver = ecc.getPackageScanClassResolver();
        Set<Class<?>> classes = resolver.findByFilter(Fungi.class::isAssignableFrom, Fungi.class.getPackageName());
        assertNotNull(classes);
        assertEquals(1, classes.size());
        assertEquals(Mushroom.class, classes.iterator().next());
    }

    @Test
    public void findAnnotated() {
        ExtendedCamelContext ecc = context.adapt(ExtendedCamelContext.class);
        PackageScanClassResolver resolver = ecc.getPackageScanClassResolver();
        Set<Class<?>> classes = resolver.findAnnotated(Singleton.class, Animal.class.getPackageName());
        assertNotNull(classes);
        assertEquals(1, classes.size());
        assertEquals(Cat.class, classes.iterator().next());
    }

    interface Animal {
    }

    interface Fungi {
    }

    @Singleton
    static final class Cat implements Animal {
    }

    static final class Dog implements Animal {
    }

    static final class Mushroom implements Fungi {
    }
}
