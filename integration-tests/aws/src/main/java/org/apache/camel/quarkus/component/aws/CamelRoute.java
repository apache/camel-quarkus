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
package org.apache.camel.quarkus.component.aws;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.camel.builder.RouteBuilder;

@RegisterForReflection
public class CamelRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("timer:quarkus-s3?repeatCount=1")
                .setHeader("CamelAwsS3Key", constant("testquarkus"))
                .setBody(constant("Quarkus is great!"))
                .to("aws-s3://camel-kafka-connector")
                .to("log:sf?showAll=true");

        from("timer:quarkus-sqs?repeatCount=1")
                .setBody(constant("Quarkus is great!"))
                .to("aws-sqs://camel-1")
                .to("log:sf?showAll=true");

        from("timer:quarkus-eks?repeatCount=1")
                .setHeader("CamelAwsEKSOperation", constant("listClusters"))
                .to("aws-eks://cluster")
                .to("log:sf?showAll=true");

        from("timer:quarkus-sns?repeatCount=1")
                .setBody(constant("Quarkus is great!"))
                .to("aws-sns://topic1")
                .to("log:sf?showAll=true");

        from("timer:quarkus-kms?repeatCount=1")
                .setHeader("CamelAwsKMSOperation", constant("listKeys"))
                .to("aws-kms://cluster")
                .to("log:sf?showAll=true");

        from("timer:quarkus-ecs?repeatCount=1")
                .to("aws-ecs://cluster?operation=listClusters")
                .to("log:sf?showAll=true");

        from("timer:quarkus-iam?repeatCount=1")
                .to("aws-iam://cluster?operation=listAccessKeys")
                .to("log:sf?showAll=true");

        from("timer:quarkus-ec2?repeatCount=1")
                .to("aws-ec2://cluster?operation=describeInstances")
                .to("log:sf?showAll=true");
    }

}
