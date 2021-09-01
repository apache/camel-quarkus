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
package org.apache.camel.quarkus.component.openapijava.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.restassured.builder.ResponseBuilder;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jboss.logging.Logger;

/**
 * Enable YAML responses to be inspected with REST Assured JsonPath expressions.
 *
 * Inspired by the original microprofile-open-api source.
 *
 * https://github.com/eclipse/microprofile-open-api/blob/master/tck/src/main/java/org/eclipse/microprofile/openapi/tck/utils/YamlToJsonFilter.java
 */
public class YamlToJsonFilter implements OrderedFilter {

    private static final Logger LOG = Logger.getLogger(YamlToJsonFilter.class);

    @Override
    public int getOrder() {
        return OrderedFilter.HIGHEST_PRECEDENCE;
    }

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
            FilterContext ctx) {
        try {
            Response response = ctx.next(requestSpec, responseSpec);

            if (response.getContentType().equals("text/yaml")) {
                String yaml = response.getBody().asString();
                if (LOG.isDebugEnabled()) {
                    LOG.debug(yaml);
                }

                ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
                Object obj = yamlReader.readValue(yaml, Object.class);

                ObjectMapper jsonWriter = new ObjectMapper();
                String json = jsonWriter.writeValueAsString(obj);

                ResponseBuilder builder = new ResponseBuilder();
                builder.clone(response);
                builder.setBody(json);
                builder.setContentType(ContentType.JSON);
                return builder.build();
            }

            return response;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert the request: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
