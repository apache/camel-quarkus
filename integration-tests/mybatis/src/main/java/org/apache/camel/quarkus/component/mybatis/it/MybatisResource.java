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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.mybatis.it.entity.Account;

@Path("/mybatis")
@ApplicationScoped
public class MybatisResource {

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

    @Path("/insertOne")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Transactional
    public Integer insertOne(Account account) {
        template.sendBody("direct:insertOne", account);
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

    private Integer getCounts() {
        return template.requestBody("mybatis:count?statementType=SelectOne", null, Integer.class);
    }
}
