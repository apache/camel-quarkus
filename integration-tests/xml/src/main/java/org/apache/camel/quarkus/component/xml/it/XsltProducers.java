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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.CamelContext;
import org.apache.camel.support.ResourceHelper;

@Dependent
public class XsltProducers {
    public static final String EXPECTED_XML_CONSTANT = "<data>FOO DATA</data>";
    @Inject
    CamelContext context;

    @Named("customURIResolver")
    public URIResolver getCustomURIResolver() {
        return new URIResolver() {

            @Override
            public Source resolve(String href, String base) throws TransformerException {
                if (href.equals("xslt/include_not_existing_resource.xsl")) {
                    try {
                        InputStream is = ResourceHelper.resolveMandatoryResourceAsInputStream(context, href);
                        return new StreamSource(is);
                    } catch (Exception e) {
                        throw new TransformerException(e);
                    }
                }

                Source constantResult = new StreamSource(new ByteArrayInputStream(EXPECTED_XML_CONSTANT.getBytes()));
                return constantResult;
            }
        };
    }

    @Named("xslt_resource")
    public String getXsltResource() throws Exception {
        return getXsltContent();
    }

    @Named("xslt_bean")
    public XsltBean getXsltBean() {
        return new XsltBean();
    }

    public static class XsltBean {
        public String getXsltResource() throws Exception {
            return getXsltContent();
        }
    }

    private static String getXsltContent() throws Exception {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("xslt/classpath-transform.xsl")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
