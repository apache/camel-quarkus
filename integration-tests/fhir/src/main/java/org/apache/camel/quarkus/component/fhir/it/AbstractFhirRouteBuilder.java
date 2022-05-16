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
            fhirContext.getRestfulClientFactory().setSocketTimeout(60000);

            FhirConfiguration configuration = new FhirConfiguration();
            configuration.setLog(false);
            configuration.setFhirContext(fhirContext);
            configuration.setServerUrl(serverUrl);
            configuration.setCompress(true);

            String sanitizedFhirVersion = fhirVersion.replace('_', '-');
            FhirComponent component = new FhirComponent();
            component.setConfiguration(configuration);
            getContext().addComponent("fhir-" + sanitizedFhirVersion, component);

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
            fromF("direct:capabilities-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://capabilities/ofType?inBody=type", sanitizedFhirVersion);

            // Create
            fromF("direct:createResource-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://create/resource?inBody=resource", sanitizedFhirVersion);

            fromF("direct:createResourceAsString-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://create/resource?inBody=resourceAsString", sanitizedFhirVersion);

            // Dataformats
            fromF("direct:json-to-%s", sanitizedFhirVersion)
                    .unmarshal(fhirJsonDataFormat)
                    .marshal(fhirJsonDataFormat);

            fromF("direct:xml-to-%s", sanitizedFhirVersion)
                    .unmarshal(fhirXmlDataFormat)
                    .marshal(fhirXmlDataFormat);

            // Delete
            fromF("direct:delete-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://delete/resource?inBody=resource", sanitizedFhirVersion);

            fromF("direct:deleteById-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://delete/resourceById?inBody=id", sanitizedFhirVersion);

            fromF("direct:deleteByStringId-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://delete/resourceById", sanitizedFhirVersion);

            fromF("direct:deleteConditionalByUrl-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://delete/resourceConditionalByUrl?inBody=url", sanitizedFhirVersion);

            // History
            fromF("direct:historyOnInstance-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://history/onInstance", sanitizedFhirVersion);

            fromF("direct:historyOnServer-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://history/onServer", sanitizedFhirVersion);

            fromF("direct:historyOnType-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://history/onType", sanitizedFhirVersion);

            // Load page
            fromF("direct:loadPageByUrl-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://load-page/byUrl", sanitizedFhirVersion);

            fromF("direct:loadPageNext-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://load-page/next?inBody=bundle", sanitizedFhirVersion);

            fromF("direct:loadPagePrevious-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://load-page/previous?inBody=bundle", sanitizedFhirVersion);

            // Meta
            fromF("direct:metaAdd-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://meta/add", sanitizedFhirVersion);

            fromF("direct:metaDelete-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://meta/delete", sanitizedFhirVersion);

            fromF("direct:metaGetFromResource-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://meta/getFromResource", sanitizedFhirVersion);

            fromF("direct:metaGetFromServer-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://meta/getFromServer?inBody=metaType", sanitizedFhirVersion);

            fromF("direct:metaGetFromType-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://meta/getFromType", sanitizedFhirVersion);

            // Operation
            fromF("direct:operationOnInstance-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://operation/onInstance", sanitizedFhirVersion);

            fromF("direct:operationOnInstanceVersion-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://operation/onInstanceVersion", sanitizedFhirVersion);

            fromF("direct:operationOnServer-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://operation/onServer", sanitizedFhirVersion);

            fromF("direct:operationOnType-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://operation/onType", sanitizedFhirVersion);

            fromF("direct:operationProcessMessage-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://operation/processMessage", sanitizedFhirVersion);

            // Patch
            fromF("direct:patchById-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://patch/patchById", sanitizedFhirVersion);

            fromF("direct:patchBySid-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://patch/patchById", sanitizedFhirVersion);

            fromF("direct:patchByUrl-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://patch/patchByUrl", sanitizedFhirVersion);

            // Read
            fromF("direct:readById-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://read/resourceById", sanitizedFhirVersion);

            fromF("direct:readByLongId-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://read/resourceById", sanitizedFhirVersion);

            fromF("direct:readByStringId-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://read/resourceById", sanitizedFhirVersion);

            fromF("direct:readByIdAndStringResource-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://read/resourceById", sanitizedFhirVersion);

            fromF("direct:readByLongIdAndStringResource-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://read/resourceById", sanitizedFhirVersion);

            fromF("direct:readByStringIdAndStringResource-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://read/resourceById", sanitizedFhirVersion);

            fromF("direct:readByStringIdAndVersion-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://read/resourceById", sanitizedFhirVersion);

            fromF("direct:readByStringIdAndVersionAndStringResource-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://read/resourceById", sanitizedFhirVersion);

            fromF("direct:readByIUrl-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://read/resourceByUrl", sanitizedFhirVersion);

            fromF("direct:readByUrl-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://read/resourceByUrl", sanitizedFhirVersion);

            fromF("direct:readByStringUrlAndStringResource-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://read/resourceByUrl", sanitizedFhirVersion);

            fromF("direct:readByUrlAndStringResource-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://read/resourceByUrl", sanitizedFhirVersion);

            // Search
            fromF("direct:searchByUrl-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://search/searchByUrl?inBody=url", sanitizedFhirVersion);

            // Transaction
            fromF("direct:transactionWithBundle-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://transaction/withBundle?inBody=bundle", sanitizedFhirVersion);

            fromF("direct:transactionWithStringBundle-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://transaction/withBundle?inBody=stringBundle", sanitizedFhirVersion);

            fromF("direct:transactionWithResources-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://transaction/withResources?inBody=resources", sanitizedFhirVersion);

            // Update
            fromF("direct:updateResource-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://update/resource", sanitizedFhirVersion);

            fromF("direct:updateResourceWithStringId-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://update/resource", sanitizedFhirVersion);

            fromF("direct:updateResourceAsString-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://update/resource", sanitizedFhirVersion);

            fromF("direct:updateResourceAsStringWithStringId-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://update/resource", sanitizedFhirVersion);

            fromF("direct:updateResourceBySearchUrl-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://update/resourceBySearchUrl", sanitizedFhirVersion);

            fromF("direct:updateResourceBySearchUrlAndResourceAsString-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://update/resourceBySearchUrl", sanitizedFhirVersion);

            // Validate
            fromF("direct:validateResource-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://validate/resource?inBody=resource", sanitizedFhirVersion);

            fromF("direct:validateResourceAsString-%s", sanitizedFhirVersion)
                    .toF("fhir-%s://validate/resource?inBody=resourceAsString", sanitizedFhirVersion);
        }
    }
}
