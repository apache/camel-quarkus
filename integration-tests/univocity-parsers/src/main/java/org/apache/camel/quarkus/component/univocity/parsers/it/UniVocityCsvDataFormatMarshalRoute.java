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
package org.apache.camel.quarkus.component.univocity.parsers.it;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.univocity.UniVocityCsvDataFormat;
import org.apache.camel.spi.DataFormat;

public class UniVocityCsvDataFormatMarshalRoute extends RouteBuilder {

    @Override
    public void configure() {
        final Map<String, DataFormat> testsDataformat = new HashMap<>();

        // Default writing of CSV
        testsDataformat.put("default", new UniVocityCsvDataFormat());

        // Write a CSV with specific headers
        testsDataformat.put("header", new UniVocityCsvDataFormat()
                .setHeaders(new String[] { "A", "C" }));

        // Write a CSV with an advanced configuration
        testsDataformat.put("advanced", new UniVocityCsvDataFormat()
                .setNullValue("N/A")
                .setEmptyValue("empty")
                .setQuote('_')
                .setQuoteAllFields(true)
                .setQuoteEscape('-')
                .setDelimiter(';'));

        for (Map.Entry<String, DataFormat> testDataformat : testsDataformat.entrySet()) {
            from("direct:csv-marshal-" + testDataformat.getKey()).marshal(testDataformat.getValue());
        }
    }

}
