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
package org.apache.camel.quarkus.core.deployment.spi;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.SimpleBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelBeanInfo;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * Hold a set of beans known to the ArC container.
 */
public final class ContainerBeansBuildItem extends SimpleBuildItem {
    private final Set<CamelBeanInfo> beans;
    private final Set<DotName> classes;

    public ContainerBeansBuildItem(Collection<BeanInfo> beans) {
        this.beans = beans.stream()
                .filter(bi -> bi.getImplClazz() != null).map(SimpleCamelBeanInfo::new).collect(Collectors.toSet());
        this.classes = beans.stream()
                .map(BeanInfo::getImplClazz).filter(Objects::nonNull).map(ClassInfo::name).collect(Collectors.toSet());
    }

    public Set<CamelBeanInfo> getBeans() {
        return beans;
    }

    public Set<DotName> getClasses() {
        return classes;
    }

    private static class SimpleCamelBeanInfo implements CamelBeanInfo {
        private final String name;
        private final String type;

        public SimpleCamelBeanInfo(BeanInfo beanInfo) {
            this.name = beanInfo.getName();
            this.type = beanInfo.getImplClazz().toString();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CamelBeanInfo)) {
                return false;
            }
            CamelBeanInfo info = (CamelBeanInfo) o;
            return Objects.equals(getName(), info.getName()) &&
                    Objects.equals(getType(), info.getType());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getName(), getType());
        }
    }
}
