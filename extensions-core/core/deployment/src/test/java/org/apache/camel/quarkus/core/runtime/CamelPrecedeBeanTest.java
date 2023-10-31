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
package org.apache.camel.quarkus.core.runtime;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.support.CamelContextHelper;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CamelPrecedeBeanTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyTestBean.class));

    @Inject
    CamelContext context;

    @Test
    public void testBeanPrecedence() {
        MyTestBean bean = CamelContextHelper.findSingleByType(context, MyTestBean.class);
        assertEquals("bar", bean.getName());
    }

    @Produces
    @Priority(100)
    MyTestBean createFoo() {
        return new MyTestBean("foo");
    }

    @Produces
    @Priority(200)
    MyTestBean createBar() {
        return new MyTestBean("bar");
    }

    public static final class MyTestBean {
        private final String name;

        public MyTestBean(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
