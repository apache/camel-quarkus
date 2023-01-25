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
package org.apache.camel.quarkus.component.twilio.it;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.twilio.http.HttpClient;
import com.twilio.http.NetworkHttpClient;
import com.twilio.http.Request;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.IncomingPhoneNumber;
import com.twilio.rest.api.v2010.account.Message;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.ConfigProvider;

@Path("/twilio")
public class TwilioResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/message")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createMessage(String body) throws Exception {
        Message message = producerTemplate.requestBody(
                "twilio://message/create?from=RAW(+15005550006)&to=RAW(+14108675310)&body=" + body, null, Message.class);
        return Response.ok(message.getSid()).build();
    }

    @Path("/purchase")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response purchasePhoneNumber() throws Exception {
        IncomingPhoneNumber phoneNumber = producerTemplate.requestBody(
                "twilio://incoming-phone-number/create?phonenumber=RAW(+15005550006)", null, IncomingPhoneNumber.class);
        return Response.ok(phoneNumber.getPhoneNumber()).build();
    }

    @Path("/call")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response phoneCall() throws Exception {
        Call call = producerTemplate.requestBody(
                "twilio://call/create?from=RAW(+15005550006)&to=RAW(+14108675310)&url=http://demo.twilio.com/docs/voice.xml",
                null, Call.class);
        return Response.ok(call.getSid()).build();
    }

    @Named("restClient")
    public TwilioRestClient restClient() {
        // If mocking is enabled, we need to ensure Twilio API calls are directed to the mock server
        Optional<String> wireMockUrl = ConfigProvider.getConfig().getOptionalValue("wiremock.url", String.class);
        if (wireMockUrl.isPresent()) {
            HttpClient client = new NetworkHttpClient() {
                @Override
                public com.twilio.http.Response makeRequest(Request originalRequest) {
                    String url = originalRequest.getUrl();

                    Request modified = new Request(originalRequest.getMethod(),
                            url.replace("https://api.twilio.com", wireMockUrl.get()));

                    Map<String, List<String>> headerParams = originalRequest.getHeaderParams();
                    for (String key : headerParams.keySet()) {
                        for (String value : headerParams.get(key)) {
                            modified.addHeaderParam(key, value);
                        }
                    }

                    Map<String, List<String>> postParams = originalRequest.getPostParams();
                    for (String key : postParams.keySet()) {
                        for (String value : postParams.get(key)) {
                            modified.addPostParam(key, value);
                        }
                    }

                    Map<String, List<String>> queryParams = originalRequest.getQueryParams();
                    for (String key : queryParams.keySet()) {
                        for (String value : queryParams.get(key)) {
                            modified.addQueryParam(key, value);
                        }
                    }

                    modified.setAuth(originalRequest.getUsername(), originalRequest.getPassword());

                    return super.makeRequest(modified);
                }
            };

            return new TwilioRestClient.Builder(
                    ConfigProvider.getConfig().getValue("camel.component.twilio.username", String.class),
                    ConfigProvider.getConfig().getValue("camel.component.twilio.password", String.class))
                            .accountSid(ConfigProvider.getConfig().getValue("camel.component.twilio.account-sid", String.class))
                            .httpClient(client)
                            .build();
        }
        return null;
    }
}
