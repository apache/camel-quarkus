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
package org.apache.camel.quarkus.component.sftp.it;

import java.io.InputStream;
import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.file.remote.SftpConfiguration;
import org.apache.camel.component.file.remote.SftpEndpoint;

@Path("/sftp")
@ApplicationScoped
public class SftpResource {

    private static final long TIMEOUT_MS = 1000;

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/get/{fileName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getFile(@PathParam("fileName") String fileName) {
        return consumerTemplate.receiveBody(
                "sftp://admin@localhost:{{camel.sftp.test-port}}/sftp?password=admin&localWorkDirectory=target&fileName="
                        + fileName,
                TIMEOUT_MS,
                String.class);
    }

    @Path("/create/{fileName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createFile(@PathParam("fileName") String fileName, String fileContent)
            throws Exception {
        producerTemplate.sendBodyAndHeader("sftp://admin@localhost:{{camel.sftp.test-port}}/sftp?password=admin", fileContent,
                Exchange.FILE_NAME, fileName);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/delete/{fileName}")
    @DELETE
    public void deleteFile(@PathParam("fileName") String fileName) {
        consumerTemplate.receiveBody(
                "sftp://admin@localhost:{{camel.sftp.test-port}}/sftp?password=admin&delete=true&fileName=" + fileName,
                TIMEOUT_MS,
                String.class);
    }

    @Path("/moveToDoneFile/{fileName}")
    @PUT
    public void moveToDoneFile(@PathParam("fileName") String fileName) {
        consumerTemplate.receiveBody(
                "sftp://admin@localhost:{{camel.sftp.test-port}}/sftp?password=admin&move=${headers.CamelFileName}.done&fileName="
                        + fileName,
                TIMEOUT_MS,
                String.class);
    }

    @Path("/certificate/create/{fileName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createFileWithCertificate(@PathParam("fileName") String fileName, String fileContent)
            throws Exception {
        producerTemplate.sendBodyAndHeader(
                "sftp://admin@localhost:{{camel.sftp.test-port}}/sftp?privateKeyUri=certs/test-key-rsa.key&certUri=certs/test-key-rsa-cert.pub",
                fileContent,
                Exchange.FILE_NAME, fileName);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/certificate/get/{fileName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getFileWithCertificate(@PathParam("fileName") String fileName) {
        return consumerTemplate.receiveBody(
                "sftp://admin@localhost:{{camel.sftp.test-port}}/sftp?privateKeyUri=certs/test-key-rsa.key&certUri=certs/test-key-rsa-cert.pub&localWorkDirectory=target&fileName="
                        + fileName,
                TIMEOUT_MS,
                String.class);
    }

    @Path("/certificateFile/create/{fileName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createFileWithCertificateFile(@PathParam("fileName") String fileName, String fileContent)
            throws Exception {
        producerTemplate.sendBodyAndHeader(
                "sftp://admin@localhost:{{camel.sftp.test-port}}/sftp?privateKeyFile=target/classes/certs/test-key-rsa.key&certFile=target/classes/certs/test-key-rsa-cert.pub",
                fileContent,
                Exchange.FILE_NAME, fileName);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/certificateFile/get/{fileName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getFileWithCertificateFile(@PathParam("fileName") String fileName) {
        return consumerTemplate.receiveBody(
                "sftp://admin@localhost:{{camel.sftp.test-port}}/sftp?privateKeyFile=target/classes/certs/test-key-rsa.key&certFile=target/classes/certs/test-key-rsa-cert.pub&localWorkDirectory=target&fileName="
                        + fileName,
                TIMEOUT_MS,
                String.class);
    }

    @Path("/certificateBytes/create/{fileName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createFileWithCertificateBytes(@PathParam("fileName") String fileName, String fileContent)
            throws Exception {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream certStream = classLoader.getResourceAsStream("certs/test-key-rsa-cert.pub");
                InputStream keyStream = classLoader.getResourceAsStream("certs/test-key-rsa.key")) {
            if (certStream == null) {
                throw new RuntimeException("Failed reading cert file");
            }

            if (keyStream == null) {
                throw new RuntimeException("Failed reading key file");
            }

            String uri = "sftp://admin@localhost:{{camel.sftp.test-port}}/sftp";
            SftpEndpoint endpoint = context.getEndpoint(uri, SftpEndpoint.class);
            SftpConfiguration config = endpoint.getConfiguration();
            config.setCertBytes(certStream.readAllBytes());
            config.setPrivateKey(keyStream.readAllBytes());

            producerTemplate.sendBodyAndHeader(endpoint, fileContent, Exchange.FILE_NAME, fileName);
            return Response
                    .created(new URI("https://camel.apache.org/"))
                    .build();
        }
    }

    @Path("/certificateBytes/get/{fileName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getFileWithCertificateBytes(@PathParam("fileName") String fileName) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream certStream = classLoader.getResourceAsStream("certs/test-key-rsa-cert.pub");
                InputStream keyStream = classLoader.getResourceAsStream("certs/test-key-rsa.key")) {
            if (certStream == null) {
                throw new RuntimeException("Failed reading cert file");
            }

            if (keyStream == null) {
                throw new RuntimeException("Failed reading key file");
            }

            String uri = "sftp://admin@localhost:{{camel.sftp.test-port}}/sftp?localWorkDirectory=target&fileName="
                    + fileName;
            SftpEndpoint endpoint = context.getEndpoint(uri, SftpEndpoint.class);
            SftpConfiguration config = endpoint.getConfiguration();
            config.setCertBytes(certStream.readAllBytes());
            config.setPrivateKey(keyStream.readAllBytes());

            return consumerTemplate.receiveBody(endpoint, TIMEOUT_MS, String.class);
        }
    }

    @Path("/certificateWithCaAlgorithms/create/{fileName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createFileWithCertificateAndCaAlgorithms(@PathParam("fileName") String fileName, String fileContent)
            throws Exception {
        producerTemplate.sendBodyAndHeader(
                "sftp://admin@localhost:{{camel.sftp.test-port}}/sftp?privateKeyUri=certs/test-key-rsa.key&certUri=certs/test-key-rsa-cert.pub&caSignatureAlgorithms=rsa-sha2-512,rsa-sha2-256,ssh-rsa",
                fileContent,
                Exchange.FILE_NAME, fileName);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/certificateWithCaAlgorithms/get/{fileName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getFileWithCertificateAndCaAlgorithms(@PathParam("fileName") String fileName) {
        return consumerTemplate.receiveBody(
                "sftp://admin@localhost:{{camel.sftp.test-port}}/sftp?privateKeyUri=certs/test-key-rsa.key&certUri=certs/test-key-rsa-cert.pub&caSignatureAlgorithms=rsa-sha2-512,rsa-sha2-256,ssh-rsa&localWorkDirectory=target&fileName="
                        + fileName,
                TIMEOUT_MS,
                String.class);
    }
}
