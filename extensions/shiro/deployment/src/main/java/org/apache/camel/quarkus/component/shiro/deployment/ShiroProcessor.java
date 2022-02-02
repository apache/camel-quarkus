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

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
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
    ReflectiveClassBuildItem registerForReflection(CombinedIndexBuildItem combinedIndex) {
        List<String> reflectiveClasses = combinedIndex.getIndex()
                .getAllKnownSubclasses(SHIRO_EXCEPTION_NAME)
                .stream()
                .map(c -> c.name().toString())
                .filter(n -> n.startsWith("org.apache.shiro.auth"))
                .collect(Collectors.toList());

        reflectiveClasses.add(CamelAuthorizationException.class.getName());

        // commons-beanutils converter types and their array counterparts need to be registered for reflection
        reflectiveClasses.add(BigDecimal.class.getName());
        reflectiveClasses.add(BigDecimal[].class.getName());
        reflectiveClasses.add(BigInteger.class.getName());
        reflectiveClasses.add(BigInteger[].class.getName());
        reflectiveClasses.add(Boolean.class.getName());
        reflectiveClasses.add(Boolean[].class.getName());
        reflectiveClasses.add(Byte.class.getName());
        reflectiveClasses.add(Byte[].class.getName());
        reflectiveClasses.add(Calendar.class.getName());
        reflectiveClasses.add(Calendar[].class.getName());
        reflectiveClasses.add(Character.class.getName());
        reflectiveClasses.add(Character[].class.getName());
        reflectiveClasses.add(java.util.Date.class.getName());
        reflectiveClasses.add(java.util.Date[].class.getName());
        reflectiveClasses.add(java.sql.Date.class.getName());
        reflectiveClasses.add(java.sql.Date[].class.getName());
        reflectiveClasses.add(Double.class.getName());
        reflectiveClasses.add(Double[].class.getName());
        reflectiveClasses.add(File.class.getName());
        reflectiveClasses.add(File[].class.getName());
        reflectiveClasses.add(Float.class.getName());
        reflectiveClasses.add(Float[].class.getName());
        reflectiveClasses.add(Integer.class.getName());
        reflectiveClasses.add(Integer[].class.getName());
        reflectiveClasses.add(Long.class.getName());
        reflectiveClasses.add(Long[].class.getName());
        reflectiveClasses.add(Short.class.getName());
        reflectiveClasses.add(Short[].class.getName());
        reflectiveClasses.add(String.class.getName());
        reflectiveClasses.add(String[].class.getName());
        reflectiveClasses.add(Time.class.getName());
        reflectiveClasses.add(Time[].class.getName());
        reflectiveClasses.add(Timestamp.class.getName());
        reflectiveClasses.add(Timestamp[].class.getName());
        reflectiveClasses.add(URL.class.getName());
        reflectiveClasses.add(URL[].class.getName());

        return new ReflectiveClassBuildItem(false, false, reflectiveClasses.toArray(new String[reflectiveClasses.size()]));
    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("org.apache.shiro", "shiro-core");
    }
}
