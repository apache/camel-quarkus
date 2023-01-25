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
package org.apache.camel.quarkus.component.bean.cdi;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;

public class Producers {

    public static interface BeanInstance {
        String getName();
    }

    //beans with default bean
    public static class WithDefaultBeanInstance implements BeanInstance {
        private final String name;

        public WithDefaultBeanInstance(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    @Unremovable
    public WithDefaultBeanInstance defaultBean() {
        return new WithDefaultBeanInstance("defaultBean");
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    public WithDefaultBeanInstance defaultOverridingBean() {
        return new WithDefaultBeanInstance("overridingBean");
    }

    //beans without default bean
    public static class WithoutDefaultBeanInstance implements BeanInstance {
        private final String name;

        public WithoutDefaultBeanInstance(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    public WithoutDefaultBeanInstance withoutDefaultBean1() {
        return new WithoutDefaultBeanInstance("bean1");
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    public WithoutDefaultBeanInstance withoutDefaultBean2() {
        return new WithoutDefaultBeanInstance("bean2");
    }

    //beans with alternate beans
    public static class WithAlternateBeanInstance implements BeanInstance {
        private final String name;

        public WithAlternateBeanInstance(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Produces
    @ApplicationScoped
    @Alternative
    @Unremovable
    public WithAlternateBeanInstance toBeAlternatedBean() {
        return new WithAlternateBeanInstance("toBeAlteredBean");
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    public WithAlternateBeanInstance alternatingBean() {
        return new WithAlternateBeanInstance("alternatingBean");
    }
}
