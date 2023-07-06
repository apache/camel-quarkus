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
package org.apache.camel.quarkus.component.swift.mx.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.prowidesoftware.swift.model.mx.MxCamt04800103;
import com.prowidesoftware.swift.model.mx.MxPacs00800107;
import com.prowidesoftware.swift.model.mx.sys.MxXsys01100102;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.io.IOUtils;

@Path("/swift-mx")
@ApplicationScoped
public class SwiftMxResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/unmarshal")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String unmarshal(byte[] message) {
        final MxCamt04800103 response = producerTemplate.requestBody("direct:mx.unmarshal", message, MxCamt04800103.class);
        return response.getMxId().id();
    }

    @Path("/unmarshaldsl")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String unmarshalDsl(byte[] message) {
        final MxCamt04800103 response = producerTemplate.requestBody("direct:mx.unmarshaldsl", message, MxCamt04800103.class);
        return response.getMxId().id();
    }

    @Path("/unmarshalFull")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String unmarshalFull(byte[] message) {
        final MxXsys01100102 response = producerTemplate.requestBody("direct:mx.unmarshalFull", message, MxXsys01100102.class);
        return response.getMxId().id();
    }

    @Path("/marshal")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String marshal(byte[] body) throws IOException {
        MxPacs00800107 message = MxPacs00800107.parse(new String(body));
        final Object response = producerTemplate.requestBody("direct:mx.marshal", message);
        MxPacs00800107 actual = MxPacs00800107.parse(IOUtils.toString((InputStream) response, StandardCharsets.UTF_8));
        return actual.getMxId().id();
    }

    @Path("/marshalFull")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean marshalFull(byte[] body) throws IOException {
        MxPacs00800107 message = MxPacs00800107.parse(new String(body));
        final Object response = producerTemplate.requestBody("direct:mx.marshalFull", message);
        BufferedReader reader = new BufferedReader(new InputStreamReader((InputStream) response, StandardCharsets.UTF_8));
        String line = reader.readLine();
        return line.contains("<?xml");
    }

    @Path("/marshalJson")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Object marshalJson(byte[] body) throws IOException {
        MxPacs00800107 message = MxPacs00800107.parse(new String(body));
        return producerTemplate.requestBody("direct:mx.marshalJson", message);
    }
}
