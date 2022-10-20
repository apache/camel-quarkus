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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ProducerTemplate;

import static org.apache.camel.component.cxf.common.message.CxfConstants.OPERATION_NAME;

@Path("/cxf-soap/mtom")
@ApplicationScoped
public class CxfSoapMtomResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/upload")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response upload(@QueryParam("imageName") String imageName, @QueryParam("mtomEnabled") boolean mtomEnabled,
            byte[] image) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(image)) {
            final String response = producerTemplate.requestBodyAndHeader(
                    "direct:" + mtomEndpoint(mtomEnabled),
                    new Object[] { ImageIO.read(bais), imageName },
                    OPERATION_NAME, "uploadImage", String.class);
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .entity(response)
                    .build();
        }
    }

    @Path("/download")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response download(@QueryParam("imageName") String imageName, @QueryParam("mtomEnabled") boolean mtomEnabled)
            throws Exception {
        final BufferedImage response = (BufferedImage) producerTemplate.requestBodyAndHeader(
                "direct:" + mtomEndpoint(mtomEnabled),
                imageName,
                OPERATION_NAME,
                "downloadImage", Image.class);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(response, "png", baos);
            byte[] bytes = baos.toByteArray();
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .entity(bytes)
                    .build();
        }
    }

    private String mtomEndpoint(boolean mtomEnabled) {
        return mtomEnabled ? "mtomEnabledInvoker" : "mtomDisabledInvoker";
    }

}
