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
package org.apache.camel.quarkus.component.dataset.it;

import org.apache.camel.builder.RouteBuilder;

public class DataSetRoutes extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:simpleDataSet")
                .to("dataset:simpleDataSet?minRate=50");

        from("direct:simpleDataSetWithIndex")
                .to("dataset:simpleDataSetWithIndex?minRate=50");

        from("direct:simpleDataSetForException")
                .to("dataset:simpleDataSetForException");

        from("dataset:simpleDataSetForConsumer").id("simple").autoStartup(false)
                .to("mock:simpleDataSetResult");

        from("direct:simpleDataSetIndexOff")
                .to("dataset:simpleDataSetIndexOff?dataSetIndex=off");

        from("direct:simpleDataSetIndexLenient")
                .to("dataset:simpleDataSetIndexLenient?dataSetIndex=lenient");

        from("direct:simpleDataSetIndexStrict")
                .to("dataset:simpleDataSetIndexStrict?dataSetIndex=strict");

        from("direct:simpleDataSetIndexStrictWithoutHeader")
                .to("dataset:simpleDataSetIndexStrictWithoutHeader?dataSetIndex=strict");

        from("direct:listDataSet")
                .to("dataset:listDataSet");

        from("dataset:listDataSetForConsumer").id("list").autoStartup(false)
                .to("mock:listDataSetResult");

        from("direct:fileDataSet")
                .to("dataset:fileDataSet");

        from("dataset:fileDataSet").id("file").autoStartup(false)
                .to("mock:fileDataSetResult");

        from("direct:fileDataSetDelimited")
                .to("dataset:fileDataSetDelimited");

        from("dataset:customDataSet").id("custom").autoStartup(false)
                .to("direct:customDataSet");

        from("direct:customDataSet")
                .to("dataset:customDataSet");

        from("dataset:preloadedDataSet?preloadSize=5").id("preload").autoStartup(false)
                .to("seda:preloadedDataSet");

        from("seda:preloadedDataSet")
                .to("dataset:preloadedDataSet?preloadSize=5");
    }
}
