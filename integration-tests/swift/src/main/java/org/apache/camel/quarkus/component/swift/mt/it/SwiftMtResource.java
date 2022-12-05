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
package org.apache.camel.quarkus.component.swift.mt.it;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.prowidesoftware.swift.model.mt.mt1xx.MT103;
import com.prowidesoftware.swift.model.mt.mt5xx.MT515;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;

@Path("/swift-mt")
@ApplicationScoped
public class SwiftMtResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/unmarshal")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String unmarshal(byte[] message) {
        final MT515 response = producerTemplate.requestBody("direct:mt.unmarshal", message, MT515.class);
        return response.getMessageType();
    }

    @Path("/unmarshaldsl")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String unmarshalDsl(byte[] message) {
        final MT515 response = producerTemplate.requestBody("direct:mt.unmarshaldsl", message, MT515.class);
        return response.getMessageType();
    }

    @Path("/marshal")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String marshal(byte[] body) throws IOException {
        MT103 message = MT103.parse(new ByteArrayInputStream(body));
        final Object response = producerTemplate.requestBody("direct:mt.marshal", message);
        MT103 actual = MT103.parse((InputStream) response);
        return actual.getMessageType();
    }

    @Path("/marshalJson")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Object marshalJson(byte[] body) throws IOException {
        MT103 message = MT103.parse(new ByteArrayInputStream(body));
        return producerTemplate.requestBody("direct:mt.marshalJson", message);
    }
}
