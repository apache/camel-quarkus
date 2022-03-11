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
package org.apache.camel.quarkus.component.fhir.it;

import java.net.MalformedURLException;
import java.net.URL;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.StrictErrorHandler;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.fhir.FhirComponent;
import org.apache.camel.component.fhir.FhirConfiguration;
import org.apache.camel.component.fhir.FhirJsonDataFormat;
import org.apache.camel.component.fhir.FhirXmlDataFormat;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public abstract class AbstractFhirRouteBuilder extends RouteBuilder {

    abstract String getFhirVersion();

    abstract FhirContext getFhirContext();

    abstract boolean isEnabled();

    @Override
    public void configure() throws Exception {
        if (isEnabled()) {
            Config config = ConfigProvider.getConfig();
            String fhirVersion = getFhirVersion();
            String serverUrl = config.getValue("camel.fhir." + fhirVersion + ".test-url", String.class);

            FhirContext fhirContext = getFhirContext();

            FhirConfiguration configuration = new FhirConfiguration();
            configuration.setLog(false);
            configuration.setFhirContext(fhirContext);
            configuration.setServerUrl(serverUrl);
            configuration.setCompress(true);

            FhirComponent component = new FhirComponent();
            component.setConfiguration(configuration);
            getContext().addComponent("fhir-" + fhirVersion, component);

            fhirContext.setParserErrorHandler(new StrictErrorHandler());
            try {
                URL url = new URL(serverUrl);
                fhirContext.getRestfulClientFactory().setProxy(url.getHost(), url.getPort());
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            FhirJsonDataFormat fhirJsonDataFormat = new FhirJsonDataFormat();
            fhirJsonDataFormat.setFhirContext(fhirContext);
            fhirJsonDataFormat.setParserErrorHandler(new StrictErrorHandler());

            FhirXmlDataFormat fhirXmlDataFormat = new FhirXmlDataFormat();
            fhirXmlDataFormat.setFhirContext(fhirContext);
            fhirXmlDataFormat.setParserErrorHandler(new StrictErrorHandler());

            // Capabilities
            fromF("direct:capabilities-%s", fhirVersion)
                    .toF("fhir-%s://capabilities/ofType?inBody=type", fhirVersion);

            // Create
            fromF("direct:createResource-%s", fhirVersion)
                    .toF("fhir-%s://create/resource?inBody=resource", fhirVersion);

            fromF("direct:createResourceAsString-%s", fhirVersion)
                    .toF("fhir-%s://create/resource?inBody=resourceAsString", fhirVersion);

            // Dataformats
            fromF("direct:json-to-%s", fhirVersion)
                    .unmarshal(fhirJsonDataFormat)
                    .marshal(fhirJsonDataFormat);

            fromF("direct:xml-to-%s", fhirVersion)
                    .unmarshal(fhirXmlDataFormat)
                    .marshal(fhirXmlDataFormat);

            // Delete
            fromF("direct:delete-%s", fhirVersion)
                    .toF("fhir-%s://delete/resource?inBody=resource", fhirVersion);

            fromF("direct:deleteById-%s", fhirVersion)
                    .toF("fhir-%s://delete/resourceById?inBody=id", fhirVersion);

            fromF("direct:deleteByStringId-%s", fhirVersion)
                    .toF("fhir-%s://delete/resourceById", fhirVersion);

            fromF("direct:deleteConditionalByUrl-%s", fhirVersion)
                    .toF("fhir-%s://delete/resourceConditionalByUrl?inBody=url", fhirVersion);

            // History
            fromF("direct:historyOnInstance-%s", fhirVersion)
                    .toF("fhir-%s://history/onInstance", fhirVersion);

            fromF("direct:historyOnServer-%s", fhirVersion)
                    .toF("fhir-%s://history/onServer", fhirVersion);

            fromF("direct:historyOnType-%s", fhirVersion)
                    .toF("fhir-%s://history/onType", fhirVersion);

            // Load page
            fromF("direct:loadPageByUrl-%s", fhirVersion)
                    .toF("fhir-%s://load-page/byUrl", fhirVersion);

            fromF("direct:loadPageNext-%s", fhirVersion)
                    .toF("fhir-%s://load-page/next?inBody=bundle", fhirVersion);

            fromF("direct:loadPagePrevious-%s", fhirVersion)
                    .toF("fhir-%s://load-page/previous?inBody=bundle", fhirVersion);

            // Meta
            fromF("direct:metaAdd-%s", fhirVersion)
                    .toF("fhir-%s://meta/add", fhirVersion);

            fromF("direct:metaDelete-%s", fhirVersion)
                    .toF("fhir-%s://meta/delete", fhirVersion);

            fromF("direct:metaGetFromResource-%s", fhirVersion)
                    .toF("fhir-%s://meta/getFromResource", fhirVersion);

            fromF("direct:metaGetFromServer-%s", fhirVersion)
                    .toF("fhir-%s://meta/getFromServer?inBody=metaType", fhirVersion);

            fromF("direct:metaGetFromType-%s", fhirVersion)
                    .toF("fhir-%s://meta/getFromType", fhirVersion);

            // Operation
            fromF("direct:operationOnInstance-%s", fhirVersion)
                    .toF("fhir-%s://operation/onInstance", fhirVersion);

            fromF("direct:operationOnInstanceVersion-%s", fhirVersion)
                    .toF("fhir-%s://operation/onInstanceVersion", fhirVersion);

            fromF("direct:operationOnServer-%s", fhirVersion)
                    .toF("fhir-%s://operation/onServer", fhirVersion);

            fromF("direct:operationOnType-%s", fhirVersion)
                    .toF("fhir-%s://operation/onType", fhirVersion);

            fromF("direct:operationProcessMessage-%s", fhirVersion)
                    .toF("fhir-%s://operation/processMessage", fhirVersion);

            // Patch
            fromF("direct:patchById-%s", fhirVersion)
                    .toF("fhir-%s://patch/patchById", fhirVersion);

            fromF("direct:patchBySid-%s", fhirVersion)
                    .toF("fhir-%s://patch/patchById", fhirVersion);

            fromF("direct:patchByUrl-%s", fhirVersion)
                    .toF("fhir-%s://patch/patchByUrl", fhirVersion);

            // Read
            fromF("direct:readById-%s", fhirVersion)
                    .toF("fhir-%s://read/resourceById", fhirVersion);

            fromF("direct:readByLongId-%s", fhirVersion)
                    .toF("fhir-%s://read/resourceById", fhirVersion);

            fromF("direct:readByStringId-%s", fhirVersion)
                    .toF("fhir-%s://read/resourceById", fhirVersion);

            fromF("direct:readByIdAndStringResource-%s", fhirVersion)
                    .toF("fhir-%s://read/resourceById", fhirVersion);

            fromF("direct:readByLongIdAndStringResource-%s", fhirVersion)
                    .toF("fhir-%s://read/resourceById", fhirVersion);

            fromF("direct:readByStringIdAndStringResource-%s", fhirVersion)
                    .toF("fhir-%s://read/resourceById", fhirVersion);

            fromF("direct:readByStringIdAndVersion-%s", fhirVersion)
                    .toF("fhir-%s://read/resourceById", fhirVersion);

            fromF("direct:readByStringIdAndVersionAndStringResource-%s", fhirVersion)
                    .toF("fhir-%s://read/resourceById", fhirVersion);

            fromF("direct:readByIUrl-%s", fhirVersion)
                    .toF("fhir-%s://read/resourceByUrl", fhirVersion);

            fromF("direct:readByUrl-%s", fhirVersion)
                    .toF("fhir-%s://read/resourceByUrl", fhirVersion);

            fromF("direct:readByStringUrlAndStringResource-%s", fhirVersion)
                    .toF("fhir-%s://read/resourceByUrl", fhirVersion);

            fromF("direct:readByUrlAndStringResource-%s", fhirVersion)
                    .toF("fhir-%s://read/resourceByUrl", fhirVersion);

            // Search
            fromF("direct:searchByUrl-%s", fhirVersion)
                    .toF("fhir-%s://search/searchByUrl?inBody=url", fhirVersion);

            // Transaction
            fromF("direct:transactionWithBundle-%s", fhirVersion)
                    .toF("fhir-%s://transaction/withBundle?inBody=bundle", fhirVersion);

            fromF("direct:transactionWithStringBundle-%s", fhirVersion)
                    .toF("fhir-%s://transaction/withBundle?inBody=stringBundle", fhirVersion);

            fromF("direct:transactionWithResources-%s", fhirVersion)
                    .toF("fhir-%s://transaction/withResources?inBody=resources", fhirVersion);

            // Update
            fromF("direct:updateResource-%s", fhirVersion)
                    .toF("fhir-%s://update/resource", fhirVersion);

            fromF("direct:updateResourceWithStringId-%s", fhirVersion)
                    .toF("fhir-%s://update/resource", fhirVersion);

            fromF("direct:updateResourceAsString-%s", fhirVersion)
                    .toF("fhir-%s://update/resource", fhirVersion);

            fromF("direct:updateResourceAsStringWithStringId-%s", fhirVersion)
                    .toF("fhir-%s://update/resource", fhirVersion);

            fromF("direct:updateResourceBySearchUrl-%s", fhirVersion)
                    .toF("fhir-%s://update/resourceBySearchUrl", fhirVersion);

            fromF("direct:updateResourceBySearchUrlAndResourceAsString-%s", fhirVersion)
                    .toF("fhir-%s://update/resourceBySearchUrl", fhirVersion);

            // Validate
            fromF("direct:validateResource-%s", fhirVersion)
                    .toF("fhir-%s://validate/resource?inBody=resource", fhirVersion);

            fromF("direct:validateResourceAsString-%s", fhirVersion)
                    .toF("fhir-%s://validate/resource?inBody=resourceAsString", fhirVersion);
        }
    }
}
