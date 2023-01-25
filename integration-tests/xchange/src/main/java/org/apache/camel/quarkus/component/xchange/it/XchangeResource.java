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

import java.net.URI;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.xchange.XChangeComponent;
import org.apache.camel.spi.annotations.Component;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.meta.CurrencyMetaData;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.knowm.xchange.kraken.KrakenExchange;

@Path("/xchange")
@ApplicationScoped
public class XchangeResource {

    public static final String DEFAULT_CRYPTO_EXCHANGE = "binance";
    public static final String ALTERNATIVE_CRYPTO_EXCHANGE = "kraken";

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/ticker/{exchange}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject currencyTicker(
            @PathParam("exchange") String cryptoExchange,
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

    @Named("xchange")
    public XChangeComponent xChangeComponent() {
        Config config = ConfigProvider.getConfig();
        Optional<String> wireMockUrl = config.getOptionalValue("wiremock.url", String.class);
        if (wireMockUrl.isPresent()) {
            return new WireMockedXChangeComponent();
        }
        return new XChangeComponent();
    }

    @Component("xchange")
    static final class WireMockedXChangeComponent extends XChangeComponent {
        @Override
        protected Exchange createExchange(Class<? extends Exchange> exchangeClass) {
            String wireMockUrlProperty;
            if (exchangeClass.equals(BinanceExchange.class)) {
                wireMockUrlProperty = "wiremock.binance.url";
            } else if (exchangeClass.equals(KrakenExchange.class)) {
                wireMockUrlProperty = "wiremock.kraken.url";
            } else {
                throw new IllegalStateException("Unsupported WireMocked exchange " + exchangeClass.getSimpleName());
            }

            Config config = ConfigProvider.getConfig();
            String wireMockUrl = config.getValue(wireMockUrlProperty, String.class);
            URI uri = URI.create(wireMockUrl);

            ExchangeSpecification specification = new ExchangeSpecification(exchangeClass);
            specification.setHost("localhost");
            specification.setPort(uri.getPort());
            specification.setSslUri(wireMockUrl);
            return ExchangeFactory.INSTANCE.createExchange(specification);
        }
    }
}
