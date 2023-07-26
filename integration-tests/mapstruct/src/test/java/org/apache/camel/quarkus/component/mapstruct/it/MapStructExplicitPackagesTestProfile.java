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
package org.apache.camel.quarkus.component.mapstruct.it;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;
import org.apache.camel.quarkus.component.mapstruct.it.mapper.car.CarMapper;
import org.apache.camel.quarkus.component.mapstruct.it.mapper.vehicle.VehicleMapper;

public class MapStructExplicitPackagesTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        // Join package names with extra whitespace to verify it is trimmed at build time
        return Map.of("camel.component.mapstruct.mapper-package-name",
                String.join(" , ", CarMapper.class.getPackageName(), VehicleMapper.class.getPackageName()));
    }
}
