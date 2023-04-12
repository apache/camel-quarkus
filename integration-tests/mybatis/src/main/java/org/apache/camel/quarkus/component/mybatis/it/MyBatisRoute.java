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

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.component.mybatis.it.entity.Account;

@ApplicationScoped
public class MyBatisRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:selectOne")
                .to("mybatis:selectAccountById?statementType=SelectOne");

        from("direct:selectList")
                .to("mybatis:selectAllAccounts?statementType=SelectList");

        from("direct:insertOne")
                .transacted()
                .to("mybatis:insertAccount?statementType=Insert")
                .process(exchange -> {
                    Account account = exchange.getIn().getBody(Account.class);
                    if (account.getFirstName().equals("Rollback")) {
                        throw new RuntimeException("Rollback");
                    }
                });

        from("direct:insertList")
                .transacted()
                .to("mybatis:batchInsertAccount?statementType=InsertList");

        from("direct:deleteOne")
                .transacted()
                .to("mybatis:deleteAccountById?statementType=Delete");

        from("direct:deleteList")
                .transacted()
                .to("mybatis:batchDeleteAccountById?statementType=DeleteList");

        from("direct:updateOne")
                .transacted()
                .to("mybatis:updateAccount?statementType=Update");

        from("direct:updateList")
                .transacted()
                .to("mybatis:batchUpdateAccount?statementType=UpdateList");

        from("mybatis:selectUnprocessedAccounts?onConsume=consumeAccount").routeId("mybatis-consumer").autoStartup(false)
                .to("mock:results");
    }
}
