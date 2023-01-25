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
package org.apache.camel.quarkus.component.grok.it;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/grok")
@ApplicationScoped
public class GrokResource {

    private static final Logger LOG = Logger.getLogger(GrokResource.class);

    @Inject
    ProducerTemplate template;

    @Path("/log")
    @GET
    public String log(String input) {
        LOG.infof("Calling log with %s", input);
        return template.requestBody("direct:log", input, String.class);
    }

    @Path("/fooBar")
    @GET
    public String fooBar(String input) {
        LOG.infof("Calling fooBar with %s", input);
        return template.requestBody("direct:fooBar", input, String.class);
    }

    @Path("/ip")
    @GET
    public String ip(String input) {
        LOG.infof("Calling ip with %s", input);
        return template.requestBody("direct:ip", input, String.class);
    }

    @Path("/qs")
    @GET
    public String qs(String input) {
        LOG.infof("Calling qs with %s", input);
        return template.requestBody("direct:qs", input, String.class);
    }

    @Path("/uuid")
    @GET
    public String uuid(String input) {
        LOG.infof("Calling uuid with %s", input);
        return template.requestBody("direct:uuid", input, String.class);
    }

    @Path("/mac")
    @GET
    public String mac(String input) {
        LOG.infof("Calling mac with %s", input);
        return template.requestBody("direct:mac", input, String.class);
    }

    @Path("/path")
    @GET
    public String path(String input) {
        LOG.infof("Calling path with %s", input);
        return template.requestBody("direct:path", input, String.class);
    }

    @Path("/uri")
    @GET
    public String uri(String input) {
        LOG.infof("Calling uri with %s", input);
        return template.requestBody("direct:uri", input, String.class);
    }

    @Path("/num")
    @GET
    public String num(String input) {
        LOG.infof("Calling num with %s", input);
        return template.requestBody("direct:num", input, String.class);
    }

    @Path("/timestamp")
    @GET
    public String timestamp(String input) {
        LOG.infof("Calling timestamp with %s", input);
        return template.requestBody("direct:timestamp", input, String.class);
    }

    @Path("/flatten")
    @GET
    public String flatten(String input) {
        LOG.infof("Calling flatten with %s", input);
        try {
            template.requestBody("direct:flatten", input, String.class);
        } catch (CamelExecutionException cex) {
            return cex.getCause().getClass().getName();
        }
        return null;
    }

    @Path("/namedOnly")
    @GET
    public String namedOnly(String input) {
        LOG.infof("Calling namedOnly with %s", input);
        Map<?, ?> r = template.requestBody("direct:namedOnly", input, Map.class);
        return String.format("%s-%s+%s", r.containsKey("URIPROTO"), r.containsKey("URIHOST"), r.containsKey("URIPATHPARAM"));
    }

    @Path("/singleMatchPerLine")
    @GET
    public String singleMatchPerLine(String input) {
        LOG.infof("Calling singleMatchPerLine with %s", input);
        return template.requestBody("direct:singleMatchPerLine", input, String.class);
    }

}
