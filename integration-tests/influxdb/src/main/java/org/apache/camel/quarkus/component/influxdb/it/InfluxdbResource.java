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
package org.apache.camel.quarkus.component.influxdb.it;

import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.component.influxdb.InfluxDbConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

@Path("/influxdb")
@ApplicationScoped
public class InfluxdbResource {
    public static final String DB_NAME = "myTestTimeSeries";
    public static final String INFLUXDB_CONNECTION_PROPERTY = "influxdb.connection.url";
    public static final String INFLUXDB_CONNECTION_NAME = "influxDbConnection";

    @Inject
    FluentProducerTemplate producerTemplate;

    @ConfigProperty(name = INFLUXDB_CONNECTION_PROPERTY)
    String connectionUrl;

    @Singleton
    @jakarta.enterprise.inject.Produces
    InfluxDB createInfluxDbConnection() {
        InfluxDB influxDbConnection = InfluxDBFactory.connect(connectionUrl);
        influxDbConnection.query(new Query("CREATE DATABASE " + DB_NAME));

        return influxDbConnection;
    }

    void disposeInfluxDbConnection(@Disposes InfluxDB influxDbConnection) {
        influxDbConnection.query(new Query("DROP DATABASE " + DB_NAME, ""));
        influxDbConnection.close();
    }

    @Path("/ping")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return producerTemplate.toF(
                "influxdb:%s?operation=ping", INFLUXDB_CONNECTION_NAME)
                .request(Pong.class)
                .getVersion();
    }

    @Path("/insert")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean insert(Point point) {
        org.influxdb.dto.Point result = producerTemplate.toF(
                "influxdb:%s?databaseName=%s&operation=insert&retentionPolicy=autogen", INFLUXDB_CONNECTION_NAME, DB_NAME)
                .withBody(point.toPoint())
                .request(org.influxdb.dto.Point.class);

        return result != null;
    }

    @Path("/batch")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public int batch(Points points) {
        return producerTemplate.toF(
                "influxdb:%s?batch=true", INFLUXDB_CONNECTION_NAME)
                .withBody(points.toBatchPoints())
                .request(BatchPoints.class)
                .getPoints().size();
    }

    @Path("/query")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String query(String query) throws Exception {
        QueryResult result = producerTemplate.toF(
                "influxdb:%s?databaseName=%s&operation=query&retentionPolicy=autogen", INFLUXDB_CONNECTION_NAME, DB_NAME)
                .withHeader(InfluxDbConstants.INFLUXDB_QUERY, query)
                .request(QueryResult.class);

        return result.getResults().stream()
                .filter(r -> r.getSeries() != null)
                .flatMap(r -> r.getSeries().stream())
                .map(QueryResult.Series::getName)
                .collect(Collectors.joining(", "));
    }
}
