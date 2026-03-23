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
package org.apache.camel.quarkus.component.aws2.sqs.it;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws2.sqs.Sqs2Constants;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

@ApplicationScoped
public class SelectorRouteBuilder extends RouteBuilder {

    static final String FILTER_ATTRIBUTE_NAME = "filter-type";
    static final String FILTER_ATTRIBUTE_SELECTED_VALUE = "selected";

    @ConfigProperty(name = "aws-sqs.selector-name")
    String selectorQueueName;

    @Inject
    SelectorMessageCollector collector;

    @Override
    public void configure() {
        from("aws2-sqs://" + selectorQueueName
                + "?messageAttributeNames=All&deleteAfterRead=true&deleteIfFiltered=false&defaultVisibilityTimeout=0")
                .filter(exchange -> {
                    Map<?, ?> attrs = exchange.getIn().getHeader(Sqs2Constants.MESSAGE_ATTRIBUTES, Map.class);
                    if (attrs == null) {
                        return false;
                    }
                    Object attrObj = attrs.get(FILTER_ATTRIBUTE_NAME);
                    if (!(attrObj instanceof MessageAttributeValue attrValue)) {
                        return false;
                    }
                    return FILTER_ATTRIBUTE_SELECTED_VALUE.equals(attrValue.stringValue());
                })
                .process(exchange -> collector.collect(exchange.getIn().getBody(String.class)));
    }
}
