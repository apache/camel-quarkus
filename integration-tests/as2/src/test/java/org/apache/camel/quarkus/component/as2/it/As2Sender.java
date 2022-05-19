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
package org.apache.camel.quarkus.component.as2.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.camel.component.as2.api.AS2ClientConnection;
import org.apache.camel.component.as2.api.AS2ClientManager;
import org.apache.camel.component.as2.api.AS2MediaType;
import org.apache.camel.component.as2.api.AS2MessageStructure;
import org.apache.http.HttpException;
import org.apache.http.entity.ContentType;

public class As2Sender {

    private static final String TARGET_HOST = "localhost";
    private static final String USER_AGENT = "Camel AS2 Endpoint";
    private static final String CLIENT_FQDN = "example.org";

    private As2Sender() {
    }

    public static As2SenderClient createClient(int port) throws IOException {
        AS2ClientConnection clientConnection = new AS2ClientConnection(As2Helper.AS2_VERSION, USER_AGENT, CLIENT_FQDN,
                TARGET_HOST, port);
        AS2ClientManager clientManager = new AS2ClientManager(clientConnection);

        return ediMessage -> clientManager.send(ediMessage, As2Helper.REQUEST_URI, As2Helper.SUBJECT, As2Helper.FROM,
                As2Helper.AS2_NAME,
                As2Helper.AS2_NAME, AS2MessageStructure.PLAIN,
                ContentType.create(AS2MediaType.APPLICATION_EDIFACT, StandardCharsets.US_ASCII.name()), null, null, null, null,
                null, As2Helper.DISPOSITION_NOTIFICATION_TO, As2Helper.SIGNED_RECEIPT_MIC_ALGORITHMS, null, null, null);
    }

    public interface As2SenderClient {

        void sendMessage(String ediMessage) throws HttpException;
    }
}
