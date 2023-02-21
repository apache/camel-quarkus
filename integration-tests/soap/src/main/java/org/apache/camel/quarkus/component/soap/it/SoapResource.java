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
package org.apache.camel.quarkus.component.soap.it;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.soap.it.service.GetCustomersByName;
import org.apache.camel.quarkus.component.soap.it.service.NoSuchCustomerException;
import org.jboss.logging.Logger;

@Path("/soap")
@ApplicationScoped
public class SoapResource {

    private static final Logger LOG = Logger.getLogger(SoapResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    FluentProducerTemplate fluentProducerTemplate;

    @Path("/marshal/{soapVersion}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    public Response marshal(@PathParam("soapVersion") String soapVersion, String message) throws Exception {
        LOG.infof("Sending to soap: %s", message);

        GetCustomersByName request = new GetCustomersByName();
        request.setName(message);

        final String response = fluentProducerTemplate.toF("direct:marshal-%s", "soap" + soapVersion)
                .withBody(request)
                .request(String.class);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/marshal/fault/{soapVersion}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_XML)
    public Response marshalFault(@PathParam("soapVersion") String soapVersion, String message) throws Exception {
        LOG.infof("Sending to soap: %s", message);
        GetCustomersByName request = new GetCustomersByName();
        request.setName(message);

        final String response = fluentProducerTemplate.toF("direct:marshal-fault-%s", "soap" + soapVersion)
                .withBody(request)
                .request(String.class);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response)
                .build();
    }

    @Path("/unmarshal/{soapVersion}")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public Response unmarshal(@PathParam("soapVersion") String soapVersion, String message) throws Exception {
        final GetCustomersByName response = fluentProducerTemplate.toF("direct:unmarshal-%s", "soap" + soapVersion)
                .withBody(message)
                .request(GetCustomersByName.class);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getName())
                .build();
    }

    @Path("/unmarshal/fault/{soapVersion}")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.TEXT_PLAIN)
    public Response unmarshalFault(@PathParam("soapVersion") String soapVersion, String message) throws Exception {
        try {
            final NoSuchCustomerException response = fluentProducerTemplate
                    .toF("direct:unmarshal-fault-%s", "soap" + soapVersion)
                    .withBody(message)
                    .request(NoSuchCustomerException.class);
        } catch (CamelExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SOAPFaultException) {
                SOAPFaultException sfe = (SOAPFaultException) cause;
                return Response
                        .created(new URI("https://camel.apache.org/"))
                        .entity(sfe.getMessage())
                        .build();
            }
        }
        return Response.serverError().entity("Expected SOAPFaultException was not thrown").build();
    }

    @Path("/marshal/unmarshal")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response marshalUnmarshal(String message) throws Exception {
        LOG.infof("Sending to soap: %s", message);
        GetCustomersByName request = new GetCustomersByName();
        request.setName(message);
        final String xml = producerTemplate.requestBody("direct:marshal-soap1.2", request, String.class);
        LOG.infof("Got response from marshal: %s", xml);

        GetCustomersByName response = producerTemplate.requestBody("direct:unmarshal-soap1.2", xml, GetCustomersByName.class);
        LOG.infof("Got response from unmarshal: %s", response);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getName())
                .build();
    }

    @Path("/qname/strategy")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response qnameStrategy(String message) throws Exception {
        GetCustomersByName request = new GetCustomersByName();
        request.setName(message);

        final String xml = producerTemplate.requestBody("direct:marshalQnameStrategy", request, String.class);

        GetCustomersByName response = producerTemplate.requestBody("direct:unmarshalQnameStrategy", xml,
                GetCustomersByName.class);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getName())
                .build();
    }

    @Path("/serviceinterface/strategy")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response serviceInterfaceStrategy(String message) throws Exception {
        GetCustomersByName request = new GetCustomersByName();
        request.setName(message);

        final String xml = producerTemplate.requestBody("direct:marshalServiceInterfaceStrategy", request, String.class);

        GetCustomersByName response = producerTemplate.requestBody("direct:unmarshalServiceInterfaceStrategy", xml,
                GetCustomersByName.class);

        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getName())
                .build();
    }
}
