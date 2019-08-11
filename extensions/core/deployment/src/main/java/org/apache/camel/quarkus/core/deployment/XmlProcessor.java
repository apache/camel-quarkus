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
package org.apache.camel.quarkus.core.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.jaxb.deployment.JaxbEnabledBuildItem;
import io.quarkus.jaxb.deployment.JaxbFileRootBuildItem;
import org.apache.camel.quarkus.core.runtime.support.Flags;

class XmlProcessor {
    @BuildStep
    JaxbFileRootBuildItem fileRoot() {
        return new JaxbFileRootBuildItem(CamelSupport.CAMEL_ROOT_PACKAGE_DIRECTORY);
    }

    @BuildStep(onlyIfNot = Flags.JaxbDisabled.class)
    JaxbEnabledBuildItem handleJaxbSupport() {
        return new JaxbEnabledBuildItem();
    }

    @BuildStep(onlyIfNot = Flags.JaxbDisabled.class)
    void registerReflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(
            new ReflectiveClassBuildItem(
                false,
                false,
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
                "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
                "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl")
        );
    }
}
