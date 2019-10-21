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
package org.apache.camel.quarkus.component.xml.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.jaxb.deployment.JaxbFileRootBuildItem;
import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.camel.quarkus.component.xml.XmlRecorder;
import org.apache.camel.quarkus.core.deployment.CamelModelJAXBContextFactoryBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelRoutesCollectorBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelSupport;
import org.apache.camel.quarkus.core.deployment.CamelXmlLoaderBuildItem;

class XmlProcessor {

    private static final String FEATURE = "camel-xml";

    @BuildStep
    JaxbFileRootBuildItem fileRoot() {
        return new JaxbFileRootBuildItem(CamelSupport.CAMEL_ROOT_PACKAGE_DIRECTORY);
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    CamelModelJAXBContextFactoryBuildItem contextFactory(XmlRecorder recorder) {
        return new CamelModelJAXBContextFactoryBuildItem(recorder.newContextFactory());
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    CamelXmlLoaderBuildItem xmlLoader(XmlRecorder recorder) {
        return new CamelXmlLoaderBuildItem(recorder.newDefaultXmlLoader());
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    public CamelRoutesCollectorBuildItem routesCollector(XmlRecorder recorder) {
        return new CamelRoutesCollectorBuildItem(recorder.newDefaultRoutesCollector());
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT)
    void initXmlReifiers(XmlRecorder recorder) {
        recorder.initXmlReifiers();
    }

    @BuildStep
    void reflective(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
            "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
            "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
            "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
            "com.sun.xml.internal.stream.XMLInputFactoryImpl",
            "com.sun.org.apache.xerces.internal.parsers.SAXParser",
            XmlConverter.class.getCanonicalName()));

        // javax.xml.namespace.QName is needed as it is used as part of the processor
        // definitions in the DSL and parsers like Jackson (used in camel-k YAML DSL)
        // fails if this class is cannot be instantiated reflectively.
        reflectiveClass.produce(
            new ReflectiveClassBuildItem(true, false, "javax.xml.namespace.QName")
        );
    }

}
