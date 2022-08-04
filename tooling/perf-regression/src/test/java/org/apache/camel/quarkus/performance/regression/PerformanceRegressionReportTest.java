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
package org.apache.camel.quarkus.performance.regression;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerformanceRegressionReportTest {

    @Test
    public void printAllShouldSucceed() throws IOException {
        PerformanceRegressionReport sut = new PerformanceRegressionReport("10m");
        sut.setCategoryMeasureForVersion("2.10.0", "JVM", 360.0);
        sut.setCategoryMeasureForVersion("2.8.0", "JVM", 380.0);
        sut.setCategoryMeasureForVersion("2.9.0", "JVM", 390.0);

        sut.setCategoryMeasureForVersion("2.10.0", "Native", 1000.0);
        sut.setCategoryMeasureForVersion("2.8.0", "Native", 1080.0);
        sut.setCategoryMeasureForVersion("2.9.0", "Native", 1090.0);

        String expected = IOUtils.resourceToString("/perf-regression-expecteds/nominal.txt", StandardCharsets.UTF_8);
        assertEquals(expected, sut.printAll());
    }

}
