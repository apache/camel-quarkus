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
package org.apache.camel.quarkus.component.optaplanner.it;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.optaplanner.OptaPlannerConstants;
import org.apache.camel.quarkus.component.optaplanner.it.bootstrap.DataGenerator;
import org.apache.camel.quarkus.component.optaplanner.it.domain.Room;
import org.apache.camel.quarkus.component.optaplanner.it.domain.TimeTable;
import org.apache.camel.quarkus.component.optaplanner.it.domain.Timeslot;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;

@Path("/optaplanner")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class OptaplannerResource {

    public static final Long SINGLETON_TIME_TABLE_ID = 1L;

    @Inject
    SolverManager<TimeTable, Long> solverManager;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @POST
    @Path("solveSync")
    public void solveSync() {
        if (SolverStatus.NOT_SOLVING == solverManager.getSolverStatus(SINGLETON_TIME_TABLE_ID)) {
            producerTemplate.sendBodyAndHeader("direct:solveSync", DataGenerator.timeTable, OptaPlannerConstants.SOLVER_MANAGER,
                    solverManager);
        }
    }

    @POST
    @Path("solveAsync")
    public void solveAsync() throws ExecutionException, InterruptedException {
        if (SolverStatus.NOT_SOLVING == solverManager.getSolverStatus(SINGLETON_TIME_TABLE_ID)) {
            producerTemplate.sendBodyAndHeader("direct:solveAsync", DataGenerator.timeTable,
                    OptaPlannerConstants.SOLVER_MANAGER, solverManager);
        }
    }

    @POST
    @Path("consumer/{enable}")
    public void mangeOptaplannerConsumer(@PathParam("enable") boolean enable) throws Exception {
        if (enable) {
            context.getRouteController().startRoute("optaplanner-consumer");
        } else {
            context.getRouteController().stopRoute("optaplanner-consumer");
        }
    }

    @GET
    @Path("solution/{mockEndpointUri}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getSolution(@PathParam("mockEndpointUri") String mockEndpointUri) {
        MockEndpoint mockEndpoint = context.getEndpoint("mock:" + mockEndpointUri, MockEndpoint.class);
        Optional<JsonObject> result = mockEndpoint.getReceivedExchanges()
                .stream()
                .map(Exchange::getMessage)
                .map(message -> {
                    if (mockEndpointUri.equals("bestSolution")) {
                        return message.getHeader(OptaPlannerConstants.BEST_SOLUTION, TimeTable.class);
                    } else {
                        return message.getBody(TimeTable.class);
                    }
                })
                .map(this::extractResults)
                .findFirst();

        if (result.isPresent()) {
            mockEndpoint.reset();
            return result.get();
        }
        return extractResults(null);
    }

    private JsonObject extractResults(TimeTable timeTable) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (timeTable != null) {
            Timeslot timeslot = timeTable.getTimeslotList().get(0);
            Room room = timeTable.getRoomList().get(0);
            builder.add("timeslot", timeslot.getId()).add("room", room.getId());
        } else {
            builder.add("timeslot", "-1").add("room", "-1");
        }
        return builder.build();
    }
}
