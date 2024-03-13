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
package org.apache.camel.quarkus.component.jt400.it.mock;

import java.util.Map;

import com.ibm.as400.access.DataStream;
import com.ibm.as400.access.MockAS400ImplRemote;
import com.ibm.as400.access.MockedResponses;
import com.ibm.as400.access.ReplyDQCommon;
import com.ibm.as400.access.ReplyDQReadNormal;
import com.ibm.as400.access.ReplyDQRequestAttributesNormal;
import com.ibm.as400.access.ReplyOk;
import com.ibm.as400.access.ReplyRCCallProgram;
import com.ibm.as400.access.ReplyRCExchangeAttributes;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jt400.Jt400Endpoint;
import org.jboss.logging.Logger;

@Path("/jt400/mock")
@ApplicationScoped
public class Jt400MockResource {

    public enum ReplyType {
        DQReadNormal, ok, DQRequestAttributesNormal, DQCommonReply, RCExchangeAttributesReply, RCCallProgramReply
    }

    private static final Logger LOG = Logger.getLogger(Jt400MockResource.class);

    private static final String COMPONENT_JT400 = "jt400";

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    MockAS400ImplRemote as400ImplRemote;

    @Path("/keyedDataQueue/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response keyedDataQueueRead() {

        Exchange ex = consumerTemplate.receive(
                "jt400://username:password@system/qsys.lib/MSGOUTDQ.DTAQ?connectionPool=#mockPool&keyed=true&format=binary&searchKey=MYKEY&searchType=GE");

        return Response.ok().entity(ex.getIn().getBody(String.class)).build();
    }

    @Path("/keyedDataQueue/write/{key}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response keyedDataQueueWrite(@PathParam("key") String key, String data) throws Exception {

        Object ex = producerTemplate.requestBodyAndHeader(
                "jt400://username:password@system/qsys.lib/MSGINDQ.DTAQ?connectionPool=#mockPool&keyed=true",
                data,
                Jt400Endpoint.KEY,
                key);
        return Response.ok().entity(ex).build();
    }

    @Path("/messageQueue/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response messageQueueRead() throws InterruptedException {
        Exchange ex = consumerTemplate.receive(
                "jt400://username:password@system/qsys.lib/MSGOUTQ.MSGQ?connectionPool=#mockPool&readTimeout=100");
        if (ex.getIn().getBody() != null) {
            //reurn ok,because something is returned (the message contains 1 char, which is not correctly converted)
            return Response.ok().build();
        }

        return Response.serverError().build();
    }

    @Path("/messageQueue/write/{key}")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response messageQueueWrite(@PathParam("key") String key, String data) throws Exception {

        Object ex = producerTemplate.requestBodyAndHeader(
                "jt400://username:password@system/qsys.lib/MSGINQ.MSGQ?connectionPool=#mockPool",
                data,
                Jt400Endpoint.KEY,
                key);
        return Response.ok().entity(ex).build();
    }

    @Path("/programCall")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response programCall() throws Exception {

        Object ex = producerTemplate.requestBody(
                "jt400://GRUPO:ATWORK@server/QSYS.LIB/assets.LIB/compute.PGM?connectionPool=#mockPool&outputFieldsIdx=1&fieldsLength=10,10,512",
                new String[] { "par1", "par2" });
        return Response.ok().entity(ex).build();
    }

    @Path("/put/mockResponse")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putMockResponse(
            Map params) throws Exception {
        DataStream dataStream = switch (ReplyType.valueOf((String) params.get("replyType"))) {
        case DQReadNormal -> new ReplyDQReadNormal((Integer) params.get("hashCode"),
                (String) params.get("senderInformation"),
                (String) params.get("entry"),
                (String) params.get("key"));
        case ok -> new ReplyOk();
        case DQCommonReply -> new ReplyDQCommon(
                (Integer) params.get("hashCode"));
        case DQRequestAttributesNormal -> new ReplyDQRequestAttributesNormal(
                (Integer) params.get("keyLength"));
        case RCExchangeAttributesReply -> new ReplyRCExchangeAttributes();
        case RCCallProgramReply -> new ReplyRCCallProgram();
        };

        MockedResponses.add(dataStream);

        return Response.ok().build();
    }

}
