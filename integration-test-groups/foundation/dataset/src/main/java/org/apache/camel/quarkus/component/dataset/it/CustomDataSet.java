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
package org.apache.camel.quarkus.component.dataset.it;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.dataset.DataSetEndpoint;
import org.apache.camel.component.dataset.DataSetSupport;

public class CustomDataSet extends DataSetSupport {
    @Override
    public void assertMessageExpected(DataSetEndpoint dataSetEndpoint, Exchange expected, Exchange actual, long index) {
        Message expectedMessage = expected.getMessage();
        Message actualMessage = actual.getMessage();

        String expectedBody = expectedMessage.getBody(String.class);
        String actualBody = actualMessage.getBody(String.class);
        if (!expectedBody.equals(actualBody)) {
            throw new AssertionError(String.format("Expected body %s but got %s", expectedBody, actualBody));
        }

        String expectedHeader = expectedMessage.getHeader("foo", String.class);
        if (expectedHeader == null) {
            throw new AssertionError("Expected header foo is null");
        }

        String actualHeader = actualMessage.getHeader("foo", String.class);
        if (actualHeader == null) {
            throw new AssertionError("Actual header foo is null");
        }

        if (!expectedHeader.equals(actualHeader)) {
            throw new AssertionError(String.format("Expected header foo to equal %s but got %s", expectedHeader, actualHeader));
        }
    }

    @Override
    protected Object createMessageBody(long messageIndex) {
        // Custom OutputTransformer processor will append ' World' to the body
        // See DataSetProducers.customDataSet
        return "Hello";
    }

    @Override
    protected void populateDefaultHeaders(Map<String, Object> map) {
        map.put("foo", "bar");
    }
}
