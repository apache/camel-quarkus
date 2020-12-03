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
package org.apache.camel.quarkus.main.deployment;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.DefaultPackageScanResourceResolver;
import org.apache.camel.quarkus.core.CamelConfig;
import org.apache.camel.quarkus.core.deployment.LanguageExpressionContentHandler;
import org.apache.camel.quarkus.core.deployment.spi.CSimpleExpressionSourceBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelRoutesBuilderClassBuildItem;
import org.apache.camel.quarkus.core.deployment.util.CamelSupport;
import org.jboss.logging.Logger;

public class CSimpleXmlProcessor {

    private static final Logger LOG = Logger.getLogger(CSimpleXmlProcessor.class);

    @BuildStep
    void collectCSimpleExpresions(
            CamelConfig config,
            List<CamelRoutesBuilderClassBuildItem> routesBuilderClasses,
            BuildProducer<CSimpleExpressionSourceBuildItem> csimpleExpressions)
            throws ParserConfigurationException, SAXException, IOException {

        final List<String> locations = Stream.of("camel.main.xml-routes", "camel.main.xml-rests")
                .map(prop -> CamelSupport.getOptionalConfigValue(prop, String[].class, new String[0]))
                .flatMap(Stream::of)
                .collect(Collectors.toList());

        try (DefaultPackageScanResourceResolver resolver = new DefaultPackageScanResourceResolver()) {
            resolver.setCamelContext(new DefaultCamelContext());
            final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setNamespaceAware(true);
            SAXParser saxParser = saxParserFactory.newSAXParser();
            for (String part : locations) {
                try {
                    try (CloseableCollection<InputStream> set = new CloseableCollection<InputStream>(
                            resolver.findResources(part))) {
                        for (InputStream is : set) {
                            LOG.debugf("Found XML routes from location: %s", part);
                            try {
                                saxParser.parse(
                                        is,
                                        new LanguageExpressionContentHandler(
                                                "csimple",
                                                (script, isPredicate) -> csimpleExpressions.produce(
                                                        new CSimpleExpressionSourceBuildItem(
                                                                script,
                                                                isPredicate,
                                                                "org.apache.camel.language.csimple.XmlRouteBuilder"))));
                            } finally {
                                if (is != null) {
                                    is.close();
                                }
                            }
                        }
                    }
                } catch (FileNotFoundException e) {
                    LOG.debugf("No XML routes found in %s. Skipping XML routes detection.", part);
                } catch (Exception e) {
                    throw new RuntimeException("Could not analyze CSimple expressions in " + part, e);
                }
            }
        }
    }

    static class CloseableCollection<E extends Closeable> implements Closeable, Iterable<E> {
        private final Collection<E> delegate;

        public CloseableCollection(Collection<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void close() throws IOException {
            List<Exception> exceptions = new ArrayList<>();
            for (Closeable closeable : delegate) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    exceptions.add(e);
                }
            }
            if (!exceptions.isEmpty()) {
                throw new IOException("Could not close a resource", exceptions.get(0));
            }
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }
    }
}
