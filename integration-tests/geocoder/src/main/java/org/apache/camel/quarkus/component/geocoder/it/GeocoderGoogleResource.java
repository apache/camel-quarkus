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
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.maps.model.GeocodingResult;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/google")
@ApplicationScoped
public class GeocoderGoogleResource {
    public static final String GOOGLE_GEOCODER_API_KEY = "google.api.key";

    private static final Logger LOG = Logger.getLogger(GeocoderGoogleResource.class);
    private static final String NO_API_KEY = "NO_API_KEY";

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "google.api.key", defaultValue = NO_API_KEY)
    String apiKey;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GeocodingResult[] getByCurrentLocation() {
        if (NO_API_KEY.equals(apiKey)) {
            // disable service if no API KEY
            LOG.errorf("Please set the API_KEY in the %s configuration key", GOOGLE_GEOCODER_API_KEY);
            return null;
        }
        LOG.infof("Retrieve info from current location");
        final GeocodingResult[] response = producerTemplate.requestBody(
                "geocoder:address:current?headersOnly=false&apiKey=" + apiKey,
                "Hello World", GeocodingResult[].class);
        LOG.infof("Response : %s", response);
        return response;
    }

    @Path("address/{address}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GeocodingResult[] getByAddress(@PathParam("address") String address) {
        if (NO_API_KEY.equals(apiKey)) {
            // disable service if no API KEY
            LOG.errorf("Please set the API_KEY in the %s configuration key", GOOGLE_GEOCODER_API_KEY);
            return null;
        }
        LOG.infof("Retrieve info from address : %s", address);
        final GeocodingResult[] response = producerTemplate.requestBody(
                "geocoder:address:" + address + "?apiKey=" + apiKey,
                "Hello World", GeocodingResult[].class);
        LOG.infof("Response: %s", response);
        return response;
    }

    @Path("lat/{lat}/lon/{lon}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public GeocodingResult[] getByCoordinate(@PathParam("lat") String latitude, @PathParam("lon") String longitude) {
        if (NO_API_KEY.equals(apiKey)) {
            // disable service if no API KEY
            LOG.errorf("Please set the API_KEY in the %s configuration key", GOOGLE_GEOCODER_API_KEY);
            return null;
        }
        LOG.infof("Retrieve  info from georgraphic coordinates latitude : %s, longitude %s", latitude, longitude);
        final GeocodingResult[] response = producerTemplate.requestBody(
                "geocoder:latlng:" + latitude + "," + longitude + "?apiKey=" + apiKey,
                "Hello World", GeocodingResult[].class);
        LOG.infof("Response : %s", response);
        return response;
    }
}
