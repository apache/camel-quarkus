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
package org.apache.camel.quarkus.component.tika;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Set;

import javax.xml.transform.TransformerConfigurationException;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.tika.TikaContent;
import io.quarkus.tika.TikaMetadata;
import io.quarkus.tika.TikaParser;
import io.quarkus.tika.runtime.TikaParserProducer;
import org.apache.camel.Component;
import org.apache.camel.Producer;
import org.apache.camel.component.tika.TikaComponent;
import org.apache.camel.component.tika.TikaConfiguration;
import org.apache.camel.component.tika.TikaEndpoint;
import org.apache.camel.component.tika.TikaParseOutputFormat;
import org.apache.camel.component.tika.TikaProducer;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.html.BoilerpipeContentHandler;

@Recorder
public class TikaRecorder {

    public RuntimeValue<TikaComponent> createTikaComponent(BeanContainer container) {
        return new RuntimeValue<>(new QuarkusTikaComponent(container.instance(TikaParserProducer.class)));
    }

    @org.apache.camel.spi.annotations.Component("tika")
    static class QuarkusTikaComponent extends TikaComponent {

        private final TikaParserProducer tikaParserProducer;

        public QuarkusTikaComponent(TikaParserProducer tikaParserProducer) {
            this.tikaParserProducer = tikaParserProducer;
        }

        @Override
        protected TikaEndpoint createEndpoint(String uri, TikaConfiguration tikaConfiguration) {
            return new QuarkusTikaEndpoint(uri, this, tikaConfiguration, tikaParserProducer);
        }
    }

    static class QuarkusTikaEndpoint extends TikaEndpoint {

        private final TikaParserProducer tikaParserProducer;

        public QuarkusTikaEndpoint(String endpointUri, Component component, TikaConfiguration tikaConfiguration,
                TikaParserProducer tikaParserProducer) {
            super(endpointUri, component, tikaConfiguration);
            this.tikaParserProducer = tikaParserProducer;
        }

        @Override
        public Producer createProducer() throws Exception {
            TikaParser tikaParser = tikaParserProducer.tikaParser();
            return new QuarkusTikaProducer(this, new Parser() {
                @Override
                public Set<MediaType> getSupportedTypes(ParseContext parseContext) {
                    return Collections.emptySet();
                }

                @Override
                public void parse(InputStream inputStream, ContentHandler contentHandler, Metadata metadata,
                        ParseContext parseContext) throws IOException, SAXException, TikaException {
                    TikaContent tc = tikaParser.parse(inputStream, contentHandler);
                    TikaMetadata tm = tc.getMetadata();
                    if (tm != null) {
                        for (String name : tm.getNames()) {
                            tm.getValues(name).stream().forEach((v) -> metadata.add(name, v));
                        }
                    }
                }
            });
        }
    }

    // TODO: Remove this when Camel Tika & Quarkus Tika versions are aligned
    // https://github.com/apache/camel-quarkus/issues/3599
    static class QuarkusTikaProducer extends TikaProducer {

        public QuarkusTikaProducer(TikaEndpoint endpoint) {
            super(endpoint);
        }

        public QuarkusTikaProducer(TikaEndpoint endpoint, Parser parser) {
            super(endpoint, parser);
        }

        @Override
        protected ContentHandler getContentHandler(TikaConfiguration configuration, OutputStream outputStream)
                throws TransformerConfigurationException, UnsupportedEncodingException {
            TikaParseOutputFormat outputFormat = configuration.getTikaParseOutputFormat();
            if (outputFormat.equals(TikaParseOutputFormat.textMain)) {
                return new BoilerpipeContentHandler(
                        new OutputStreamWriter(outputStream, configuration.getTikaParseOutputEncoding()));
            }
            return super.getContentHandler(configuration, outputStream);
        }
    }

}
