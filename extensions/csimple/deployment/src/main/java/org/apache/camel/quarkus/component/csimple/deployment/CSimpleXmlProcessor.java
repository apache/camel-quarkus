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
package org.apache.camel.quarkus.component.csimple.deployment;

import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import org.apache.camel.quarkus.core.deployment.LanguageExpressionContentHandler;
import org.apache.camel.quarkus.core.deployment.main.CamelMainHelper;
import org.jboss.logging.Logger;

public class CSimpleXmlProcessor {
    private static final Logger LOG = Logger.getLogger(CSimpleXmlProcessor.class);

    @BuildStep
    void collectCSimpleExpresions(BuildProducer<CSimpleExpressionSourceBuildItem> csimpleExpressions) throws Exception {
        final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        final SAXParser saxParser = saxParserFactory.newSAXParser();

        CamelMainHelper.forEachMatchingResource(
                resource -> {
                    try (InputStream is = resource.getInputStream()) {
                        saxParser.parse(
                                is,
                                new LanguageExpressionContentHandler(
                                        "csimple",
                                        (script, isPredicate) -> csimpleExpressions.produce(
                                                new CSimpleExpressionSourceBuildItem(
                                                        script,
                                                        isPredicate,
                                                        "org.apache.camel.language.csimple.XmlRouteBuilder"))));
                    } catch (FileNotFoundException e) {
                        LOG.debugf("No XML routes found in %s. Skipping XML routes detection.", resource.getLocation());
                    } catch (Exception e) {
                        throw new RuntimeException("Could not analyze CSimple expressions in " + resource.getLocation(), e);
                    }
                });
    }
}
