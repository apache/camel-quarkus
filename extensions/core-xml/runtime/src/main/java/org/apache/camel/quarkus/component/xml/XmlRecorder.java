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
package org.apache.camel.quarkus.component.xml;

import javax.xml.bind.JAXBException;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.DefaultModelJAXBContextFactory;
import org.apache.camel.main.DefaultRoutesCollector;
import org.apache.camel.main.RoutesCollector;
import org.apache.camel.model.ValidateDefinition;
import org.apache.camel.model.validator.PredicateValidatorDefinition;
import org.apache.camel.quarkus.core.XmlLoader;
import org.apache.camel.reifier.ProcessorReifier;
import org.apache.camel.reifier.ValidateReifier;
import org.apache.camel.reifier.validator.PredicateValidatorReifier;
import org.apache.camel.reifier.validator.ValidatorReifier;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.graalvm.nativeimage.ImageInfo;

@Recorder
public class XmlRecorder {

    public RuntimeValue<ModelJAXBContextFactory> newContextFactory() {
        DefaultModelJAXBContextFactory factory = new DefaultModelJAXBContextFactory();
        if (ImageInfo.inImageBuildtimeCode()) {
            try {
                factory.newJAXBContext();
            } catch (JAXBException e) {
                throw new RuntimeCamelException("Unable to initialize Camel JAXBContext", e);
            }
        }
        return new RuntimeValue<>(factory);
    }

    public RuntimeValue<XmlLoader> newDefaultXmlLoader() {
        return new RuntimeValue<>(new DefaultXmlLoader());
    }

    public RuntimeValue<RoutesCollector> newDefaultRoutesCollector() {
        return new RuntimeValue<>(new DefaultRoutesCollector());
    }

    public void initXmlReifiers() {
        ProcessorReifier.registerReifier(ValidateDefinition.class, ValidateReifier::new);
        ValidatorReifier.registerReifier(PredicateValidatorDefinition.class, PredicateValidatorReifier::new);
    }
}
