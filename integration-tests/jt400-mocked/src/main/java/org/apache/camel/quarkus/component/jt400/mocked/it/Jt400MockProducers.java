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
package org.apache.camel.quarkus.component.jt400.mocked.it;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ibm.as400.access.AS400ConnectionPool;
import com.ibm.as400.access.MockAS400;
import com.ibm.as400.access.MockAS400ImplRemote;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

public class Jt400MockProducers {

    @Produces
    @ApplicationScoped
    @Named("collected-data")
    public Map<String, List<String>> collectedData() {
        Map<String, List<String>> result = new HashMap<>();
        result.put("queue", new CopyOnWriteArrayList<>());
        return result;
    }

    //-------------------------- mocked backend ------------------------------------------------

    @Produces
    @ApplicationScoped
    MockAS400ImplRemote produceMockAS400ImplRemote() {
        return new MockAS400ImplRemote();
    }

    @Produces
    @ApplicationScoped
    MockAS400 produceMockAS400(MockAS400ImplRemote as400ImplRemote) {
        return new MockAS400(as400ImplRemote);
    }

    @Produces
    @ApplicationScoped
    @Named("mockPool")
    AS400ConnectionPool produceConnectionPool(MockAS400 mockAS400) {
        return new MockAS400ConnectionPool(mockAS400);
    }
}
