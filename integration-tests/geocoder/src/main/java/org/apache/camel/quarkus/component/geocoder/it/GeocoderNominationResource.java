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

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.runtime.LaunchMode;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.geocoder.GeoCoderConstants;
import org.apache.camel.component.geocoder.GeocoderStatus;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/nomination")
@ApplicationScoped
public class GeocoderNominationResource {
    private static final Logger LOG = Logger.getLogger(GeocoderNominationResource.class);

    @ConfigProperty(name = "quarkus.http.test-port")
    Optional<Integer> httpTestPort;

    @ConfigProperty(name = "quarkus.http.port")
    Optional<Integer> httpPort;

    @Inject
    ProducerTemplate producerTemplate;

    @Path("address/{address}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GeocoderResult getByCurrentLocation(@PathParam("address") String address) throws Exception {
        LOG.infof("Retrieve info from address %s", address);
        Exchange result = producerTemplate.request("geocoder:address:" + address +
                "?type=NOMINATIM&serverUrl=RAW(" + getServerUrl() + ")", exchange -> {
                    exchange.getMessage().setBody("Hello Body");
                });
        return extractResult(result);
    }

    @Path("lat/{lat}/lon/{lon}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GeocoderResult getByCoordinate(@PathParam("lat") String latitude, @PathParam("lon") String longitude)
            throws Exception {
        LOG.infof("Retrieve info from georgraphic coordinates latitude : %s, longitude %s", latitude, longitude);
        Exchange result = producerTemplate.request("geocoder:latlng:" + latitude + "," + longitude +
                "?type=NOMINATIM&serverUrl=RAW(" + getServerUrl() + ")", exchange -> {
                    exchange.getMessage().setBody("Hello Body");
                });
        return extractResult(result);
    }

    /**
     * Creates result from exchange headers
     *
     * @param  exchange
     * @return
     */
    private GeocoderResult extractResult(Exchange exchange) throws Exception {
        Exception exception = exchange.getException();
        if (exception != null) {
            throw exception;
        }

        Message message = exchange.getIn();
        return new GeocoderResult()
                .withLat(extractString(message, GeoCoderConstants.LAT))
                .withLng(extractString(message, GeoCoderConstants.LNG))
                .withLatLng(extractString(message, GeoCoderConstants.LATLNG))
                .withAddress(extractString(message, GeoCoderConstants.ADDRESS))
                .withStatus(message.getHeader(GeoCoderConstants.STATUS, GeocoderStatus.class))
                .withCountry(extractString(message, GeoCoderConstants.COUNTRY_SHORT),
                        extractString(message, GeoCoderConstants.COUNTRY_LONG))
                .withCity(extractString(message, GeoCoderConstants.CITY))
                .withPostalCode(extractString(message, GeoCoderConstants.POSTAL_CODE))
                .withRegion(extractString(message, GeoCoderConstants.REGION_CODE),
                        extractString(message, GeoCoderConstants.REGION_NAME));
    }

    /**
     * extracts a String from header
     *
     * @param  message
     * @param  name
     * @return
     */
    private String extractString(Message message, String name) {
        return message.getHeader(name, String.class);
    }

    private String getServerUrl() {
        Config config = ConfigProvider.getConfig();
        Optional<String> wiremockUrl = config.getOptionalValue("wiremock.url", String.class);
        if (wiremockUrl.isPresent()) {
            int port = LaunchMode.current().equals(LaunchMode.TEST) ? httpTestPort.get() : httpPort.get();
            return String.format("http://localhost:%d/fake/nominatim/api", port);
        }
        return "https://nominatim.openstreetmap.org";
    }
}
