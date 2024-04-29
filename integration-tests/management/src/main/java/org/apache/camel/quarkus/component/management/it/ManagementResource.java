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
package org.apache.camel.quarkus.component.management.it;

import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;

@Path("/management")
public class ManagementResource {

    @Inject
    ProducerTemplate template;

    @Inject
    CamelContext camelContext;

    @GET
    @Path("/context/name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getContextName() {
        return camelContext.getName();
    }

    @GET
    @Path("/attribute")
    @Produces(MediaType.TEXT_PLAIN)
    public String getMBeanAttribute(@QueryParam("name") String name, @QueryParam("attribute") String attribute)
            throws Exception {
        ObjectInstance mbean = getMBean(name);
        if (mbean != null) {
            return String.valueOf(getMBeanServer().getAttribute(mbean.getObjectName(), attribute));
        }
        return null;
    }

    @POST
    @Path("/invoke")
    @Produces(MediaType.TEXT_PLAIN)
    public String invokeMBeanOperation(@QueryParam("name") String name, @QueryParam("operation") String operation)
            throws Exception {
        ObjectInstance mbean = getMBean(name);
        if (mbean != null) {
            return String.valueOf(getMBeanServer().invoke(mbean.getObjectName(), operation, new Object[] {}, new String[] {}));
        }
        return null;
    }

    @POST
    @Path("/invoke/route")
    public String invokeRoute(@QueryParam("endpointUri") String endpointUri) {
        return template.requestBody(endpointUri, null, String.class);
    }

    @PATCH
    @Path("/route")
    public void updateRoute(@QueryParam("routeId") String routeId, String xml) throws Exception {
        String mbeanName = "org.apache.camel:context=%s,type=routes,name=\"%s\"".formatted(getContextName(), routeId);
        ObjectInstance mbean = getMBean(mbeanName);
        getMBeanServer().invoke(mbean.getObjectName(), "updateRouteFromXml",
                new Object[] { xml },
                new String[] { String.class.getName() });
    }

    @DELETE
    @Path("/route")
    public void removeRoute(@QueryParam("routeId") String routeId) throws Exception {
        // No need for JMX in this case as we're just cleaning up for a test case
        camelContext.removeRoute(routeId);
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
