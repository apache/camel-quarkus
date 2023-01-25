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
package org.apache.camel.quarkus.component.pg.replication.slot.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class PgReplicationSlotRoute extends RouteBuilder {

    public static final String PG_AUTHORITY_CFG_KEY = "quarkus.camel.pg-replication-slot.test.authority";
    public static final String PG_DBNAME_CFG_KEY = "quarkus.camel.pg-replication-slot.test.db-name";
    public static final String PG_PASSRD_CFG_KEY = "quarkus.camel.pg-replication-slot.test.password";
    public static final String PG_USER_CFG_KEY = "quarkus.camel.pg-replication-slot.test.user";

    private static final String URI_FORMAT = "pg-replication-slot://{{%s}}/{{%s}}/{{%s}}_test_slot:test_decoding?user={{%s}}&password={{%s}}&slotOptions.skip-empty-xacts=true&slotOptions.include-xids=false";

    @Inject
    PgReplicationSlotResource resource;

    @Override
    public void configure() {
        fromF(URI_FORMAT, PG_AUTHORITY_CFG_KEY, PG_DBNAME_CFG_KEY, PG_DBNAME_CFG_KEY, PG_USER_CFG_KEY, PG_PASSRD_CFG_KEY)
                .process(e -> {
                    resource.logReplicationEvent(e.getMessage().getBody(String.class));
                });
    }

}
