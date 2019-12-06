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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.StrictErrorHandler;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.fhir.FhirJsonDataFormat;
import org.apache.camel.component.fhir.FhirXmlDataFormat;
import org.apache.camel.quarkus.component.fhir.FhirFlags;

public class FhirR5RouteBuilder extends RouteBuilder {

    private static final Boolean ENABLED = new FhirFlags.R5Enabled().getAsBoolean();

    @Override
    public void configure() {
        if (ENABLED) {
            CamelContext context = getContext();
            FhirContext fhirContext = FhirContext.forR5();
            fhirContext.setParserErrorHandler(new StrictErrorHandler());
            context.getRegistry().bind("fhirContext", fhirContext);

            FhirJsonDataFormat fhirJsonDataFormat = new FhirJsonDataFormat();
            fhirJsonDataFormat.setFhirVersion(FhirVersionEnum.R5.name());
            fhirJsonDataFormat.setParserErrorHandler(new StrictErrorHandler());

            FhirXmlDataFormat fhirXmlDataFormat = new FhirXmlDataFormat();
            fhirXmlDataFormat.setFhirVersion(FhirVersionEnum.R5.name());
            fhirXmlDataFormat.setParserErrorHandler(new StrictErrorHandler());

            from("direct:json-to-r5")
                    .unmarshal(fhirJsonDataFormat)
                    .marshal(fhirJsonDataFormat);

            from("direct:xml-to-r5")
                    .unmarshal(fhirXmlDataFormat)
                    .marshal(fhirXmlDataFormat);

            from("direct:create-r5")
                    .to("fhir://create/resource?log={{fhir.log}}&serverUrl={{camel.fhir.test-url}}&&inBody=resourceAsString&fhirVersion=R5&fhirContext=#fhirContext");
        }
    }
}
