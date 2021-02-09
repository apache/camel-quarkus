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
import org.apache.camel.component.aws2.translate.Translate2Constants;
import org.apache.camel.component.aws2.translate.Translate2LanguageEnum;

public class CamelRoute extends RouteBuilder {

    @Override
    public void configure() {

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

        from("timer:quarkus-translate?repeatCount=1")
                .setHeader(Translate2Constants.SOURCE_LANGUAGE, constant(Translate2LanguageEnum.ITALIAN))
                .setHeader(Translate2Constants.TARGET_LANGUAGE, constant(Translate2LanguageEnum.GERMAN))
                .setBody(constant("Ciao"))
                .to("aws2-translate://cluster?operation=translateText")
                .log("Translation: ${body}");

        from("timer:quarkus-ecs?repeatCount=1")
                .to("aws2-ecs://cluster?operation=listClusters")
                .to("log:sf?showAll=true");

        from("timer:quarkus-eks?repeatCount=1")
                .setHeader("CamelAwsEKSOperation", constant("listClusters"))
                .to("aws2-eks://cluster")
                .to("log:sf?showAll=true");

        from("timer:quarkus-iam?repeatCount=1")
                .to("aws2-iam://cluster?operation=listAccessKeys")
                .to("log:sf?showAll=true");

        from("timer:quarkus-kms?repeatCount=1")
                .setHeader("CamelAwsKMSOperation", constant("listKeys"))
                .to("aws2-kms://cluster");

        from("timer:quarkus-mq?repeatCount=1")
                .to("aws2-mq://test?operation=listBrokers")
                .to("log:sf?showAll=true");

        from("timer:quarkus-msk?repeatCount=1")
                .to("aws2-msk://cluster?operation=listClusters")
                .to("log:sf?showAll=true");

        from("timer:quarkus-athena?repeatCount=1")
                .to("aws2-athena://cluster?operation=listQueryExecutions")
                .to("log:sf?showAll=true");

        from("timer:quarkus-lambda?repeatCount=1")
                .to("aws2-lambda://cluster?operation=listFunctions")
                .to("log:sf?showAll=true");

        from("timer:quarkus-sts?repeatCount=1")
                .to("aws2-sts://myaccount?operation=getSessionToken")
                .to("log:sf?showAll=true");

        from("timer:quarkus-eventbridge?repeatCount=1")
                .to("aws2-eventbridge://default?operation=listRules")
                .to("log:sf?showAll=true");

    }

}
