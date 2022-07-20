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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.dataset.DataSetEndpoint;
import org.apache.camel.component.dataset.DataSetTestEndpoint;
import org.apache.camel.component.dataset.FileDataSet;
import org.apache.camel.component.dataset.ListDataSet;
import org.apache.camel.component.dataset.SimpleDataSet;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.CamelContextHelper;

@Path("/dataset")
@ApplicationScoped
public class DataSetResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @GET
    @Path("/simple")
    public void dataSetSimple(@QueryParam("useIndexHeader") boolean useIndexHeader) throws InterruptedException {
        String dataSetName = useIndexHeader ? "simpleDataSetWithIndex" : "simpleDataSet";
        SimpleDataSet simpleDataSet = CamelContextHelper.lookup(context, dataSetName, SimpleDataSet.class);

        DataSetEndpoint endpoint = context.getEndpoint("dataset:" + dataSetName + "?minRate=50", DataSetEndpoint.class);
        endpoint.expectedMessageCount((int) simpleDataSet.getSize());

        for (int i = 0; i < simpleDataSet.getSize(); i++) {
            if (useIndexHeader) {
                producerTemplate.sendBodyAndHeader("direct:simpleDataSetWithIndex", simpleDataSet.getDefaultBody(),
                        Exchange.DATASET_INDEX, i);
            } else {
                producerTemplate.sendBody("direct:simpleDataSet", simpleDataSet.getDefaultBody());
            }
        }

        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            endpoint.reset();
        }
    }

    @GET
    @Path("/simple/exception")
    public Response simpleDataSetAssertionException() throws InterruptedException {
        DataSetEndpoint endpoint = context.getEndpoint("dataset:simpleDataSetForException", DataSetEndpoint.class);

        producerTemplate.sendBody("direct:simpleDataSetForException", "invalid payload");

        try {
            endpoint.assertIsSatisfied(5000);
        } catch (Throwable t) {
            if (t instanceof AssertionError) {
                return Response.ok().build();
            } else {
                return Response.serverError().build();
            }
        } finally {
            endpoint.reset();
        }

        return Response.serverError().build();
    }

    @GET
    @Path("/simple/consumer")
    public void dataSetSimpleConsumer() throws Exception {
        MockEndpoint endpoint = context.getEndpoint("mock:simpleDataSetResult", MockEndpoint.class);
        endpoint.expectedBodiesReceived("<hello>world!</hello>");

        context.getRouteController().startRoute("simple");
        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            context.getRouteController().stopRoute("simple");
            endpoint.reset();
        }
    }

    @GET
    @Path("/list")
    public void dataSetList() throws InterruptedException {
        ListDataSet listDataSet = CamelContextHelper.lookup(context, "listDataSet", ListDataSet.class);
        DataSetEndpoint endpoint = context.getEndpoint("dataset:listDataSet", DataSetEndpoint.class);
        endpoint.expectedMessageCount((int) listDataSet.getSize());

        producerTemplate.sendBodyAndHeader("direct:listDataSet", "Hello", Exchange.DATASET_INDEX, 0);
        producerTemplate.sendBodyAndHeader("direct:listDataSet", "World", Exchange.DATASET_INDEX, 1);

        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            endpoint.reset();
        }
    }

    @GET
    @Path("/list/consumer")
    public void dataSetListConsumer() throws Exception {
        MockEndpoint endpoint = context.getEndpoint("mock:listDataSetResult", MockEndpoint.class);
        endpoint.expectedBodiesReceived("Hello", "World");

        context.getRouteController().startRoute("list");
        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            context.getRouteController().stopRoute("list");
            endpoint.reset();
        }
    }

    @GET
    @Path("/file")
    public void dataSetFile() throws InterruptedException {
        FileDataSet fileDataSet = CamelContextHelper.lookup(context, "fileDataSet", FileDataSet.class);
        DataSetEndpoint endpoint = context.getEndpoint("dataset:fileDataSet", DataSetEndpoint.class);
        endpoint.expectedMessageCount((int) fileDataSet.getSize());

        producerTemplate.sendBodyAndHeader("direct:fileDataSet", "Hello World\n", Exchange.DATASET_INDEX, 0);

        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            endpoint.reset();
        }
    }

    @GET
    @Path("/file/delimited")
    public void dataSetFileDelimited() throws InterruptedException {
        FileDataSet fileDataSet = CamelContextHelper.lookup(context, "fileDataSetDelimited", FileDataSet.class);
        DataSetEndpoint endpoint = context.getEndpoint("dataset:fileDataSetDelimited", DataSetEndpoint.class);
        endpoint.expectedMessageCount((int) fileDataSet.getSize());

        producerTemplate.sendBodyAndHeader("direct:fileDataSetDelimited", "Hello", Exchange.DATASET_INDEX, 0);
        producerTemplate.sendBodyAndHeader("direct:fileDataSetDelimited", "World", Exchange.DATASET_INDEX, 1);

        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            endpoint.reset();
        }
    }

    @GET
    @Path("/file/consumer")
    public void dataSetFileConsumer() throws Exception {
        MockEndpoint endpoint = context.getEndpoint("mock:fileDataSetResult", MockEndpoint.class);
        endpoint.expectedBodiesReceived("Hello World\n");

        context.getRouteController().startRoute("file");
        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            context.getRouteController().stopRoute("file");
            endpoint.reset();
        }
    }

    @GET
    @Path("/custom")
    public void dataSetCustom() throws Exception {
        context.getRouteController().startRoute("custom");

        CustomDataSet customDataSet = CamelContextHelper.lookup(context, "customDataSet", CustomDataSet.class);
        DataSetEndpoint endpoint = context.getEndpoint("dataset:customDataSet", DataSetEndpoint.class);
        endpoint.setExpectedMessageCount((int) customDataSet.getSize());

        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            context.getRouteController().stopRoute("custom");
            endpoint.reset();
        }
    }

    @GET
    @Path("/simple/index/off")
    public void dataSetIndexOff() throws InterruptedException {
        SimpleDataSet simpleDataSet = CamelContextHelper.lookup(context, "simpleDataSetIndexOff", SimpleDataSet.class);
        DataSetEndpoint endpoint = context.getEndpoint("dataset:simpleDataSetIndexOff?dataSetIndex=off", DataSetEndpoint.class);
        endpoint.setExpectedMessageCount((int) simpleDataSet.getSize());

        for (int i = 0; i < simpleDataSet.getSize(); i++) {
            if (0 == i % 2) {
                producerTemplate.sendBodyAndHeader("direct:simpleDataSetIndexOff", simpleDataSet.getDefaultBody(),
                        Exchange.DATASET_INDEX, i - 1);
            } else {
                producerTemplate.sendBody("direct:simpleDataSetIndexOff", simpleDataSet.getDefaultBody());
            }
        }

        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            endpoint.reset();
        }
    }

    @GET
    @Path("/simple/index/lenient")
    public void dataSetIndexLenient() throws InterruptedException {
        SimpleDataSet simpleDataSet = CamelContextHelper.lookup(context, "simpleDataSetIndexLenient", SimpleDataSet.class);
        DataSetEndpoint endpoint = context.getEndpoint("dataset:simpleDataSetIndexLenient?dataSetIndex=lenient",
                DataSetEndpoint.class);
        endpoint.setExpectedMessageCount((int) simpleDataSet.getSize());

        for (int i = 0; i < simpleDataSet.getSize(); i++) {
            if (0 == i % 2) {
                producerTemplate.sendBodyAndHeader("direct:simpleDataSetIndexLenient", simpleDataSet.getDefaultBody(),
                        Exchange.DATASET_INDEX, i);
            } else {
                producerTemplate.sendBody("direct:simpleDataSetIndexLenient", simpleDataSet.getDefaultBody());
            }
        }

        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            endpoint.reset();
        }
    }

    @GET
    @Path("/simple/index/strict")
    public Response dataSetIndexStrict(@QueryParam("useIndexHeader") boolean useIndexHeader) throws InterruptedException {
        String dataSet = useIndexHeader ? "simpleDataSetIndexStrict" : "simpleDataSetIndexStrictWithoutHeader";
        SimpleDataSet simpleDataSet = CamelContextHelper.lookup(context, dataSet, SimpleDataSet.class);
        DataSetEndpoint endpoint = context.getEndpoint("dataset:" + dataSet + "?dataSetIndex=strict", DataSetEndpoint.class);
        endpoint.setExpectedMessageCount((int) simpleDataSet.getSize());

        for (int i = 0; i < simpleDataSet.getSize(); i++) {
            if (useIndexHeader) {
                producerTemplate.sendBodyAndHeader("direct:simpleDataSetIndexStrict", simpleDataSet.getDefaultBody(),
                        Exchange.DATASET_INDEX, i);
            } else {
                producerTemplate.sendBody("direct:simpleDataSetIndexStrictWithoutHeader", simpleDataSet.getDefaultBody());
            }
        }

        try {
            endpoint.assertIsSatisfied(5000);
        } catch (Throwable t) {
            return Response.serverError().entity(t.getMessage()).build();
        } finally {
            endpoint.reset();
        }

        return Response.ok().build();
    }

    @GET
    @Path("/preload")
    public void dataSetPreloaded() throws Exception {
        SimpleDataSet preloadedDataSet = CamelContextHelper.lookup(context, "preloadedDataSet", SimpleDataSet.class);
        DataSetEndpoint endpoint = context.getEndpoint("dataset:preloadedDataSet?preloadSize=5", DataSetEndpoint.class);
        endpoint.setExpectedMessageCount((int) preloadedDataSet.getSize());

        context.getRouteController().startRoute("preload");
        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            context.getRouteController().stopRoute("preload");
            endpoint.reset();
        }

    }

    @GET
    @Path("/test/seda")
    public void dataSetTestSeda() throws Exception {
        producerTemplate.sendBody("seda:dataSetTestSeda", "Hello World");

        // We do this here so that dataset-test can pull the messages sent to the seda endpoint
        context.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:dataSetTestSeda").id("dataSetTestSeda")
                        .to("dataset-test:seda:dataSetTestSeda?timeout=0");
            }
        });

        DataSetTestEndpoint endpoint = context.getEndpoint("dataset-test:seda:dataSetTestSeda?timeout=0",
                DataSetTestEndpoint.class);
        endpoint.expectedMessageCount(1);

        producerTemplate.sendBody("direct:dataSetTestSeda", "Hello World");

        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            context.removeRoute("dataSetTestSeda");
            endpoint.reset();
        }
    }

    @GET
    @Path("/test/seda/any/order")
    public void dataSetTestSedaAnyOrder() throws Exception {
        producerTemplate.sendBody("seda:dataSetTestSedaAnyOrder", "Hello World");
        producerTemplate.sendBody("seda:dataSetTestSedaAnyOrder", "Bye World");

        // We do this here so that dataset-test can pull the messages sent to the seda endpoint
        context.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:dataSetTestSedaAnyOrder").id("dataSetTestSedaAnyOrder")
                        .to("dataset-test:seda:dataSetTestSedaAnyOrder?timeout=0&anyOrder=true");
            }
        });

        DataSetTestEndpoint endpoint = context.getEndpoint("dataset-test:seda:dataSetTestSedaAnyOrder?timeout=0&anyOrder=true",
                DataSetTestEndpoint.class);
        endpoint.expectedMessageCount(2);

        producerTemplate.sendBody("direct:dataSetTestSedaAnyOrder", "Bye World");
        producerTemplate.sendBody("direct:dataSetTestSedaAnyOrder", "Hello World");

        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            context.removeRoute("dataSetTestSedaAnyOrder");
            endpoint.reset();
        }
    }

    @GET
    @Path("/test/split")
    public void dataSetTestSplit() throws Exception {
        java.nio.file.Path path = Files.createTempFile("dataSetTestSplit", ".txt");
        Files.write(path, "Hello\nWorld\nBye\nWorld".getBytes(StandardCharsets.UTF_8));

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:dataSetTestSplit").id("dataSetTestSplit")
                        .toF("dataset-test:file:%s?noop=true&split=true&timeout=1000&fileName=%s", path.getParent(),
                                path.getFileName());
            }
        });

        DataSetTestEndpoint endpoint = context.getEndpoint(
                String.format("dataset-test:file:%s?noop=true&split=true&timeout=1000&fileName=%s", path.getParent(),
                        path.getFileName()),
                DataSetTestEndpoint.class);
        endpoint.expectedMessageCount(4);

        producerTemplate.sendBody("direct:dataSetTestSplit", "Hello");
        producerTemplate.sendBody("direct:dataSetTestSplit", "World");
        producerTemplate.sendBody("direct:dataSetTestSplit", "Bye");
        producerTemplate.sendBody("direct:dataSetTestSplit", "World");

        try {
            endpoint.assertIsSatisfied(5000);
        } finally {
            context.removeRoute("dataSetTestSplit");
            endpoint.reset();
            Files.deleteIfExists(path);
        }
    }
}
