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
package org.apache.camel.quarkus.core;

import io.quarkus.test.junit.DisabledOnIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class CoreThreadPoolsTest {

    @Test
    public void testDefaultThreadPoolConfiguredByProperties() {
        get("/core/thread-pools/default").then().body(is("default|true|5|10|20|DiscardOldest"));
    }

    @DisabledOnIntegrationTest("https://github.com/apache/camel-quarkus/issues/4011")
    @Test
    public void testCustomThreadPoolsConfiguredByProperties() {
        get("/core/thread-pools/customPool").then().body(is("customPool|false|1|10|20|Abort"));
    }

}
