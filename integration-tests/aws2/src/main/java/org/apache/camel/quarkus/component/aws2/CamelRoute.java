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
package org.apache.camel.quarkus.component.aws2;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws2.cw.Cw2Constants;

public class CamelRoute extends RouteBuilder {

    @Override
    public void configure() {

        from("timer:quarkus-sqs?repeatCount=1")
                .setBody(constant("Quarkus is great!"))
                .to("aws2-sqs://camel-1?delaySeconds=5")
                .to("log:sf?showAll=true");

        from("timer:quarkus-s3?repeatCount=1")
                .setHeader("CamelAwsS3Key", constant("testquarkus"))
                .setBody(constant("Quarkus is great!"))
                .to("aws2-s3://camel-kafka-connector")
                .to("log:sf?showAll=true");

        from("timer:quarkus-sns?repeatCount=1")
                .setBody(constant("Quarkus is great!"))
                .to("aws2-sns://topic1")
                .to("log:sf?showAll=true");

        from("timer:quarkus-ec2?repeatCount=1")
                .to("aws2-ec2://instance?operation=describeInstances")
                .to("log:sf?showAll=true");

        from("timer:quarkus-cw?repeatCount=1")
                .setBody(constant("Quarkus is great!"))
                .setHeader(Cw2Constants.METRIC_NAME, constant("ExchangesCompleted"))
                .setHeader(Cw2Constants.METRIC_VALUE, constant("2.0"))
                .setHeader(Cw2Constants.METRIC_UNIT, constant("Count"))
                .to("aws2-cw://test")
                .to("log:sf?showAll=true");
    }

}
