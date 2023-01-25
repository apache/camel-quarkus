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
package org.apache.camel.quarkus.component.pdf.it;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.pdf.PdfHeaderConstants;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.jboss.logging.Logger;

@Path("/pdf")
@ApplicationScoped
public class PdfResource {

    private static final Logger LOG = Logger.getLogger(PdfResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    private byte[] document;

    @Path("/createFromText")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response createFromText(String message) throws Exception {
        document = producerTemplate.requestBody(
                "pdf:create?fontSize=6&pageSize=PAGE_SIZE_A5&font=Courier", message, byte[].class);

        LOG.infof("The PDDocument has been created and contains %d bytes", document.length);

        return Response.created(new URI("pdf/extractText")).entity(document).build();
    }

    @Path("/appendText")
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response appendText(String message) throws Exception {
        document = producerTemplate.requestBodyAndHeader("pdf:append", message,
                PdfHeaderConstants.PDF_DOCUMENT_HEADER_NAME, PDDocument.load(document), byte[].class);

        LOG.infof("The PDDocument has been updated and now contains %d bytes", document.length);

        return Response.ok().entity(document).build();
    }

    @Path("/extractText")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() throws Exception {
        LOG.info("Extracting text from the PDDocument");
        return producerTemplate.requestBody("pdf:extractText", PDDocument.load(document), String.class);
    }

    @Path("/encrypt/standard")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response encryptStandard(
            @QueryParam("ownerPassword") String ownerPassword,
            @QueryParam("userPassword") String userPassword,
            String message) throws Exception {

        AccessPermission permission = AccessPermission.getOwnerAccessPermission();
        StandardProtectionPolicy policy = new StandardProtectionPolicy(ownerPassword, userPassword, permission);

        byte[] document = producerTemplate.requestBodyAndHeader(
                "pdf:create?fontSize=6&pageSize=PAGE_SIZE_A5&font=Courier",
                message,
                PdfHeaderConstants.PROTECTION_POLICY_HEADER_NAME,
                policy,
                byte[].class);

        return Response.created(new URI("pdf/extractText")).entity(document).build();
    }

    @Path("/decrypt/standard")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response decryptStandard(@QueryParam("password") String password, byte[] rawDocument) throws IOException {
        StandardDecryptionMaterial material = new StandardDecryptionMaterial(password);

        PDDocument document = PDDocument.load(new ByteArrayInputStream(rawDocument), password);

        String result = producerTemplate.requestBodyAndHeader(
                "pdf:extractText",
                document,
                PdfHeaderConstants.DECRYPTION_MATERIAL_HEADER_NAME,
                material,
                String.class);

        return Response.ok().entity(result).build();
    }
}
