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
package org.apache.camel.quarkus.variables.it;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;

@Path("/variables")
@ApplicationScoped
public class VariablesResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/setLocalVariable")
    @POST
    public String setLocalVariable(String body) throws Exception {
        MockEndpoint end = context.getEndpoint("mock:setLocalVariableEnd", MockEndpoint.class);
        end.expectedMessageCount(1);
        end.expectedVariableReceived(VariablesRoutes.VARIABLE_NAME, VariablesRoutes.VARIABLE_VALUE);

        producerTemplate.requestBody("direct:setLocalVariableStart", body, String.class);

        // make sure we got the message
        end.assertIsSatisfied();

        // lets get the variable value
        List<Exchange> exchanges = end.getExchanges();
        Exchange exchange = exchanges.get(0);

        return exchange.getVariable(VariablesRoutes.VARIABLE_NAME, String.class);
    }

    @Path("/setGlobalVariable")
    @POST
    public Response setGlobalVariable(String body) throws Exception {
        if (context.getVariable(VariablesRoutes.VARIABLE_NAME) != null) {
            return Response.status(500).entity(String.format("Variable '%s' has to be null before sending message to the rout.",
                    VariablesRoutes.VARIABLE_VALUE)).build();
        }

        MockEndpoint end = context.getEndpoint("mock:setGlobalVariableEnd", MockEndpoint.class);
        end.expectedMessageCount(1);

        producerTemplate.requestBody("direct:setGlobalVariableStart", body, String.class);

        // make sure we got the message
        end.assertIsSatisfied();

        // lets get the variable value
        List<Exchange> exchanges = end.getExchanges();
        Exchange exchange = exchanges.get(0);

        String resp = exchange.getVariable(VariablesRoutes.VARIABLE_NAME, String.class) + ","
                + context.getVariable(VariablesRoutes.VARIABLE_NAME, String.class);
        return Response.ok().entity(resp).build();
    }

    @Path("/removeLocalVariable")
    @POST
    public String removeLocalVariable(String body) throws Exception {
        MockEndpoint end = context.getEndpoint("mock:removeLocalVariableEnd", MockEndpoint.class);
        end.expectedMessageCount(1);

        MockEndpoint mid = context.getEndpoint("mock:removeLocalVariableMid", MockEndpoint.class);
        mid.expectedMessageCount(1);

        producerTemplate.requestBody("direct:removeLocalVariableStart", body, String.class);

        // make sure we got the message
        end.assertIsSatisfied();
        mid.assertIsSatisfied();

        // lets get the variable value
        List<Exchange> exchanges = end.getExchanges();
        Exchange endExchange = exchanges.get(0);
        exchanges = mid.getExchanges();
        Exchange midExchange = exchanges.get(0);

        return midExchange.getVariable(VariablesRoutes.VARIABLE_NAME, String.class) + ","
                + endExchange.getVariable(VariablesRoutes.VARIABLE_NAME, String.class);
    }

    @Path("/removeGlobalVariable")
    @POST
    public String removeGlobalVariable(String body) throws Exception {
        MockEndpoint mid = context.getEndpoint("mock:removeGlobalVariableMid", MockEndpoint.class);
        mid.expectedMessageCount(1);

        producerTemplate.requestBody("direct:removeGlobalVariableStart", body, String.class);

        // make sure we got the message
        mid.assertIsSatisfied();

        // lets get the variable value
        List<Exchange> exchanges = mid.getExchanges();
        Exchange midExchange = exchanges.get(0);

        return midExchange.getVariable(VariablesRoutes.VARIABLE_NAME, String.class) + ","
                + context.getVariable(VariablesRoutes.VARIABLE_NAME, String.class);
    }

    @Path("/customGlobalRepository")
    @POST
    public Response customGlobalRepository(String body) throws Exception {
        context.getRouteController().startRoute("customGlobalRepository");

        MockEndpoint end = context.getEndpoint("mock:setGlobalCustomEnd", MockEndpoint.class);
        end.expectedMessageCount(1);

        producerTemplate.requestBody("direct:setGlobalCustomStart", body, String.class);

        // make sure we got the message
        end.assertIsSatisfied();

        // lets get the variable value
        List<Exchange> exchanges = end.getExchanges();
        Exchange exchange = exchanges.get(0);

        String resp = exchange.getVariable(VariablesRoutes.VARIABLE_NAME, String.class) + ","
                + context.getVariable(VariablesRoutes.VARIABLE_NAME, String.class);
        return Response.ok().entity(resp).build();
    }

    @Path("/convert")
    @POST
    public String convert(String body) throws Exception {
        MockEndpoint end = context.getEndpoint("mock:convertEnd", MockEndpoint.class);
        end.expectedMessageCount(1);

        producerTemplate.requestBody("direct:convertStart", body, String.class);

        // make sure we got the message
        end.assertIsSatisfied();

        List<Exchange> exchanges = end.getExchanges();
        Exchange exchange = exchanges.get(0);

        return exchange.getVariable(VariablesRoutes.VARIABLE_NAME, String.class);
    }

    @Path("/filter/{city}")
    @POST
    public Response filter(String body, @PathParam("city") String city) throws Exception {
        MockEndpoint end = context.getEndpoint("mock:filterEnd", MockEndpoint.class);
        end.expectedMessageCount(1);

        producerTemplate.requestBodyAndHeader("direct:filterStart", body, "city", city, String.class);

        try {
            Exchange exhange = end.assertExchangeReceived(0);
            return Response.ok().entity(exhange.getIn().getBody(String.class)).build();
        } catch (AssertionError e) {
            return Response.status(204).build();
        }

    }

}
