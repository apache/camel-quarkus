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
package org.apache.camel.quarkus.component.dfdl.it;

import org.apache.camel.builder.RouteBuilder;

public class DfdlRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:parse").to("dfdl:X12-837P.dfdl.xsd");
        from("direct:unparse").to("dfdl:X12-837P.dfdl.xsd?parseDirection=UNPARSE");
        from("direct:marshal").marshal().dfdl("X12-837P.dfdl.xsd");
        from("direct:unmarshal").unmarshal().dfdl("X12-837P.dfdl.xsd");
    }

}
