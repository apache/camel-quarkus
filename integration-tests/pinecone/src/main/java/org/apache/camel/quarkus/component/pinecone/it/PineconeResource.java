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
package org.apache.camel.quarkus.component.pinecone.it;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import io.pinecone.clients.Pinecone;
import io.pinecone.proto.UpsertResponse;
import io.pinecone.unsigned_indices_model.QueryResponseWithUnsignedIndices;
import io.pinecone.unsigned_indices_model.ScoredVectorWithUnsignedIndices;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.pinecone.PineconeVectorDbComponent;
import org.apache.camel.component.pinecone.PineconeVectorDbConfiguration;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jetbrains.annotations.NotNull;
import org.openapitools.db_control.client.model.IndexModel;

@Path("/pinecone")
@ApplicationScoped
public class PineconeResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Path("/index")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public float query(List<Float> vectors) {
        QueryResponseWithUnsignedIndices result = producerTemplate.requestBody("direct:query", vectors,
                QueryResponseWithUnsignedIndices.class);
        List<ScoredVectorWithUnsignedIndices> matchesList = result.getMatchesList();
        if (matchesList.size() != 1) {
            return 0;
        }
        return matchesList.get(0).getScore();
    }

    @Path("/index")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String createServerlessIndex() {
        IndexModel indexModel = producerTemplate.requestBody("direct:createServerlessIndex", null, IndexModel.class);
        return indexModel.getName();
    }

    @Path("/index")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public int upsert(List<Float> vectors) {
        UpsertResponse result = producerTemplate.requestBody("direct:upsert", vectors, UpsertResponse.class);
        return result.getUpsertedCount();
    }

    @Path("/index")
    @DELETE
    public void deleteIndex() {
        producerTemplate.sendBody("direct:deleteIndex", null);
    }

    @Named("pinecone")
    PineconeVectorDbComponent pineconeVectorDbComponent() {
        Pinecone client = createPineconeClient();
        PineconeVectorDbComponent component = new PineconeVectorDbComponent();
        PineconeVectorDbConfiguration configuration = new PineconeVectorDbConfiguration();
        configuration.setClient(client);
        component.setConfiguration(configuration);
        return component;
    }

    static Pinecone createPineconeClient() {
        Optional<String> wireMockUrl = ConfigProvider.getConfig().getOptionalValue("wiremock.url", String.class);
        String apiKey = ConfigProvider.getConfig().getValue("camel.component.pinecone.token", String.class);
        Pinecone.Builder builder = new Pinecone.Builder(apiKey);
        if (wireMockUrl.isPresent()) {
            String baseUri = wireMockUrl.get();
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(new Interceptor() {
                        @NotNull
                        @Override
                        public Response intercept(@NotNull Interceptor.Chain chain) throws IOException {
                            Request originalRequest = chain.request();
                            Request wireMockedRequest = originalRequest.newBuilder()
                                    .url(baseUri + originalRequest.url().encodedPath())
                                    .build();
                            return chain.proceed(wireMockedRequest);
                        }
                    })
                    .build();
            return builder.withOkHttpClient(client).build();
        }
        return builder.build();
    }
}
