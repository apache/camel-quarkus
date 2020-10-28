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
package org.apache.camel.quarkus.component.nagios.it;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.nagios.NagiosConstants;
import org.jboss.logging.Logger;

@Path("/nagios")
@ApplicationScoped
public class NagiosResource {

    public static final String NSCA_PORT_CFG_KEY = "quarkus.camel.nagios.test.nsca-port";
    public static final String NSCA_HOST_CFG_KEY = "quarkus.camel.nagios.test.nsca-host";

    private static final Logger LOG = Logger.getLogger(NagiosResource.class);

    @Inject
    ProducerTemplate template;

    @Path("/send")
    @GET
    public void send() {
        LOG.infof("Calling send()");

        Map<String, Object> headers = new HashMap<>();
        headers.put(NagiosConstants.LEVEL, "CRITICAL");
        headers.put(NagiosConstants.HOST_NAME, "myHost");
        headers.put(NagiosConstants.SERVICE_NAME, "myService");
        String uri = String.format("nagios:{{%s}}:{{%s}}?password=secret", NSCA_HOST_CFG_KEY, NSCA_PORT_CFG_KEY);
        template.sendBodyAndHeaders(uri, "Hello Nagios", headers);
    }
}
