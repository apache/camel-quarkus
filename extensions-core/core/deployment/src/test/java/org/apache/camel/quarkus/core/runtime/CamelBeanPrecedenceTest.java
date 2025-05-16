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

import io.quarkus.arc.DefaultBean;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.annotation.Identifier;
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
import static org.junit.jupiter.api.Assertions.assertNull;

public class CamelBeanPrecedenceTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Inject
    CamelContext context;

    @Test
    public void findSingleByTypeForDefaultBeansWithPriority() {
        BeanA bean = CamelContextHelper.findSingleByType(context, BeanA.class);
        assertEquals("bar", bean.name());
    }

    @Test
    public void findSingleByTypeForDefaultBeansWithSamePriority() {
        BeanB bean = CamelContextHelper.findSingleByType(context, BeanB.class);
        assertNull(bean);
    }

    @Test
    public void findSingleByTypeForDefaultBeansWithoutPriority() {
        BeanC bean = CamelContextHelper.findSingleByType(context, BeanC.class);
        assertNull(bean);
    }

    @Test
    public void findSingleByTypeForDefaultBean() {
        BeanD bean = CamelContextHelper.findSingleByType(context, BeanD.class);
        assertEquals("foo", bean.name());
    }

    @Test
    public void findSingleByTypeWhereIdentifierQualifierAppliedToBean() {
        BeanE bean = CamelContextHelper.findSingleByType(context, BeanE.class);
        assertEquals("foo", bean.name());
    }

    @Test
    public void findSingleByTypeWhereSingleBeanExists() {
        BeanF bean = CamelContextHelper.findSingleByType(context, BeanF.class);
        assertEquals("foo", bean.name());
    }

    @Test
    public void findSingleByTypeWhereNoBeanProduced() {
        BeanG bean = CamelContextHelper.findSingleByType(context, BeanG.class);
        assertNull(bean);
    }

    // Default beans with priority
    @Produces
    @Priority(100)
    BeanA createFooBeanA() {
        return new BeanA("foo");
    }

    @Produces
    @Priority(200)
    BeanA createBarBeanA() {
        return new BeanA("bar");
    }

    // Default beans with same priority
    @Produces
    @Priority(100)
    BeanB createFooBeanB() {
        return new BeanB("foo");
    }

    @Produces
    @Priority(100)
    BeanB createBarBeanB() {
        return new BeanB("bar");
    }

    // Multiple default beans without priority
    @Produces
    BeanC createFooBeanC() {
        return new BeanC("foo");
    }

    @Produces
    BeanC createBarBeanC() {
        return new BeanC("bar");
    }

    // Multiple beans with DefaultBean override
    @Produces
    BeanD createFooBeanD() {
        return new BeanD("foo");
    }

    @Produces
    @DefaultBean
    BeanD createBarBeanD() {
        return new BeanD("bar");
    }

    // Multiple beans with @Identifier qualifier
    @Produces
    BeanE createFooBeanE() {
        return new BeanE("foo");
    }

    @Produces
    @Identifier("bar")
    BeanE createBarBeanE() {
        return new BeanE("bar");
    }

    // Single bean
    @Produces
    BeanF createFooBeanF() {
        return new BeanF("foo");
    }

    public record BeanA(String name) {
    }

    public record BeanB(String name) {
    }

    public record BeanC(String name) {
    }

    public record BeanD(String name) {
    }

    public record BeanE(String name) {
    }

    public record BeanF(String name) {
    }

    public record BeanG(String name) {
    }
}
