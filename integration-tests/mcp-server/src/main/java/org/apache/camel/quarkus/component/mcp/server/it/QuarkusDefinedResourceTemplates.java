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
package org.apache.camel.quarkus.component.mcp.server.it;

import io.quarkiverse.mcp.server.RequestUri;
import io.quarkiverse.mcp.server.ResourceTemplate;
import io.quarkiverse.mcp.server.TextResourceContents;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Declarative resource templates used to verify that Camel routes coexist safely with application templates.
 */
@ApplicationScoped
public class QuarkusDefinedResourceTemplates {

    @ResourceTemplate(name = "annotated_resource_template", uriTemplate = "crm://annotated/{id}", description = "Template declared by the application")
    TextResourceContents annotated(RequestUri uri, String id) {
        return TextResourceContents.create(uri.value(), "annotated " + id);
    }

    @ResourceTemplate(name = "name_clash_template", uriTemplate = "crm://annotated-name/{id}", description = "Template whose name is also used by a Camel route")
    TextResourceContents nameClash(RequestUri uri, String id) {
        return TextResourceContents.create(uri.value(), "annotated name " + id);
    }

    // Quarkiverse MCP Server accepts URI-template variables in query parameters. Camel's
    // AiResourceUriTemplate parser intentionally does not parse this form.
    @ResourceTemplate(name = "query_resource_template", uriTemplate = "crm://search?q={term}", description = "Search template with a query parameter")
    TextResourceContents search(RequestUri uri, String term) {
        return TextResourceContents.create(uri.value(), "search " + term);
    }
}
