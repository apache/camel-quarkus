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
package org.apache.camel.quarkus.component.splunk.hec.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.splunkhec.SplunkHECConstants;
import org.apache.camel.quarkus.test.support.splunk.SplunkConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/splunk-hec")
@ApplicationScoped
public class SplunkHecResource {

    @Inject
    ProducerTemplate producer;

    @ConfigProperty(name = SplunkConstants.PARAM_HEC_PORT)
    Integer hecPort;

    @ConfigProperty(name = SplunkConstants.PARAM_REMOTE_HOST)
    String host;

    @ConfigProperty(name = SplunkConstants.PARAM_TEST_INDEX)
    String index;

    @ConfigProperty(name = SplunkConstants.PARAM_HEC_TOKEN)
    String token;

    @Path("/send")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String send(String data, @QueryParam("indexTime") Long indexTime) {
        String url = String.format("splunk-hec:%s:%s?token=%s&skipTlsVerify=true&index=%s", host, hecPort, token, index);
        return producer.requestBodyAndHeader(url, data, SplunkHECConstants.INDEX_TIME, indexTime, String.class);
    }
}
