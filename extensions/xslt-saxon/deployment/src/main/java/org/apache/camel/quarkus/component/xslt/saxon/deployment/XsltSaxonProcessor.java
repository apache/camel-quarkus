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
package org.apache.camel.quarkus.component.xslt.saxon.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import net.sf.saxon.Configuration;
import net.sf.saxon.functions.StringJoin;
import net.sf.saxon.functions.String_1;
import net.sf.saxon.functions.Tokenize_1;
import org.apache.camel.component.xslt.saxon.XsltSaxonBuilder;
import org.jboss.logging.Logger;
import org.xmlresolver.loaders.XmlLoader;

class XsltSaxonProcessor {

    private static final Logger LOG = Logger.getLogger(XsltSaxonProcessor.class);
    private static final String FEATURE = "camel-xslt-saxon";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClasses) {
        reflectiveClasses.produce(ReflectiveClassBuildItem
                .builder(Configuration.class, String_1.class, Tokenize_1.class, StringJoin.class).build());
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(XmlLoader.class).build());
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(XsltSaxonBuilder.class).build());

        runtimeInitializedClasses
                .produce(new RuntimeInitializedClassBuildItem("org.apache.hc.client5.http.impl.auth.NTLMEngineImpl"));
    }
}
