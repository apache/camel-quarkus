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
package org.apache.camel.quarkus.component.dataformat.it;

import java.time.ZonedDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import net.fortuna.ical4j.model.Calendar;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.dataformat.it.model.TestPojo;

@Path("/dataformat")
@ApplicationScoped
public class DataformatResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/snakeyaml/marshal/{route}")
    @GET
    @Produces("text/yaml")
    public String snakeYamlmarshal(@PathParam("route") String route, @QueryParam("name") String name) {
        return producerTemplate.requestBody("direct:snakeyaml-" + route + "-marshal", new TestPojo(name), String.class);
    }

    @Path("/snakeyaml/unmarshal/{route}")
    @POST
    @Consumes("text/yaml")
    @Produces(MediaType.TEXT_PLAIN)
    public String snakeYamlUnmarshal(@PathParam("route") String route, String yaml) throws Exception {
        TestPojo pojo = producerTemplate.requestBody("direct:snakeyaml-" + route + "-unmarshal", yaml, TestPojo.class);
        return pojo.getName();
    }

    @Path("/ical/marshal")
    @GET
    @Produces("text/calendar")
    public String icalmarshal(
            @QueryParam("start") String start,
            @QueryParam("end") String end,
            @QueryParam("summary") String summary,
            @QueryParam("attendee") String attendee) {
        Calendar cal = ICalUtils.createTestCalendar(ZonedDateTime.parse(start), ZonedDateTime.parse(end), summary, attendee);
        return producerTemplate.requestBody("direct:ical-marshal", cal, String.class);
    }

    @Path("/ical/unmarshal")
    @POST
    @Consumes("text/calendar")
    @Produces(MediaType.TEXT_PLAIN)
    public String icalUnmarshal(String body) throws Exception {
        final Calendar cal = producerTemplate.requestBody("direct:ical-unmarshal", body, Calendar.class);
        return cal.toString();
    }
}
