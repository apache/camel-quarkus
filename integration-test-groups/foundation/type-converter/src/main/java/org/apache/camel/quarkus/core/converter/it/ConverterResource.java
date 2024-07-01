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
package org.apache.camel.quarkus.core.converter.it;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.quarkus.core.converter.it.model.MyExchangePair;
import org.apache.camel.quarkus.core.converter.it.model.MyNotRegisteredPair;
import org.apache.camel.quarkus.core.converter.it.model.MyNullablePair;
import org.apache.camel.quarkus.core.converter.it.model.MyTestPair;
import org.apache.camel.quarkus.it.support.typeconverter.pairs.MyBulk1Pair;
import org.apache.camel.quarkus.it.support.typeconverter.pairs.MyBulk2Pair;
import org.apache.camel.quarkus.it.support.typeconverter.pairs.MyLoaderPair;
import org.apache.camel.quarkus.it.support.typeconverter.pairs.MyRegistryPair;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.util.CollectionHelper;

@Path("/converter")
@ApplicationScoped
public class ConverterResource {
    @Inject
    CamelContext context;

    @Path("/myRegistryPair")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public MyRegistryPair converterMyRegistryPair(String input) {
        return context.getTypeConverter().convertTo(MyRegistryPair.class, input);
    }

    @Path("/myTestPair/string")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public MyTestPair fromStringToMyTestPair(String input) {
        return context.getTypeConverter().convertTo(MyTestPair.class, input);
    }

    @Path("/myTestPair/int")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public MyTestPair fromIntToMyTestPair(String input) {
        Integer valueToConvert = Integer.valueOf(input);
        return context.getTypeConverter().convertTo(MyTestPair.class, valueToConvert);
    }

    @Path("/myTestPair/float")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public MyTestPair fromFloatToMyTestPair(String input) {
        Float valueToConvert = Float.valueOf(input);
        return context.getTypeConverter().convertTo(MyTestPair.class, valueToConvert);
    }

    @Path("/myLoaderPair")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public MyLoaderPair fromMyLoaderPair(String input) {
        return context.getTypeConverter().convertTo(MyLoaderPair.class, input);
    }

    @Path("/myBulk1Pair")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public MyBulk1Pair convertMyBulk1Pair(String input) {
        return context.getTypeConverter().convertTo(MyBulk1Pair.class, input);
    }

    @Path("/myBulk2Pair")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public MyBulk2Pair convertMyBulk2Pair(String input) {
        return context.getTypeConverter().convertTo(MyBulk2Pair.class, input);
    }

    @Path("/myNullablePair")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public MyNullablePair convertMyNullablePair(String input) {
        return context.getTypeConverter().convertTo(MyNullablePair.class, input);
    }

    @Path("/resetStatistics")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public void resetStatistics() {
        context.getTypeConverterRegistry().getStatistics().reset();
    }

    @Path("/getStatisticsHit")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> converterGetStatistics() {
        long hit = context.getTypeConverterRegistry().getStatistics().getHitCounter();
        long miss = context.getTypeConverterRegistry().getStatistics().getMissCounter();
        return CollectionHelper.mapOf("hit", hit, "miss", miss);
    }

    @Path("/fallback")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public MyTestPair convertFallback(String input) {
        return context.getTypeConverter().convertTo(MyTestPair.class,
                "org.apache.camel.quarkus.core.converter.it.model.MyTestPair:" + input);
    }

    @Path("/myExchangePair")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public MyExchangePair convertMyExchangePair(String input, @QueryParam("converterValue") String converterValue) {
        Exchange e = new DefaultExchange(context);
        e.setProperty(StaticMethodConverter.CONVERTER_VALUE, converterValue);
        return context.getTypeConverter().convertTo(MyExchangePair.class, e, input);
    }

    @Path("/myNotRegisteredPair")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public MyNotRegisteredPair convertNotRegisteredPair(String input, @QueryParam("converterValue") String converterValue) {
        return context.getTypeConverter().convertTo(MyNotRegisteredPair.class, input);
    }
}
