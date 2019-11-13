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
package org.apache.camel.quarkus.component.xslt.it;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/xslt")
@ApplicationScoped
public class XsltResource {

    private static final Logger LOG = Logger.getLogger(XsltResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/get")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String get() throws Exception {
        String body = "<mail><subject>Hey</subject><body>Hello world!</body></mail>";
        String message = producerTemplate.requestBody("direct:start", body, String.class);
        LOG.infof("Received from xslt: %s", message);
        return message;
    }

}
