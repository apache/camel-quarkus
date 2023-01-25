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
package org.apache.camel.quarkus.component.braintree.it;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.braintreegateway.BraintreeGateway;
import com.braintreegateway.Result;
import com.braintreegateway.Transaction;
import com.braintreegateway.TransactionRequest;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.braintree.BraintreeComponent;
import org.apache.camel.component.braintree.internal.BraintreeApiCollection;
import org.apache.camel.component.braintree.internal.TransactionGatewayApiMethod;

@Path("/braintree")
public class BraintreeResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/token")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String generateToken() throws Exception {
        return producerTemplate.requestBody("braintree:clientToken/generate", null, String.class);
    }

    @Path("/sale")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response sale() throws Exception {
        String api = BraintreeApiCollection.getCollection().getApiName(TransactionGatewayApiMethod.class).getName();
        TransactionRequest transaction = new TransactionRequest()
                .amount(new BigDecimal("100.00"))
                .paymentMethodNonce("fake-valid-nonce")
                .options()
                .submitForSettlement(true)
                .done();
        Result<Transaction> result = producerTemplate.requestBody("braintree:" + api + "/sale?inBody=request", transaction,
                Result.class);

        CamelContext camelContext = producerTemplate.getCamelContext();
        BraintreeComponent component = camelContext.getComponent("braintree", BraintreeComponent.class);
        BraintreeGateway gateway = component.getGateway(component.getConfiguration());
        gateway.testing().settle(result.getTarget().getId());

        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("success", result.isSuccess());
        objectBuilder.add("transactionId", result.getTarget().getId());

        return Response.status(Response.Status.OK).entity(objectBuilder.build()).build();
    }

    @Path("/refund")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response refund(String transactionId) throws Exception {
        String api = BraintreeApiCollection.getCollection().getApiName(TransactionGatewayApiMethod.class).getName();
        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelBraintree.id", transactionId);
        headers.put("CamelBraintree.amount", new BigDecimal("99.00"));

        Result<Transaction> result = producerTemplate.requestBodyAndHeaders("braintree:" + api + "/refund", null, headers,
                Result.class);

        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("success", result.isSuccess());

        return Response.status(Response.Status.OK).entity(objectBuilder.build()).build();
    }
}
