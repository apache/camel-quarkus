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
import javax.inject.Named;

import io.quarkus.arc.Unremovable;
import org.apache.camel.CamelContext;
import org.apache.camel.component.geocoder.GeoCoderComponent;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class GeocoderProducers {

    @ConfigProperty(name = "google.api.key")
    String googleApiKey;

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
    GeoCoderComponent geocoderComponent(CamelContext camelContext, MockApiService mockApiService)
            throws IllegalAccessException, NoSuchFieldException, InstantiationException {
        final String wireMockUrl = System.getProperty("wiremock.url");
        final GeoCoderComponent result = new GeoCoderComponent();
        result.setCamelContext(camelContext);

        if (wireMockUrl != null) {
            result.setGeoApiContext(mockApiService.createGeoApiContext(wireMockUrl, googleApiKey));
        }
        return result;
    }
}
