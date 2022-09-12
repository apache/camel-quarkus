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
package org.apache.camel.quarkus.component.jcache.it;

import java.util.concurrent.TimeUnit;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jcache.policy.JCachePolicy;

@ApplicationScoped
public class JcacheRoutes extends RouteBuilder {
    @Inject
    @Named("jcachePolicy")
    JCachePolicy jcachePolicy;

    @Override
    public void configure() throws Exception {
        from("direct:getCachedValue")
                .setProperty("In-Cache", constant("Cached Response"))
                .policy(jcachePolicy)
                .setBody(constant("Hello World"))
                .setProperty("In-Cache", constant("Non-Cached Response"))
                .end();
    }

    @Produces
    @ApplicationScoped
    @Named
    public JCachePolicy jcachePolicy() {
        MutableConfiguration configuration = new MutableConfiguration<>();
        configuration.setTypes(String.class, Object.class);
        configuration.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 10)));
        CacheManager cacheManager = Caching.getCachingProvider().getCacheManager();
        Cache cache = cacheManager.createCache("MyJCache", configuration);

        JCachePolicy jcachePolicy = new JCachePolicy();
        jcachePolicy.setCache(cache);
        jcachePolicy.setKeyExpression(simple("${header.Cache-Key}"));
        return jcachePolicy;
    }
}
