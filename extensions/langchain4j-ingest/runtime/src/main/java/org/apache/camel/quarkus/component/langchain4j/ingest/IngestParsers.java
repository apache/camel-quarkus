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

import java.io.ByteArrayInputStream;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.xml.sax.InputSource;

import org.apache.camel.Exchange;
import org.apache.camel.model.ProcessorDefinition;

/**
 * The parse stage of the ingestion routes, and everything Tika-shaped that comes with it.
 */
final class IngestParsers {

    /** The supported parsers; the configuration value is the lower-case name. */
    enum Parser {
        TIKA,
        DOCLING;

        String configName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /**
         * The one place an unknown value can fail at runtime — the build already rejected it
         * for configured pipelines, {@code IngestPipeline.parser(String)} for builder ones.
         * {@code null} stays {@code null}: no parser configured.
         */
        static Parser of(String value) {
            if (value == null) {
                return null;
            }
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Unknown parser " + value);
            }
        }
    }

    private IngestParsers() {
    }

    /**
     * The optional parse stage; the route is returned unchanged when the pipeline has no
     * parser. Each case is one parser's complete story: Tika parses in-process — its plain-text
     * output format loses small documents behind an unflushed writer, so the parse runs as
     * XHTML (the encoding pinned, Tika's default being the platform charset) and the body text
     * is lifted out afterwards. Docling hands the payload to a Docling Serve instance and
     * answers finished markdown, nothing to post-process.
     */
    static ProcessorDefinition<?> parseStep(ProcessorDefinition<?> route, Parser parser) {
        if (parser == null) {
            return route;
        }
        return switch (parser) {
        case TIKA -> route.to("tika:parse?tikaParseOutputEncoding=UTF-8")
                .process(IngestParsers::tikaXhtmlToText);
        // the body is pinned to bytes: the docling producer rejects streams, reads a leading-/
        // String as a server-side file path, and honours CamelDocling* control headers - none of
        // which a consumer-delivered payload may decide, hence the header sweep
        case DOCLING -> route.convertBodyTo(byte[].class)
                .removeHeaders("CamelDocling*")
                .to("docling:convert?operation=CONVERT_TO_MARKDOWN&contentInBody=true");
        };
    }

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = xhtmlBuilderFactory();

    /**
     * Lifts the text out of Tika's XHTML. The endpoint's own plain-text output format loses
     * content smaller than its encoder buffer behind an unflushed writer, so the parse runs with
     * the default XHTML output instead and the markup is dropped here.
     */
    private static void tikaXhtmlToText(Exchange exchange) throws Exception {
        // the raw bytes, so the XML prolog's pinned UTF-8 governs the decode - a String read
        // would go through the exchange charset heuristic, which headers (including
        // document-controlled ones after the parse) can steer to the wrong charset
        byte[] xhtml = exchange.getIn().getBody(byte[].class);
        DocumentBuilder builder;
        // the factory is configured once; builder creation is cheap but the factory is not
        // documented thread-safe
        synchronized (DOCUMENT_BUILDER_FACTORY) {
            builder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
        }
        org.w3c.dom.Document document = builder.parse(new InputSource(new ByteArrayInputStream(xhtml)));
        // the body subtree only: the whole document would prepend Tika's <head><title> - the
        // PDF Title metadata or the resource name - to the ingested text
        org.w3c.dom.Node body = document.getElementsByTagNameNS("http://www.w3.org/1999/xhtml", "body").item(0);
        exchange.getIn().setBody((body != null ? body : document.getDocumentElement()).getTextContent());
    }

    /**
     * The XHTML is produced locally by Tika, but a document is attacker-supplied input, so the
     * usual XML hardening applies.
     */
    private static DocumentBuilderFactory xhtmlBuilderFactory() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setNamespaceAware(true);
            return factory;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot configure the XHTML parser", e);
        }
    }
}
