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
package org.apache.camel.quarkus.component.smb.it;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.hierynomus.smbj.share.File;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.smb.SmbConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/smb")
@ApplicationScoped
public class SmbResource {

    @EndpointInject("mock:result")
    private MockEndpoint mock;

    @ConfigProperty(name = "smb.host")
    String host;

    @ConfigProperty(name = "smb.port")
    String port;

    @ConfigProperty(name = "smb.username")
    String username;

    @ConfigProperty(name = "smb.password")
    String password;

    @ConfigProperty(name = "smb.share")
    String share;

    @Inject
    ConsumerTemplate consumer;

    @Inject
    ProducerTemplate producer;

    @Inject
    @Named("smbReceivedMsgs")
    List<Map<String, String>> receivedContents;

    @POST
    @Path("/send/{fileName}")
    public void send(String content,
            @PathParam("fileName") String fileName,
            @QueryParam("fileExist") String fileExist) throws Exception {
        if (fileExist == null || fileExist.isEmpty()) {
            producer.sendBodyAndHeader("direct:send", content, Exchange.FILE_NAME, fileName);
        } else {
            producer.sendBodyAndHeaders("direct:send", content, Map.of(SmbConstants.SMB_FILE_EXISTS, fileExist,
                    Exchange.FILE_NAME, fileName));
        }
    }

    @POST
    @Path("/receive")
    public String receive(String fileName) throws Exception {

        String uri = String.format("smb:%s:%s/%s?username=%s&password=%s&searchPattern=%s&path=/", host, port, share,
                username, password, fileName);
        var shareFile = consumer.receiveBody(uri, File.class);
        return new String(shareFile.getInputStream().readAllBytes(), "UTF-8");
    }

    @POST
    @Path("/receivedMsgs")
    public String receivedMsgs() throws Exception {

        return receivedContents.stream().map(m -> m.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue()) // Format each entry as "key=value"
                .collect(Collectors.joining(",")))
                .collect(Collectors.joining(";"));
    }

    @GET
    @Path("/validate")
    public void validateSmbResults() throws Exception {
        mock.expectedMessageCount(100);
        mock.assertIsSatisfied();
    }
}
