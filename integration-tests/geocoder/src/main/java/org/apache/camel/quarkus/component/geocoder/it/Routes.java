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
package org.apache.camel.quarkus.component.geocoder.it;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import io.quarkus.arc.Unremovable;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.geocoder.GeoCoderComponent;
import org.apache.camel.quarkus.test.mock.backend.MockBackendUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Routes extends RouteBuilder {

    @ConfigProperty(name = "google.api.key")
    String googleApiKey;
    @ConfigProperty(name = "quarkus.http.test-port")
    int httpTestPort;
    @ConfigProperty(name = "quarkus.http.port")
    int httpPort;
    @Inject
    MockApiService mockApiService;

    private String getBaseUri() {
        final boolean isNativeMode = "executable".equals(System.getProperty("org.graalvm.nativeimage.kind"));
        return "AIzaFakeKey".equals(googleApiKey)
                ? "http://localhost:" + (isNativeMode ? httpPort : httpTestPort)
                : "https://maps.googleapis.com";
    }

    /**
     * We need to implement some conditional configuration of the {@link GeoCoderComponent} thus we create it
     * programmatically and publish via CDI.
     *
     * @return a configured {@link GeoCoderComponent}
     */
    @Produces
    @ApplicationScoped
    @Unremovable
    @Named("geocoder")
    GeoCoderComponent geocoderComponent() throws IllegalAccessException, NoSuchFieldException, InstantiationException {
        final GeoCoderComponent result = new GeoCoderComponent();
        result.setCamelContext(getContext());
        result.setGeoApiContext(mockApiService.createGeoApiContext(getBaseUri(), googleApiKey));
        return result;
    }

    @Override
    public void configure() throws Exception {
        if (MockBackendUtils.startMockBackend(true)) {
            from("platform-http:///maps/api/geocode/json?httpMethodRestrict=GET")
                    .process(e -> load(createMockGeocodeResponse(), e));
            from("platform-http:///geolocation/v1/geolocate?httpMethodRestrict=POST")
                    .process(e -> load(createMockGeolocateResponse(), e));
        }
    }

    private String createMockGeolocateResponse() {
        return "{\n" +
                "  \"location\": {\n" +
                "    \"lat\": 71.5388001,\n" +
                "    \"lng\": -66.885417\n" +
                "  },\n" +
                "  \"accuracy\": 578963\n" +
                "} ";
    }

    private String createMockGeocodeResponse() {
        return "{\n"
                + "   \"results\" : [\n"
                + "      {\n"
                + "         \"address_components\" : [\n"
                + "            {\n"
                + "               \"long_name\" : \"1600\",\n"
                + "               \"short_name\" : \"1600\",\n"
                + "               \"types\" : [ \"street_number\" ]\n"
                + "            }\n"
                + "         ],\n"
                + "         \"formatted_address\" : \"1600 Amphitheatre Parkway, Mountain View, "
                + "CA 94043, USA\",\n"
                + "         \"geometry\" : {\n"
                + "            \"location\" : {\n"
                + "               \"lat\" : 37.4220033,\n"
                + "               \"lng\" : -122.0839778\n"
                + "            },\n"
                + "            \"location_type\" : \"ROOFTOP\",\n"
                + "            \"viewport\" : {\n"
                + "               \"northeast\" : {\n"
                + "                  \"lat\" : 37.4233522802915,\n"
                + "                  \"lng\" : -122.0826288197085\n"
                + "               },\n"
                + "               \"southwest\" : {\n"
                + "                  \"lat\" : 37.4206543197085,\n"
                + "                  \"lng\" : -122.0853267802915\n"
                + "               }\n"
                + "            }\n"
                + "         },\n"
                + "         \"types\" : [ \"street_address\" ]\n"
                + "      }\n"
                + "   ],\n"
                + "   \"status\" : \"OK\"\n"
                + "}";

    }

    private void load(String response, Exchange exchange) {
        exchange.getMessage().setBody(response);
    }

}
