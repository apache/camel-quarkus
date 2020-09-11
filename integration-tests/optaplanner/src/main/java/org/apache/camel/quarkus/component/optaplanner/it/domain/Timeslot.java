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
package org.apache.camel.quarkus.component.optaplanner.it.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

import javax.validation.constraints.NotNull;

import org.optaplanner.core.api.domain.lookup.PlanningId;

/**
 * adapted from optaplanner quarkus quickstart :
 * https://github.com/quarkusio/quarkus-quickstarts/blob/master/optaplanner-quickstart/src/main/java/org/acme/optaplanner/domain/TimeTable.java
 */
public class Timeslot {

    @PlanningId
    @NotNull
    private Long id;

    @NotNull
    private DayOfWeek dayOfWeek;
    @NotNull
    private LocalTime startTime;
    @NotNull
    private LocalTime endTime;

    private static AtomicLong increment = new AtomicLong(1);

    public Timeslot() {
    }

    public Timeslot(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
        this.id = increment.getAndIncrement();
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Long getId() {
        return id;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return dayOfWeek + " " + startTime;
    }

}
