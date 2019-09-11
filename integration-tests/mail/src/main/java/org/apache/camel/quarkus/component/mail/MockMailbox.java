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
package org.apache.camel.quarkus.component.mail;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.jvnet.mock_javamail.Mailbox;

@Path("/mock/{username}")
@ApplicationScoped
public class MockMailbox {

    @GET
    @Path("/size")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSize(@PathParam("username") String username) throws Exception {
        Mailbox mailbox = Mailbox.get(username);
        return Integer.toString(mailbox.size());
    }

    @GET
    @Path("/{id}/content")
    @Produces(MediaType.TEXT_PLAIN)
    public String getContent(@PathParam("username") String username, @PathParam("id") int id) throws Exception {
        Mailbox mailbox = Mailbox.get(username);
        return mailbox.get(id).getContent().toString();
    }

    @GET
    @Path("/{id}/subject")
    @Produces(MediaType.TEXT_PLAIN)
    public String getSubject(@PathParam("username") String username, @PathParam("id") int id) throws Exception {
        Mailbox mailbox = Mailbox.get(username);
        return mailbox.get(id).getSubject();
    }
}
