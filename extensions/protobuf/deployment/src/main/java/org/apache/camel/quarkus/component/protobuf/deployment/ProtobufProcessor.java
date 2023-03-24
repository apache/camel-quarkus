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
package org.apache.camel.quarkus.component.protobuf.deployment;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessageV3;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

class ProtobufProcessor {

    private static final Logger LOG = Logger.getLogger(ProtobufProcessor.class);
    private static final String FEATURE = "camel-protobuf";
    private static final DotName[] MESSAGE_CLASS_DOT_NAMES = new DotName[] {
            DotName.createSimple(GeneratedMessageV3.class.getName()),
            DotName.createSimple(GeneratedMessage.class.getName())
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            CombinedIndexBuildItem combinedIndexBuildItem) {

        IndexView index = combinedIndexBuildItem.getIndex();
        for (DotName dotName : MESSAGE_CLASS_DOT_NAMES) {
            index.getAllKnownSubclasses(dotName)
                    .stream()
                    .map(classInfo -> ReflectiveClassBuildItem.builder(classInfo.name().toString()).methods()
                            .build())
                    .forEach(reflectiveClasses::produce);
        }
    }

}
