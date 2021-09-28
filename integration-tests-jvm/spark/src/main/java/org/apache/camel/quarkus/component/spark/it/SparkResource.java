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
package org.apache.camel.quarkus.component.spark.it;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.spark.DataFrameCallback;
import org.apache.camel.component.spark.RddCallback;
import org.apache.camel.component.spark.Sparks;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaRDDLike;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.hive.HiveContext;

import static org.apache.camel.component.spark.SparkConstants.SPARK_DATAFRAME_CALLBACK_HEADER;
import static org.apache.camel.component.spark.SparkConstants.SPARK_RDD_CALLBACK_HEADER;

@Path("/spark")
@ApplicationScoped
public class SparkResource {

    @Inject
    CamelContext context;

    String sparkUri = "spark:rdd?rdd=#testFileRdd";

    String sparkDataFrameUri = "spark:dataframe?dataFrame=#jsonCars";

    String sparkHiveUri = "spark:hive";

    private JavaSparkContext sparkContext;
    private HiveContext hiveContext;
    private java.nio.file.Path rddFilePath;
    private java.nio.file.Path carsJsonPath;

    @PostConstruct
    void init() {
        this.sparkContext = Sparks.createLocalSparkContext();
        this.hiveContext = new HiveContext(sparkContext.sc());
        try {
            java.nio.file.Path tmpDir = Paths.get("target/tmp");
            Files.createDirectories(tmpDir);

            this.rddFilePath = copyResource(tmpDir, "testrdd.txt");
            this.carsJsonPath = copyResource(tmpDir, "cars.json");
        } catch (IOException e) {
            throw new RuntimeException("Could not create a temporary file", e);
        }

    }

    private java.nio.file.Path copyResource(java.nio.file.Path tmpDir, final String resource) throws IOException {
        final java.nio.file.Path file = tmpDir.resolve(resource);
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            Files.copy(in, file);
        }
        return file;
    }

    @javax.enterprise.inject.Produces
    @Named
    JavaRDD<String> testFileRdd() {
        return sparkContext.textFile(rddFilePath.toString());
    }

    @javax.enterprise.inject.Produces
    @Named
    Dataset<Row> jsonCars() {
        Dataset<Row> jsonCars = hiveContext.read().json(carsJsonPath.toString());
        jsonCars.registerTempTable("cars");
        return jsonCars;
    }

    @javax.enterprise.inject.Produces
    @Named
    RddCallback countLinesTransformation() {
        return new org.apache.camel.component.spark.RddCallback() {
            @Override
            public Object onRdd(JavaRDDLike rdd, Object... payloads) {
                return rdd.count();
            }
        };
    }

    @javax.enterprise.inject.Produces
    @Named
    HiveContext hiveContext() {
        return hiveContext;
    }

    @Inject
    ProducerTemplate template;

    @Path("/rdd/count")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Long rddCount() throws Exception {
        return template.requestBodyAndHeader(sparkUri, null, SPARK_RDD_CALLBACK_HEADER,
                new org.apache.camel.component.spark.RddCallback() {
                    @Override
                    public Long onRdd(JavaRDDLike rdd, Object... payloads) {
                        return rdd.count();
                    }
                }, Long.class);
    }

    @Path("/dataframe/{model}/count")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Long dataframeCount(@PathParam("model") String model) throws Exception {
        return template.requestBodyAndHeader(sparkDataFrameUri, model, SPARK_DATAFRAME_CALLBACK_HEADER,
                new DataFrameCallback<Long>() {
                    @Override
                    public Long onDataFrame(Dataset<Row> dataFrame, Object... payloads) {
                        String model = (String) payloads[0];
                        return dataFrame.where(dataFrame.col("model").eqNullSafe(model)).count();
                    }
                },
                Long.class);
    }

    @Path("/hive/count")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Long hiveCount() throws Exception {
        return template.requestBody(sparkHiveUri + "?collect=false", "SELECT * FROM cars", Long.class);
    }

}
