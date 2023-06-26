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

import java.nio.charset.Charset;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.netty.handler.codec.http.FullHttpRequest;
import jakarta.inject.Named;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.netty.http.JAASSecurityAuthenticator;
import org.apache.camel.component.netty.http.NettyHttpComponent;
import org.apache.camel.component.netty.http.NettyHttpConfiguration;
import org.apache.camel.component.netty.http.NettyHttpMessage;
import org.apache.camel.component.netty.http.NettyHttpSecurityConfiguration;
import org.apache.camel.component.netty.http.SecurityConstraintMapping;
import org.apache.camel.component.rest.RestConstants;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.quarkus.component.netty.http.auth.TestAuthenticator;
import org.apache.camel.quarkus.component.netty.http.pojo.UserPojo;
import org.eclipse.microprofile.config.ConfigProvider;

public class NettyHttpRoutes extends RouteBuilder {
    @Named("jaasConfig")
    NettyHttpSecurityConfiguration jaasConfig() {
        final NettyHttpSecurityConfiguration config = new NettyHttpSecurityConfiguration();
        config.setRealm("Quarkus");
        JAASSecurityAuthenticator jaas = new JAASSecurityAuthenticator();
        jaas.setName("Quarkus");
        config.setSecurityAuthenticator(jaas);
        return config;
    }

    @Named("securityConfig")
    NettyHttpSecurityConfiguration securityConfiguration() {
        final NettyHttpSecurityConfiguration config = new NettyHttpSecurityConfiguration();
        config.setRealm("Quarkus");
        config.setSecurityAuthenticator(new TestAuthenticator());
        return config;
    }

    // Each ACL configuration is using a separate route/endpoint, so all of them are using /* mapping
    @Named("acladmin")
    NettyHttpSecurityConfiguration aclAdmin() {
        final NettyHttpSecurityConfiguration config = securityConfiguration();
        SecurityConstraintMapping securityConstraintMapping = new SecurityConstraintMapping();
        securityConstraintMapping.addInclusion("/*", "admin");
        config.setSecurityConstraint(securityConstraintMapping);
        return config;
    }

    @Named("aclguest")
    NettyHttpSecurityConfiguration aclGuest() {
        final NettyHttpSecurityConfiguration config = securityConfiguration();
        SecurityConstraintMapping securityConstraintMapping = new SecurityConstraintMapping();
        securityConstraintMapping.addInclusion("/*", "admin,guest");
        config.setSecurityConstraint(securityConstraintMapping);
        return config;
    }

    @Named("aclpublic")
    NettyHttpSecurityConfiguration aclPublic() {
        final NettyHttpSecurityConfiguration config = securityConfiguration();
        SecurityConstraintMapping securityConstraintMapping = new SecurityConstraintMapping();
        securityConstraintMapping.addExclusion("/*");
        config.setSecurityConstraint(securityConstraintMapping);
        return config;
    }

    @Named("aclwildcard")
    NettyHttpSecurityConfiguration aclWildcard() {
        final NettyHttpSecurityConfiguration config = securityConfiguration();
        SecurityConstraintMapping securityConstraintMapping = new SecurityConstraintMapping();
        securityConstraintMapping.addInclusion("/*", "*");
        config.setSecurityConstraint(securityConstraintMapping);
        return config;
    }

    @Named("netty-http")
    NettyHttpComponent component() {
        NettyHttpComponent component = new NettyHttpComponent();
        NettyHttpConfiguration config = new NettyHttpConfiguration();
        // This helps to stabilize the tests when running on windows, as occasionally when invoking the same route from a parameterized test,
        // the next request got the same channel as the previous request that was not fully done yet and it caused the next test to fail.
        config.setReuseChannel(true);
        component.setConfiguration(config);
        return component;
    }

    @Override
    public void configure() throws Exception {
        restConfiguration().component("netty-http")
                .host("localhost").port(ConfigProvider.getConfig().getValue("camel.netty-http.restPort", Integer.class));

        from("netty-http:http://localhost:{{camel.netty-http.port}}/request")
                .process(ex -> {
                    final FullHttpRequest req = ex.getIn(NettyHttpMessage.class).getHttpRequest();
                    ex.getIn().setBody(
                            String.join(",", req.method().name(), req.content().toString(Charset.defaultCharset()),
                                    StreamSupport.stream(req.headers().spliterator(), false)
                                            .map(h -> h.getKey() + ":" + h.getValue()).collect(Collectors.joining(","))));
                });

        from("netty-http:http://localhost:{{camel.netty-http.port}}/response").transform().simple("Received message ${body}");

        from("netty-http:http://localhost:{{camel.netty-http.port}}/auth?securityConfiguration=#securityConfig").log("success");

        from("netty-http:http://localhost:{{camel.netty-http.port}}/jaas?securityConfiguration=#jaasConfig").log("success");

        from("netty-http:http://localhost:{{camel.netty-http.port}}/acls/admin?securityConfiguration=#acladmin").log("success");
        from("netty-http:http://localhost:{{camel.netty-http.port}}/acls/guest?securityConfiguration=#aclguest").log("success");
        from("netty-http:http://localhost:{{camel.netty-http.port}}/acls/wildcard?securityConfiguration=#aclwildcard")
                .log("success");
        from("netty-http:http://localhost:{{camel.netty-http.port}}/acls/public?securityConfiguration=#aclpublic")
                .log("success");

        from("netty-http:http://localhost:{{camel.netty-http.port}}/wildcard?matchOnUriPrefix=true").setBody()
                .constant("wildcard matched");

        from("netty-http:proxy://localhost:{{camel.netty-http.proxyPort}}?reuseChannel=false")
                .toD("netty-http:http://${headers." + Exchange.HTTP_HOST + "}:${headers." + Exchange.HTTP_PORT + "}/${headers."
                        + Exchange.HTTP_PATH + "}");
        from("netty-http:http://localhost:{{camel.netty-http.port}}/proxy").setBody().constant("proxy");

        rest("/rest")
                .get("/").to("direct:printMethod")
                .post("/").to("direct:printMethod")
                .put("/").to("direct:printMethod")
                .post("/json").bindingMode(RestBindingMode.json).consumes("application/json").type(UserPojo.class)
                .to("direct:printBody")
                .post("/xml").bindingMode(RestBindingMode.xml).consumes("application/xml").type(UserPojo.class)
                .to("direct:printBody");

        from("direct:printMethod").setBody().header(RestConstants.HTTP_METHOD);
        from("direct:printBody").process(e -> {
            e.getIn().setHeader(Exchange.CONTENT_TYPE, "text/plain");
            e.getIn().setBody(e.getIn().getBody(UserPojo.class).toString());
        });
    }
}
