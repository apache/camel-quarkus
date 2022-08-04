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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.artifact.versioning.ComparableVersion;

import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

/**
 * Provide a human readable performance regression report ready to be printed to
 * the console. For each camel-quarkus version, the report will print:
 * + The throughput for each measure in a category (JVM, Native) in a new column 
 * + The percent increase throughput compared to the previous row in the same column
 */
public class PerformanceRegressionReport {

    private String duration;
    private TreeMap<ComparableVersion, Map<String, Double>> measuresPerVersion = new TreeMap<>();

    public PerformanceRegressionReport(String duration) {
        this.duration = duration;
    }

    public void setCategoryMeasureForVersion(String cqVersion, String category, double throughput) {
        ComparableVersion version = new ComparableVersion(cqVersion);
        measuresPerVersion.computeIfAbsent(version, k -> new HashMap<>()).put(category, throughput);
    }

    public String printAll() {
        Table table = Table.create("Camel Quarkus Throughput Performance Increase Compared to Previous Version");

        StringColumn cqVersionsColumn = StringColumn.create("Camel Quarkus version");
        StringColumn durationsColumn = StringColumn.create("Duration");
        StringColumn jvmMeasuresColumn = StringColumn.create("JVM req/s [%increase]");
        StringColumn nativeMeasuresColumn = StringColumn.create("Native req/s [%increase]");
        StringColumn statusColumn = StringColumn.create("Status");
        double previousJvmMeasure = Double.POSITIVE_INFINITY;
        double previousNativeMeasure = Double.POSITIVE_INFINITY;

        for (Map.Entry<ComparableVersion, Map<String, Double>> measurePerVersion : measuresPerVersion.entrySet()) {
            cqVersionsColumn.append(measurePerVersion.getKey().toString());
            durationsColumn.append(duration);
            boolean regressionDetected = false;

            double jvmMeasure = measurePerVersion.getValue().get("JVM");
            double percentIncreaseJvm = (previousJvmMeasure == Double.POSITIVE_INFINITY) ? 0.0 : ((jvmMeasure / previousJvmMeasure) - 1.0) * 100.0;
            jvmMeasuresColumn.append(String.format("%.2f req/s [%+.2f%%]", jvmMeasure, percentIncreaseJvm));
            previousJvmMeasure = jvmMeasure;
            if (percentIncreaseJvm <= -5.00) {
                regressionDetected = true;
            }

            if (measurePerVersion.getValue().containsKey("Native")) {
                double nativeMeasure = measurePerVersion.getValue().get("Native");
                double percentIncreaseNative = (previousNativeMeasure == Double.POSITIVE_INFINITY) ? 0.0 : ((nativeMeasure / previousNativeMeasure) - 1.0) * 100.0;
                nativeMeasuresColumn.append(String.format("%.2f req/s [%+.2f%%]", nativeMeasure, percentIncreaseNative));
                previousNativeMeasure = nativeMeasure;
                if (percentIncreaseNative <= -5.00) {
                    regressionDetected = true;
                }
            }

            statusColumn.append(regressionDetected ? "Potential performance regression" : "OK");
        }

        if (!nativeMeasuresColumn.isEmpty()) {
            table.addColumns(cqVersionsColumn, durationsColumn, jvmMeasuresColumn, nativeMeasuresColumn, statusColumn);
        } else {
            table.addColumns(cqVersionsColumn, durationsColumn, jvmMeasuresColumn, statusColumn);
        }

        return table.printAll();
    }

}
