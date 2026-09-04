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
package org.apache.camel.quarkus.component.langchain4j.ingest;

import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.quarkus.component.langchain4j.ingest.core.IngestResult;
import org.apache.camel.spi.IdempotentRepository;
import org.jboss.logging.Logger;

/**
 * The named steps of the ingestion routes: each factory returns one processor of the chains
 * {@link IngestRoutes} assembles, so a route reads as a list of step names.
 */
final class IngestSteps {

    /** Exchange property carrying the resolved document id. */
    static final String DOCUMENT_ID_PROPERTY = "CamelQuarkusIngestDocumentId";

    private static final Logger LOG = Logger.getLogger(IngestSteps.class);

    private IngestSteps() {
    }

    /**
     * The ingest stage of a directory pipeline: fire and forget. The file consumer discards the
     * result, so an EMPTY outcome would otherwise leave no trace at all — and it commits the
     * file's key, so the file is not retried until it changes. With a parser that deserves a
     * warning: a parse to nothing typically means a missing Tika parser module or an image-only
     * document. The id comes from the exchange property captured before the parse — a parser
     * (Tika) copies document metadata over the headers, so a header read here could be spoofed
     * by the document itself.
     */
    static Processor directoryIngestProcessor(PipelineSpec spec) {
        return exchange -> {
            IngestResult result = spec.service().ingest(
                    (String) exchange.getProperty(DOCUMENT_ID_PROPERTY),
                    exchange.getIn().getBody(String.class));
            if (result.outcome() == IngestResult.Outcome.EMPTY) {
                if (spec.parser() != null) {
                    LOG.warnf("Ingestion pipeline '%s': document '%s' parsed to no text and was skipped; its "
                            + "key is committed, so it is not retried until the file changes (missing parser "
                            + "module? image-only document?)", spec.name(), result.documentId());
                } else {
                    LOG.debugf("Ingestion pipeline '%s': document '%s' contained no text, nothing was written",
                            spec.name(), result.documentId());
                }
            }
        };
    }

    /**
     * The ingest stage of a consumer-fed pipeline without a register: the result is the reply.
     * Like every ingest stage, it reads the id property captured before the parse, never a
     * post-parse header.
     */
    static Processor plainIngestProcessor(PipelineSpec spec) {
        return exchange -> {
            String id = (String) exchange.getProperty(DOCUMENT_ID_PROPERTY);
            exchange.getIn().setBody(spec.service().ingest(id, exchange.getIn().getBody(String.class)));
        };
    }

    /** Resolves the document id up front and carries it as an exchange property. */
    static Processor resolveDocumentIdProcessor(PipelineSpec spec, Expression documentId) {
        return exchange -> exchange.setProperty(DOCUMENT_ID_PROPERTY,
                requireDocumentId(spec.name(), documentId, exchange));
    }

    /** The ingest stage inside the register's claim: like the plain one, but EMPTY releases it. */
    static Processor claimedIngestProcessor(PipelineSpec spec, IdempotentRepository repository) {
        return exchange -> {
            String id = (String) exchange.getProperty(DOCUMENT_ID_PROPERTY);
            IngestResult result = spec.service().ingest(id, exchange.getIn().getBody(String.class));
            if (result.outcome() == IngestResult.Outcome.EMPTY) {
                // a blank document wrote nothing, so it must not keep the eager claim
                // on the id - a later, populated delivery under the same id would be
                // answered SKIPPED. The completion-time confirm() of the removed key
                // is a no-op in the memory, file and JDBC repositories alike
                repository.remove(id);
            }
            exchange.getIn().setBody(result);
        };
    }

    /** A duplicate delivery kept its original body; answered SKIPPED so a caller can tell. */
    static Processor answerDuplicateProcessor(PipelineSpec spec) {
        return exchange -> {
            if (exchange.getProperty(Exchange.DUPLICATE_MESSAGE, false, Boolean.class)) {
                exchange.getIn().setBody(new IngestResult(spec.name(),
                        (String) exchange.getProperty(DOCUMENT_ID_PROPERTY), 0,
                        IngestResult.Outcome.SKIPPED));
            }
        };
    }

    private static String requireDocumentId(String name, Expression documentId, Exchange exchange) {
        String id = documentId.evaluate(exchange, String.class);
        if (id == null) {
            throw new IllegalArgumentException("Ingestion pipeline '" + name + "': no document id. "
                    + "Set the " + IngestHeaders.DOCUMENT_ID + " header, or point "
                    + "quarkus.camel.langchain4j.ingest." + name + ".source.document-id at where the "
                    + "consumer puts it.");
        }
        return id;
    }
}
