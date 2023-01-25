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

import com.google.maps.model.GeocodingResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/google")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class GeocoderGoogleResource {
    private static final Logger LOG = Logger.getLogger(GeocoderGoogleResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @ConfigProperty(name = "google.api.key", defaultValue = "AIzaFakeKey")
    String googleApiKey;

    @GET
    public GeocodingResult[] getByCurrentLocation() {
        LOG.infof("Retrieve info from current location");
        final GeocodingResult[] response = producerTemplate.requestBody(
                String.format("geocoder:address:current?apiKey=%s", googleApiKey),
                null, GeocodingResult[].class);
        LOG.infof("Response : %s", (Object[]) response);
        return response;
    }

    @Path("address/{address}")
    @GET
    public GeocodingResult[] getByAddress(@PathParam("address") String address) {
        LOG.infof("Retrieve info from address : %s", address);
        final GeocodingResult[] response = producerTemplate.requestBody(
                String.format("geocoder:address:%s?apiKey=%s", address, googleApiKey),
                null, GeocodingResult[].class);
        LOG.infof("Response: %s", (Object[]) response);
        return response;
    }

    @Path("lat/{lat}/lon/{lon}")
    @GET
    public GeocodingResult[] getByCoordinate(@PathParam("lat") String latitude, @PathParam("lon") String longitude) {
        LOG.infof("Retrieve  info from georgraphic coordinates latitude : %s, longitude %s", latitude, longitude);
        final GeocodingResult[] response = producerTemplate.requestBody(
                String.format("geocoder:latlng:%s,%s?apiKey=%s", latitude, longitude, googleApiKey),
                null, GeocodingResult[].class);
        LOG.infof("Response : %s", (Object[]) response);
        return response;
    }
}
