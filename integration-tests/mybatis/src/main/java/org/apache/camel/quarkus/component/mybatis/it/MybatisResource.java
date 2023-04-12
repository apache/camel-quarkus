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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
