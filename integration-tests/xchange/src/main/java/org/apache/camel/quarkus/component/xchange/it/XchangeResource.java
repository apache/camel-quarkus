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
package org.apache.camel.quarkus.component.xchange.it;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.meta.CurrencyMetaData;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;

@Path("/xchange")
@ApplicationScoped
public class XchangeResource {

    // TODO: Reinstate binance as the default crypto exchange and kraken as the secondary
    // https://github.com/apache/camel-quarkus/issues/3016
    public static final String DEFAULT_CRYPTO_EXCHANGE = "kraken";

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/ticker/{exchange}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject currencyTicker(@PathParam("exchange") String cryptoExchange,
            @QueryParam("currencyPair") String currencyPair) {
        Ticker ticker = producerTemplate.requestBody(
                "xchange:" + cryptoExchange + "?service=marketdata&method=ticker&currencyPair=" + currencyPair, null,
                Ticker.class);
        return Json.createObjectBuilder()
                .add("last", ticker.getLast().longValue())
                .add("bid", ticker.getBid().longValue())
                .add("ask", ticker.getAsk().longValue())
                .build();
    }

    @Path("/currency")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public JsonObject currencies() {
        List<Currency> currencies = producerTemplate.requestBody(
                "xchange:" + DEFAULT_CRYPTO_EXCHANGE + "?service=metadata&method=currencies", null,
                List.class);
        JsonArrayBuilder builder = Json.createArrayBuilder();
        currencies.forEach(c -> builder.add(c.getSymbol()));
        return Json.createObjectBuilder().add("currencies", builder.build()).build();
    }

    @Path("/currency/metadata/{symbol}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String currencyMetadata(@PathParam("symbol") String symbol) {
        CurrencyMetaData metaData = producerTemplate.requestBody(
                "xchange:" + DEFAULT_CRYPTO_EXCHANGE + "?service=metadata&method=currencyMetaData",
                Currency.getInstance(symbol), CurrencyMetaData.class);
        return metaData.getScale().toString();
    }

    @Path("/currency/pairs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public JsonObject currencyPairs() {
        List<CurrencyPair> currencyPairs = producerTemplate.requestBody(
                "xchange:" + DEFAULT_CRYPTO_EXCHANGE + "?service=metadata&method=currencyPairs",
                null, List.class);
        JsonArrayBuilder builder = Json.createArrayBuilder();
        currencyPairs.forEach(cp -> builder.add(cp.toString()));
        return Json.createObjectBuilder().add("currencyPairs", builder.build()).build();
    }

    @Path("/currency/pairs/metadata")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String currencyPairsMetadata(@QueryParam("base") String base, @QueryParam("counter") String counter) {
        CurrencyPairMetaData metaData = producerTemplate.requestBody(
                "xchange:" + DEFAULT_CRYPTO_EXCHANGE + "?service=metadata&method=currencyPairMetaData",
                new CurrencyPair(base, counter),
                CurrencyPairMetaData.class);
        return metaData.getTradingFee().toPlainString();
    }
}
