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
package org.apache.camel.quarkus.component.xslt;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

/**
 * An {@link URIResolver} to use at runtime.
 */
public class RuntimeUriResolver implements URIResolver {

    private static final StreamSource FAKE_SOURCE = new StreamSource(new InputStream() {
        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException(
                    "The translet available in the application archive should be used instead of loading the XSLT template anew");
        }
    });

    /** A map from XSL source URIs to unqualified translet names */
    private final Map<String, String> uriToTransletName;

    private RuntimeUriResolver(Map<String, String> uriMap) {
        this.uriToTransletName = uriMap;
    }

    /**
     * Returns a fake {@link StreamSource} that throws an {@link UnsupportedOperationException} upon read. The real
     * resolution happens via {@link #getTransletClassName(String)}
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        return FAKE_SOURCE;
    }

    /**
     * @param  uri the URI whose translet is seeked
     * @return     the unqualified translet name associated with the given {@code uri} or
     *             {@code null} if the given XSLT resource was not compiled to a translet at build time.
     */
    public String getTransletClassName(String uri) {
        return uriToTransletName.get(uri);
    }

    /**
     * A {@link RuntimeUriResolver} builder.
     */
    public static class Builder {
        private Map<String, String> uriMap = new LinkedHashMap<String, String>();

        public Builder entry(String templateUri, String transletClassName) {
            uriMap.put(templateUri, transletClassName);
            return this;
        }

        public RuntimeUriResolver build() {
            final RuntimeUriResolver result = new RuntimeUriResolver(uriMap);
            uriMap = null; // invalidate the collection so that it cannot be changed anymore
            return result;
        }
    }

}
