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
package org.apache.camel.quarkus.core.deployment.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.apache.camel.util.IOHelper;
import org.jboss.jandex.DotName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaResource.class);

    private String type;
    private String name;
    private DotName className;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DotName getClassName() {
        return className;
    }

    public void setClassName(DotName className) {
        this.className = className;
    }

    public String getLocation() {
        String packageName = className.prefix().toString();
        return packageName.replace('.', '/') + "/" + name + ".json";
    }

    public String load() {
        InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream(getLocation());
        if (resource != null) {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Resolved JSON schema resource: {}", getLocation());
                }

                return IOHelper.loadText(resource);
            } catch (IOException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Unable to load JSON schema resource: {}", getLocation(), e);
                }
            } finally {
                IOHelper.close(resource);
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SchemaResource that = (SchemaResource) o;
        return Objects.equals(type, that.type)
                && Objects.equals(name, that.name)
                && Objects.equals(className, that.className);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, className);
    }

    @Override
    public String toString() {
        return "SchemaResource{" + "type='" + type + '\'' + ", name='" + name + '\'' + ", packageName='" + className + '\''
                + '}';
    }
}
