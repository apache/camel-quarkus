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
package org.apache.camel.quarkus.k.support;

public final class Constants {
    public static final String ENV_CAMEL_K_ROUTES = "CAMEL_K_ROUTES";
    public static final String PROPERTY_CAMEL_K_ROUTES = "camel.k.routes";

    public static final String ENV_CAMEL_K_CONF = "CAMEL_K_CONF";
    public static final String PROPERTY_CAMEL_K_CONF = "camel.k.conf";

    public static final String ENV_CAMEL_K_CONF_D = "CAMEL_K_CONF_D";
    public static final String PROPERTY_CAMEL_K_CONF_D = "camel.k.conf.d";

    public static final String ENV_CAMEL_K_CUSTOMIZERS = "CAMEL_K_CUSTOMIZERS";
    public static final String PROPERTY_CAMEL_K_CUSTOMIZER = "camel.k.customizer";

    public static final String ENV_CAMEL_K_MOUNT_PATH_CONFIGMAPS = "CAMEL_K_MOUNT_PATH_CONFIGMAPS";
    public static final String PROPERTY_CAMEL_K_MOUNT_PATH_CONFIGMAPS = "camel.k.mount-path.configmaps";

    public static final String ENV_CAMEL_K_MOUNT_PATH_SECRETS = "CAMEL_K_MOUNT_PATH_SECRETS";
    public static final String PROPERTY_CAMEL_K_MOUNT_PATH_SECRETS = "camel.k.mount-path.secrets";

    public static final String SCHEME_REF = "ref";
    public static final String SCHEME_PREFIX_REF = SCHEME_REF + ":";
    public static final String SCHEME_CLASS = "class";
    public static final String SCHEME_PREFIX_CLASS = SCHEME_CLASS + ":";
    public static final String SCHEME_CLASSPATH = "classpath";
    public static final String SCHEME_PREFIX_CLASSPATH = SCHEME_CLASSPATH + ":";
    public static final String SCHEME_FILE = "file";
    public static final String SCHEME_PREFIX_FILE = SCHEME_FILE + ":";

    public static final String LOGGING_LEVEL_PREFIX = "logging.level.";
    public static final String SOURCE_LOADER_INTERCEPTOR_RESOURCE_PATH = "META-INF/services/org.apache.camel.quarkus.k/loader/interceptor/";
    public static final String CONTEXT_CUSTOMIZER_RESOURCE_PATH = "META-INF/services/org.apache.camel.quarkus.k/customizer/";

    public static final String ENABLE_CUSTOMIZER_PATTERN = "(camel\\.k\\.)?customizer\\.([\\w][\\w-]*)\\.enabled";

    public static final String PROPERTY_PREFIX_REST_COMPONENT_PROPERTY = "camel.rest.componentProperty.";
    public static final String PROPERTY_PREFIX_REST_ENDPOINT_PROPERTY = "camel.rest.endpointProperty.";

    public static final String CUSTOMIZER_PREFIX = "camel.k.customizer.";
    public static final String CUSTOMIZER_PREFIX_FALLBACK = "customizer.";
    public static final String LOADER_INTERCEPTOR_PREFIX = "camel.k.loader.interceptor.";
    public static final String LOADER_INTERCEPTOR_PREFIX_FALLBACK = "loader.interceptor.";

    private Constants() {
    }
}
