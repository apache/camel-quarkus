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
package org.apache.camel.quarkus.component.jaxb.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.jaxb.it.model.Person;
import org.apache.camel.quarkus.component.jaxb.it.model.factory.FactoryInstantiatedPerson;
import org.apache.camel.quarkus.component.jaxb.it.model.namespaced.NamespacedPerson;
import org.apache.camel.quarkus.component.jaxb.it.model.partial.PartClassPerson;
import org.apache.camel.quarkus.component.jaxb.it.model.pojo.PojoPerson;
import org.apache.camel.quarkus.component.jaxb.it.model.simple.SimplePerson;
import org.apache.camel.util.ObjectHelper;

import static org.apache.camel.quarkus.component.jaxb.it.JaxbHelper.personToJson;

@Path("/jaxb")
@ApplicationScoped
public class JaxbResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/marshal")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response marshal(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("age") int age) {

        Person person = new SimplePerson();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setAge(age);

        Exchange result = producerTemplate.request("direct:marshal", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setBody(person);
            }
        });

        Exception exception = result.getException();
        if (exception != null) {
            String xml = String.format("<error>%s</error>", exception.getMessage());
            return Response.serverError().entity(xml).build();
        }

        Message message = result.getMessage();
        String contentType = message.getHeader(Exchange.CONTENT_TYPE, String.class);
        String xml = message.getBody(String.class);

        if (ObjectHelper.isEmpty(contentType) || !contentType.equals("application/xml")) {
            throw new IllegalStateException("Expected content type application/xml but got " + contentType);
        }

        if (xml.startsWith("<?xml")) {
            throw new IllegalStateException("XML prolog was not expected as JaxbDataFormat.fragment = true");
        }

        return Response.ok(xml).build();
    }

    @Path("/unmarshal")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unmarshal(String xml) {
        Exchange result = producerTemplate.request("direct:unmarshal", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setBody(xml);
            }
        });

        Exception exception = result.getException();
        if (exception != null) {
            String errorXml = String.format("{\"error\": \"%s\"}", exception.getMessage());
            return Response.serverError().entity(errorXml).build();
        }

        Person person = result.getMessage().getBody(SimplePerson.class);
        return Response.ok(personToJson(person)).build();
    }

    @Path("/marshal/dsl")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response marshalWithJaxbDsl(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("age") int age) {

        Person person = new SimplePerson();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setAge(age);

        String xml = producerTemplate.requestBody("direct:marshalJaxbDsl", person, String.class);

        return Response.ok(xml).build();
    }

    @Path("/unmarshal/dsl")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unmarshalWithJaxbDsl(String xml) {
        Person person = producerTemplate.requestBody("direct:unmarshalJaxbDsl", xml, Person.class);
        return Response.ok(personToJson(person)).build();
    }

    @Path("/marshal/namespace/prefix")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response marshalWithNamespacePrefix(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("age") int age) {

        Person person = new NamespacedPerson();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setAge(age);

        String xml = producerTemplate.requestBody("direct:marshalNamespacePrefix", person, String.class);

        return Response.ok(xml).build();
    }

    @Path("/unmarshal/namespace/prefix")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unmarshalWithNamespacePrefix(String xml) {
        Person person = producerTemplate.requestBody("direct:unmarshalNamespacePrefix", xml, NamespacedPerson.class);
        return Response.ok(personToJson(person)).build();
    }

    @Path("/marshal/encoding")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response marshalWithEncoding(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("age") int age) {

        Person person = new SimplePerson();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setAge(age);

        String xml = producerTemplate.requestBody("direct:marshalEncoding", person, String.class);

        return Response.ok(xml).build();
    }

    @Path("/unmarshal/encoding")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unmarshalWithEncoding(String xml) {
        Person person = producerTemplate.requestBody("direct:unmarshalEncoding", xml, SimplePerson.class);
        return Response.ok(personToJson(person)).build();
    }

    @Path("/marshal/xml")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response marshalWithExistingXmlPayload(String xml) {
        String response = producerTemplate.requestBody("direct:marshalWithMustBeJAXBElementFalse", xml, String.class);
        return Response.ok(response).build();
    }

    @Path("/marshal/part/class")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response marshalPartial(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("age") int age,
            @QueryParam("useHeader") boolean useHeader) {

        Person person = new PartClassPerson();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setAge(age);

        String uri = useHeader ? "direct:marshalPartClassFromHeader" : "direct:marshalPartClass";

        String response = producerTemplate.requestBody(uri, person, String.class);
        return Response.ok(response).build();
    }

    @Path("/unmarshal/part/class")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public Response unmarshalPartial(@QueryParam("useHeader") boolean useHeader, String xml) {
        String uri = useHeader ? "direct:unmarshalPartClassFromHeader" : "direct:unmarshalPartClass";

        Person person = producerTemplate.requestBody(uri, xml, PartClassPerson.class);
        return Response.ok(personToJson(person)).build();
    }

    @Path("/unmarshal/ignore/element")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response unmarshalWithIgnoreJaxbElement(String xml) {
        JAXBElement<Person> element = producerTemplate.requestBody("direct:unmarshalIgnoreJaxbElement", xml, JAXBElement.class);
        Person person = element.getValue();
        return Response.ok(personToJson(person)).build();
    }

    @Path("/marshal/custom/properties")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response marshalWithCustomProperties(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("age") int age) {

        Person person = new SimplePerson();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setAge(age);

        String xml = producerTemplate.requestBody("direct:marshalCustomProperties", person, String.class);

        return Response.ok(xml).build();
    }

    @Path("/marshal/custom/stream/writer")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response marshalWithCustomStreamWriter(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("age") int age) {

        Person person = new SimplePerson();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setAge(age);

        String xml = producerTemplate.requestBody("direct:marshalCustomStreamWriter", person, String.class);

        return Response.ok(xml).build();
    }

    @Path("/marshal/with/object/factory")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response marshalWithObjectFactory(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("age") int age) {

        Person person = new FactoryInstantiatedPerson();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setAge(age);

        String xml = producerTemplate.requestBody("direct:marshalWithObjectFactory", person, String.class);

        return Response.ok(xml).build();
    }

    @Path("/marshal/without/object/factory")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response marshalWithoutObjectFactory(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("age") int age) {

        Person person = new PojoPerson();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setAge(age);

        String json = producerTemplate.requestBody("direct:marshalWithoutObjectFactory", person, String.class);

        return Response.ok(json).build();
    }

    @Path("/marshal/non/namespace/schema/location")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response marshalWithNoNamespaceSchemaLocation(
            @QueryParam("firstName") String firstName,
            @QueryParam("lastName") String lastName,
            @QueryParam("age") int age) {

        Person person = new SimplePerson();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setAge(age);

        String xml = producerTemplate.requestBody("direct:marshalNoNamespaceSchemaLocation", person, String.class);

        return Response.ok(xml).build();
    }
}
