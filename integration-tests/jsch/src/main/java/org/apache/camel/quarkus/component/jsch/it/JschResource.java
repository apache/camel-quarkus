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
package org.apache.camel.quarkus.component.jsch.it;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/jsch")
public class JschResource {

    @ConfigProperty(name = "jsch.username")
    String username;

    @ConfigProperty(name = "jsch.password")
    String password;

    @ConfigProperty(name = "jsch.host")
    String host;

    @ConfigProperty(name = "jsch.port")
    int port;

    @Inject
    FluentProducerTemplate fluentProducerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/file/copy")
    @POST
    public Response copyFile(@QueryParam("message") String message, String path) throws Exception {
        java.nio.file.Path filePath = Paths.get(path);
        String knownHosts = setupKnownHosts(filePath.getParent());

        fluentProducerTemplate
                .toF("scp://%s:%s?username=%s&password=%s&knownHostsFile=%s&chmod=755&privateKeyFile=classpath:camel-key.pgp",
                        host, port, username, password, knownHosts)
                .withHeader(Exchange.FILE_NAME, filePath.getFileName().toString())
                .withBody(message)
                .send();
        return Response.ok().build();
    }

    @Path("/file/get")
    @Produces(MediaType.TEXT_PLAIN)
    @GET
    public String getFile(@QueryParam("path") String path) throws Exception {
        java.nio.file.Path filePath = Paths.get(path);
        String knownHosts = setupKnownHosts(filePath.getParent());
        String fileName = filePath.getFileName().toString();

        // camel-jsch is producer only, hence use sftp to retrieve remote files
        return consumerTemplate.receiveBodyNoWait(
                "sftp://admin@{{jsch.host}}:{{jsch.port}}?username={{jsch.username}}&password={{jsch.password}}"
                        + "&knownHostsFile=" + knownHosts
                        + "&localWorkDirectory=target&fileName=" + fileName,
                String.class);
    }

    private String setupKnownHosts(java.nio.file.Path baseDir) throws Exception {
        java.nio.file.Path knownHosts = baseDir.resolve("known_hosts");

        // Generate known_hosts file so that we can trust the local server
        if (!Files.exists(knownHosts)) {
            Files.createFile(knownHosts);

            JSch jsch = new JSch();
            jsch.setKnownHosts(knownHosts.toAbsolutePath().toString());
            Session s = jsch.getSession(username, host, port);
            s.setConfig("StrictHostKeyChecking", "ask");

            s.setConfig("HashKnownHosts", "no");
            s.setUserInfo(new UserInfo() {
                @Override
                public String getPassphrase() {
                    return null;
                }

                @Override
                public String getPassword() {
                    return password;
                }

                @Override
                public boolean promptPassword(String message) {
                    return true;
                }

                @Override
                public boolean promptPassphrase(String message) {
                    return false;
                }

                @Override
                public boolean promptYesNo(String message) {
                    return true;
                }

                @Override
                public void showMessage(String message) {
                }
            });

            s.connect();
            s.disconnect();
        }

        return knownHosts.toString();
    }
}
