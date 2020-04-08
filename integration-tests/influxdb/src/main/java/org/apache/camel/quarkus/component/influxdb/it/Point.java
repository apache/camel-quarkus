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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Point {

    private String measurement;
    private Long time;
    private Map<String, Long> fields = new HashMap<>();

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Map<String, Long> getFields() {
        return fields;
    }

    public void addField(String key, Long value) {
        this.fields.put(key, value);
    }

    public void setFields(Map<String, Long> fields) {
        this.fields = fields;
    }

    public org.influxdb.dto.Point toPoint() {
        org.influxdb.dto.Point.Builder pointBuilder = org.influxdb.dto.Point.measurement(this.measurement)
                .time(this.time, TimeUnit.MILLISECONDS);

        this.fields.entrySet().stream().forEach(e -> pointBuilder.addField(e.getKey(), e.getValue()));

        return pointBuilder.build();
    }
}
