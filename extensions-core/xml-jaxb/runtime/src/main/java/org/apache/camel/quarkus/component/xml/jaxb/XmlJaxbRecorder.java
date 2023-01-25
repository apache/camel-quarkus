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
package org.apache.camel.quarkus.component.xml.jaxb;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import jakarta.xml.bind.JAXBException;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.apache.camel.spi.ModelToXMLDumper;
import org.apache.camel.xml.jaxb.DefaultModelJAXBContextFactory;
import org.apache.camel.xml.jaxb.JaxbModelToXMLDumper;
import org.graalvm.nativeimage.ImageInfo;

@Recorder
public class XmlJaxbRecorder {

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

    public RuntimeValue<ModelToXMLDumper> newJaxbModelToXMLDumper() {
        return new RuntimeValue<>(new JaxbModelToXMLDumper());
    }
}
