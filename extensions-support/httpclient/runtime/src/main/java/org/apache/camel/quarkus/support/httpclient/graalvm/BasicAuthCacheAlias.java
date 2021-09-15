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
package org.apache.camel.quarkus.support.httpclient.graalvm;

import java.util.Map;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScheme;
import org.apache.http.client.AuthCache;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.util.Args;

/**
 * <code>BasicAuthCacheAlias</code> is an alias of <code>org.apache.http.impl.client.BasicAuthCache</code>
 * that work in native mode by skipping AuthScheme serialization.
 */
@TargetClass(className = "org.apache.http.impl.client.BasicAuthCache")
public final class BasicAuthCacheAlias implements AuthCache {

    @Alias
    private Map<HttpHost, AuthScheme> map;
    @Alias
    private SchemePortResolver schemePortResolver;

    @Alias
    public BasicAuthCacheAlias(final SchemePortResolver schemePortResolver) {
    }

    @Alias
    public BasicAuthCacheAlias() {
    }

    @Alias
    protected HttpHost getKey(final HttpHost host) {
        return null;
    }

    @Substitute
    @Override
    public void put(final HttpHost host, final AuthScheme authScheme) {
        Args.notNull(host, "HTTP host");
        if (authScheme == null) {
            return;
        }
        this.map.put(getKey(host), authScheme);
    }

    @Substitute
    @Override
    public AuthScheme get(final HttpHost host) {
        Args.notNull(host, "HTTP host");
        return this.map.get(getKey(host));
    }

    @Alias
    @Override
    public void remove(final HttpHost host) {
    }

    @Alias
    @Override
    public void clear() {
    }

    @Alias
    @Override
    public String toString() {
        return null;
    }

}
