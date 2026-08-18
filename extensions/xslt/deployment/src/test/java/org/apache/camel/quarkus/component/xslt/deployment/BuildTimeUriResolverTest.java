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
package org.apache.camel.quarkus.component.xslt.deployment;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;

import org.apache.camel.quarkus.support.xalan.XalanTransformerFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BuildTimeUriResolverTest {

    @Test
    void systemIdIsClasspathUri() {
        BuildTimeUriResolver resolver = new BuildTimeUriResolver();
        BuildTimeUriResolver.ResolutionResult resolved = resolver.resolve("xslt/nested-include.xsl");

        assertEquals("classpath:xslt/nested-include.xsl", resolved.source.getSystemId());
    }

    @Test
    void relativeIncludeIsResolvedAgainstParent() throws Exception {
        BuildTimeUriResolver resolver = new BuildTimeUriResolver();
        Source included = resolver.resolve("mid.xsl", "classpath:xslt/nested/parent.xsl");

        assertEquals("classpath:xslt/nested/mid.xsl", included.getSystemId());
    }

    @Test
    void nestedClasspathIncludesAreCompiled() throws Exception {
        BuildTimeUriResolver resolver = new BuildTimeUriResolver();
        BuildTimeUriResolver.ResolutionResult resolved = resolver.resolve("xslt/nested-include.xsl");

        TransformerFactory tf = new XalanTransformerFactory();
        tf.setURIResolver(resolver);
        Templates templates = tf.newTemplates(resolved.source);

        assertNotNull(templates);
    }

}
