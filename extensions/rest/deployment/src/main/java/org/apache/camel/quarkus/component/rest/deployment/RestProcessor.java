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
package org.apache.camel.quarkus.component.rest.deployment;

import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.apache.camel.component.rest.RestComponent;
import org.apache.camel.quarkus.component.rest.RestRecorder;
import org.apache.camel.quarkus.component.rest.graal.NoJAXBContext;
import org.apache.camel.quarkus.core.deployment.CamelServiceFilter;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelServiceFilterBuildItem;
import org.apache.camel.quarkus.support.common.CamelCapabilities;

class RestProcessor {
    private static final String FEATURE = "camel-rest";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    //
    // RestAssured brings XML bind APIs to the classpath:
    //
    //     [INFO] +- io.rest-assured:rest-assured:jar:4.3.0:test
    //      [INFO] |  +- org.codehaus.groovy:groovy:jar:3.0.2:test
    //      [INFO] |  +- org.codehaus.groovy:groovy-xml:jar:3.0.2:test
    //      [INFO] |  +- org.apache.httpcomponents:httpclient:jar:4.5.11:test
    //      [INFO] |  |  +- org.apache.httpcomponents:httpcore:jar:4.4.13:test
    //      [INFO] |  |  \- commons-codec:commons-codec:jar:1.13:test
    //      [INFO] |  +- org.apache.httpcomponents:httpmime:jar:4.5.3:test
    //      [INFO] |  +- org.hamcrest:hamcrest:jar:2.1:test
    //      [INFO] |  +- org.ccil.cowan.tagsoup:tagsoup:jar:1.2.1:test
    //      [INFO] |  +- io.rest-assured:json-path:jar:4.3.0:test
    //      [INFO] |  |  +- org.codehaus.groovy:groovy-json:jar:3.0.2:test
    //      [INFO] |  |  \- io.rest-assured:rest-assured-common:jar:4.3.0:test
    //   >> [INFO] |  \- io.rest-assured:xml-path:jar:4.3.0:test
    //      [INFO] |     +- org.apache.commons:commons-lang3:jar:3.9:test
    //   >> [INFO] |     +- jakarta.xml.bind:jakarta.xml.bind-api:jar:2.3.2:test
    //      [INFO] |     |  \- jakarta.activation:jakarta.activation-api:jar:1.2.1:test
    //      [INFO] |     \- org.apache.sling:org.apache.sling.javax.activation:jar:0.1.0:test
    //
    // For tests in JVM mode the condition NoJAXBContext is always false as a consequence of
    // RestAssured transitive dependencies so we need an additional check on the presence of
    // the org.apache.camel.xml.jaxb feature to make the behaviour consistent ion both modes.
    //
    // Excluding io.rest-assured:xml-path from the transitive dependencies does not seem to work
    // as it lead to the RestAssured framework to fail to instantiate.
    //

    @BuildStep(onlyIf = NoJAXBContext.class)
    void serviceFilter(
            Capabilities capabilities,
            BuildProducer<CamelServiceFilterBuildItem> serviceFilter) {

        // if jaxb is configured, don't replace the method
        if (capabilities.isCapabilityPresent(CamelCapabilities.XML_JAXB)) {
            return;
        }

        serviceFilter.produce(new CamelServiceFilterBuildItem(CamelServiceFilter.forComponent("rest")));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(onlyIf = NoJAXBContext.class)
    void restComponent(
            RestRecorder recorder,
            Capabilities capabilities,
            BuildProducer<CamelBeanBuildItem> camelBeans) {

        // if jaxb is configured, don't replace the method
        if (capabilities.isCapabilityPresent(CamelCapabilities.XML_JAXB)) {
            return;
        }

        camelBeans.produce(
                new CamelBeanBuildItem(
                        "rest",
                        RestComponent.class.getName(),
                        recorder.createRestComponent()));
    }
}
