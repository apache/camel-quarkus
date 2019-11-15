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
package org.apache.camel.quarkus.component.exec.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.util.FileUtil;

public class ExecRoutes extends RouteBuilder {

    private static final boolean IS_WINDOWS = FileUtil.isWindows();

    @Override
    public void configure() throws Exception {
        from("direct:start")
                .toF("exec:%s?args=%s", getCommand(), getArgs());
    }

    private String getCommand() {
        return IS_WINDOWS ? "cmd.exe" : "/bin/echo";
    }

    private String getArgs() {
        return IS_WINDOWS ? "/C echo Hello Camel Quarkus" : "Hello Camel Quarkus";
    }
}
