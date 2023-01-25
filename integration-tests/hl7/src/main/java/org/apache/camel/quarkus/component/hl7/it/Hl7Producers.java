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

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.Version;
import ca.uhn.hl7v2.parser.GenericParser;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.validation.ValidationContext;
import ca.uhn.hl7v2.validation.builder.ValidationRuleBuilder;
import ca.uhn.hl7v2.validation.impl.ValidationContextFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.component.hl7.HL7DataFormat;
import org.apache.camel.component.hl7.HL7MLLPNettyDecoderFactory;
import org.apache.camel.component.hl7.HL7MLLPNettyEncoderFactory;
import org.apache.camel.component.hl7.Hl7Terser;
import org.apache.camel.spi.DataFormat;

public class Hl7Producers {

    @ApplicationScoped
    @Named("hl7encoder")
    public HL7MLLPNettyEncoderFactory hl7MLLPNettyEncoderFactory() {
        HL7MLLPNettyEncoderFactory factory = new HL7MLLPNettyEncoderFactory();
        factory.setConvertLFtoCR(true);
        return factory;
    }

    @ApplicationScoped
    @Named("hl7decoder")
    public HL7MLLPNettyDecoderFactory hl7MLLPNettyDecoderFactory() {
        HL7MLLPNettyDecoderFactory factory = new HL7MLLPNettyDecoderFactory();
        factory.setConvertLFtoCR(true);
        return factory;
    }

    @ApplicationScoped
    @Named
    public DataFormat hl7DataFormat() {
        return new HL7DataFormat();
    }

    @ApplicationScoped
    @Named
    public TerserBean terserBean() {
        return new TerserBean();
    }

    @ApplicationScoped
    @Named
    public Parser parser() {
        ValidationRuleBuilder builder = new ValidationRuleBuilder() {
            @Override
            protected void configure() {
                // Configure a fake validation scenario where the patient id should match a specific value
                forVersion(Version.V22)
                        .message("ADT", "*")
                        .terser("PID-2", isEqual("00009999"));
            }
        };

        ValidationContext customValidationContext = ValidationContextFactory.fromBuilder(builder);
        HapiContext customContext = new DefaultHapiContext(customValidationContext);
        return new GenericParser(customContext);
    }

    static class TerserBean {
        public String patientId(@Hl7Terser(value = "PID-3-1") String patientId) {
            return patientId;
        }
    }
}
