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
package org.apache.camel.quarkus.component.hbase.it;

import java.util.Collections;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.jboss.logging.Logger;

public class HBaseTestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOG = Logger.getLogger(HBaseTestResource.class);
    // must be the same as in the config of camel component
    static final Integer CLIENT_PORT = 21818;

    private HBaseTestingUtility hbaseUtil;

    @Override
    public Map<String, String> start() {
        try {
            Configuration conf = HBaseConfiguration.create();
            conf.set("test.hbase.zookeeper.property.clientPort", CLIENT_PORT.toString());
            hbaseUtil = new HBaseTestingUtility(conf);
            hbaseUtil.startMiniCluster(1);
        } catch (Exception e) {
            throw new RuntimeException("Could not start HBase cluster.", e);
        }
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        try {
            hbaseUtil.shutdownMiniCluster();
        } catch (Exception e) {
            LOG.warn("Error shutting down the HBase container", e);
        }
    }

}
