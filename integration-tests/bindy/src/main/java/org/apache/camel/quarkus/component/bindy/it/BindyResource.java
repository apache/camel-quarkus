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
package org.apache.camel.quarkus.component.bindy.it;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.bindy.it.model.CsvOrder;
import org.apache.camel.quarkus.component.bindy.it.model.FixedLengthOrder;
import org.apache.camel.quarkus.component.bindy.it.model.FixedLengthWithLocale;
import org.apache.camel.quarkus.component.bindy.it.model.Header;
import org.apache.camel.quarkus.component.bindy.it.model.MessageOrder;
import org.apache.camel.quarkus.component.bindy.it.model.NameWithLengthSuffix;
import org.apache.camel.quarkus.component.bindy.it.model.Security;
import org.apache.camel.quarkus.component.bindy.it.model.Trailer;
import org.jboss.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Path("/bindy")
@ApplicationScoped
public class BindyResource {

    private static final Logger LOG = Logger.getLogger(BindyResource.class);

    private static final String CSV = "bindy-order-name-16,BINDY-COUNTRY,fr,A1B2C3";
    private static final String FIXED_LENGTH_ORDER = "BobSpa\r\n";
    private static final String MESSAGE_ORDER = "1=BE.CHM.0018=BEGIN9=2010=22022=458=camel - quarkus - bindy test\r\n";

    @Inject
    ProducerTemplate template;

    @Path("/marshalCsvRecordShouldSucceed")
    @GET
    public void marshalCsvRecordShouldSucceed() {
        LOG.debugf("Invoking marshalCsvRecordShouldSucceed()");

        CsvOrder order = new CsvOrder();
        order.setNameWithLengthSuffix(NameWithLengthSuffix.ofString("bindy-order-name"));
        order.setCountry("bindy-country");
        order.setLanguage("fr");
        order.setClientReference("A1B2C3");

        String marshalled = template.requestBody("direct:marshal-csv-record", order, String.class);

        assertEquals(CSV, marshalled);
    }

    @Path("/unMarshalCsvRecordShouldSucceed")
    @GET
    public void unMarshalCsvRecordShouldSucceed() {
        LOG.debugf("Invoking unMarshalCsvRecordShouldSucceed()");

        CsvOrder order = template.requestBody("direct:unmarshal-csv-record", CSV, CsvOrder.class);

        assertNotNull(order);
        assertNotNull(order.getNameWithLengthSuffix());
        assertEquals("bindy-order-name-16-19", order.getNameWithLengthSuffix().toString());
        assertEquals("B_ND_-C__NTR_", order.getCountry());
        assertEquals("FR", order.getLanguage());
        assertEquals("A_B_C_", order.getClientReference());
    }

    @Path("/marshalFixedLengthRecordShouldSucceed")
    @GET
    public void marshalFixedLengthRecordShouldSucceed() {
        LOG.debugf("Invoking marshalFixedLengthRecordShouldSucceed()");

        FixedLengthOrder order = new FixedLengthOrder();
        order.setName("Bob");
        order.setCountry("Spa");

        String marshalled = template.requestBody("direct:marshal-fixed-length-record", order, String.class);

        assertEquals(FIXED_LENGTH_ORDER, marshalled);
    }

    @Path("/unMarshalFixedLengthRecordShouldSucceed")
    @GET
    public void unMarshalFixedLengthRecordShouldSucceed() {
        LOG.debugf("Invoking unMarshalFixedLengthRecordShouldSucceed()");

        String uri = "direct:unmarshal-fixed-length-record";
        FixedLengthOrder order = template.requestBody(uri, FIXED_LENGTH_ORDER, FixedLengthOrder.class);

        assertNotNull(order);
        assertEquals("Bob", order.getName());
        assertEquals("Spa", order.getCountry());
    }

    @Path("/marshalFixedLengthWithLocaleShouldSucceed")
    @GET
    public void marshalFixedLengthWithLocaleShouldSucceed() {
        LOG.debugf("Invoking marshalFixedLengthWithLocaleShouldSucceed()");

        FixedLengthWithLocale object = new FixedLengthWithLocale();
        object.setNumber(3.2);

        String marshalled = template.requestBody("direct:marshal-fixed-length-with-locale", object, String.class);

        assertEquals("3,200\r\n", marshalled);
    }

    @Path("/marshalMessageShouldSucceed")
    @GET
    public void marshalMessageShouldSucceed() {
        LOG.debugf("Invoking marshalMessageShouldSucceed()");

        MessageOrder order = new MessageOrder();
        order.setAccount("BE.CHM.001");
        order.setHeader(new Header());
        order.getHeader().setBeginString("BEGIN");
        order.getHeader().setBodyLength(20);
        order.setSecurities(new ArrayList<>());
        order.getSecurities().add(new Security());
        order.getSecurities().get(0).setIdSource("4");
        order.setText("camel - quarkus - bindy test");
        order.setTrailer(new Trailer());
        order.getTrailer().setCheckSum(220);
        Map<String, Object> model = new HashMap<>();
        model.put(MessageOrder.class.getName(), order);
        model.put(Header.class.getName(), order.getHeader());
        model.put(Trailer.class.getName(), order.getTrailer());
        model.put(Security.class.getName(), order.getSecurities().get(0));

        String marshalled = template.requestBody("direct:marshal-message", Arrays.asList(model), String.class);

        assertEquals(MESSAGE_ORDER, marshalled);
    }

    @Path("/unMarshalMessageShouldSucceed")
    @GET
    public void unMarshalMessageShouldSucceed() {
        LOG.debugf("Invoking unMarshalMessageShouldSucceed()");

        MessageOrder order = template.requestBody("direct:unmarshal-message", MESSAGE_ORDER, MessageOrder.class);

        assertNotNull(order);
        assertEquals("BE.CHM.001", order.getAccount());
        assertNotNull(order.getHeader());
        assertEquals("BEGIN", order.getHeader().getBeginString());
        assertEquals(20, order.getHeader().getBodyLength());
        assertNotNull(order.getSecurities());
        assertEquals(1, order.getSecurities().size());
        assertEquals("4", order.getSecurities().get(0).getIdSource());
        assertEquals("camel - quarkus - bindy test", order.getText());
        assertNotNull(order.getTrailer());
        assertEquals(220, order.getTrailer().getCheckSum());
    }
}
