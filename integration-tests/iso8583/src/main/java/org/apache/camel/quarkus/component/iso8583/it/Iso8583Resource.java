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
package org.apache.camel.quarkus.component.iso8583.it;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Map;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoValue;
import com.solab.iso8583.MessageFactory;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;

@Path("/iso8583")
public class Iso8583Resource {
    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/marshal")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String marshal(File iso8583Message) throws IOException, ParseException {
        MessageFactory<?> messageFactory = new MessageFactory<>();
        messageFactory.setConfigPath("j8583-config.xml");

        byte[] bytes = context.getTypeConverter().convertTo(byte[].class, iso8583Message);
        IsoMessage message = messageFactory.parseMessage(bytes, "ISO015000055".getBytes().length);

        return producerTemplate.requestBody("direct:marshal", message, String.class);
    }

    @Path("/unmarshal")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unmarshal(File iso8583Message) throws Exception {
        IsoMessage result = producerTemplate.requestBody("direct:unmarshal", iso8583Message, IsoMessage.class);
        if (result != null) {
            IsoValue<BigDecimal> amount = result.getAt(4);
            return Response.ok(Map.of(
                    "type", amount.getType().name(),
                    "value", amount.getValue().toString()))
                    .build();
        }
        return Response.noContent().build();
    }
}
