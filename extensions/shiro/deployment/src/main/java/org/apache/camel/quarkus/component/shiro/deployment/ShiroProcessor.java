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
package org.apache.camel.quarkus.component.shiro.deployment;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.EnableAllSecurityServicesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.CamelAuthorizationException;
import org.apache.shiro.ShiroException;
import org.jboss.jandex.DotName;

class ShiroProcessor {

    private static final String FEATURE = "camel-shiro";

    private static final DotName SHIRO_EXCEPTION_NAME = DotName.createSimple(ShiroException.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    EnableAllSecurityServicesBuildItem enableAllSecurity() {
        return new EnableAllSecurityServicesBuildItem();
    }

    @BuildStep
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        List<String> dtos = combinedIndex.getIndex()
                .getAllKnownSubclasses(SHIRO_EXCEPTION_NAME)
                .stream()
                .map(c -> c.name().toString())
                .filter(n -> n.startsWith("org.apache.shiro.auth"))
                .collect(Collectors.toList());

        dtos.add(CamelAuthorizationException.class.getName());
        dtos.add(Boolean[].class.getName());
        dtos.add(Float[].class.getName());
        dtos.add(java.util.Date[].class.getName());
        dtos.add(Calendar[].class.getName());
        dtos.add(java.sql.Date[].class.getName());
        dtos.add(Time[].class.getName());
        dtos.add(Timestamp[].class.getName());

        return new ReflectiveClassBuildItem(false, false, dtos.toArray(new String[dtos.size()]));
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("org.apache.shiro", "shiro-core");
    }
}
