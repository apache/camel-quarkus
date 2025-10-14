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
package org.apache.camel.quarkus.component.flink.it;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.flink.DataSetCallback;
import org.apache.camel.component.flink.FlinkConstants;
import org.apache.camel.component.flink.Flinks;
import org.apache.camel.component.flink.VoidDataStreamCallback;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.jboss.logging.Logger;

@Path("/flink")
@ApplicationScoped
public class FlinkResource {

    private static final Logger LOG = Logger.getLogger(FlinkResource.class);

    private static final String COMPONENT_FLINK = "flink";

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate template;

    String flinkDataSetUri = "flink:dataSet?dataSet=#myDataSet";
    String flinkDataStreamUri = "flink:datastream?datastream=#myDataStream";

    @Path("/dataset/{filePath}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response dataSetFromTextFile(@PathParam("filePath") String filePath) {

        if (Files.exists(Paths.get(filePath))) {
            ExecutionEnvironment env = Flinks.createExecutionEnvironment();
            env.getConfiguration().setString("io.tmp.dirs", "target");
            DataSet<String> myDataSet = env.readTextFile(filePath);
            context.getRegistry().bind("myDataSet", myDataSet);
            context.getRegistry().bind("countTotal", addDataSetCallback());
            Long totalCount = template.requestBody(
                    flinkDataSetUri + "&dataSetCallback=#countTotal", null, Long.class);
            return Response.ok(totalCount).build();
        }

        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @Path("/datastream/{filePath}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response loadStream(@PathParam("filePath") String filePath, String data) throws IOException {
        java.nio.file.Path path = Paths.get(filePath);
        if (path != null) {
            Configuration configuration = new Configuration();
            configuration.setString("io.tmp.dirs", "target");
            StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(configuration);
            DataStream<String> datastream = env.fromElements(data);
            context.getRegistry().bind("myDataStream", datastream);
            template.sendBodyAndHeader(flinkDataStreamUri, null,
                    FlinkConstants.FLINK_DATASTREAM_CALLBACK_HEADER,
                    new VoidDataStreamCallback() {
                        @Override
                        public void doOnDataStream(DataStream dataStream, Object... objects) throws Exception {
                            dataStream.writeAsText(filePath,
                                    org.apache.flink.core.fs.FileSystem.WriteMode.OVERWRITE);
                            dataStream.getExecutionEnvironment().execute();
                        }
                    });
            return Response.ok(Files.size(path)).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();

    }

    DataSetCallback addDataSetCallback() {
        return new DataSetCallback() {
            @Override
            public Object onDataSet(DataSet ds, Object... payloads) {
                try {
                    return ds.count();
                } catch (Exception e) {
                    return null;
                }
            }
        };
    }

}
