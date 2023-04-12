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
package org.apache.camel.quarkus.component.mybatis.it;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.quarkus.component.mybatis.it.entity.Account;

@Path("/mybatis")
@ApplicationScoped
public class MybatisResource {
    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate template;

    @Path("/selectOne")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Account selectOne(@QueryParam("id") Integer id) {
        Account account = template.requestBody("direct:selectOne", id, Account.class);
        if (account == null) {
            throw new NotFoundException();
        }
        return account;
    }

    @Path("/selectList")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List selectList() {
        return template.requestBody("direct:selectList", null, List.class);
    }

    @Path("/insertOne")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Integer insertOne(Account account) {
        template.sendBody("direct:insertOne", account);
        return getCounts();
    }

    @Path("/insertList")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Integer insertList(List<Account> accounts) {
        template.sendBody("direct:insertList", accounts);
        return getCounts();
    }

    @Path("/deleteOne")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Integer deleteOne(@QueryParam("id") Integer id) {
        template.sendBody("direct:deleteOne", id);
        return getCounts();
    }

    @Path("/deleteList")
    @DELETE
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Integer deleteList(List<Integer> ids) {
        template.sendBody("direct:deleteList", ids);
        return getCounts();
    }

    @Path("/updateOne")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Integer updateOne(Account account) {
        template.sendBody("direct:updateOne", account);
        return getCounts();
    }

    @Path("/updateList")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Integer updateList(Map<String, Object> params) {
        template.sendBody("direct:updateList", params);
        return getCounts();
    }

    private Integer getCounts() {
        return template.requestBody("mybatis:count?statementType=SelectOne", null, Integer.class);
    }

    @Path("/consumer")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List consumer() throws Exception {
        MockEndpoint results = context.getEndpoint("mock:results", MockEndpoint.class);
        results.expectedMessageCount(2);

        context.getRouteController().startRoute("mybatis-consumer");
        MockEndpoint.assertIsSatisfied(context);

        return template.requestBody("mybatis:selectProcessedAccounts?statementType=SelectList", null, List.class);
    }
}
