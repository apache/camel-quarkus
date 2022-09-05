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
package org.apache.camel.quarkus.test.support.google;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.ListIterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleCloudContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCloudContext.class);

    private final ArrayList<AutoCloseable> closeables = new ArrayList<>();
    private final Map<String, String> properties = new LinkedHashMap<>();
    private boolean usingMockBackend;

    /**
     * Add an {@link AutoCloseable} to be closed after running Google Cloud tests
     *
     * @param  closeable the {@link AutoCloseable} to add
     * @return           this {@link GoogleCloudContext}
     */
    public GoogleCloudContext closeable(AutoCloseable closeable) {
        closeables.add(closeable);
        return this;
    }

    /**
     * Close all {@link AutoCloseable}s registered via {@link #closeable(AutoCloseable)}
     */
    public void close() {
        ListIterator<AutoCloseable> it = closeables.listIterator(closeables.size());
        while (it.hasPrevious()) {
            AutoCloseable c = it.previous();
            try {
                c.close();
            } catch (Exception e) {
                LOGGER.warn(String.format("Could not close %s", c), e);
            }
        }
    }

    /**
     * Add a key-value pair to the system properties seen by google cloud tests
     *
     * @param  key
     * @param  value
     * @return       this {@link GoogleCloudContext}
     */
    public GoogleCloudContext property(String key, String value) {
        properties.put(key, value);
        return this;
    }

    /**
     * @return a read-only view of {@link #properties}
     */
    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public boolean isUsingMockBackend() {
        return usingMockBackend;
    }

    void setUsingMockBackend(boolean usingMockBackend) {
        this.usingMockBackend = usingMockBackend;
    }
}
