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

import ca.uhn.fhir.context.FhirVersionEnum;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.fhir.FhirJsonDataFormat;
import org.apache.camel.component.fhir.FhirXmlDataFormat;
import org.apache.camel.quarkus.component.fhir.FhirFlags;

public class FhirDstu2RouteBuilder extends RouteBuilder {

    @Override
    public void configure() {
        Boolean dstu2 = new FhirFlags.Dstu2Enabled().getAsBoolean();;
        if (dstu2) {
            FhirJsonDataFormat fhirJsonDataFormat = new FhirJsonDataFormat();
            fhirJsonDataFormat.setFhirVersion(FhirVersionEnum.DSTU2.name());

            FhirXmlDataFormat fhirXmlDataFormat = new FhirXmlDataFormat();
            fhirXmlDataFormat.setFhirVersion(FhirVersionEnum.DSTU2.name());

            from("direct:json-to-dstu2")
                    .unmarshal(fhirJsonDataFormat)
                    .marshal(fhirJsonDataFormat);

            from("direct:xml-to-dstu2")
                    .unmarshal(fhirXmlDataFormat)
                    .marshal(fhirXmlDataFormat);

            from("direct:create-dstu2")
                    .to("fhir://create/resource?inBody=resourceAsString&log={{fhir.verbose}}&serverUrl={{fhir.dstu2.url}}&fhirVersion=DSTU2");
        }
    }
}
