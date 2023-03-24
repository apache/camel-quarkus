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
package org.apache.camel.quarkus.component.xpath.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.logging.Logger;

class XPathProcessor {

    private static final String FEATURE = "camel-xpath";
    private static final Logger LOG = Logger.getLogger(XPathProcessor.class);
    private static final String[] CORE_XPATH_FUNCTION_CLASSES = new String[] {
            "com.sun.org.apache.xpath.internal.functions.FuncBoolean",
            "com.sun.org.apache.xpath.internal.functions.FuncCeiling",
            "com.sun.org.apache.xpath.internal.functions.FuncConcat",
            "com.sun.org.apache.xpath.internal.functions.FuncContains",
            "com.sun.org.apache.xpath.internal.functions.FuncCount",
            "com.sun.org.apache.xpath.internal.functions.FuncCurrent",
            "com.sun.org.apache.xpath.internal.functions.FuncDoclocation",
            "com.sun.org.apache.xpath.internal.functions.FuncExtElementAvailable",
            "com.sun.org.apache.xpath.internal.functions.FuncExtFunction",
            "com.sun.org.apache.xpath.internal.functions.FuncExtFunctionAvailable",
            "com.sun.org.apache.xpath.internal.functions.FuncFalse",
            "com.sun.org.apache.xpath.internal.functions.FuncFloor",
            "com.sun.org.apache.xpath.internal.functions.FuncGenerateId",
            "com.sun.org.apache.xpath.internal.functions.FuncHere",
            "com.sun.org.apache.xpath.internal.functions.FuncId",
            "com.sun.org.apache.xpath.internal.functions.FuncLang",
            "com.sun.org.apache.xpath.internal.functions.FuncLast",
            "com.sun.org.apache.xpath.internal.functions.FuncLocalPart",
            "com.sun.org.apache.xpath.internal.functions.FuncNamespace",
            "com.sun.org.apache.xpath.internal.functions.FuncNormalizeSpace",
            "com.sun.org.apache.xpath.internal.functions.FuncNot",
            "com.sun.org.apache.xpath.internal.functions.FuncNumber",
            "com.sun.org.apache.xpath.internal.functions.FuncPosition",
            "com.sun.org.apache.xpath.internal.functions.FuncQname",
            "com.sun.org.apache.xpath.internal.functions.FuncRound",
            "com.sun.org.apache.xpath.internal.functions.FuncStartsWith",
            "com.sun.org.apache.xpath.internal.functions.FuncString",
            "com.sun.org.apache.xpath.internal.functions.FuncStringLength",
            "com.sun.org.apache.xpath.internal.functions.FuncSubstring",
            "com.sun.org.apache.xpath.internal.functions.FuncSubstringAfter",
            "com.sun.org.apache.xpath.internal.functions.FuncSubstringBefore",
            "com.sun.org.apache.xpath.internal.functions.FuncSum",
            "com.sun.org.apache.xpath.internal.functions.FuncSystemProperty",
            "com.sun.org.apache.xpath.internal.functions.FuncTranslate",
            "com.sun.org.apache.xpath.internal.functions.FuncTrue",
            "com.sun.org.apache.xpath.internal.functions.FuncUnparsedEntityURI"
    };

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    SystemPropertyBuildItem xpathSystemProperties() {
        // See https://issues.apache.org/jira/browse/XALANJ-2540
        return new SystemPropertyBuildItem("org.apache.xml.dtm.DTMManager", "org.apache.xml.dtm.ref.DTMManagerDefault");
    }

    @BuildStep
    void registerCoreXPathFunctionsAsReflective(BuildProducer<ReflectiveClassBuildItem> producer) {
        for (String coreXPathFunctionClass : CORE_XPATH_FUNCTION_CLASSES) {
            LOG.debugf("Registerering core XPath function class '%s' as reflective", coreXPathFunctionClass);
            producer.produce(ReflectiveClassBuildItem.builder(coreXPathFunctionClass).build());
        }
    }
}
