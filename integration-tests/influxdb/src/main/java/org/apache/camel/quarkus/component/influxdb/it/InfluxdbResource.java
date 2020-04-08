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

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.influxdb.InfluxDbConstants;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.jboss.logging.Logger;

@Path("/influxdb")
@ApplicationScoped
public class InfluxdbResource {

    private static final Logger LOG = Logger.getLogger(InfluxdbResource.class);

    public static final String DB_NAME = "myTestTimeSeries";

    public static final String INFLUXDB_CONNECTION_PROPERTY = "quarkus.influxdb.connection.url";
    public static final String INFLUXDB_VERSION = "1.7.10";

    private static final String INFLUXDB_CONNECTION = "http://{{" + INFLUXDB_CONNECTION_PROPERTY + "}}/";
    private static final String INFLUXDB_CONNECTION_NAME = "influxDb_connection";
    private static final String INFLUXDB_ENDPOINT_URL = "influxdb:" + INFLUXDB_CONNECTION_NAME;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    private InfluxDB influxDB;

    void onStart(@Observes org.apache.camel.quarkus.core.CamelMainEvents.BeforeConfigure ev) {
        influxDB = InfluxDBFactory.connect(context.getPropertiesComponent().parseUri(INFLUXDB_CONNECTION));

        influxDB.query(new Query("CREATE DATABASE " + DB_NAME));

        context.getRegistry().bind(INFLUXDB_CONNECTION_NAME, influxDB);
    }

    void beforeStop(@Observes org.apache.camel.quarkus.core.CamelMainEvents.BeforeStop ev) {
        if (influxDB != null) {
            influxDB.query(new Query("DROP DATABASE " + DB_NAME, ""));
            influxDB.close();
        }
    }

    @Path("/ping")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        Pong pong = producerTemplate.requestBody(INFLUXDB_ENDPOINT_URL + "?operation=ping", null, Pong.class);

        return pong.getVersion();
    }

    @Path("/insert")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public boolean insert(Point point) {
        org.influxdb.dto.Point p = point.toPoint();

        org.influxdb.dto.Point result = producerTemplate.requestBody(
                INFLUXDB_ENDPOINT_URL + "?databaseName=" + DB_NAME + "&operation=insert&retentionPolicy=autogen", p,
                org.influxdb.dto.Point.class);

        return result != null;
    }

    @Path("/batch")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String batch(Points points) {
        BatchPoints p = points.toBatchPoints();

        BatchPoints result = producerTemplate.requestBody(INFLUXDB_ENDPOINT_URL + "?batch=true", p,
                BatchPoints.class);

        return String.valueOf(result.getPoints().size());
    }

    @Path("/query")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String query(String query) throws Exception {
        Exchange exchange = producerTemplate.request(
                INFLUXDB_ENDPOINT_URL + "?databaseName=" + DB_NAME + "&operation=query&retentionPolicy=autogen",
                e -> e.getIn().setHeader(InfluxDbConstants.INFLUXDB_QUERY, query));
        List<QueryResult.Result> results = exchange.getMessage().getBody(QueryResult.class).getResults();
        return results.stream()
                .flatMap(r -> r.getSeries() != null ? r.getSeries().stream() : null)
                .map(s -> s.getName())
                .collect(Collectors.joining(", "));

    }

}
