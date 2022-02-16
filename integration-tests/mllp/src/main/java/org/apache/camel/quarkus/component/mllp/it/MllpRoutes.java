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
package org.apache.camel.quarkus.component.mllp.it;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mllp.MllpConstants;
import org.apache.camel.component.mllp.MllpInvalidMessageException;
import org.eclipse.microprofile.config.ConfigProvider;

@RegisterForReflection(targets = MllpInvalidMessageException.class, fields = false)
public class MllpRoutes extends RouteBuilder {

    public static final String MLLP_HOST = "localhost";

    @Override
    public void configure() throws Exception {
        Integer mllpPort = ConfigProvider.getConfig().getValue("mllp.test.port", Integer.class);

        onException(MllpInvalidMessageException.class)
                .to("mock:invalid");

        fromF("mllp://%s:%d?validatePayload=true", MLLP_HOST, mllpPort)
                .convertBodyTo(String.class);

        from("direct:validMessage")
                .toF("mllp://%s:%d", MLLP_HOST, mllpPort)
                .setBody(header(MllpConstants.MLLP_ACKNOWLEDGEMENT));

        from("direct:invalidMessage")
                .toF("mllp://%s:%d?exchangePattern=InOnly", MLLP_HOST, mllpPort)
                .setBody(header(MllpConstants.MLLP_ACKNOWLEDGEMENT));
    }
}
