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
package org.apache.camel.quarkus.component.beanio.it;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.beanio.it.model.A1Record;
import org.apache.camel.quarkus.component.beanio.it.model.B1Record;
import org.apache.camel.quarkus.component.beanio.it.model.Employee;
import org.apache.camel.quarkus.component.beanio.it.model.EmployeeAnnotated;
import org.apache.camel.quarkus.component.beanio.it.model.Error;
import org.apache.camel.quarkus.component.beanio.it.model.Header;
import org.apache.camel.quarkus.component.beanio.it.model.Separator;
import org.apache.camel.quarkus.component.beanio.it.model.Trailer;

@Path("/beanio")
public class BeanioResource {
    public static String DATA_FORMAT = "yyyy-MM-dd";
    private final SimpleDateFormat formatter = new SimpleDateFormat(DATA_FORMAT);

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;
    @Inject
    org.jboss.logging.Logger logger;

    @Path("/marshal")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String marshal(List<Employee> employees, @QueryParam("type") String type) {
        return producerTemplate.requestBodyAndHeader("direct:marshal", employees, "type", type, String.class);
    }

    @Path("/marshal/annotated")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String marshalAnnotated(List<EmployeeAnnotated> employees) {
        return producerTemplate.requestBodyAndHeader("direct:marshal", employees, "type", "ANNOTATED", String.class);
    }

    @Path("/unmarshal")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response unmarshal(String data, @QueryParam("type") String type) {
        List<Employee> employees = producerTemplate.requestBodyAndHeader("direct:unmarshal", data, "type", type, List.class);
        return getResponse(employees);
    }

