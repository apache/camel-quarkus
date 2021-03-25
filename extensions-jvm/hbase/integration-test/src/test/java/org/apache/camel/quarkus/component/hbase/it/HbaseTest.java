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

import java.io.IOException;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;

@QuarkusTest
@QuarkusTestResource(HBaseTestResource.class)
class HbaseTest {

    @Test
    public void e2e() throws IOException, InterruptedException {

        createTable("testtable", "family-1");

        RestAssured.given()
                .body("foo")
                .post("/hbase/put/testtable/1/family-1/column-1")
                .then()
                .statusCode(201);

        RestAssured.get("/hbase/get/testtable")
                .then()
                .statusCode(200)
                .body("rows.size()", is(1),
                        "rows[0].id", is("1"),
                        "rows[0].cells.size()", is(1),
                        "rows[0].cells[0].family", is("family-1"),
                        "rows[0].cells[0].qualifier", is("column-1"),
                        "rows[0].cells[0].value", is("foo"));
    }

    protected void createTable(String name, byte[][] families) throws IOException {
        TableDescriptorBuilder builder = TableDescriptorBuilder.newBuilder(TableName.valueOf(name));
        for (byte[] fam : families) {
            builder.setColumnFamily(ColumnFamilyDescriptorBuilder.of(fam));
        }
        connectHBase().getAdmin().createTable(builder.build());
    }

    protected void createTable(String name, String family) throws IOException {
        createTable(name, new byte[][] { family.getBytes() });
    }

    protected Connection connectHBase() throws IOException {
        Connection connection = ConnectionFactory.createConnection(defaultConf());
        return connection;
    }

    public static Configuration defaultConf() {
        Configuration conf = HBaseConfiguration.create();
        conf.set("test.hbase.zookeeper.property.clientPort", HBaseTestResource.CLIENT_PORT.toString());
        return conf;
    }

}
