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

import org.apache.camel.BindToRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.test.support.xslt.MyExtensionFunction1;
import org.apache.camel.quarkus.test.support.xslt.MyExtensionFunction2;
import org.apache.camel.support.builder.Namespaces;
import org.apache.xpath.XPathAPI;

public class XsltRouteBuilder extends RouteBuilder {
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
                .process(exchange -> {
                    Document body = exchange.getIn().getBody(Document.class);
                    String country = XPathAPI.eval(body, "//order/country").toString();
                    exchange.getIn().setBody("Country " + country);
                })
                .otherwise()
                .setBody(constant("Invalid country code"));

        from(DIRECT_XTOKENIZE)
                .split()
                .xtokenize("//C:child", new Namespaces("C", "urn:c"))
                .to("seda:xtokenize-result");

    }

    @BindToRegistry
    private MyExtensionFunction1 function1 = new MyExtensionFunction1();

    @BindToRegistry
    private MyExtensionFunction2 function2 = new MyExtensionFunction2();
}
