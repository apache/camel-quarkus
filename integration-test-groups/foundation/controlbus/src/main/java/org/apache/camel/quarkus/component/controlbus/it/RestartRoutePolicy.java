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
package org.apache.camel.quarkus.component.controlbus.it;

import jakarta.inject.Singleton;
import org.apache.camel.Route;
import org.apache.camel.support.RoutePolicySupport;

@Singleton
public class RestartRoutePolicy extends RoutePolicySupport {

    private int startCount = 0;
    private int stopCount = 0;

    @Override
    public void onStart(Route route) {
        this.startCount++;
    }

    @Override
    public void onStop(Route route) {
        this.stopCount++;
    }

    public int getStart() {
        return this.startCount;
    }

    public int getStop() {
        return this.stopCount;
    }

    public void reset() {
        this.startCount = 0;
        this.stopCount = 0;
    }
}
