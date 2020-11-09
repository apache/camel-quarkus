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
package org.apache.camel.quarkus.component.msv.deployment;

import com.sun.msv.verifier.jarv.FactoryLoaderImpl;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.apache.camel.support.processor.validation.DefaultValidationErrorHandler;
import org.relaxng.datatype.DatatypeLibraryFactory;

class MsvProcessor {

    private static final String FEATURE = "camel-msv";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerReflectiveClasses(BuildProducer<ReflectiveClassBuildItem> producer) {
        producer.produce(new ReflectiveClassBuildItem(false, false, FactoryLoaderImpl.class));
        producer.produce(new ReflectiveClassBuildItem(false, false, DatatypeLibraryFactory.class));
        producer.produce(new ReflectiveClassBuildItem(false, false, DefaultValidationErrorHandler.class));

        String datatypeLibraryLoaderClass = "org.relaxng.datatype.helpers.DatatypeLibraryLoader$Service$Loader2";
        producer.produce(new ReflectiveClassBuildItem(false, false, datatypeLibraryLoaderClass));
    }

    @BuildStep
    void registerResources(BuildProducer<NativeImageResourceBuildItem> producer) {
        String verifierFactory = "META-INF/services/org.iso_relax.verifier.VerifierFactoryLoader";
        producer.produce(new NativeImageResourceBuildItem(verifierFactory));
    }

    @BuildStep
    void registerResourceBundles(BuildProducer<NativeImageResourceBundleBuildItem> producer) {
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.verifier.regexp.xmlschema.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.verifier.regexp.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.verifier.identity.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.verifier.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.relaxns.verifier.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.relaxns.grammar.relax.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.reader.xmlschema.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.reader.trex.ng.comp.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.reader.trex.ng.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.reader.trex.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.reader.relax.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.reader.dtd.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.grammar.trex.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.grammar.relaxng.Messages"));
        producer.produce(new NativeImageResourceBundleBuildItem("com.sun.msv.driver.textui.Messages"));
    }
}
