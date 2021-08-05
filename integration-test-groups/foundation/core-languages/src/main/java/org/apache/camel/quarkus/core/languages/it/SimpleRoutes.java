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
package org.apache.camel.quarkus.core.languages.it;

import org.apache.camel.builder.RouteBuilder;

public class SimpleRoutes extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:filter-simple").filter().simple("${in.header.premium} == true").setBody(constant("PREMIUM"));

        from("direct:transform-simple").transform().simple("Hello ${in.header.user} !");

        from("direct:resource-simple").transform().simple("resource:classpath:mysimple.txt");

        from("direct:mandatoryBodyAs-simple").filter().simple("${mandatoryBodyAs(String).toUpperCase()} == 'GOLD'")
                .setBody(constant("PREMIUM"));

        from("direct:bodyIs-simple").filter().simple("${body} is 'java.nio.ByteBuffer'").setBody(constant("BYTE_BUFFER"));

        from("direct:languageSimple")
                .transform().language("simple", "Hello ${body} from language().simple()");

    }

}
