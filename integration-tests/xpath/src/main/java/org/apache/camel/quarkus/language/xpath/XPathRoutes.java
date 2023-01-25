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
package org.apache.camel.quarkus.language.xpath;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class XPathRoutes extends RouteBuilder {

    @Inject
    @Named("priceBean")
    PriceBean priceBean;

    @Override
    public void configure() {
        from("direct:transform").transform().xpath("//students/student/name/text()");

        from("direct:choice").choice().when().xpath("/body[@id='a']").setBody(constant("A"));

        from("direct:coreXPathFunctions").transform().xpath("concat('foo-',//person/@name)", String.class);

        from("direct:camelXPathFunctions").choice().when().xpath("in:header('foo') = 'bar'").setBody(constant("BAR"));

        from("direct:resource").transform().xpath("resource:classpath:myxpath.txt");

        from("direct:annotation").transform().method(priceBean, "read");

        from("direct:properties").choice().when().xpath("$type = function:properties('foo')").setBody(constant("FOO"));

        from("direct:simple").choice().when().xpath("//name = function:simple('{{bar}}')").setBody(constant("BAR"));
    }
}
