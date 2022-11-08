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
package org.apache.camel.quarkus.component.file.cluster;

import java.util.concurrent.TimeUnit;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.catalog.impl.TimePatternConverter;
import org.apache.camel.component.file.cluster.FileLockClusterService;

@Recorder
public class FileLockClusterServiceRecorder {

    public RuntimeValue<FileLockClusterService> createFileLockClusterService(FileLockClusterServiceConfig config) {
        FileLockClusterService flcs = new FileLockClusterService();

        config.id.ifPresent(id -> flcs.setId(id));
        config.root.ifPresent(root -> flcs.setRoot(root));
        config.order.ifPresent(order -> flcs.setOrder(order));
        config.acquireLockDelay.ifPresent(delay -> {
            flcs.setAcquireLockDelay(TimePatternConverter.toMilliSeconds(delay), TimeUnit.MILLISECONDS);
        });
        config.acquireLockInterval.ifPresent(interval -> {
            flcs.setAcquireLockInterval(TimePatternConverter.toMilliSeconds(interval), TimeUnit.MILLISECONDS);
        });

        config.attributes.forEach((key, value) -> {
            flcs.setAttribute(key, value);
        });

        return new RuntimeValue<FileLockClusterService>(flcs);
    }

}
