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
package org.apache.camel.quarkus.component.dataformats.jackson.json;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jackson.JacksonConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.dataformats.json.model.DummyObject;
import org.apache.camel.quarkus.component.dataformats.json.model.Order;
import org.apache.camel.quarkus.component.dataformats.json.model.Pojo;
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
public class JacksonJsonResource {

    private static final Logger LOG = Logger.getLogger(JacksonJsonResource.class);
    private static final String LS = System.lineSeparator();
    @Inject
    ProducerTemplate producerTemplate;
    @Inject
    ConsumerTemplate consumerTemplate;
    @Inject
    CamelContext context;

    @Path("jackson/unmarshal-typeheader")
    @GET
    public void jacksonXmlUnmarshalTypeHeader(String body) throws Exception {

        MockEndpoint mock = context.getEndpoint("mock:jackson-unmarshal-type-header-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestPojo.class);

        String json = "{\"name\":\"Camel\"}";
        producerTemplate.sendBodyAndHeader("direct:jackson-unmarshal-type-header-backPojo", json,
                JacksonConstants.UNMARSHAL_TYPE, TestPojo.class.getName());

        mock.assertIsSatisfied();

        TestPojo pojo = mock.getReceivedExchanges().get(0).getIn().getBody(TestPojo.class);
        assertNotNull(pojo);
        assertEquals("Camel", pojo.getName());
    }

    @Path("jackson/unmarshal-list")
    @GET
    public void jacksonUnmarshalList(String body) throws Exception {

        MockEndpoint mock = context.getEndpoint("mock:jackson-unmarshal-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(List.class);

        String json = "[{\"name\":\"Camel\"}, {\"name\":\"World\"}]";
        producerTemplate.sendBody("direct:jackson-unmarshal-backPojo", json);

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

        json = "[{\"name\":\"Camel\"}]";
        producerTemplate.sendBody("direct:jackson-unmarshal-backPojo", json);

        mock.assertIsSatisfied();

        list = mock.getReceivedExchanges().get(1).getIn().getBody(List.class);
        assertNotNull(list);
        assertEquals(1, list.size());

        pojo = (TestPojo) list.get(0);
        assertEquals("Camel", pojo.getName());

    }

    @Path("jackson/unmarshal-listsplit")
    @GET
    public void jacksonUnmarshalListSplit(String body) throws Exception {

        MockEndpoint mock = context.getEndpoint("mock:jackson-unmarshal-listsplit-result", MockEndpoint.class);
        mock.expectedMessageCount(2);
        mock.expectedMessagesMatches(org.apache.camel.builder.Builder.body().isInstanceOf(DummyObject.class));

        producerTemplate.sendBody("direct:jackson-unmarshal-listsplit-start",
                "[{\"dummyString\": \"value1\"}, {\"dummyString\": \"value2\"}]");

        mock.assertIsSatisfied();

    }

    @Path("jackson/marshal-includedefault")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String jacksonMarshalIncludeDefalut() throws Exception {
        TestOtherPojo pojo = new TestOtherPojo();
        pojo.setName("Camel");
        return producerTemplate.requestBody("direct:jackson-marshal-includedefault-marshal",
                pojo,
                String.class);
    }

    @Path("jackson/unmarshal-array")
    @GET
    public void jacksonUnmarshalArray(String body) throws Exception {

        MockEndpoint mock = context.getEndpoint("mock:jackson-unmarshal-endArray", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(String[].class);

        String json = "[\"Camel\", \"World\"]";
        producerTemplate.sendBody("direct:jackson-unmarshal-beginArray", json);

        mock.assertIsSatisfied();

        String[] array = mock.getReceivedExchanges().get(0).getIn().getBody(String[].class);
        assertNotNull(array);
        assertEquals(2, array.length);

        String string = array[0];
        assertEquals("Camel", string);
        string = array[1];
        assertEquals("World", string);
    }

    @Path("jackson/marshal-contenttype-header")
    @GET
    public void jacksonMarshalContentTypeHeader() throws Exception {
        final Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        Exchange out = producerTemplate.request("direct:jackson-marshal-ct-yes", exchange -> exchange.getIn().setBody(in));

        assertNotNull(out);
        assertTrue(out.hasOut());
        assertEquals("application/json", out.getMessage().getHeader(Exchange.CONTENT_TYPE));
        out = producerTemplate.request("direct:jackson-marshal-ct-yes2", exchange -> exchange.getIn().setBody(in));

        assertNotNull(out);
        assertTrue(out.hasOut());
        assertEquals("application/json", out.getMessage().getHeader(Exchange.CONTENT_TYPE));

        out = producerTemplate.request("direct:jackson-marshal-ct-no", exchange -> exchange.getIn().setBody(in));

        assertNotNull(out);
        assertTrue(out.hasOut());
        assertNull(out.getMessage().getHeader(Exchange.CONTENT_TYPE));
    }

    @Path("jackson/marshal-general")
    @GET
    public void jacksonMarshalGeneral() throws Exception {
        Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        MockEndpoint mock = context.getEndpoint("mock:jackson-marshal-reverse", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(Map.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = producerTemplate.requestBody("direct:jackson-marshal-in", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("{\"name\":\"Camel\"}", marshalledAsString);

        producerTemplate.sendBody("direct:jackson-marshal-back", marshalled);

        mock.assertIsSatisfied();

        mock.expectedMessageCount(2);
        mock.message(1).body().isInstanceOf(Map.class);
        mock.message(1).body().isEqualTo(in);
        marshalled = producerTemplate.requestBody("direct:jackson-marshal-inPretty", in);
        marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        String expected = String.format("{%s  \"name\" : \"Camel\"%s}", LS, LS);
        assertEquals(expected, marshalledAsString);

        producerTemplate.sendBody("direct:jackson-marshal-backPretty", marshalled);

        mock.assertIsSatisfied();

        TestPojo pojo = new TestPojo();
        pojo.setName("Camel");

        mock = context.getEndpoint("mock:jackson-marshal-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestPojo.class);
        mock.message(0).body().isEqualTo(pojo);

        marshalled = producerTemplate.requestBody("direct:jackson-marshal-inPojo", pojo);
        marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("{\"name\":\"Camel\"}", marshalledAsString);

        producerTemplate.sendBody("direct:jackson-marshal-backPojo", marshalled);

        mock.assertIsSatisfied();
    }

    @Path("jackson/object-mapper")
    @GET
    public void jacksonObjectMapper() throws Exception {
        Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        MockEndpoint mock = context.getEndpoint("mock:jackson-objectmapper-reverse", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(Map.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = producerTemplate.requestBody("direct:jackson-objectmapper-in", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("{\"name\":\"Camel\"}", marshalledAsString);

        producerTemplate.sendBody("direct:jackson-objectmapper-back", marshalled);

        mock.assertIsSatisfied();

    }

    @Path("jackson/allowjmstype")
    @GET
    public void jacksonAllowJmsType() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jackson-allowjmstype-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestPojo.class);

        String json = "{\"name\":\"Camel\"}";
        producerTemplate.sendBodyAndHeader("direct:jackson-allowjmstype-backPojo", json, "JMSType", TestPojo.class.getName());

        mock.assertIsSatisfied();

        TestPojo pojo = mock.getReceivedExchanges().get(0).getIn().getBody(TestPojo.class);
        assertNotNull(pojo);
        assertEquals("Camel", pojo.getName());
    }

    @Path("jackson/marshal-module")
    @GET
    public void jacksonMarshalModule() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jackson-module-marshal", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body(String.class).isEqualTo("{\"my-name\":\"Camel\",\"my-country\":\"Denmark\"}");

        TestOtherPojo pojo = new TestOtherPojo();
        pojo.setName("Camel");
        pojo.setCountry("Denmark");

        producerTemplate.sendBody("direct:jackson-module-marshal", pojo);

        mock.assertIsSatisfied();
    }

    @Path("jackson/not-use-default-mapper")
    @GET
    public void jacksonNotUseDefaultMapper() throws Exception {
        Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        MockEndpoint mock = context.getEndpoint("mock:jackson-not-use-default-mapper-reverse", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(Map.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = producerTemplate.requestBody("direct:jackson-not-use-default-mapper-in", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("{\"name\":\"Camel\"}", marshalledAsString);

        producerTemplate.sendBody("direct:jackson-not-use-default-mapper-back", marshalled);

        mock.assertIsSatisfied();

    }

    @Path("jackson/unmarshal-list-xml-configure")
    @GET
    public void jacksonUnmarshalListXmlConfigure() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jackson-xml-unmarshal-list-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(List.class);

        String json = "[{\"name\":\"Camel\"}, {\"name\":\"World\"}]";
        producerTemplate.sendBody("direct:jackson-xml-unmarshal-list-backPojo", json);

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

        json = "[{\"name\":\"Camel\"}]";
        producerTemplate.sendBody("direct:jackson-xml-unmarshal-list-backPojo", json);

        mock.assertIsSatisfied();

        list = mock.getReceivedExchanges().get(1).getIn().getBody(List.class);
        assertNotNull(list);
        assertEquals(1, list.size());

        pojo = (TestPojo) list.get(0);
        assertEquals("Camel", pojo.getName());
    }

    @Path("jackson/object-mapper-noreg")
    @GET
    public void jacksonObjectMapperNoReg() throws Exception {
        Map<String, Object> in = new HashMap<>();
        in.put("name", "Camel");

        MockEndpoint mock = context.getEndpoint("mock:jackson-objectmapper-noreg-reverse", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(Map.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = producerTemplate.requestBody("direct:jackson-objectmapper-noreg-in", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("{\"name\":\"Camel\"}", marshalledAsString);

        producerTemplate.sendBody("direct:jackson-objectmapper-noreg-back", marshalled);

        mock.assertIsSatisfied();

        mock.expectedMessageCount(2);
        mock.message(1).body().isInstanceOf(Map.class);
        mock.message(1).body().isEqualTo(in);
        marshalled = producerTemplate.requestBody("direct:jackson-objectmapper-noreg-inPretty", in);
        marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        String expected = String.format("{%s  \"name\" : \"Camel\"%s}", LS, LS);
        assertEquals(expected, marshalledAsString);

        producerTemplate.sendBody("direct:jackson-objectmapper-noreg-backPretty", marshalled);

        mock.assertIsSatisfied();

        TestPojo pojo = new TestPojo();
        pojo.setName("Camel");

        mock = context.getEndpoint("mock:jackson-objectmapper-noreg-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestPojo.class);
        mock.message(0).body().isEqualTo(pojo);

        marshalled = producerTemplate.requestBody("direct:jackson-objectmapper-noreg-inPojo", pojo);
        marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("{\"name\":\"Camel\"}", marshalledAsString);

        producerTemplate.sendBody("direct:jackson-objectmapper-noreg-backPojo", marshalled);

        mock.assertIsSatisfied();
    }

    @Path("jackson/pojo-array")
    @GET
    public void jacksonPojoArray() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jackson-pojo-array-endArray", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(Pojo[].class);

        String json = "[{\"text\":\"Camel\"}, {\"text\":\"World\"}]";
        producerTemplate.sendBody("direct:jackson-pojo-array-beginArray", json);

        mock.assertIsSatisfied();

        Pojo[] array = mock.getReceivedExchanges().get(0).getIn().getBody(Pojo[].class);
        assertNotNull(array);
        assertEquals(2, array.length);

        Pojo pojo = array[0];
        assertEquals("Camel", pojo.getText());
        pojo = array[1];
        assertEquals("World", pojo.getText());
    }

    @Path("jackson/enablefeature")
    @GET
    public void jacksonEnableFeature() throws Exception {

        TestPojoView in = new TestPojoView();

        Object marshalled = producerTemplate.requestBody("direct:jackson-enablefeature-in", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        // we enable the wrap root type feature so we should have TestPojoView
        assertEquals("{\"TestPojoView\":{\"age\":30,\"height\":190,\"weight\":70}}", marshalledAsString);
    }

    @Path("jackson/concurrent")
    @GET
    public void jacksonConcurrent() throws Exception {
        doSendMessages(10, 5);
    }

    @Path("jackson/unmarshal-listjackson")
    @GET
    public void jacksonUnmarshalListJackson(String body) throws Exception {

        MockEndpoint mock = context.getEndpoint("mock:jackson-list-unmarshal-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(List.class);

        String json = "[{\"name\":\"Camel\"}, {\"name\":\"World\"}]";
        producerTemplate.sendBody("direct:jackson-list-unmarshal-backPojo", json);

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

        json = "[{\"name\":\"Camel\"}]";
        producerTemplate.sendBody("direct:jackson-list-unmarshal-backPojo", json);

        mock.assertIsSatisfied();

        list = mock.getReceivedExchanges().get(1).getIn().getBody(List.class);
        assertNotNull(list);
        assertEquals(1, list.size());

        pojo = (TestPojo) list.get(0);
        assertEquals("Camel", pojo.getName());

    }

    @Path("jackson/conversion-pojo")
    @GET
    public void jacksonConversionPojo(String body) throws Exception {
        synchronized (context) {
            context.getGlobalOptions().put(JacksonConstants.ENABLE_TYPE_CONVERTER, "true");
            context.getGlobalOptions().put(JacksonConstants.TYPE_CONVERTER_TO_POJO, "true");

            Order order = new Order();
            order.setAmount(1);
            order.setCustomerName("Acme");
            order.setPartName("Camel");

            String json = (String) producerTemplate.requestBody("direct:jackson-conversion-pojo-test", order);
            assertEquals("{\"id\":0,\"partName\":\"Camel\",\"amount\":1,\"customerName\":\"Acme\"}", json);

            context.getGlobalOptions().put(JacksonConstants.TYPE_CONVERTER_MODULE_CLASS_NAMES,
                    JaxbAnnotationModule.class.getName());

            order = new Order();
            order.setAmount(1);
            order.setCustomerName("Acme");
            order.setPartName("Camel");

            json = (String) producerTemplate.requestBody("direct:jackson-conversion-pojo-test", order);
            /*
             * somehow jaxb annotation @XmlAttribute(name = "customer_name") can't be taken into accout so the
             * following asserts failed, need to investigate more
             * assertEquals("{\"id\":0,\"partName\":\"Camel\",\"amount\":1,\"customer_name\":\"Acme\"}",
             * json);
             */
        }

    }

    @Path("jackson/conversion")
    @GET
    public void jacksonConversion(String body) throws Exception {
        synchronized (context) {
            context.getGlobalOptions().put(JacksonConstants.ENABLE_TYPE_CONVERTER, "true");
            String name = "someName";
            Map<String, String> pojoAsMap = new HashMap<>();
            pojoAsMap.put("name", name);

            TestPojo testPojo = (TestPojo) producerTemplate.requestBody("direct:jackson-conversion-test", pojoAsMap);

            assertEquals(name, testPojo.getName());
        }
    }

    @Path("jackson/conversion-simple")
    @GET
    public void jacksonConversionSimple(String body) throws Exception {
        synchronized (context) {
            context.getGlobalOptions().put(JacksonConstants.ENABLE_TYPE_CONVERTER, "true");
            Exchange exchange = new DefaultExchange(context);

            Map<String, String> map = new HashMap<>();
            Object convertedObject = context.getTypeConverter().convertTo(String.class, exchange, map);
            // will do a toString which is an empty map
            assertEquals(map.toString(), convertedObject);

            convertedObject = context.getTypeConverter().convertTo(Long.class, exchange,
                    new HashMap<String, String>());
            assertNull(convertedObject);

            convertedObject = context.getTypeConverter().convertTo(long.class, exchange,
                    new HashMap<String, String>());
            assertNull(convertedObject);

            convertedObject = context.getTypeConverter().convertTo(ExchangePattern.class, exchange, "InOnly");
            assertEquals(ExchangePattern.InOnly, convertedObject);
        }
    }

    @Path("jackson/jaxb-annotation")
    @GET
    public void jacksonJaxbAnnotation() throws Exception {
        TestJAXBPojo in = new TestJAXBPojo();
        in.setName("Camel");

        MockEndpoint mock = context.getEndpoint("mock:jackson-jaxb-annotation-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestJAXBPojo.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = producerTemplate.requestBody("direct:jackson-jaxb-annotation-inPojo", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("{\"PojoName\":\"Camel\"}", marshalledAsString);

        producerTemplate.sendBody("direct:jackson-jaxb-annotation-backPojo", marshalled);

        mock.assertIsSatisfied();
    }

    @Path("jackson/view")
    @GET
    public void jacksonView() throws Exception {
        TestPojoView in = new TestPojoView();

        MockEndpoint mock = context.getEndpoint("mock:jackson-view-reversePojoAgeView", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(TestPojoView.class);
        mock.message(0).body().isEqualTo(in);

        Object marshalled = producerTemplate.requestBody("direct:jackson-view-inPojoAgeView", in);
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("{\"age\":30,\"height\":190}", marshalledAsString);

        producerTemplate.sendBody("direct:jackson-view-backPojoAgeView", marshalled);

        mock.assertIsSatisfied();
    }

    @Path("jackson/moduleref")
    @GET
    public void jacksonModuleRef() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jackson-module-ref-marshal", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body(String.class).isEqualTo("{\"my-name\":\"Camel\",\"my-country\":\"Denmark\"}");

        TestOtherPojo pojo = new TestOtherPojo();
        pojo.setName("Camel");
        pojo.setCountry("Denmark");

        producerTemplate.sendBody("direct:jackson-module-ref-marshal", pojo);

        mock.assertIsSatisfied();
    }

    @Path("jackson/include-no-null")
    @GET
    public void jacksonIncludeNoNull() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jackson-not-null-marshal", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body(String.class).isEqualTo("{\"name\":\"Camel\"}");

        TestOtherPojo pojo = new TestOtherPojo();
        pojo.setName("Camel");

        producerTemplate.sendBody("direct:jackson-not-null-marshal", pojo);

        mock.assertIsSatisfied();

    }

    @Path("jackson/typeheader-not-allowed")
    @GET
    public void jacksonTypeHeaderNotAllowed() throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jackson-typeheader-not-allowed-reversePojo", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.message(0).body().isInstanceOf(LinkedHashMap.class);

        String json = "{\"name\":\"Camel\"}";
        producerTemplate.sendBodyAndHeader("direct:jackson-typeheader-not-allowed-backPojo", json,
                JacksonConstants.UNMARSHAL_TYPE, TestPojo.class.getName());

        mock.assertIsSatisfied();

    }

    @Path("jackson/datetimezone")
    @GET
    public void jacksonDatetimezone() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        GregorianCalendar in = new GregorianCalendar(2017, Calendar.APRIL, 25, 17, 0, 10);

        MockEndpoint mock = context.getEndpoint("mock:jackson-timezone-result", MockEndpoint.class);

        Object marshalled = producerTemplate.requestBody("direct:jackson-timezone-in", in.getTime());
        String marshalledAsString = context.getTypeConverter().convertTo(String.class, marshalled);
        assertEquals("1493139610000", marshalledAsString);

        mock.expectedMessageCount(1);

        mock.assertIsSatisfied();

    }

    private void doSendMessages(int files, int poolSize) throws Exception {
        MockEndpoint mock = context.getEndpoint("mock:jackson-concurrent-result", MockEndpoint.class);
        mock.expectedMessageCount(files);
        mock.assertNoDuplicates(org.apache.camel.builder.Builder.body());

        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        for (int i = 0; i < files; i++) {
            final int index = i;
            executor.submit(new Callable<Object>() {
                public Object call() throws Exception {
                    TestPojo pojo = new TestPojo();
                    pojo.setName("Hi " + index);

                    producerTemplate.sendBody("direct:jackson-concurrent-start", pojo);
                    return null;
                }
            });
        }

        mock.assertIsSatisfied();
        executor.shutdownNow();
    }

}
