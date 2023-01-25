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
package org.apache.camel.quarkus.component.weather.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.weather.WeatherConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/weather")
@ApplicationScoped
public class WeatherResource {

    private static final Logger LOG = Logger.getLogger(WeatherResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @ConfigProperty(name = "open.weather.api-id")
    String weatherApiId;

    @Path("location/{location}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWeatherByLocation(@PathParam("location") String location) {
        LOG.infof("Retrieve weather with location : %s", location);
        final String response = producerTemplate.requestBodyAndHeader(
                "weather:foo?location=random&appid=" + weatherApiId,
                "Hello World", WeatherConstants.WEATHER_LOCATION, location, String.class);
        LOG.infof("Got response from weather: %s", response);
        return Response
                .ok()
                .entity(response)
                .build();
    }

    @Path("lat/{lat}/lon/{lon}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWeatherByCoordinate(@PathParam("lat") String latitude, @PathParam("lon") String longitude) {
        LOG.infof("Retrieve weather with georgraphic coordinates latitude : %s, longitude %s", latitude, longitude);
        final String response = producerTemplate.requestBody(
                "weather:foo?lat=" + latitude + "&lon=" + longitude + "&appid=" + weatherApiId,
                "Hello World", String.class);
        LOG.infof("Got response from weather: %s", response);
        return Response
                .ok()
                .entity(response)
                .build();
    }

    @Path("zip/{zip}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWeatherByZip(@PathParam("zip") String zip) {
        LOG.infof("Retrieve weather with georgraphic coordinates zip %s", zip);
        final String response = producerTemplate.requestBody(
                "weather:foo?zip=" + zip + "&appid=" + weatherApiId, "Hello World", String.class);
        LOG.infof("Got response from weather: %s", response);
        return Response
                .ok()
                .entity(response)
                .build();
    }

    @Path("ids/{ids}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWeatherByIds(@PathParam("ids") String ids) {
        LOG.infof("Retrieve weather with georgraphic coordinates ids %s", ids);
        final String response = producerTemplate.requestBody(
                "weather:foo?ids=" + ids + "&appid=" + weatherApiId, "Hello World", String.class);
        LOG.infof("Got response from weather: %s", response);
        return Response
                .ok()
                .entity(response)
                .build();
    }

    @Path("location/{location}/period/{period}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWeatherByPeriod(@PathParam("location") String location, @PathParam("period") String period) {
        LOG.infof("Retrieve weather with location : %s and period %s", location, period);
        final String response = producerTemplate.requestBodyAndHeader(
                "weather:foo?location=random&appid=" + weatherApiId + "&period=" + period,
                "Hello World", WeatherConstants.WEATHER_LOCATION, location, String.class);
        LOG.infof("Got response from weather: %s", response);
        return Response
                .ok()
                .entity(response)
                .build();
    }

    @Path("{location}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getWeather(@PathParam("location") String location) throws Exception {
        final String message = consumerTemplate.receiveBody(
                "weather:foo?appid=" + weatherApiId + "&location=" + location, String.class);
        return Response
                .ok()
                .entity(message)
                .build();
    }

}
