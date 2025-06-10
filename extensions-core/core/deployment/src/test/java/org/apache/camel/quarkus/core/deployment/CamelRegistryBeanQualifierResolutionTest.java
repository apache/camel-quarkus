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

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.function.Consumer;

import io.quarkus.arc.Unremovable;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.StaticBytecodeRecorderBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import org.apache.camel.CamelContext;
import org.apache.camel.quarkus.core.CamelBeanQualifierResolver;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanQualifierResolverBuildItem;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CamelRegistryBeanQualifierResolutionTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            String methodName = "execute";
                            BytecodeRecorderImpl recorder = new BytecodeRecorderImpl(
                                    true,
                                    getClass().getSimpleName(),
                                    methodName,
                                    Integer.toString(methodName.hashCode()),
                                    true, s -> null);

                            RuntimeValue<CamelBeanQualifierResolver> runtimeValueFoo = recorder
                                    .newInstance(FooNamedBeanQualifierResolver.class.getName());
                            context.produce(new StaticBytecodeRecorderBuildItem(recorder));

                            CamelBeanQualifierResolverBuildItem buildItemFoo = new CamelBeanQualifierResolverBuildItem(
                                    Foo.class,
                                    "foo",
                                    runtimeValueFoo);
                            context.produce(buildItemFoo);

                            RuntimeValue<CamelBeanQualifierResolver> runtimeValueBar = recorder
                                    .newInstance(BarNamedBeanQualifierResolver.class.getName());
                            context.produce(new StaticBytecodeRecorderBuildItem(recorder));

                            CamelBeanQualifierResolverBuildItem buildItemBar = new CamelBeanQualifierResolverBuildItem(
                                    Bar.class,
                                    "bar",
                                    runtimeValueBar);
                            context.produce(buildItemBar);
                        }
                    }).produces(StaticBytecodeRecorderBuildItem.class).produces(CamelBeanQualifierResolverBuildItem.class)
                            .build();
                }
            })
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    CamelContext context;

    @Test
    public void testBeanLookupWithQualifiers() {
        // Foo & Bar should be resolvable since we have a CamelBeanQualifierResolver for those types
        Set<Foo> fooBeans = context.getRegistry().findByType(Foo.class);
        assertEquals(1, fooBeans.size());

        Foo foo = context.getRegistry().lookupByNameAndType("foo", Foo.class);
        assertNotNull(foo);

        foo = context.getRegistry().findSingleByType(Foo.class);
        assertNotNull(foo);

        Bar bar = context.getRegistry().lookupByNameAndType("bar", Bar.class);
        assertNotNull(bar);

        // We don't care about qualifiers when doing findByType so we should find all Bar beans
        Set<Bar> barBeans = context.getRegistry().findByType(Bar.class);
        assertEquals(2, barBeans.size());

        // There are 2 beans so we can't find using findSingleByType
        bar = context.getRegistry().findSingleByType(Bar.class);
        assertNull(bar);

        // Bar2 should not be resolvable as there is no CamelBeanQualifierResolver to help with its bean resolution
        Bar bar2 = context.getRegistry().lookupByNameAndType("bar2", Bar.class);
        assertNull(bar2);
    }

    @Qualifier
    @Retention(RUNTIME)
    @Target({ TYPE, METHOD })
    public @interface CamelQuarkusQualifier {
        String value() default "";

        class CamelQuarkusLiteral extends AnnotationLiteral<CamelQuarkusQualifier> implements CamelQuarkusQualifier {
            private final String value;

            public CamelQuarkusLiteral(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return this.value;
            }
        }
    }

    public static class FooNamedBeanQualifierResolver implements CamelBeanQualifierResolver {
        @Override
        public Annotation[] resolveQualifiers() {
            return new Annotation[] { new CamelQuarkusQualifier.CamelQuarkusLiteral("foo") };
        }
    }

    public static class BarNamedBeanQualifierResolver implements CamelBeanQualifierResolver {
        @Override
        public Annotation[] resolveQualifiers() {
            return new Annotation[] { new CamelQuarkusQualifier.CamelQuarkusLiteral("bar") };
        }
    }

    public static class Foo {
    }

    public static class Bar {
    }

    @ApplicationScoped
    public static class Service {
        @Unremovable
        @Produces
        @CamelQuarkusQualifier("foo")
        public Foo foo() {
            return new Foo();
        }

        @Unremovable
        @Produces
        @CamelQuarkusQualifier("bar")
        public Bar bar() {
            return new Bar();
        }

        @Unremovable
        @Produces
        @CamelQuarkusQualifier("bar2")
        public Bar bar2() {
            return new Bar();
        }
    }
}
