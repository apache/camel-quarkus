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
package org.apache.camel.quarkus.k.runtime;

public final class ApplicationConstants {
    public static final String ENV_CAMEL_K_CONF = "CAMEL_K_CONF";
    public static final String PROPERTY_CAMEL_K_CONF = "camel.k.conf";

    public static final String ENV_CAMEL_K_CONF_D = "CAMEL_K_CONF_D";
    public static final String PROPERTY_CAMEL_K_CONF_D = "camel.k.conf.d";

    public static final String ENV_CAMEL_K_MOUNT_PATH_CONFIGMAPS = "CAMEL_K_MOUNT_PATH_CONFIGMAPS";
    public static final String PROPERTY_CAMEL_K_MOUNT_PATH_CONFIGMAPS = "camel.k.mount-path.configmaps";
    public static final String PATH_CONFIGMAPS = "_configmaps";

    public static final String ENV_CAMEL_K_MOUNT_PATH_SECRETS = "CAMEL_K_MOUNT_PATH_SECRETS";
    public static final String PROPERTY_CAMEL_K_MOUNT_PATH_SECRETS = "camel.k.mount-path.secrets";
    public static final String PATH_SECRETS = "_secrets";

    public static final String ENV_CAMEL_K_MOUNT_PATH_SERVICEBINDINGS = "CAMEL_K_MOUNT_PATH_SERVICEBINDINGS";
    public static final String PROPERTY_CAMEL_K_MOUNT_PATH_SERVICEBINDINGS = "camel.k.mount-path.servicebindings";
    public static final String PATH_SERVICEBINDINGS = "_servicebindings";

    private ApplicationConstants() {
    }
}
