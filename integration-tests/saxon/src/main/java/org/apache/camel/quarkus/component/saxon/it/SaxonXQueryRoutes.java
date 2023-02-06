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
import jakarta.inject.Named;
import net.sf.saxon.Configuration;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class SaxonXQueryRoutes extends RouteBuilder {

    @Named("saxonConf")
    public Configuration loadConf() {
        Configuration conf = new Configuration();
        conf.registerExtensionFunction(new SimpleExtension());
        return conf;
    }

    @Override
    public void configure() {
        from("direct:filter").filter().xquery("/person[@name='James']").setBody(constant("JAMES"));

        from("direct:transform").transform().xquery("concat(/Envelope/Body/getEmployee/EmpId/text(),\"Suffix\")", String.class);

        from("direct:resource").transform().xquery("resource:classpath:myxquery.txt", String.class);

        from("direct:produce").to("xquery:transform.xquery");

        from("direct:extension").to("xquery:transformWithExtension.xquery?configuration=#saxonConf");

        from("direct:bean").bean("myBean");
    }

}
