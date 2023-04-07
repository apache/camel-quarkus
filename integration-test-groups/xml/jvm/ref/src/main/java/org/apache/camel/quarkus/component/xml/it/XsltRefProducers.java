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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;

@Dependent
public class XsltRefProducers {

    @Named("xslt_resource")
    public String getXsltResource() throws Exception {
        return getXsltContent();
    }

    private static String getXsltContent() throws Exception {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("xslt/classpath-transform.xsl")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
