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
import javax.inject.Inject;

import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class Routes extends RouteBuilder {
    @Inject
    MyBean bean;

    @Override
    public void configure() throws Exception {

        // async producer
        from("direct:solveAsync").to("optaplanner:anything?useSolverManager=true&async=true&problemId="
                + OptaplannerResource.SINGLETON_TIME_TABLE_ID);

        // async consumer
        from("optaplanner:anything?useSolverManager=true&problemId=" + OptaplannerResource.SINGLETON_TIME_TABLE_ID)
                .bean(bean, "updateBestSolution");

        // sync producer
        from("direct:solveSync")
                .to("optaplanner:anything?useSolverManager=true&problemId=" + OptaplannerResource.SINGLETON_TIME_TABLE_ID);
    }
}
