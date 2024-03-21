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
package org.apache.camel.quarkus.component.jt400.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.ConvTable;
import com.ibm.as400.access.NLSImplNative;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.NativeImageEnableAllCharsetsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

class Jt400Processor {

    private static final Logger LOG = Logger.getLogger(Jt400Processor.class);
    private static final String FEATURE = "camel-jt400";
    private static final DotName CONV_TABLE_NAME = DotName.createSimple(ConvTable.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<RuntimeInitializedClassBuildItem> runtimeInitializedClasses() {
        List<RuntimeInitializedClassBuildItem> items = new ArrayList<>();
        items.add(new RuntimeInitializedClassBuildItem("com.ibm.as400.access.CredentialVault"));
        return items;
    }

    @BuildStep
    NativeImageEnableAllCharsetsBuildItem charset() {
        return new NativeImageEnableAllCharsetsBuildItem();
    }

    @BuildStep
    RuntimeReinitializedClassBuildItem runtimeReiinitializedClass() {
        return new RuntimeReinitializedClassBuildItem(AS400.class.getName());
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClassesProducer,
            CombinedIndexBuildItem combinedIndex) {
        IndexView index = combinedIndex.getIndex();

        reflectiveClassesProducer.produce(ReflectiveClassBuildItem.builder(NLSImplNative.class).build());
        reflectiveClassesProducer.produce(ReflectiveClassBuildItem.builder("com.ibm.as400.access.SocketContainerInet").build());

        Pattern pattern = Pattern.compile("com.ibm.as400.access.*Remote");
        index.getKnownClasses().stream()
                .filter(c -> pattern.matcher(c.name().toString()).matches())
                .map(c -> ReflectiveClassBuildItem.builder(c.name().toString()).build())
                .forEach(reflectiveClassesProducer::produce);

        combinedIndex.getIndex()
                .getAllKnownSubclasses(CONV_TABLE_NAME)
                .stream()
                .map(c -> ReflectiveClassBuildItem.builder(c.name().toString()).build())
                .forEach(reflectiveClassesProducer::produce);

    }

    @BuildStep
    IndexDependencyBuildItem registerDependencyForIndex() {
        return new IndexDependencyBuildItem("net.sf.jt400", "jt400", "java11");
    }

}
