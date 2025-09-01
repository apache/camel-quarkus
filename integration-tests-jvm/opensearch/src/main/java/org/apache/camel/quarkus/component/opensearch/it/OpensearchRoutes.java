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
package org.apache.camel.quarkus.component.opensearch.it;

import org.apache.camel.builder.RouteBuilder;

public class OpensearchRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:indexDoc")
                .to("opensearch://opensearch?operation=Index");

        from("direct:bulkIndex")
                .to("opensearch://opensearch?operation=Bulk");

        from("direct:getDoc")
                .to("opensearch://opensearch?operation=GetById");

        from("direct:multiget")
                .to("opensearch://opensearch?operation=MultiGet");

        from("direct:deleteDoc")
                .to("opensearch://opensearch?operation=Delete");

        from("direct:search")
                .to("opensearch://opensearch?operation=Search&useScroll=true&scrollKeepAliveMs=30000");

        from("direct:multiSearch")
                .to("opensearch://opensearch?operation=MultiSearch");

        from("direct:scrollContinue")
                .to("opensearch://_search/scroll?operation=Search&useScroll=true");
    }

}
