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
package org.apache.camel.quarkus.component.kudu.it;

import org.apache.camel.builder.RouteBuilder;

public class KuduRoute extends RouteBuilder {
    public static final String KUDU_AUTHORITY_CONFIG_KEY = "camel.kudu.test.master.rpc-authority";
    public static final String TABLE_NAME = "TestTable";

    @Override
    public void configure() {
        final String kuduEndpointUriFormat = "kudu:{{" + KUDU_AUTHORITY_CONFIG_KEY + "}}/" + TABLE_NAME + "?operation=%s";

        from("direct:create_table")
                .toF(kuduEndpointUriFormat, "create_table");

        from("direct:insert")
                .toF(kuduEndpointUriFormat, "insert");

        from("direct:update")
                .toF(kuduEndpointUriFormat, "update");

        from("direct:upsert")
                .toF(kuduEndpointUriFormat, "upsert");

        from("direct:delete")
                .toF(kuduEndpointUriFormat, "delete");

        from("direct:scan")
                .toF(kuduEndpointUriFormat, "scan");
    }
}
