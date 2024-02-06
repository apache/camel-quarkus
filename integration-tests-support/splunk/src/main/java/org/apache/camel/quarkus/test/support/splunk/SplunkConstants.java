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
package org.apache.camel.quarkus.test.support.splunk;

public final class SplunkConstants {

    public static final String PARAM_TEST_INDEX = "org.apache.camel.quarkus.component.splunk.hec.it.SplunkHecResource_testIndex";
    public static final String PARAM_REMOTE_HOST = "org.apache.camel.quarkus.component.splunk.hec.it.SplunkHecResource_host";
    public static final String PARAM_HEC_PORT = "org.apache.camel.quarkus.component.splunk.hec.it.SplunkHecResource_hecPort";
    public static final String PARAM_HEC_TOKEN = "org.apache.camel.quarkus.component.splunk.hec.it.SplunkHecResource_hecToken";
    public static final String PARAM_REMOTE_PORT = "org.apache.camel.quarkus.component.splunk.hec.it.SplunkHecResource_remotePort";
    public static final String PARAM_TCP_PORT = "org.apache.camel.quarkus.component.splunk.it.SplunkResource_tcpPort";

    public static final int TCP_PORT = 9998;

    private SplunkConstants() {
        // Utility class
    }
}
