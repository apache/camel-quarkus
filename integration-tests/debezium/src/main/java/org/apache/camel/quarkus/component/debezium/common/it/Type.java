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
package org.apache.camel.quarkus.component.debezium.common.it;

/**
 * Each supported debezium connecter has to have its own Type.
 * Types are used to return type specific names of system properties, which will be used for sharing container
 * properties.
 */
public enum Type {

    postgres, mysql, sqlserver, mongodb;

    /** name of the camel component */
    public String getComponent() {
        return "debezium-" + name();
    }

    public String getPropertyHostname() {
        return name() + "_hostname";
    }

    public String getPropertyPort() {
        return name() + "_port";
    }

    public String getPropertyOffsetFileName() {
        return name() + "_offsetStorageFileName";
    }

    public String getPropertyUsername() {
        return name() + "_username";
    }

    public String getPropertyPassword() {
        return name() + "_password";
    }

    public String getPropertyJdbc() {
        return name() + "_jdbc";
    }
}
