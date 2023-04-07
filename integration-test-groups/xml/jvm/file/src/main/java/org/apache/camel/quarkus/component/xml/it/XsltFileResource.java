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
package org.apache.camel.quarkus.component.xml.it;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.camel.ProducerTemplate;

@Path("/xml")
@ApplicationScoped
public class XsltFileResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/xslt-file")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String xsltFile(String body) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("xslt/classpath-transform.xsl")) {
            File file = File.createTempFile("xslt", ".xsl");
            Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return producerTemplate.requestBody("xslt:file:" + file, body, String.class);
        }
    }
}
