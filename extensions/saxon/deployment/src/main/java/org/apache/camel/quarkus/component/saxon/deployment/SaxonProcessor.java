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
package org.apache.camel.quarkus.component.saxon.deployment;

import java.util.Collection;

import org.w3c.dom.Document;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import net.sf.saxon.Configuration;
import net.sf.saxon.functions.SystemFunction;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.xmlresolver.loaders.XmlLoader;

class SaxonProcessor {

    private static final Logger LOG = Logger.getLogger(SaxonProcessor.class);
    private static final String FEATURE = "camel-saxon";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerReflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            CombinedIndexBuildItem index) {

        // Needed to register default object models when initializing the net.sf.saxon.java.JavaPlatform
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(Document.class).methods(false).fields(false).build());
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(XmlLoader.class).methods(false).fields(false).build());

        // Register saxon functions as reflective
        Collection<ClassInfo> cis = index.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(SystemFunction.class.getName()));
        cis.stream().forEach(ci -> {
            String clazzName = ci.asClass().name().toString();
            ReflectiveClassBuildItem clazz = ReflectiveClassBuildItem.builder(clazzName).methods(false).fields(false).build();
            LOG.debugf("Registering saxon function '%s' as reflective", clazzName);
            reflectiveClasses.produce(clazz);
        });

        // Needed for xpath expression with saxon
        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(Configuration.class).methods(false).fields(false).build());
        reflectiveClasses
                .produce(ReflectiveClassBuildItem.builder(XPathFactoryImpl.class).methods(false).fields(false).build());
    }

    @BuildStep
    void indexSaxonHe(BuildProducer<IndexDependencyBuildItem> deps) {
        deps.produce(new IndexDependencyBuildItem("net.sf.saxon", "Saxon-HE"));
    }

    @BuildStep
    void runtimeInit(BuildProducer<RuntimeInitializedClassBuildItem> deps) {
        deps.produce(new RuntimeInitializedClassBuildItem("org.apache.hc.client5.http.impl.auth.NTLMEngineImpl"));
    }
}
