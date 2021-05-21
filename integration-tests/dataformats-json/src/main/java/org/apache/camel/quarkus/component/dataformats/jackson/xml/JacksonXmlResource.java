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
package org.apache.camel.quarkus.component.dataformats.jackson.xml;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jacksonxml.JacksonXMLConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.dataformats.json.model.TestJAXBPojo;
import org.apache.camel.quarkus.component.dataformats.json.model.TestOtherPojo;
import org.apache.camel.quarkus.component.dataformats.json.model.TestPojo;
import org.apache.camel.quarkus.component.dataformats.json.model.TestPojoView;
import org.apache.camel.support.DefaultExchange;
import org.jboss.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Path("/dataformats-json")
@ApplicationScoped
public class JacksonXmlResource {

    private static final Logger LOG = Logger.getLogger(JacksonXmlResource.class);
    private static final String LS = System.lineSeparator();
    @Inject
    ProducerTemplate producerTemplate;
    @Inject
    ConsumerTemplate consumerTemplate;
    @Inject
    CamelContext context;

    @Path("jacksonxml/unmarshal-type-header")
    @POST
    @Consumes(MediaType.TEXT_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public TestPojo jacksonXmlUnmarshalTypeHeader(String body) {

        return producerTemplate
                .requestBodyAndHeader("direct:jacksonxml-unmarshal-type-header", body, JacksonXMLConstants.UNMARSHAL_TYPE,
                        TestPojo.class.getName(),
                        TestPojo.class);

    }

    @Path("jacksonxml/unmarshal-list")
    @GET
    public void jacksonXmlUnmarshalList(String body) throws Exception {

        MockEndpoint mock = context.getEndpoint("mock:jacksonxml-unmarshal-list-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(List.class);

        String json = "<list><pojo name=\"Camel\"/><pojo name=\"World\"/></list>";
        producerTemplate.sendBody("direct:jacksonxml-unmarshal-list", json);

        mock.assertIsSatisfied();

        List list = mock.getReceivedExchanges().get(0).getIn().getBody(List.class);
        assertNotNull(list);
        assertEquals(2, list.size());

        TestPojo pojo = (TestPojo) list.get(0);
        assertEquals("Camel", pojo.getName());
        pojo = (TestPojo) list.get(1);
        assertEquals("World", pojo.getName());

        mock.expectedMessageCount(2);
        mock.message(1).body().isInstanceOf(List.class);

        json = "<list><pojo name=\"Camel\"/></list>";
        producerTemplate.sendBody("direct:jacksonxml-unmarshal-list", json);

        mock.assertIsSatisfied();

        list = mock.getReceivedExchanges().get(1).getIn().getBody(List.class);
        assertNotNull(list);
        assertEquals(1, list.size());

        pojo = (TestPojo) list.get(0);
        assertEquals("Camel", pojo.getName());

    }

    @SuppressWarnings("rawtypes")
    @Path("jacksonxml/unmarshal-listsplit")
    @POST
    @Consumes(MediaType.TEXT_XML)
    @Produces(MediaType.APPLICATION_JSON)
    public List jacksonXmlUnmarshalListSplit(String body) {

        return producerTemplate
                .requestBody("direct:jacksonxml-unmarshal-listsplit", body, List.class);

    }

    @Path("jacksonxml/marshal-includedefault")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String jacksonXmlMarshalIncludeDefalut() throws Exception {
        TestOtherPojo pojo = new TestOtherPojo();
        pojo.setName("Camel");
        return producerTemplate.requestBody("direct:jacksonxml-marshal-includedefault",
                pojo,
                String.class);
    }

    @Path("jacksonxml/marshal-contenttype-header")
    @GET
    public void jacksonXmlMarshalContentTypeHeader() throws Exception {
        final Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        Exchange out = producerTemplate.request("direct:jacksonxml-marshal-ct-yes", exchange -> exchange.getIn().setBody(in));

        assertNotNull(out);
        assertTrue(out.hasOut());
        assertEquals("application/xml", out.getMessage().getHeader(Exchange.CONTENT_TYPE));
        out = producerTemplate.request("direct:jacksonxml-marshal-ct-yes2", exchange -> exchange.getIn().setBody(in));

        assertNotNull(out);
        assertTrue(out.hasOut());
        assertEquals("application/xml", out.getMessage().getHeader(Exchange.CONTENT_TYPE));

        out = producerTemplate.request("direct:jacksonxml-marshal-ct-no", exchange -> exchange.getIn().setBody(in));

        assertNotNull(out);
        assertTrue(out.hasOut());
        assertNull(out.getMessage().getHeader(Exchange.CONTENT_TYPE));
    }

    @Path("jacksonxml/marshal-general")
    @GET
    public void jacksonXmlMarshalGeneral() throws Exception {
        Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        MockEndpoint mock = context.getEndpoint("mock:reverse", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(Map.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = producerTemplate.requestBody("direct:jacksonxml-marshal-in", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("<HashMap><name>Camel</name></HashMap>", marshalledAsString);

        producerTemplate.sendBody("direct:jacksonxml-marshal-back", marshalled);

        mock.assertIsSatisfied();

        mock.expectedMessageCount(2);
        mock.message(1).body().isInstanceOf(Map.class);
        mock.message(1).body().isEqualTo(in);
        marshalled = producerTemplate.requestBody("direct:jacksonxml-marshal-inPretty", in);
        marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        String expected = "<HashMap>" + LS + "  <name>Camel</name>" + LS + "</HashMap>" + LS;
        assertEquals(expected, marshalledAsString);

        producerTemplate.sendBody("direct:jacksonxml-marshal-backPretty", marshalled);

        mock.assertIsSatisfied();

        TestPojo pojo = new TestPojo();
        pojo.setName("Camel");

        mock = context.getEndpoint("mock:reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestPojo.class);
        mock.message(0).body().isEqualTo(pojo);

        marshalled = producerTemplate.requestBody("direct:jacksonxml-marshal-inPojo", pojo);
        marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("<TestPojo><name>Camel</name></TestPojo>", marshalledAsString);

        producerTemplate.sendBody("direct:jacksonxml-marshal-backPojo", marshalled);

        mock.assertIsSatisfied();
    }

    @Path("jacksonxml/marshal-allowjmstype")
    @GET
    public void jacksonXmlMarshalAllowJmsType() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:allowjmstype-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        //mock.message(0).body().isInstanceOf(TestPojo.class);

        String json = "<pojo name=\"Camel\"/>";
        producerTemplate.sendBodyAndHeader("direct:jacksonxml-marshal-allowjmstype-backPojo", json, "JMSType",
                TestPojo.class.getName());

        mock.assertIsSatisfied();

        TestPojo pojo = mock.getReceivedExchanges().get(0).getIn().getBody(TestPojo.class);
        assertNotNull(pojo);
        assertEquals("Camel", pojo.getName());
    }

    @Path("jacksonxml/marshal-module")
    @GET
    public void jacksonXmlMarshalModule() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jacksonxml-marshal-module", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body(String.class)
                .isEqualTo("<TestOtherPojo><my-name>Camel</my-name><my-country>Denmark</my-country></TestOtherPojo>");

        TestOtherPojo pojo = new TestOtherPojo();
        pojo.setName("Camel");
        pojo.setCountry("Denmark");

        producerTemplate.sendBody("direct:jacksonxml-marshal-module", pojo);

        mock.assertIsSatisfied();
    }

    @Path("jacksonxml/unmarshal-springlist")
    @GET
    public void jacksonXmlUnmarshalSpringList() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jacksonxml-unmarshal-spring-list-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(List.class);

        String json = "<list><pojo name=\"Camel\"/><pojo name=\"World\"/></list>";
        producerTemplate.sendBody("direct:jacksonxml-unmarshal-spring-list-backPojo", json);

        mock.assertIsSatisfied();

        List list = mock.getReceivedExchanges().get(0).getIn().getBody(List.class);
        assertNotNull(list);
        assertEquals(2, list.size());

        TestPojo pojo = (TestPojo) list.get(0);
        assertEquals("Camel", pojo.getName());
        pojo = (TestPojo) list.get(1);
        assertEquals("World", pojo.getName());

        mock.expectedMessageCount(2);
        mock.message(1).body().isInstanceOf(List.class);

        json = "<list><pojo name=\"Camel\"/></list>";
        producerTemplate.sendBody("direct:jacksonxml-unmarshal-spring-list-backPojo", json);

        mock.assertIsSatisfied();

        list = mock.getReceivedExchanges().get(1).getIn().getBody(List.class);
        assertNotNull(list);
        assertEquals(1, list.size());

        pojo = (TestPojo) list.get(0);
        assertEquals("Camel", pojo.getName());
    }

    @Path("jacksonxml/marshal-spring-enablefeature")
    @GET
    public void jacksonXmlMarshalSpringEnableFeature() throws Exception {
        TestPojoView in = new TestPojoView();

        Object marshalled = producerTemplate.requestBody("direct:jacksonxml-marshal-spring-enablefeature", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        // we enable the wrap root type feature so we should have TestPojoView
        assertEquals("<TestPojoView><age>30</age><height>190</height><weight>70</weight></TestPojoView>", marshalledAsString);
    }

    @Path("jacksonxml/marshal-concurrent")
    @GET
    public void jacksonXmlMarshalConcurrent() throws Exception {
        doSendMessages(10, 5);
    }

    @Path("jacksonxml/marshal-conversion")
    @GET
    public void jacksonXmlMarshalConversion() throws Exception {
        synchronized (context) {
            String original = context.getGlobalOptions().get(JacksonXMLConstants.ENABLE_TYPE_CONVERTER);
            try {
                context.getGlobalOptions().put(JacksonXMLConstants.ENABLE_TYPE_CONVERTER, "true");
                String name = "someName";
                Map<String, String> pojoAsMap = new HashMap<>();
                pojoAsMap.put("name", name);

                TestPojo testPojo = (TestPojo) producerTemplate
                        .requestBody("direct:jacksonxml-marshal-conversion", pojoAsMap);

                assertEquals(name, testPojo.getName());
            } finally {
                context.getGlobalOptions().put(JacksonXMLConstants.ENABLE_TYPE_CONVERTER, original);
            }
        }
    }

    @Path("jacksonxml/unmarshal-listjackson")
    @GET
    public void jacksonXmlUnmarshalListJackon() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jacksonxml-unmarshal-listjackson", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(List.class);

        String json = "<list><pojo name=\"Camel\"/><pojo name=\"World\"/></list>";
        producerTemplate.sendBody("direct:jacksonxml-unmarshal-listjackson", json);

        mock.assertIsSatisfied();

        List list = mock.getReceivedExchanges().get(0).getIn().getBody(List.class);
        assertNotNull(list);
        assertEquals(2, list.size());

        TestPojo pojo = (TestPojo) list.get(0);
        assertEquals("Camel", pojo.getName());
        pojo = (TestPojo) list.get(1);
        assertEquals("World", pojo.getName());

        mock.expectedMessageCount(2);
        mock.message(1).body().isInstanceOf(List.class);

        json = "<list><pojo name=\"Camel\"/></list>";
        producerTemplate.sendBody("direct:jacksonxml-unmarshal-listjackson", json);

        mock.assertIsSatisfied();

        list = mock.getReceivedExchanges().get(1).getIn().getBody(List.class);
        assertNotNull(list);
        assertEquals(1, list.size());

        pojo = (TestPojo) list.get(0);
        assertEquals("Camel", pojo.getName());
    }

    @Path("jacksonxml/springjackson")
    @GET
    public void jacksonXmlSpringJackson() throws Exception {
        Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        MockEndpoint mock = context.getEndpoint("mock:jacksonxml-xml-reverse", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(Map.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = producerTemplate.requestBody("direct:jacksonxml-xml-in", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("<HashMap><name>Camel</name></HashMap>", marshalledAsString);

        producerTemplate.sendBody("direct:jacksonxml-xml-back", marshalled);

        mock.assertIsSatisfied();

        mock.expectedMessageCount(2);
        mock.message(1).body().isInstanceOf(Map.class);
        mock.message(1).body().isEqualTo(in);

        marshalled = producerTemplate.requestBody("direct:jacksonxml-xml-inPretty", in);
        marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        String expected = "<HashMap>" + LS + "  <name>Camel</name>" + LS + "</HashMap>" + LS;
        assertEquals(expected, marshalledAsString);

        producerTemplate.sendBody("direct:jacksonxml-xml-back", marshalled);

        mock.assertIsSatisfied();

        mock = context.getEndpoint("mock:jacksonxml-xml-reversePojo", MockEndpoint.class);
        TestPojo pojo = new TestPojo();
        pojo.setName("Camel");

        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestPojo.class);
        mock.message(0).body().isEqualTo(pojo);

        marshalled = producerTemplate.requestBody("direct:jacksonxml-xml-inPojo", pojo);
        marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("<TestPojo><name>Camel</name></TestPojo>", marshalledAsString);

        producerTemplate.sendBody("direct:jacksonxml-xml-backPojo", marshalled);

        mock.assertIsSatisfied();

        TestPojoView view = new TestPojoView();

        mock = context.getEndpoint("mock:jacksonxml-xml-reverseAgeView", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestPojoView.class);
        mock.message(0).body().isEqualTo(view);

        marshalled = producerTemplate.requestBody("direct:jacksonxml-xml-inAgeView", view);
        marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        /*JsonView doesn't work correctly in native mode, need to investigate more*/
        //assertEquals("<TestPojoView><age>30</age><height>190</height></TestPojoView>", marshalledAsString);

        producerTemplate.sendBody("direct:jacksonxml-xml-backAgeView", marshalled);

        mock.assertIsSatisfied();
    }

    @Path("jacksonxml/jackson-conversion")
    @GET
    public void jacksonXmlJacksonConversion() throws Exception {
        synchronized (context) {
            String original = context.getGlobalOptions().get(JacksonXMLConstants.ENABLE_TYPE_CONVERTER);
            try {
                Exchange exchange = new DefaultExchange(context);
                context.getGlobalOptions().put(JacksonXMLConstants.ENABLE_TYPE_CONVERTER, "true");
                Map<String, String> body = new HashMap<>();
                Object convertedObject = context.getTypeConverter().convertTo(String.class, exchange, body);
                // will do a toString which is an empty map
                assertEquals(body.toString(), convertedObject);

                convertedObject = context.getTypeConverter().convertTo(Long.class, exchange,
                        new HashMap<String, String>());
                assertNull(convertedObject);

                convertedObject = context.getTypeConverter().convertTo(long.class, exchange,
                        new HashMap<String, String>());
                assertNull(convertedObject);
            } finally {
                context.getGlobalOptions().put(JacksonXMLConstants.ENABLE_TYPE_CONVERTER, original);
            }
        }
    }

    @Path("jacksonxml/jaxb-annotation")
    @GET
    public void jacksonXmlJaxbAnnotation() throws Exception {
        TestJAXBPojo in = new TestJAXBPojo();
        in.setName("Camel");

        MockEndpoint mock = context.getEndpoint("mock:jacksonxml-jaxbannotation-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestJAXBPojo.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = producerTemplate.requestBody("direct:jacksonxml-jaxbannotation-inPojo", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("<XMLPojo><PojoName>Camel</PojoName></XMLPojo>", marshalledAsString);

        producerTemplate.sendBody("direct:jacksonxml-jaxbannotation-backPojo", marshalled);

        mock.assertIsSatisfied();
    }

    @Path("jacksonxml/jsonview")
    @GET
    public void jacksonXmlJsonview() throws Exception {
        TestPojoView in = new TestPojoView();

        MockEndpoint mock = context.getEndpoint("mock:jacksonxml-jsonview-reversePojoAgeView", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestPojoView.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = producerTemplate.requestBody("direct:jacksonxml-jsonview-inPojoAgeView", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        /*JsonView doesn't work correctly in native mode, need to investigate more*/
        //assertEquals("<TestPojoView><age>30</age><height>190</height></TestPojoView>", marshalledAsString);

        producerTemplate.sendBody("direct:jacksonxml-jsonview-backPojoAgeView", marshalled);

        mock.assertIsSatisfied();

        mock = context.getEndpoint("mock:jacksonxml-jsonview-reversePojoWeightView", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestPojoView.class);
        mock.message(0).body().isEqualTo(in);

        marshalled = producerTemplate.requestBody("direct:jacksonxml-jsonview-inPojoWeightView", in);
        marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);

        /*JsonView doesn't work correctly in native mode, need to investigate more*/
        //assertEquals("<TestPojoView><height>190</height><weight>70</weight></TestPojoView>", marshalledAsString);

        producerTemplate.sendBody("direct:jacksonxml-jsonview-backPojoWeightView", marshalled);

        mock.assertIsSatisfied();
    }

    @Path("jacksonxml/moduleref")
    @GET
    public void jacksonXmlModuleRef() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jacksonxml-moduleref-marshal", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body(String.class)
                .isEqualTo("<TestOtherPojo><my-name>Camel</my-name><my-country>Denmark</my-country></TestOtherPojo>");

        TestOtherPojo pojo = new TestOtherPojo();
        pojo.setName("Camel");
        pojo.setCountry("Denmark");

        producerTemplate.sendBody("direct:jacksonxml-moduleref-marshal", pojo);

        mock.assertIsSatisfied();
    }

    @Path("jacksonxml/include-no-null")
    @GET
    public void jacksonXmlIncludeNoNull() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jacksonxml-include-non-null-marshal", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body(String.class).isEqualTo("<TestOtherPojo><name>Camel</name></TestOtherPojo>");

        TestOtherPojo pojo = new TestOtherPojo();
        pojo.setName("Camel");

        producerTemplate.sendBody("direct:jacksonxml-include-non-null-marshal", pojo);

        mock.assertIsSatisfied();
    }

    @Path("jacksonxml/typeheader-not-allowed")
    @GET
    public void jacksonXmlTypeHeaderNotAllowed() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jacksonxml-typeheader-not-allowed-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);

        String json = "<pojo name=\"Camel\"/>";
        producerTemplate.sendBodyAndHeader("direct:jacksonxml-typeheader-not-allowed-backPojo", json,
                JacksonXMLConstants.UNMARSHAL_TYPE, TestPojo.class.getName());

        mock.assertIsSatisfied();

    }

    @Path("jacksonxml/datetimezone")
    @GET
    public void jacksonXmlDatetimezone() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jacksonxml-datatimezone-result", MockEndpoint.class);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        GregorianCalendar in = new GregorianCalendar(2017, Calendar.APRIL, 25, 17, 0, 10);

        Object marshalled = producerTemplate.requestBody("direct:jacksonxml-datatimezone-in", in.getTime());
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("<Date>1493139610000</Date>", marshalledAsString);

        mock.expectedMessageCount(1);

        mock.assertIsSatisfied();

    }

    private void doSendMessages(int files, int poolSize) throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jacksonxml-marshal-concurrent-result", MockEndpoint.class);
        mock.expectedMessageCount(files);
        mock.assertNoDuplicates(org.apache.camel.builder.Builder.body());

        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        for (int i = 0; i < files; i++) {
            final int index = i;
            executor.submit(new Callable<Object>() {
                public Object call() throws Exception {
                    TestPojo pojo = new TestPojo();
                    pojo.setName("Hi " + index);

                    producerTemplate.sendBody("direct:jacksonxml-marshal-concurrent-start", pojo);
                    return null;
                }
            });
        }

        mock.assertIsSatisfied();
        executor.shutdownNow();
    }

}