    @Path("/unmarshal/annotated")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response unmarshalAnnotated(String data) {
        List<EmployeeAnnotated> employees = producerTemplate.requestBodyAndHeader("direct:unmarshal", data, "type", "ANNOTATED",
                List.class);
        JsonArrayBuilder array = Json.createArrayBuilder();
        for (EmployeeAnnotated employee : employees) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("firstName", employee.getFirstName());
            builder.add("lastName", employee.getLastName());
            builder.add("title", employee.getTitle());
            builder.add("hireDate", formatter.format(employee.getHireDate()));
            builder.add("salary", employee.getSalary());
            array.add(builder.build());
        }
        return Response.ok(array.build()).build();
    }

    @Path("/marshal/single/object")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String marshalSingleObject(Map<String, String> message) {
        return producerTemplate.requestBody("direct:marshalSingleObject", message, String.class);
    }

    @Path("/unmarshal/single/object")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> unmarshalSingleObject(String message) {
        return producerTemplate.requestBody("direct:unmarshalSingleObject", message, Map.class);
    }

    @Path("/unmarshal/with/error/handler")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response unmarshalWithErrorHandler(String data) {
        List<Object> results = producerTemplate.requestBody("direct:unmarshalWithErrorHandler", data, List.class);
        JsonArrayBuilder array = Json.createArrayBuilder();
        for (Object object : results) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            if (object instanceof Employee) {
                Employee employee = (Employee) object;
                builder.add("firstName", employee.getFirstName());
                builder.add("lastName", employee.getLastName());
                builder.add("title", employee.getTitle());
                builder.add("hireDate", formatter.format(employee.getHireDate()));
                builder.add("salary", employee.getSalary());
            } else if (object instanceof Error) {
                Error error = (Error) object;
                builder.add("message", error.getMessage());
                builder.add("record", error.getRecord());
            } else {
                throw new IllegalStateException("Unsupported type: " + object.getClass());
            }

            array.add(builder.build());
        }
        return Response.ok(array.build()).build();
    }

    @Path("/marshal/complex/object")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Object marshalComplexObject() throws Exception {
        return producerTemplate.requestBody("direct:marshalComplexObject", createComplexObject(), Object.class);
    }

    @Path("/unmarshal/complex/object")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response unmarshalComplexObject(String data) {
        List<Object> results = producerTemplate.requestBody("direct:unmarshalComplexObject", data, List.class);
        JsonArrayBuilder array = Json.createArrayBuilder();
        for (Object object : results) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            if (object instanceof Header) {
                Header header = (Header) object;
                builder.add("identifier", header.getIdentifier());
                builder.add("recordType", header.getRecordType());
                builder.add("date", formatter.format(header.getHeaderDate()));
            } else if (object instanceof Separator) {
                Separator separator = (Separator) object;
                builder.add("value", separator.getValue());
            } else if (object instanceof A1Record) {
                A1Record record = (A1Record) object;
                BigDecimal price = new BigDecimal(record.getCurrentPrice());
                builder.add("price", price.setScale(2, RoundingMode.HALF_UP).toPlainString());
                builder.add("sedol", record.getSedol());
                builder.add("source", record.getSource());
            } else if (object instanceof B1Record) {
                B1Record record = (B1Record) object;
                builder.add("securityName", record.getSecurityName());
                builder.add("sedol", record.getSedol());
                builder.add("source", record.getSource());
            } else if (object instanceof Trailer) {
                Trailer trailer = (Trailer) object;
                builder.add("numberOfRecords", trailer.getNumberOfRecords());
            } else {
                throw new IllegalStateException("Unsupported type: " + object.getClass());
            }

            array.add(builder.build());
        }

        return Response.ok(array.build()).build();
    }

    private List<Object> createComplexObject() throws Exception {
        String source = "camel-beanio";
        List<Object> complexObject = new ArrayList<>();
        Date date = formatter.parse("2008-08-03");
        Header hFirst = new Header("A1", date, "PRICE");
        Header hSecond = new Header("B1", date, "SECURITY");
        Separator headerEnd = new Separator("HEADER END");

        A1Record first = new A1Record("0001917", source, 12345.678900);
        A1Record second = new A1Record("0002374", source, 59303290.020);
        B1Record third = new B1Record("0015219", source, "SECURITY ONE");
        Separator sectionEnd = new Separator("END OF SECTION 1");
        A1Record fourth = new A1Record("0076647", source, 0.0000000001);
        A1Record fifth = new A1Record("0135515", source, 999999999999d);
        B1Record sixth = new B1Record("2000815", source, "SECURITY TWO");
        B1Record seventh = new B1Record("2207122", source, "SECURITY THR");

        complexObject.add(hFirst);
        complexObject.add(hSecond);
        complexObject.add(headerEnd);
        complexObject.add(first);
        complexObject.add(second);
        complexObject.add(third);
        complexObject.add(sectionEnd);
        complexObject.add(fourth);
        complexObject.add(fifth);
        complexObject.add(sixth);
        complexObject.add(seventh);

        Trailer trailer = new Trailer(7);
        complexObject.add(trailer);
        return complexObject;
    }

    @Path("/split")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response split(String data) throws Exception {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:splitEmployees", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(3);

        producerTemplate.sendBody("direct:unmarshalWithSplitter", data);

        mockEndpoint.assertIsSatisfied(5000);
        List<Exchange> exchanges = mockEndpoint.getExchanges();

        List<Employee> employees = exchanges.stream().map(e -> e.getMessage().getBody(Employee.class))
                .collect(Collectors.toList());
        return getResponse(employees);
    }

    @Path("/unmarshal/global")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public Response marshalEmployees(String csv) {
        List<Employee> employees = producerTemplate.requestBody("direct:unmarshalGlobal", csv, List.class);
        return getResponse(employees);
    }

    private Response getResponse(List<Employee> employees) {
        JsonArrayBuilder array = Json.createArrayBuilder();
        for (Employee employee : employees) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("firstName", employee.getFirstName());
            builder.add("lastName", employee.getLastName());
            builder.add("title", employee.getTitle());
            builder.add("hireDate", formatter.format(employee.getHireDate()));
            builder.add("salary", employee.getSalary());
            array.add(builder.build());
        }
        return Response.ok(array.build()).build();
    }
}
