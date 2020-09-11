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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.optaplanner.OptaPlannerConstants;
import org.apache.camel.quarkus.component.optaplanner.it.bootstrap.DataGenerator;
import org.apache.camel.quarkus.component.optaplanner.it.domain.TimeTable;
import org.jboss.logging.Logger;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;

@Path("/optaplanner")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class OptaplannerResource {

    private static final Logger LOG = Logger.getLogger(OptaplannerResource.class);

    public static final Long SINGLETON_TIME_TABLE_ID = 1L;

    @Inject
    SolverManager<TimeTable, Long> solverManager;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    MyBean bean;

    @GET
    @Path("solveSync")
    public TimeTable solveSync() {
        if (SolverStatus.NOT_SOLVING == solverManager.getSolverStatus(SINGLETON_TIME_TABLE_ID)) {
            TimeTable finalSolution = producerTemplate.requestBodyAndHeader(
                    "direct:solveSync", DataGenerator.timeTable,
                    OptaPlannerConstants.SOLVER_MANAGER, solverManager, TimeTable.class);
            return finalSolution;
        }
        return DataGenerator.timeTable;
    }

    @GET
    @Path("solveAsync")
    public TimeTable solveAsync() throws ExecutionException, InterruptedException {
        // reset best Solution
        bean.setBestSolution(null);
        if (SolverStatus.NOT_SOLVING == solverManager.getSolverStatus(SINGLETON_TIME_TABLE_ID)) {
            CompletableFuture<TimeTable> response = producerTemplate.asyncRequestBodyAndHeader(
                    "direct:solveAsync", DataGenerator.timeTable, OptaPlannerConstants.SOLVER_MANAGER,
                    solverManager, TimeTable.class);

            TimeTable finalSolution = response.get();
            return finalSolution;
        }
        return DataGenerator.timeTable;
    }

    @GET
    @Path("newBestSolution")
    public TimeTable getNewBestSolution() {
        return bean.getBestSolution();
    }

}
