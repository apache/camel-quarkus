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
package org.apache.camel.quarkus.component.ldap.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AllowJNDIBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class LdapProcessor {

    private static final String FEATURE = "camel-ldap";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AllowJNDIBuildItem enableJNDI() {
        return new AllowJNDIBuildItem();
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflection) {
        reflection.produce(ReflectiveClassBuildItem
                .builder("com.sun.jndi.ldap.LdapCtxFactory", "com.sun.jndi.dns.DnsContextFactory",
                        "com.sun.jndi.rmi.registry.RegistryContextFactory")
                .build());
    }
}
