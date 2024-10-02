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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.hierynomus.smbj.share.File;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.smb.SmbConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SmbRoute extends RouteBuilder {

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
    @Named("smbReceivedMsgs")
    List<Map<String, String>> receivedContents;

    @Override
    public void configure() throws Exception {
        from("smb:{{smb.host}}:{{smb.port}}/{{smb.share}}?username={{smb.username}}&password={{smb.password}}&path=/&repeatCount=1&searchPattern=*.txt")
                .to("mock:result");

        from("direct:send")
                .toF("smb:%s:%s/%s?username=%s&password=%s&path=/", host, port, share, username, password);

        from("smb:{{smb.host}}:{{smb.port}}/{{smb.share}}?username={{smb.username}}&password={{smb.password}}&path=/&searchPattern=*.tx1")
                .process(e -> {
                    receivedContents.add(Map.of(
                            "path", e.getIn().getBody(File.class).getPath(),
                            "content", new String(e.getIn().getBody(InputStream.class).readAllBytes(), "UTF-8"),
                            SmbConstants.SMB_FILE_PATH, e.getIn().getHeader(SmbConstants.SMB_FILE_PATH, String.class),
                            SmbConstants.SMB_UNC_PATH, e.getIn().getHeader(SmbConstants.SMB_UNC_PATH, String.class)));
                });
    }

    static class Producers {

        @Singleton
        @Produces
        @Named("smbReceivedMsgs")
        List<Map<String, String>> smbReceivedMsgs() {
            return new CopyOnWriteArrayList<>();
        }
    }

}
