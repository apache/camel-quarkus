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
package org.apache.camel.quarkus.component.saxon.it;

import jakarta.enterprise.context.ApplicationScoped;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.language.xpath.XPathBuilder;

@ApplicationScoped
public class SaxonXPathRoutes extends RouteBuilder {

    @Override
    public void configure() {
        XPathBuilder builderViaFactory = XPathBuilder.xpath("/items/@count > 1").factory(new XPathFactoryImpl());
        from("direct:factory").choice().when(builderViaFactory).setBody(constant("Multiple items via factory option"));

        XPathBuilder builderViaObjectModel = XPathBuilder.xpath("/items/@count > 1")
                .objectModel("http://saxon.sf.net/jaxp/xpath/om");
        from("direct:object-model").choice().when(builderViaObjectModel)
                .setBody(constant("Multiple items via objectModel option"));

        XPathBuilder builderViaSaxon = XPathBuilder.xpath("/items/@count > 1").saxon();
        from("direct:saxon").choice().when(builderViaSaxon).setBody(constant("Multiple items via saxon option"));

        XPathBuilder builderWithFunction = XPathBuilder.xpath("sum(/items/item/@price) > 25").saxon();
        from("direct:function").choice().when(builderWithFunction).setBody(constant("Price sum > 25")).otherwise()
                .setBody(constant("Price sum <= 25"));
    }

}
