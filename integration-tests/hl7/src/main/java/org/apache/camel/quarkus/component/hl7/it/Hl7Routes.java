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
package org.apache.camel.quarkus.component.hl7.it;

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.model.v22.message.ADT_A01;
import ca.uhn.hl7v2.parser.Parser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.component.hl7.HL7.ack;
import static org.apache.camel.component.hl7.HL7.hl7terser;

@ApplicationScoped
public class Hl7Routes extends RouteBuilder {

    @Inject
    Parser parser;

    @Override
    public void configure() throws Exception {
        from("netty:tcp://localhost:{{camel.hl7.test-tcp-port}}?sync=true&encoders=#hl7encoder&decoders=#hl7decoder")
                .convertBodyTo(ADT_A01.class)
                .to("mock:result");

        from("direct:validate")
                .unmarshal("hl7DataFormat");

        from("direct:validateCustom")
                .unmarshal().hl7(false)
                .marshal().hl7(parser);

        from("direct:marshalUnmarshal")
                .unmarshal("hl7DataFormat")
                .marshal("hl7DataFormat");

        from("direct:hl7terser")
                .setHeader("PATIENT_ID", hl7terser("PID-3-1"));

        from("direct:hl7terserBean")
                .bean("terserBean");

        from("direct:unmarshalXml")
                .unmarshal("hl7DataFormat");

        from("direct:ack")
                .unmarshal("hl7DataFormat")
                .transform(ack(AcknowledgmentCode.CA));
    }
}
