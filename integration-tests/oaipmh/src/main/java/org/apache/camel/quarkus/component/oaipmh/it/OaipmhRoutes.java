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
package org.apache.camel.quarkus.component.oaipmh.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.support.builder.Namespaces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class OaipmhRoutes extends RouteBuilder {

    @ConfigProperty(name = "camel.oaipmh.its.http.server.authority")
    String httpServerAuthority;
    @ConfigProperty(name = "camel.oaipmh.its.https.server.authority")
    String httpsServerAuthority;

    public void configure() {

        final Namespaces ns = new Namespaces();
        ns.add("default", "http://www.openarchives.org/OAI/2.0/");
        ns.add("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        ns.add("dc", "http://purl.org/dc/elements/1.1/");

        final String listRecordsXpath = "/default:OAI-PMH/default:ListRecords/default:record/default:metadata/oai_dc:dc/dc:title/text()";
        final String getRecordXpath = "/default:OAI-PMH/default:GetRecord/default:record/default:metadata/oai_dc:dc/dc:title/text()";

        fromF("oaipmh://%s/oai/request?delay=1000&from=2020-06-01T00:00:00Z&initialDelay=1000", httpServerAuthority)
                .split(xpath(listRecordsXpath, ns))
                .to("mock:consumerListRecords");

        fromF("oaipmh://%s/index.php?page=oai&delay=1000&from=2020-02-01T00:00:00Z&initialDelay=1000", httpServerAuthority)
                .split(xpath(listRecordsXpath, ns))
                .to("mock:consumerListRecordsParticularCase");

        final String uriFormat = "oaipmh://%s/oai/request?ssl=true&ignoreSSLWarnings=true&delay=1000&verb=Identify&initialDelay=1000";
        fromF(uriFormat, httpsServerAuthority)
                .to("mock:consumerIdentifyHttps");

        from("direct:producerListRecords")
                .setHeader("CamelOaimphFrom", constant("2020-06-01T00:00:00Z"))
                .toF("oaipmh://%s/oai/request", httpServerAuthority)
                .split(body())
                .split(xpath(listRecordsXpath, ns))
                .to("mock:producerListRecords");

        from("direct:producerGetRecord")
                .setHeader("CamelOaimphVerb", constant("GetRecord"))
                .toF("oaipmh://%s/oai/request", httpServerAuthority)
                .split(body())
                .split(xpath(getRecordXpath, ns))
                .to("mock:producerGetRecord");
    }
}
