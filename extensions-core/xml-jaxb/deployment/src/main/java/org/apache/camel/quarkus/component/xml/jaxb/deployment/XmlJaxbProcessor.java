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

import java.util.function.BooleanSupplier;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.jaxb.deployment.JaxbFileRootBuildItem;
import org.apache.camel.quarkus.component.xml.jaxb.XmlJaxbRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelModelJAXBContextFactoryBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelModelToXMLDumperBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;

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
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    CamelModelJAXBContextFactoryBuildItem contextFactory(XmlJaxbRecorder recorder) {
        return new CamelModelJAXBContextFactoryBuildItem(recorder.newContextFactory());
    }

    @BuildStep(onlyIfNot = XmlIoPresent.class)
    @Record(value = ExecutionTime.STATIC_INIT, optional = true)
    CamelModelToXMLDumperBuildItem xmlModelDumper(XmlJaxbRecorder recorder) {
        return new CamelModelToXMLDumperBuildItem(recorder.newJaxbModelToXMLDumper());
    }

    /**
     * Normally we'd use Capabilities to detect xml-io-dsl, but in this case we must suppress the xmlModelDumper
     * build step running. Since we can't have multiple active producers of CamelModelToXMLDumperBuildItem.
     */
    static class XmlIoPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Thread.currentThread().getContextClassLoader().loadClass("org.apache.camel.xml.LwModelToXMLDumper");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
