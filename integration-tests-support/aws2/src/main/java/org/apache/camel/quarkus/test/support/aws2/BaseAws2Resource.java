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
package org.apache.camel.quarkus.test.support.aws2;

import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

/**
 * Parent for testing resources for aws2 extensions.
 *
 * This class adds endpoints to force default credentials to be used + provides variable useDefaultCredentials
 * to be used in inherited classes.
 *
 * Rest endpoints are expected by test classes.
 */
public class BaseAws2Resource {

    private static final Logger LOG = Logger.getLogger(BaseAws2Resource.class);

    private boolean useDefaultCredentials;

    private final String serviceName;

    private boolean clearAwsredentials;

    public BaseAws2Resource(String serviceName) {
        this.serviceName = serviceName;
    }

    @Path("/setUseDefaultCredentialsProvider")
    @POST
    public Response setUseDefaultCredentials(boolean useDefaultCredentialsProvider) throws Exception {
        this.useDefaultCredentials = useDefaultCredentialsProvider;
        return Response.ok().build();
    }

    @Path("/initializeDefaultCredentials")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response initializeDefaultCredentials(boolean initialize) throws Exception {
        if (initialize) {
            //set flag to clear at the end
            clearAwsredentials = true;
            LOG.debug(
                    "Setting both System.properties `aws.secretAccessKey` and `aws.accessKeyId` to cover defaultCredentialsProviderTest.");
            //defaultCredentials provider gets the credentials from fixed location. One of them is system.properties,
            //therefore to succeed the test, system.properties has to be initialized with the values from the configuration
            Aws2Helper.setAwsSystemCredentials(
                    ConfigProvider.getConfig().getValue("camel.component.aws2-" + serviceName + ".access-key", String.class),
                    ConfigProvider.getConfig().getValue("camel.component.aws2-" + serviceName + ".secret-key", String.class));

        } else {
            LOG.debug("Clearing both System.properties `aws.secretAccessKey` and `aws.accessKeyId`.");
            Aws2Helper.clearAwsSystemCredentials();
            clearAwsredentials = false;
        }

        return Response.ok().build();
    }

    /**
     * Listeners ensures, that system credentials are cleared at the end of the lifecycle.
     * Tests are clearing them by itself, this is just a precaution.
     */
    void onStop(@Observes ShutdownEvent ev) {
        if (clearAwsredentials) {
            Aws2Helper.clearAwsSystemCredentials();
        }
    }

    public boolean isUseDefaultCredentials() {
        return useDefaultCredentials;
    }
}
