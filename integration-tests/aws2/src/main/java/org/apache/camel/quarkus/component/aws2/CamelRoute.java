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
import org.apache.camel.component.aws2.translate.Translate2Constants;
import org.apache.camel.component.aws2.translate.Translate2LanguageEnum;

public class CamelRoute extends RouteBuilder {

    @Override
    public void configure() {
        // TODO: Restore these - https://github.com/apache/camel-quarkus/issues/8912
        //        from("timer:quarkus-athena?repeatCount=1")
        //                .to("aws2-athena://cluster?operation=listQueryExecutions")
        //                .to("log:sf?showAll=true");

        from("timer:quarkus-eventbridge?repeatCount=1")
                .to("aws2-eventbridge://default?operation=listRules")
                .to("log:sf?showAll=true");

        // TODO: Restore these - https://github.com/apache/camel-quarkus/issues/8912
        //        from("timer:quarkus-bedrock?repeatCount=1")
        //                .to("aws-bedrock://myaccount?operation=invokeTextModel")
        //                .to("log:sf?showAll=true");

        from("timer:quarkus-translate?repeatCount=1")
                .setHeader(Translate2Constants.SOURCE_LANGUAGE, constant(Translate2LanguageEnum.ITALIAN))
                .setHeader(Translate2Constants.TARGET_LANGUAGE, constant(Translate2LanguageEnum.GERMAN))
                .setBody(constant("Ciao"))
                .to("aws2-translate://cluster?operation=translateText")
                .log("Translation: ${body}");

    }

}
