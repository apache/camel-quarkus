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

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.hbase.HBaseAttribute;
import org.apache.camel.component.hbase.HBaseConstants;
import org.apache.camel.component.hbase.model.HBaseData;

@Path("/hbase")
@ApplicationScoped
public class HbaseResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Path("/get/{table}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public HBaseData get(@PathParam("table") String table) {
        return consumerTemplate.receiveBody("hbase://" + table, 5000, HBaseData.class);
    }

    @Path("/put/{table}/{id}/{family}/{column}")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response put(String body, @PathParam("table") String table, @PathParam("id") String id,
            @PathParam("family") String family, @PathParam("column") String column) throws Exception {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(HBaseAttribute.HBASE_ROW_ID.asHeader(), id);
        headers.put(HBaseAttribute.HBASE_FAMILY.asHeader(), family);
        headers.put(HBaseAttribute.HBASE_QUALIFIER.asHeader(), column);
        headers.put(HBaseAttribute.HBASE_VALUE.asHeader(), body);
        headers.put(HBaseConstants.OPERATION, HBaseConstants.PUT);

        producerTemplate.sendBodyAndHeaders("hbase://" + table, null, headers);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }
}
