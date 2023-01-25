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
package org.apache.camel.quarkus.component.cxf.soap.mtom.awt.it;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.imageio.ImageIO;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;

import static org.apache.camel.component.cxf.common.message.CxfConstants.OPERATION_NAME;

@Path("/cxf-soap/mtom-awt")
@ApplicationScoped
public class CxfSoapMtomAwtResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/image/{imageName}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public Response upload(@PathParam("imageName") String imageName, @QueryParam("mtomEnabled") boolean mtomEnabled,
            byte[] image) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(image)) {
            final String response = producerTemplate.requestBodyAndHeader(
                    "direct:" + mtomEndpoint(mtomEnabled),
                    new ImageData(ImageIO.read(bais), imageName),
                    OPERATION_NAME, "uploadImage", String.class);
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .entity(response)
                    .build();
        }
    }

    @Path("/image/{imageName}")
    @GET
    public Response download(@PathParam("imageName") String imageName, @QueryParam("mtomEnabled") boolean mtomEnabled)
            throws Exception {
        final ImageData image = (ImageData) producerTemplate.requestBodyAndHeader(
                "direct:" + mtomEndpoint(mtomEnabled),
                imageName,
                OPERATION_NAME,
                "downloadImage", ImageData.class);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write((BufferedImage) image.getData(), "png", baos);
            byte[] bytes = baos.toByteArray();
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .entity(bytes)
                    .build();
        }
    }

    private String mtomEndpoint(boolean mtomEnabled) {
        return mtomEnabled ? "mtomAwtEnabledInvoker" : "mtomAwtDisabledInvoker";
    }

}
