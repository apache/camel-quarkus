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
package org.apache.camel.quarkus.component.jing.deployment;

import com.thaiopensource.validate.auto.AutoSchemaReceiver;
import com.thaiopensource.validate.auto.SchemaReaderLoaderSchemaReceiverFactory;
import com.thaiopensource.validate.auto.SchemaReceiverFactory;
import com.thaiopensource.validate.rng.SAXSchemaReceiverFactory;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.relaxng.datatype.DatatypeLibraryFactory;

class JingProcessor {

    private static final String FEATURE = "camel-jing";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerReflectiveClasses(BuildProducer<ReflectiveClassBuildItem> producers) {
        producers.produce(new ReflectiveClassBuildItem(false, false, AutoSchemaReceiver.class));
        producers.produce(new ReflectiveClassBuildItem(false, false, DatatypeLibraryFactory.class));
        producers.produce(new ReflectiveClassBuildItem(false, false, SchemaReaderLoaderSchemaReceiverFactory.class));
        producers.produce(new ReflectiveClassBuildItem(false, false, SchemaReceiverFactory.class));
        producers.produce(new ReflectiveClassBuildItem(false, false, SAXSchemaReceiverFactory.class));

        String utilLoaderClass = "com.thaiopensource.util.Service$Loader2";
        producers.produce(new ReflectiveClassBuildItem(false, false, utilLoaderClass));
        String datatypeLibraryLoaderClass = "org.relaxng.datatype.helpers.DatatypeLibraryLoader$Service$Loader2";
        producers.produce(new ReflectiveClassBuildItem(false, false, datatypeLibraryLoaderClass));
    }

    @BuildStep
    void registerResourceBundles(BuildProducer<NativeImageResourceBundleBuildItem> producer) {
        producer.produce(new NativeImageResourceBundleBuildItem("com.thaiopensource.relaxng.impl.resources.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.thaiopensource.relaxng.parse.compact.resources.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.thaiopensource.relaxng.util.resources.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.thaiopensource.validate.auto.resources.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.thaiopensource.xml.sax.resources.Messages"));
    }

    @BuildStep
    void registerResources(BuildProducer<NativeImageResourceBuildItem> producer) {
        String versionResource = "com/thaiopensource/relaxng/util/resources/Version.properties";
        producer.produce(new NativeImageResourceBuildItem(versionResource));

        String autoSrfImpls = "META-INF/services/com.thaiopensource.validate.auto.SchemaReceiverFactory";
        producer.produce(new NativeImageResourceBuildItem(autoSrfImpls));
    }
}
