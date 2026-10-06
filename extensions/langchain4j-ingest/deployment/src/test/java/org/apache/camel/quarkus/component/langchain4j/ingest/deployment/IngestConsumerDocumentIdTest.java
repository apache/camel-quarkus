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
package org.apache.camel.quarkus.component.langchain4j.ingest.deployment;

import io.quarkus.test.QuarkusExtensionTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.langchain4j.ingest.IngestResult;
import org.apache.camel.quarkus.component.langchain4j.ingest.IngestHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * The document id on consumer-fed pipelines without a parser, as with the previous route builder:
 * the header wins and the exchange property is only the fallback, and a missing id is reported
 * under the configured header. Application-wide sink Kamelet properties do not reach them either.
 */
class IngestConsumerDocumentIdTest {

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestEmbeddingBeans.class, ValidationTestSupport.class))
            // any consumer the test classpath has: the tests hand their exchanges to the routes directly
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.docs.source.uri", "file:target/consumer-document-id-docs")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.docs.source.document-id", "X-Doc-Id")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.docs.embedding-store", "store")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.docs.embedding-model", "model")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.plain.source.uri", "file:target/consumer-document-id-plain")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.plain.embedding-store", "store")
            .overrideConfigKey("quarkus.camel.langchain4j.ingest.plain.embedding-model", "model")
            // meant for an application's own use of the sink Kamelet: the pipelines pin both options
            .overrideConfigKey("camel.kamelet.langchain4j-ingest-sink.documentIdHeader", "Other-Id")
            .overrideConfigKey("camel.kamelet.langchain4j-ingest-sink.minDocumentSize", "1000");

    @Inject
    CamelContext context;

    @Test
    void missingIdNamesTheConfiguredHeader() throws Exception {
        Exchange exchange = deliver("docs", e -> {
        });
        ValidationTestSupport.assertFailure(exchange.getException(), "no document id. Set the X-Doc-Id header");
    }

    @Test
    void idInThePropertyOfTheConfiguredHeaderIsIngested() throws Exception {
        assertIngested(deliver("docs", e -> e.setProperty("X-Doc-Id", "manual.txt")), "manual.txt");
    }

    @Test
    void headerWinsOverStaleExchangeProperty() throws Exception {
        assertIngested(deliver("plain", e -> {
            e.getMessage().setHeader(IngestHeaders.DOCUMENT_ID, "fresh.txt");
            e.setProperty(IngestHeaders.DOCUMENT_ID, "stale.txt");
        }), "fresh.txt");
    }

    @Test
    @SuppressWarnings("deprecation")
    void idOnlyInTheExchangePropertyIsIngested() throws Exception {
        assertIngested(deliver("plain", e -> e.setProperty(IngestHeaders.DOCUMENT_ID, "property.txt")), "property.txt");
        assertIngested(deliver("plain", e -> e.setProperty(IngestHeaders.LEGACY_DOCUMENT_ID, "legacy.txt")), "legacy.txt");
    }

    @Test
    void appWideSinkPropertiesDoNotReachThePipeline() throws Exception {
        // minDocumentSize=1000 would filter this short document
        assertIngested(deliver("plain", e -> e.getMessage().setHeader(IngestHeaders.DOCUMENT_ID, "short.txt")),
                "short.txt");
        // documentIdHeader=Other-Id would be named here
        ValidationTestSupport.assertFailure(deliver("plain", e -> {
        }).getException(), "no document id. Set the CamelLangChain4jIngestDocumentId header");
    }

    private static void assertIngested(Exchange exchange, String documentId) {
        Assertions.assertNull(exchange.getException());
        IngestResult result = exchange.getMessage().getBody(IngestResult.class);
        Assertions.assertEquals(IngestResult.Outcome.INGESTED, result.outcome());
        Assertions.assertEquals(documentId, result.documentId());
    }

    /** Runs an exchange through the pipeline's route as if its consumer had delivered it. */
    private Exchange deliver(String pipeline, Processor customizer) throws Exception {
        Consumer consumer = context.getRoute("langchain4j-ingest-" + pipeline).getConsumer();
        Exchange exchange = consumer.createExchange(false);
        exchange.getMessage().setBody("The pump tolerates five bar.");
        customizer.process(exchange);
        consumer.getProcessor().process(exchange);
        return exchange;
    }
}
