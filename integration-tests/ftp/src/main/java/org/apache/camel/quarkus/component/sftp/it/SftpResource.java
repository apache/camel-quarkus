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

    @Path("/hostcert/create/{fileName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createFileWithHostCertVerification(@PathParam("fileName") String fileName, String fileContent)
            throws Exception {
        String knownHostsFile = createHostCaKnownHostsFile();
        String port = context.resolvePropertyPlaceholders("{{camel.sftp.hostcert.test-port}}");
        String uri = "sftp://admin@localhost:" + port
                + "/sftp?password=admin&strictHostKeyChecking=yes&useUserKnownHostsFile=false&caSignatureAlgorithms=ssh-ed25519,rsa-sha2-512,rsa-sha2-256,ssh-rsa&knownHostsFile="
                + knownHostsFile;
        producerTemplate.sendBodyAndHeader(uri, fileContent, Exchange.FILE_NAME, fileName);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/hostcert/get/{fileName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getFileWithHostCertVerification(@PathParam("fileName") String fileName) throws Exception {
        String knownHostsFile = createHostCaKnownHostsFile();
        return consumerTemplate.receiveBody(
                "sftp://admin@localhost:{{camel.sftp.hostcert.test-port}}/sftp?password=admin&strictHostKeyChecking=yes&useUserKnownHostsFile=false&caSignatureAlgorithms=ssh-ed25519,rsa-sha2-512,rsa-sha2-256,ssh-rsa&knownHostsFile="
                        + knownHostsFile + "&localWorkDirectory=target&fileName=" + fileName,
                TIMEOUT_MS,
                String.class);
    }

    @Path("/hostcert/delete/{fileName}")
    @DELETE
    public Response deleteFileWithHostCert(@PathParam("fileName") String fileName) throws Exception {
        String knownHostsFile = createHostCaKnownHostsFile();
        consumerTemplate.receiveBody(
                "sftp://admin@localhost:{{camel.sftp.hostcert.test-port}}/sftp?password=admin&strictHostKeyChecking=yes&useUserKnownHostsFile=false&caSignatureAlgorithms=ssh-ed25519,rsa-sha2-512,rsa-sha2-256,ssh-rsa&knownHostsFile="
                        + knownHostsFile + "&delete=true&fileName=" + fileName,
                TIMEOUT_MS,
                String.class);
        return Response.noContent().build();
    }

    @Path("/hostcertWithAlgorithms/create/{fileName}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createFileWithHostCertAndAlgorithms(@PathParam("fileName") String fileName, String fileContent)
            throws Exception {
        String knownHostsFile = createHostCaKnownHostsFile();
        producerTemplate.sendBodyAndHeader(
                "sftp://admin@localhost:{{camel.sftp.hostcert.test-port}}/sftp?password=admin&strictHostKeyChecking=yes&useUserKnownHostsFile=false&knownHostsFile="
                        + knownHostsFile + "&caSignatureAlgorithms=ssh-ed25519,rsa-sha2-512,rsa-sha2-256",
                fileContent,
                Exchange.FILE_NAME, fileName);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .build();
    }

    @Path("/hostcertWithAlgorithms/get/{fileName}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getFileWithHostCertAndAlgorithms(@PathParam("fileName") String fileName) throws Exception {
        String knownHostsFile = createHostCaKnownHostsFile();
        return consumerTemplate.receiveBody(
                "sftp://admin@localhost:{{camel.sftp.hostcert.test-port}}/sftp?password=admin&strictHostKeyChecking=yes&useUserKnownHostsFile=false&knownHostsFile="
                        + knownHostsFile
                        + "&caSignatureAlgorithms=ssh-ed25519,rsa-sha2-512,rsa-sha2-256&localWorkDirectory=target&fileName="
                        + fileName,
                TIMEOUT_MS,
                String.class);
    }

    @Path("/hostcertWithAlgorithms/delete/{fileName}")
    @DELETE
    public Response deleteFileWithHostCertAndAlgorithms(@PathParam("fileName") String fileName) throws Exception {
        String knownHostsFile = createHostCaKnownHostsFile();
        consumerTemplate.receiveBody(
                "sftp://admin@localhost:{{camel.sftp.hostcert.test-port}}/sftp?password=admin&strictHostKeyChecking=yes&useUserKnownHostsFile=false&knownHostsFile="
                        + knownHostsFile
                        + "&caSignatureAlgorithms=ssh-ed25519,rsa-sha2-512,rsa-sha2-256&delete=true&fileName="
                        + fileName,
                TIMEOUT_MS,
                String.class);
        return Response.noContent().build();
    }

    /**
     * Creates a known_hosts file with @cert-authority entry for the host CA.
     * This allows the client to verify the server's host certificate.
     */
    private String createHostCaKnownHostsFile() throws Exception {
        String resourcePath = "certs/host-ca.pub";
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (InputStream stream = classLoader.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new RuntimeException("Failed to load host CA public key from: " + resourcePath);
            }
            return processHostCaStream(stream);
        }
    }

    private String processHostCaStream(InputStream hostCaStream) throws Exception {
        String hostCaPubKey = new String(hostCaStream.readAllBytes()).trim();
        String port = context.resolvePropertyPlaceholders("{{camel.sftp.hostcert.test-port}}");

        // Create known_hosts with @cert-authority entry
        String knownHostsContent = String.format("@cert-authority [localhost]:%s %s%n", port, hostCaPubKey);

        // Use temp directory instead of "target" which may not exist in native mode runtime
        java.nio.file.Path knownHostsPath = java.nio.file.Files.createTempFile("known_hosts_hostcert", ".txt");
        java.nio.file.Files.writeString(knownHostsPath, knownHostsContent);

        return knownHostsPath.toAbsolutePath().toString();
    }
}
