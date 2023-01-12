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
rest {
    configuration {
        contextPath = "/root"
    }

    path("/my/path") {
        get("/get") {
            id("routes-with-rest-get")
            produces("text/plain")
            to("direct:get")
        }
    }

    post {
        id("routes-with-rest-post")
        path("/post")
        consumes("text/plain")
        produces("text/plain")
        to("direct:post")
    }
}

from("direct:get")
    .id("routes-with-rest-dsl-get")
    .transform().constant("Hello World")
from("direct:post")
    .id("routes-with-rest-dsl-post")
    .setBody().simple("Hello \${body}")

from("direct:routes-with-rest")
    .id("routes-with-rest")
    .process().message {
        m -> m.headers["AnotherHeader"] = "AnotherHeaderValue"
    }
    .filter().simple("\${header.AnotherHeader} == 'AnotherHeaderValue'")
    .setBody().constant("true")
