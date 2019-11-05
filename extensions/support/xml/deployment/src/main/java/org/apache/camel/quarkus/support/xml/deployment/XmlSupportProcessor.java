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
package org.apache.camel.quarkus.support.xml.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class XmlSupportProcessor {
    @BuildStep
    void reflective(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(
                        false,
                        false,
                        "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
                        "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
                        "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl",
                        "com.sun.xml.internal.stream.XMLInputFactoryImpl",
                        "com.sun.org.apache.xerces.internal.parsers.SAXParser"));

        reflectiveClass.produce(
                new ReflectiveClassBuildItem(
                        false,
                        false,
                        "org.apache.camel.converter.jaxp.XmlConverter"));

        // javax.xml.namespace.QName is needed as it is used as part of the processor
        // definitions in the DSL and parsers like Jackson (used in camel-k YAML DSL)
        // fails if this class is cannot be instantiated reflectively.
        reflectiveClass.produce(
                new ReflectiveClassBuildItem(true, false, "javax.xml.namespace.QName"));
    }
}
