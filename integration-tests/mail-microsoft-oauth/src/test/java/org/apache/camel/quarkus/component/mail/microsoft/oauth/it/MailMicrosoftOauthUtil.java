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
package org.apache.camel.quarkus.component.mail.microsoft.oauth.it;

import java.util.LinkedList;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.item.sendmail.SendMailPostRequestBody;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class MailMicrosoftOauthUtil {

    public static void sendMessage(String subject, String content) {

        Config config = ConfigProvider.getConfig();
        String email = config.getValue(MailMicrosoftOauthResource.USERNAME_PROPERTY, String.class);
        String clientId = config.getValue(MailMicrosoftOauthResource.CLIENT_ID_PROPERTY, String.class);
        String clientSecret = config.getValue(MailMicrosoftOauthResource.CLIENT_SECRET_PROPERTY, String.class);
        String tenantId = config.getValue(MailMicrosoftOauthResource.TENANT_ID_PROPERTY, String.class);

        ClientSecretCredential graph = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();

        GraphServiceClient graphClient = new GraphServiceClient(graph);

        SendMailPostRequestBody sendMailPostRequestBody = new SendMailPostRequestBody();
        Message message = new Message();
        message.setSubject(subject);
        ItemBody body = new ItemBody();
        body.setContentType(BodyType.Text);
        body.setContent(content);
        message.setBody(body);
        LinkedList<Recipient> toRecipients = new LinkedList<Recipient>();
        Recipient recipient = new Recipient();
        EmailAddress emailAddress = new EmailAddress();
        emailAddress.setAddress(email);
        recipient.setEmailAddress(emailAddress);
        toRecipients.add(recipient);
        message.setToRecipients(toRecipients);
        sendMailPostRequestBody.setMessage(message);
        sendMailPostRequestBody.setSaveToSentItems(false);

        graphClient.users().byUserId(email).sendMail().post(sendMailPostRequestBody);
    }
}
