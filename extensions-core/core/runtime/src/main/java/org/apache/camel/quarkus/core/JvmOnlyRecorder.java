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
package org.apache.camel.quarkus.core;

import io.quarkus.runtime.annotations.Recorder;
import org.jboss.logging.Logger;

@Recorder
public class JvmOnlyRecorder {
    private static final Logger LOG = Logger.getLogger(JvmOnlyRecorder.class);

    public static void warnJvmInNative(Logger log, String feature) {
        log.warnf(
                "The %s extension was not tested in native mode."
                        + " You may want to report about the success or failure running it in native mode on https://github.com/apache/camel-quarkus/issues?q=is%%3Aissue+%s",
                feature, feature.replace('-', '+'));
    }

    public void warnJvmInNative(String feature) {
        warnJvmInNative(LOG, feature);
    }

}
