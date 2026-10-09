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
package org.apache.camel.quarkus.component.odata.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.odata.ODataConstants;

public class OdataRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // READ_SET is the default operation
        from("direct:readAirports")
                .to("odata:{{odata.service.url}}/Airports?select=IcaoCode,Name&count=true");

        from("direct:readPerson")
                .setHeader(ODataConstants.OPERATION, constant("READ_ENTRY"))
                .to("odata:{{odata.service.url}}/People");

        // The operation can be set by the endpoint option as well as by the header
        from("direct:createPerson")
                .to("odata:{{odata.service.url}}/People?operation=CREATE");

        // A Map body is serialized to JSON
        from("direct:createPersonFromMap")
                .setHeader(ODataConstants.OPERATION, constant("CREATE"))
                .to("odata:{{odata.service.url}}/People");

        from("direct:updatePerson")
                .setHeader(ODataConstants.OPERATION, constant("UPDATE"))
                .to("odata:{{odata.service.url}}/People");

        from("direct:deletePerson")
                .setHeader(ODataConstants.OPERATION, constant("DELETE"))
                .to("odata:{{odata.service.url}}/People");
    }
}
