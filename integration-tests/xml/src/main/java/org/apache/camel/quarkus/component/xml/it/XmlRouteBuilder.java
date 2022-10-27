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

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.xslt.saxon.XsltSaxonAggregationStrategy;
import org.apache.camel.support.builder.Namespaces;

// These reflections registrations should be removed with fixing https://github.com/apache/camel-quarkus/issues/1615
@RegisterForReflection(classNames = {
        "net.sf.saxon.Configuration",
        "net.sf.saxon.functions.String_1",
        "net.sf.saxon.functions.Tokenize_1",
        "net.sf.saxon.functions.StringJoin",
        "org.xmlresolver.loaders.XmlLoader",
        "org.apache.camel.component.xslt.saxon.XsltSaxonBuilder" })
public class XmlRouteBuilder extends RouteBuilder {
    public static final String DIRECT_HTML_TRANSFORM = "direct:html-transform";
    public static final String DIRECT_HTML_TO_TEXT = "direct:html-to-text";
    public static final String DIRECT_XML_CBR = "direct:xml-cbr";
    public static final String DIRECT_XTOKENIZE = "direct:xtokenize";

    @Override
    public void configure() {
        from(DIRECT_HTML_TRANSFORM)
                .convertBodyTo(Document.class)
                .to("xslt:xslt/html-transform.xsl");

        from(DIRECT_HTML_TO_TEXT)
                .convertBodyTo(Document.class)
                .to("xslt:xslt/html-to-text.xsl");

        from(DIRECT_XML_CBR)
                .choice()
                .when(xpath("//order/country = 'UK'"))
                .setBody(constant("Country UK"))
                .otherwise()
                .setBody(constant("Invalid country code"));

        from(DIRECT_XTOKENIZE)
                .split()
                .xtokenize("//C:child", new Namespaces("C", "urn:c"))
                .to("seda:xtokenize-result");

        from("file:src/test/resources?noop=true&sortBy=file:name&antInclude=*.xml")
                .routeId("aggregate").noAutoStartup()
                .aggregate(new XsltSaxonAggregationStrategy("xslt/aggregate.xsl"))
                .constant(true)
                .completionFromBatchConsumer()
                .log("after aggregate body: ${body}")
                .to("mock:transformed");
    }
}
