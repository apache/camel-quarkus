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
package org.apache.camel.quarkus.component.xml.jaxb.deployment;

import java.util.Arrays;
import java.util.List;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.jaxb.deployment.JaxbFileRootBuildItem;
import org.apache.camel.quarkus.component.xml.jaxb.XmlJaxbRecorder;
import org.apache.camel.quarkus.core.deployment.CamelModelJAXBContextFactoryBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelModelToXMLDumperBuildItem;
import org.apache.camel.quarkus.core.deployment.CamelRoutesLoaderBuildItems;
import org.apache.camel.quarkus.core.deployment.CamelSupport;
import org.apache.camel.quarkus.support.common.CamelCapabilities;

class XmlJaxbProcessor {

    private static final String FEATURE = "camel-xml-jaxb";

    @BuildStep
    JaxbFileRootBuildItem fileRoot() {
        return new JaxbFileRootBuildItem(CamelSupport.CAMEL_ROOT_PACKAGE_DIRECTORY);
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<CapabilityBuildItem> capabilities() {
        return Arrays.asList(
                new CapabilityBuildItem(CamelCapabilities.XML),
                new CapabilityBuildItem(CamelCapabilities.XML_JAXB));
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    CamelModelJAXBContextFactoryBuildItem contextFactory(XmlJaxbRecorder recorder) {
        return new CamelModelJAXBContextFactoryBuildItem(recorder.newContextFactory());
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    CamelRoutesLoaderBuildItems.Xml xmlLoader(XmlJaxbRecorder recorder) {
        return new CamelRoutesLoaderBuildItems.Xml(recorder.newJaxbXMLRoutesDefinitionLoader());
    }

    @BuildStep
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    CamelModelToXMLDumperBuildItem xmlModelDumper(XmlJaxbRecorder recorder) {
        return new CamelModelToXMLDumperBuildItem(recorder.newJaxbModelToXMLDumper());
    }
}
