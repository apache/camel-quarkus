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
package org.apache.camel.quarkus.component.docling.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class DoclingRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        // Route to convert document to Markdown
        from("direct:convertToMarkdown")
                .to("docling:convert?operation=CONVERT_TO_MARKDOWN&contentInBody=true")
                .log("Converted to Markdown: ${body}");

        // Route to convert document to HTML
        from("direct:convertToHtml")
                .to("docling:convert?operation=CONVERT_TO_HTML&contentInBody=true")
                .log("Converted to HTML: ${body}");

        // Route to extract text from document
        from("direct:extractText")
                .to("docling:convert?operation=EXTRACT_TEXT&contentInBody=true")
                .log("Extracted text: ${body}");

        // Route to extract metadata from document
        from("direct:extractMetadata")
                .to("docling:convert?operation=EXTRACT_METADATA&contentInBody=true")
                .log("Extracted metadata: ${body}");

        // Route to convert document to JSON
        from("direct:convertToJson")
                .to("docling:convert?operation=CONVERT_TO_JSON&contentInBody=true")
                .log("Converted to JSON: ${body}");

        // Async route to convert document to Markdown
        from("direct:convertToMarkdownAsync")
                .to("docling:convert?operation=CONVERT_TO_MARKDOWN&contentInBody=true&useAsyncMode=true")
                .log("Converted to Markdown (async): ${body}");

        // Async route to convert document to HTML
        from("direct:convertToHtmlAsync")
                .to("docling:convert?operation=CONVERT_TO_HTML&contentInBody=true&useAsyncMode=true")
                .log("Converted to HTML (async): ${body}");

        // Async route to convert document to JSON
        from("direct:convertToJsonAsync")
                .to("docling:convert?operation=CONVERT_TO_JSON&contentInBody=true&useAsyncMode=true")
                .log("Converted to JSON (async): ${body}");

        from("direct:convertToJsonWithCLI")
                .to("docling:convert?operation=CONVERT_TO_JSON&contentInBody=true&useDoclingServe=false");
    }
}
