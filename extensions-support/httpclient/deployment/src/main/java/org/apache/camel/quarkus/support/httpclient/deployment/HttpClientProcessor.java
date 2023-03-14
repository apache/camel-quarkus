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
package org.apache.camel.quarkus.support.httpclient.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

class HttpClientProcessor {
    private static final DotName HTTP_REQUEST_BASE_NAME = DotName.createSimple(
            "org.apache.http.client.methods.HttpRequestBase");

    @BuildStep
    AdditionalApplicationArchiveMarkerBuildItem markers() {
        return new AdditionalApplicationArchiveMarkerBuildItem("org/apache/http/client");
    }

    @BuildStep
    void registerForReflection(
            CombinedIndexBuildItem index,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        IndexView view = index.getIndex();

        for (ClassInfo info : view.getAllKnownSubclasses(HTTP_REQUEST_BASE_NAME)) {
            reflectiveClasses
                    .produce(ReflectiveClassBuildItem.builder(info.name().toString()).methods(true).fields(false).build());
        }
    }

    @BuildStep
    NativeImageResourceBuildItem suffixListResource() {
        // Required by org.apache.http.conn.util.PublicSuffixMatcher
        return new NativeImageResourceBuildItem("mozilla/public-suffix-list.txt");
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitializedClasses() {
        return new RuntimeInitializedClassBuildItem("org.apache.http.impl.auth.NTLMEngineImpl");
    }
}
