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
package org.apache.camel.quarkus.component.xml.it;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.camel.builder.RouteBuilder;

public class XmlRouteBuilder extends RouteBuilder {
    public static final String DIRECT_HTML_TO_DOM = "direct:html-to-dom";
    public static final String DIRECT_HTML_TRANSFORM = "direct:html-transfrom";
    public static final String DIRECT_HTML_TO_TEXT = "direct:html-to-text";

    @Override
    public void configure() {
        from(DIRECT_HTML_TO_DOM)
                .unmarshal().tidyMarkup()
                .process(exchange -> {
                    final Document doc = exchange.getIn().getBody(Document.class);
                    final Node html = doc.getDocumentElement();
                    if (!"html".equals(html.getLocalName())) {
                        throw new IllegalStateException(
                                "Expected <html> as the last child of of the document; found " + html.getLocalName());
                    }
                    final Node body = html.getLastChild();
                    if (!"body".equals(body.getLocalName())) {
                        throw new IllegalStateException(
                                "Expected <body> as the last child of <html>; found " + body.getLocalName());
                    }
                    final Node p = body.getFirstChild();
                    if (!"p".equals(p.getLocalName())) {
                        throw new IllegalStateException("Expected <p> as the first child of <body>; found " + p.getLocalName());
                    }
                    final Node text = p.getFirstChild();
                    if (text.getNodeType() != Node.TEXT_NODE) {
                        throw new IllegalStateException("Expected a text node as the first child of <p>; found " + text);
                    }
                    exchange.getIn().setBody(text.getTextContent());
                });

        from(DIRECT_HTML_TRANSFORM)
                .unmarshal().tidyMarkup()
                // tagSoup produces DOM that is then consumed by XSLT
                .to("xslt:xslt/html-transform.xsl");

        from(DIRECT_HTML_TO_TEXT)
                .unmarshal().tidyMarkup()
                // tagSoup produces DOM that is then consumed by XSLT
                .to("xslt:xslt/html-to-text.xsl");
    }
}
