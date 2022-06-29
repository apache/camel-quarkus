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
package org.apache.camel.quarkus.component.quartz.it;

import org.apache.camel.builder.RouteBuilder;

public class QuartzRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("quartz:quartz/1 * * * * ?")
                .setBody(constant("Hello Camel Quarkus quartz"))
                .to("seda:quartz-result");

        from("cron:tab?schedule=0/1 * * * * ?")
                .setBody(constant("Hello Camel Quarkus cron"))
                .to("seda:cron-result");

        from("quartzFromProperties:properties/* 1 * * * ")
                .setBody(constant("Hello Camel Quarkus Quartz Properties"))
                .to("seda:quartz-properties-result");

        // cron trigger
        from("quartz://cronTrigger?cron=0/1+*+*+*+*+?&trigger.timeZone=Europe/Stockholm")
                .setBody(constant("Hello Camel Quarkus Quartz From Cron Trigger"))
                .to("seda:quartz-cron-trigger-result");

        from("quartz://misfire?cron=0/1+*+*+*+*+?&trigger.timeZone=Europe/Stockholm&trigger.misfireInstruction=2")
                .to("seda:quartz-cron-misfire-result");
    }
}
