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
package org.apache.camel.quarkus.component.langchain4j.tokenizer.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.langchain4j.tokenizer.config.LangChain4JQwenConfiguration;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.tokenizer.LangChain4jTokenizerDefinition.TokenizerType;

@Path("/langchain4j-tokenizer")
@ApplicationScoped
public class Langchain4jTokenizerResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/lines")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public Response tokenizeLines(String data) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:tokenizeLines", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(20);
        producerTemplate.sendBody("direct:tokenizeLines", data);
        mockEndpoint.assertIsSatisfied();
        return Response.ok().build();
    }

    @Path("/paragraphs")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public Response tokenizeParagraphs(String data) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:tokenizeParagraphs", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(4);
        producerTemplate.sendBody("direct:tokenizeParagraphs", data);
        mockEndpoint.assertIsSatisfied();
        return Response.ok().build();
    }

    @Path("/sentences")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public Response tokenizeSentences(String data) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:tokenizeSentences", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(4);
        producerTemplate.sendBody("direct:tokenizeSentences", data);
        mockEndpoint.assertIsSatisfied();
        return Response.ok().build();
    }

    @Path("/resolve")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response resolveAbsentTokenizer(String tokenizerType) {
        TokenizerType type = TokenizerType.valueOf(tokenizerType);
        try {
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    if (!type.equals(TokenizerType.QWEN)) {
                        from("direct:resolve" + tokenizerType)
                                .tokenize(tokenizer()
                                        .byLine()
                                        .maxTokens(1024, "gpt-4o-mini")
                                        .maxOverlap(10)
                                        .using(type)
                                        .end())
                                .split()
                                .body()
                                .to("mock:resolve" + tokenizerType);
                    } else {
                        LangChain4JQwenConfiguration configuration = new LangChain4JQwenConfiguration();
                        configuration.setMaxTokens(500);
                        configuration.setMaxOverlap(50);
                        configuration.setType(tokenizerType);
                        configuration.setModelName("test");
                        configuration.setApiKey("test-key");

                        from("direct:resolve" + tokenizerType)
                                .tokenize(tokenizer()
                                        .byLine()
                                        .configuration(configuration)
                                        .end())
                                .split()
                                .body()
                                .to("mock:resolve" + tokenizerType);
                    }
                }
            });
        } catch (Exception e) {
            if ((e.getCause() instanceof UnsupportedOperationException)) {
                // Successful response if we get the exception type that we expect
                // from the build time generated tokenizer classes
                return Response.ok().build();
            }
        }
        return Response.serverError().build();
    }
}
