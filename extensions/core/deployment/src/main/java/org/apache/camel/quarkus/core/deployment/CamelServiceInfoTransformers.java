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

import java.nio.file.Path;
import java.nio.file.Paths;

public final class CamelServiceInfoTransformers {

    private static final Path CONFIGURER_PATH = Paths.get("configurer");

    private CamelServiceInfoTransformers() {
    }

    /**
     * Configurers need to get registered under a different name in the registry than the name of
     * the service file
     *
     * @param  serviceInfo the {@link CamelServiceInfo} that will possibly be transformed
     * @return             the given {@code serviceInfo} or a new {@link CamelServiceInfo}
     */
    public static CamelServiceInfo configurer(CamelServiceInfo serviceInfo) {
        final Path path = serviceInfo.path;
        final int pathLen = path.getNameCount();
        return (pathLen >= 2 && CONFIGURER_PATH.equals(path.getName(pathLen - 2)))
                ? serviceInfo.withName(path.getFileName().toString() + "-configurer")
                : serviceInfo;
    }
}
