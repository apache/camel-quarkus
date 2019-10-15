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
package org.apache.camel.quarkus.component.netty.http;


import java.net.URI;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;

@QuarkusTest
public class CamelTest {

    @Test
    public void testNetty4Http() throws Exception {
        RestAssured.when().get(new URI("http://localhost:8999/foo"))
            .then().body(is("Netty Hello World"));
    }

    @Test
    public void netty4HttpProducer() throws Exception {
        RestAssured.when().get(new URI("http://localhost:8999/producer"))
            .then().statusCode(200).body(is("Netty Hello World"));
    }

}
