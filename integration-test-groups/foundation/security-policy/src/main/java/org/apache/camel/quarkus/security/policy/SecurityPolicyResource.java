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
package org.apache.camel.quarkus.security.policy;

import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.main.SecurityPolicyResult;

@Path("/security-policy")
@ApplicationScoped
public class SecurityPolicyResource {

    @Inject
    CamelContext camelContext;

    @GET
    @Path("/has-violations")
    @Produces(MediaType.TEXT_PLAIN)
    public String hasViolations() {
        SecurityPolicyResult result = camelContext.getCamelContextExtension()
                .getContextPlugin(SecurityPolicyResult.class);
        if (result == null) {
            return "null";
        }
        return String.valueOf(result.hasViolations());
    }

    @GET
    @Path("/violation-categories")
    @Produces(MediaType.TEXT_PLAIN)
    public String violationCategories() {
        SecurityPolicyResult result = camelContext.getCamelContextExtension()
                .getContextPlugin(SecurityPolicyResult.class);
        if (result == null) {
            return "";
        }
        return result.getViolations().stream()
                .map(v -> v.category())
                .collect(Collectors.joining(","));
    }

    @GET
    @Path("/violation-property-keys")
    @Produces(MediaType.TEXT_PLAIN)
    public String violationPropertyKeys() {
        SecurityPolicyResult result = camelContext.getCamelContextExtension()
                .getContextPlugin(SecurityPolicyResult.class);
        if (result == null) {
            return "";
        }
        return result.getViolations().stream()
                .map(v -> v.propertyKey())
                .collect(Collectors.joining(","));
    }

    @GET
    @Path("/violation-policies")
    @Produces(MediaType.TEXT_PLAIN)
    public String violationPolicies() {
        SecurityPolicyResult result = camelContext.getCamelContextExtension()
                .getContextPlugin(SecurityPolicyResult.class);
        if (result == null) {
            return "";
        }
        return result.getViolations().stream()
                .map(v -> v.policy())
                .collect(Collectors.joining(","));
    }
}
