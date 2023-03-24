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
package org.apache.camel.quarkus.component.rss.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class RssProcessor {

    private static final String FEATURE = "camel-rss";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResources() {
        return new NativeImageResourceBuildItem("com/rometools/rome/rome.properties");
    }

    @BuildStep
    IndexDependencyBuildItem indexDependencies() {
        return new IndexDependencyBuildItem("com.rometools", "rome");
    }

    @BuildStep
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        // Register for reflection feed parser / generator classes from rome.properties
        try (InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("com/rometools/rome/rome.properties")) {
            Properties properties = new Properties();
            properties.load(stream);

            List<String> parserGenerators = new ArrayList<>();
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                for (String className : entry.getValue().toString().split(" ")) {
                    parserGenerators.add(className);
                }
            }

            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(parserGenerators.toArray(new String[parserGenerators.size()]))
                            .build());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Rome does some reflective work on classes that can be cloned
        String[] clonableClasses = new String[] {
                "com.rometools.rome.feed.module.DCModuleImpl",
                "com.rometools.rome.feed.module.SyModuleImpl",
                "com.rometools.rome.feed.module.ModuleImpl",
                "java.util.Date",
        };
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(clonableClasses).methods().build());
    }
}
