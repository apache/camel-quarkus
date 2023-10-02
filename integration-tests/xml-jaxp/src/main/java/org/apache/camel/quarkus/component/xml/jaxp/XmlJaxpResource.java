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
package org.apache.camel.quarkus.component.xml.jaxp;

import javax.xml.transform.OutputKeys;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.converter.jaxp.XmlConverter;

@Path("/xml/jaxp")
public class XmlJaxpResource {
    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("convert")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public String applyXmlJaxpTypeConversion(@QueryParam("endpointUri") String endpointUri, String xml) {
        return producerTemplate.requestBody(endpointUri, xml, String.class);
    }

    @Path("convert/context/global/options")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    @POST
    public String applyXmlJaxpTypeConversionWithContextGlobalProperties(@QueryParam("endpointUri") String endpointUri,
            String xml) {
        try {
            context.getGlobalOptions().put(XmlConverter.OUTPUT_PROPERTIES_PREFIX + OutputKeys.ENCODING, "UTF-8");
            context.getGlobalOptions().put(XmlConverter.OUTPUT_PROPERTIES_PREFIX + OutputKeys.STANDALONE, "no");
            return applyXmlJaxpTypeConversion(endpointUri, xml);
        } finally {
            context.getGlobalOptions().remove(XmlConverter.OUTPUT_PROPERTIES_PREFIX);
            context.getGlobalOptions().remove(XmlConverter.OUTPUT_PROPERTIES_PREFIX);
        }
    }
}
