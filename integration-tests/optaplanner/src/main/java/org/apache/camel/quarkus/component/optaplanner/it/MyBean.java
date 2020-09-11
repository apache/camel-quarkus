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

import javax.enterprise.context.ApplicationScoped;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.Exchange;
import org.apache.camel.component.optaplanner.OptaPlannerConstants;
import org.apache.camel.quarkus.component.optaplanner.it.domain.TimeTable;

@RegisterForReflection
@ApplicationScoped
public class MyBean {

    public TimeTable bestSolution;

    public TimeTable getBestSolution() {
        return bestSolution;
    }

    public void setBestSolution(TimeTable bestSolution) {
        this.bestSolution = bestSolution;
    }

    public void updateBestSolution(Exchange exchange) {
        if (exchange != null) {
            TimeTable newBestSolution = exchange.getMessage().getHeader(OptaPlannerConstants.BEST_SOLUTION, TimeTable.class);
            if (newBestSolution != null) {
                this.bestSolution = newBestSolution;
            }
        }
    }
}
