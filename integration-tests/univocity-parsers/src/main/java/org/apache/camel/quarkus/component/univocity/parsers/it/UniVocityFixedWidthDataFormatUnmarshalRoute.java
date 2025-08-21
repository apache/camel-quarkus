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
import org.apache.camel.dataformat.univocity.UniVocityFixedDataFormat;
import org.apache.camel.spi.DataFormat;

public class UniVocityFixedWidthDataFormatUnmarshalRoute extends RouteBuilder {

    @Override
    public void configure() {
        final Map<String, DataFormat> testsDataformat = new HashMap<>();

        // Default reading of fixed-width
        UniVocityFixedDataFormat def = new UniVocityFixedDataFormat();
        def.setFieldLengths("3,3,5");
        testsDataformat.put("default", def);

        // Reading fixed-width as Map
        UniVocityFixedDataFormat map = new UniVocityFixedDataFormat();
        map.setFieldLengths("3,3,5");
        map.setAsMap(true);
        map.setHeaderExtractionEnabled(true);
        testsDataformat.put("map", map);

        // Reading fixed-width as Map with specific headers
        UniVocityFixedDataFormat mapWithHeaders = new UniVocityFixedDataFormat();
        mapWithHeaders.setFieldLengths("3,3,5");
        mapWithHeaders.setAsMap(true);
        mapWithHeaders.setHeaders("A,B,C");
        testsDataformat.put("mapWithHeaders", mapWithHeaders);

        // Reading fixed-width using an iterator
        UniVocityFixedDataFormat lazy = new UniVocityFixedDataFormat();
        lazy.setFieldLengths("3,3,5");
        lazy.setLazyLoad(true);
        testsDataformat.put("lazy", lazy);

        // Reading fixed-width using advanced configuration
        UniVocityFixedDataFormat advanced = new UniVocityFixedDataFormat();
        advanced.setFieldLengths("3,3");
        advanced.setNullValue("N/A");
        advanced.setPadding('_');
        advanced.setComment('!');
        advanced.setSkipEmptyLines(true);
        testsDataformat.put("advanced", advanced);

        for (Map.Entry<String, DataFormat> testDataformat : testsDataformat.entrySet()) {
            from("direct:fixed-width-unmarshal-" + testDataformat.getKey()).unmarshal(testDataformat.getValue());
        }
    }

}
