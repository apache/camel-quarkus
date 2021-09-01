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
package org.apache.camel.quarkus.component.openapijava.it;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.CollectionFormat;
import org.apache.camel.model.rest.RestParamType;
import org.apache.camel.quarkus.component.openapijava.it.model.Fruit;

public class OpenApiRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {

        rest()
                .get("/fruits/list")
                .id("list")
                .produces(MediaType.APPLICATION_JSON)
                .route()
                .setBody().constant(getFruits())
                .marshal().json()
                .endRest()

                .get("/operation/spec")
                .param()
                .type(RestParamType.header)
                .name("header_number")
                .dataType("integer")
                .required(true)
                .allowableValues("1", "2", "3")
                .defaultValue("1")
                .description("Header Param Number")
                .endParam()
                .param()
                .type(RestParamType.query)
                .name("query_letter")
                .dataType("string")
                .required(false)
                .allowableValues("A", "B", "C")
                .defaultValue("B")
                .description("Query Param Letter")
                .collectionFormat(CollectionFormat.multi)
                .endParam()
                .responseMessage()
                .code(418)
                .message("I am a teapot")
                .responseModel(Integer.class)
                .header("rate")
                .description("API Rate Limit")
                .dataType("integer")
                .endHeader()
                .endResponseMessage()
                .responseMessage()
                .code("error")
                .message("Response Error")
                .endResponseMessage()
                .route()
                .setBody().constant("GET: /operation/spec")
                .endRest()

                .get("/security/scopes")
                .security("OAuth2", "scope1,scope2,scope3")
                .route()
                .setBody().constant("GET: /security/scopes")
                .endRest()

                .get("/security/api/key")
                .route()
                .setBody().constant("GET: /security/api/key/header")
                .endRest()
                .securityDefinitions()
                .apiKey("X-API-Key", "The API key")
                .withHeader("X-API-KEY")
                .end()
                .end()

                .get("/security/basic/auth")
                .route()
                .setBody().constant("/security/basic/auth")
                .endRest()
                .securityDefinitions()
                .basicAuth("basicAuth", "Basic Authentication")
                .end()

                .get("/security/bearer/token")
                .route()
                .setBody().constant("/security/bearer/token")
                .endRest()
                .securityDefinitions()
                .bearerToken("bearerAuth", "Bearer Token Authentication")
                .end()

                .get("/security/mutual/tls")
                .route()
                .setBody().constant("/security/mutual/tls")
                .endRest()
                .securityDefinitions()
                .mutualTLS("mutualTLS")
                .end()

                .get("/security/oauth2")
                .route()
                .setBody().constant("/security/oauth2")
                .endRest()
                .securityDefinitions()
                .oauth2("oauth2", "OAuth2 Authentication")
                .flow("implicit")
                .authorizationUrl("https://secure.apache.org/fake/oauth2/authorize")
                .withScope("scope1", "Scope 1")
                .withScope("scope2", "Scope 2")
                .withScope("scope3", "Scope 3")
                .end()
                .end()

                .get("/security/openid")
                .route()
                .setBody().constant("/security/openid")
                .endRest()
                .securityDefinitions()
                .openIdConnect("openId", "https://secure.apache.org/fake/openid-configuration")
                .end();
    }

    private Set<Fruit> getFruits() {
        Set<Fruit> fruits = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));
        fruits.add(new Fruit("Apple", "Winter fruit"));
        fruits.add(new Fruit("Pineapple", "Tropical fruit"));
        return fruits;
    }
}
