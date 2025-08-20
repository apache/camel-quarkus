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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.apache.camel.quarkus.component.optaplanner.it.domain.Lesson;
import org.apache.camel.quarkus.component.optaplanner.it.domain.TimeTable;
import org.apache.camel.quarkus.component.optaplanner.it.solver.TimeTableConstraintProvider;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;

/**
 * Replicates optaplanner-quarkus configuration from application.properties
 *
 * TODO: Remove when optaplanner-quarkus > 10.0.0 is available - https://github.com/apache/camel-quarkus/issues/7533
 */
@ApplicationScoped
public class SolverManagerProducers {
    @Singleton
    SolverManager<TimeTable, Long> solverManager() {
        return SolverManager.create(SolverFactory.create(new SolverConfig()
                .withSolutionClass(TimeTable.class)
                .withEntityClasses(Lesson.class)
                .withConstraintProviderClass(TimeTableConstraintProvider.class)
                .withTerminationConfig(new TerminationConfig().withBestScoreLimit("0hard/*soft").withSecondsSpentLimit(30L))));
    }
}
