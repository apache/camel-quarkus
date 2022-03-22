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

import java.util.function.Predicate;

@FunctionalInterface
public interface CamelServiceFilter extends Predicate<CamelServiceBuildItem> {
    String CAMEL_SERVICE_BASE_PATH = "META-INF/services/org/apache/camel";

    static CamelServiceFilter forPathEndingWith(String path) {
        return serviceInfo -> serviceInfo.path.endsWith(path);
    }

    static CamelServiceFilter forService(String name) {
        return forPathEndingWith(CAMEL_SERVICE_BASE_PATH + "/" + name);
    }

    static CamelServiceFilter forComponent(String name) {
        return forPathEndingWith(CAMEL_SERVICE_BASE_PATH + "/component/" + name);
    }

    static CamelServiceFilter forLanguage(String name) {
        return forPathEndingWith(CAMEL_SERVICE_BASE_PATH + "/language/" + name);
    }

    static CamelServiceFilter forDataFormat(String name) {
        return forPathEndingWith(CAMEL_SERVICE_BASE_PATH + "/dataformat/" + name);
    }

    static CamelServiceFilter forName(String name) {
        return serviceInfo -> serviceInfo.name.equals(name);
    }

    static CamelServiceFilter forType(String type) {
        return serviceInfo -> serviceInfo.type.equals(type);
    }
}
