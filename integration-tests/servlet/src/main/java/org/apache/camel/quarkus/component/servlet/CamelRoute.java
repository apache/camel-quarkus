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
package org.apache.camel.quarkus.component.servlet;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class CamelRoute extends RouteBuilder {
    @Inject
    MultiPartProcessor multiPartProcessor;

    @Inject
    @Named("servletConfigInfoProcessor")
    Processor servletConfigInfoProcessor;

    @Override
    public void configure() {
        // by default the camel-quarkus-rest component sets platform-http
        // as the component that provides the transport and since here we
        // are testing the servlet component. we have to force it
        restConfiguration()
                .component("servlet");

        rest()
                .get("/rest-get")
                .to("seda:echoMethodPath")

                .post("/rest-post")
                .to("seda:echoMethodPath")

                .put("/rest-put")
                .to("seda:echoMethodPath")

                .patch("/rest-patch")
                .to("seda:echoMethodPath")

                .delete("/rest-delete")
                .to("seda:echoMethodPath")

                .head("/rest-head")
                .to("seda:echoMethodPath");

        from("servlet://hello?matchOnUriPrefix=true")
                .to("seda:echoMethodPath");

        from("servlet://options?servletName=options-method-servlet&optionsEnabled=true")
                .to("seda:echoMethodPath");

        from("servlet://trace?servletName=trace-method-servlet&traceEnabled=true")
                .to("seda:echoMethodPath");

        from("servlet://transfer/exception?transferException=true&muteException=false")
                .throwException(new CustomException());

        from("servlet://params")
                .setBody().simple("${header.prefix} ${header.suffix}");

        from("servlet://configuration")
                .process(servletConfigInfoProcessor);

        from("servlet://custom?servletName=custom-servlet")
                .setBody(constant("GET: /custom"));

        from("servlet://named?servletName=my-named-servlet")
                .setBody(constant("GET: /my-named-servlet"));

        from("seda:echoMethodPath")
                .setBody().simple("${header.CamelHttpMethod}: ${header.CamelServletContextPath}");

        from("servlet://multipart/default?attachmentMultipartBinding=true")
                .process(multiPartProcessor);

        from("servlet://multipart?servletName=multipart-servlet&attachmentMultipartBinding=true")
                .process(multiPartProcessor);

        from("servlet://eager-init?servletName=eager-init-servlet&matchOnUriPrefix=true")
                .setHeader("servletName").constant("eager-init-servlet")
                .process(servletConfigInfoProcessor);

        from("servlet://async?servletName=async-servlet&matchOnUriPrefix=true")
                .setHeader("servletName").constant("async-servlet")
                .process(servletConfigInfoProcessor);

        from("servlet://force-await?servletName=sync-async-servlet&matchOnUriPrefix=true")
                .setHeader("servletName").constant("sync-async-servlet")
                .process(servletConfigInfoProcessor);

        from("servlet://execute?servletName=custom-executor-servlet&matchOnUriPrefix=true")
                .setHeader("servletName").constant("custom-executor-servlet")
                .process(servletConfigInfoProcessor);
    }
}
