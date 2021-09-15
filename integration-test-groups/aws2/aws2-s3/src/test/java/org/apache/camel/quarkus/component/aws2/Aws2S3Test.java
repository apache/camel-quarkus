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
package org.apache.camel.quarkus.component.aws2;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.apache.camel.quarkus.test.support.aws2.Aws2TestResource;

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2S3Test extends Aws2S3BaseTest {

    @Override
    public String getBaseUri() {
        return "/aws2/s3";
    }
}

@QuarkusTest
@QuarkusTestResource(Aws2TestResource.class)
class Aws2S3QuarkusTest extends Aws2S3BaseTest {

    @Override
    public String getBaseUri() {
        return "/aws2/s3-quarkus";
    }
}
