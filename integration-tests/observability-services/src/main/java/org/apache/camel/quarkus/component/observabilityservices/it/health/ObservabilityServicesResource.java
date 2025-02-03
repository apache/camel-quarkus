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
package org.apache.camel.quarkus.component.observabilityservices.it.health;

import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.FluentProducerTemplate;

@Path("/observability-services")
public class ObservabilityServicesResource {
    @Inject
    FluentProducerTemplate fluentProducerTemplate;

    @Inject
    PrometheusMeterRegistry prometheusMeterRegistry;

    @GET
    @Path("/registry")
    @Produces(MediaType.TEXT_PLAIN)
    public Response prometheusMeterRegistry() {
        if (prometheusMeterRegistry != null) {
            return Response.ok().entity(prometheusMeterRegistry.getClass().getName()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("/trace")
    @Produces(MediaType.TEXT_PLAIN)
    public String trace(String message) {
        return fluentProducerTemplate.to("direct:start")
                .withBody(message)
                .request(String.class);
    }

    @GET
    @Path("/jmx/attribute")
    @Produces(MediaType.TEXT_PLAIN)
    public String getMBeanAttribute(@QueryParam("name") String name, @QueryParam("attribute") String attribute)
            throws Exception {
        ObjectInstance mbean = getMBean(name);
        if (mbean != null) {
            return String.valueOf(getMBeanServer().getAttribute(mbean.getObjectName(), attribute));
        }
        return null;
    }

    private ObjectInstance getMBean(String name) throws MalformedObjectNameException {
        ObjectName objectName = new ObjectName(name);
        Set<ObjectInstance> mbeans = getMBeanServer().queryMBeans(objectName, null);
        Iterator<ObjectInstance> iterator = mbeans.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }

    private MBeanServer getMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }
}
