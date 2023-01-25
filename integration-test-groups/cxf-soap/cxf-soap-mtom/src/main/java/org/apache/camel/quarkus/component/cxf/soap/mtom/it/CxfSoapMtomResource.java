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
package org.apache.camel.quarkus.component.cxf.soap.mtom.it;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.attachment.AttachmentMessage;

import static org.apache.camel.component.cxf.common.message.CxfConstants.OPERATION_NAME;
import static org.apache.camel.quarkus.component.cxf.soap.mtom.it.CxfSoapMtomRoutes.ROUTE_PAYLOAD_MODE_RESULT_HEADER_KEY_NAME;

@Path("/cxf-soap/mtom")
@ApplicationScoped
public class CxfSoapMtomResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/upload")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public Response upload(@QueryParam("imageName") String imageName, @QueryParam("mtomEnabled") boolean mtomEnabled,
            @QueryParam("endpointDataFormat") String endpointDataFormat, byte[] image) throws Exception {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(OPERATION_NAME, "uploadImage");
        headers.put("endpointDataFormat", endpointDataFormat);
        headers.put("mtomEnabled", mtomEnabled);
        Object body = new Object[] { new ImageFile(image), imageName };
        Exchange result = producerTemplate.request("direct:invoker", exchange -> {
            exchange.getIn().setBody(body);
            exchange.getIn().setHeaders(headers);
        });
        Object response = null;
        if ("PAYLOAD".equals(endpointDataFormat)) {
            response = result.getMessage().getHeader(ROUTE_PAYLOAD_MODE_RESULT_HEADER_KEY_NAME);
        } else {
            response = result.getMessage().getBody(String.class);
        }
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/download")
    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response download(@QueryParam("imageName") String imageName, @QueryParam("mtomEnabled") boolean mtomEnabled,
            @QueryParam("endpointDataFormat") String endpointDataFormat)
            throws Exception {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(OPERATION_NAME, "downloadImage");
        headers.put("endpointDataFormat", endpointDataFormat);
        headers.put("mtomEnabled", mtomEnabled);
        Exchange result = producerTemplate.request("direct:invoker", exchange -> {
            exchange.setPattern(ExchangePattern.InOut);
            exchange.getIn().setBody(imageName);
            exchange.getIn().setHeaders(headers);
        });
        byte[] response = null;
        if ("PAYLOAD".equals(endpointDataFormat)) {
            response = ((ByteArrayInputStream) result.getMessage(AttachmentMessage.class).getAttachment(imageName).getContent())
                    .readAllBytes();
        } else {
            response = result.getMessage().getBody(ImageFile.class).getContent();
        }
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

}
