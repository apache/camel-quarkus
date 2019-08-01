package org.apache.camel.quarkus.infinispan.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.component.infinispan.InfinispanManager;
import org.apache.camel.util.ObjectHelper;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.commons.api.BasicCacheContainer;
import org.slf4j.Logger;

@TargetClass(InfinispanManager.class)
final class Target_org_apache_camel_component_infinispan_InfinispanManager {
    @Alias
    private BasicCacheContainer cacheContainer;
    @Alias
    private static transient Logger LOGGER;

    @Substitute
    public <K, V> BasicCache<K, V> getCache(String cacheName) {
        BasicCache<K, V> cache;
        if (ObjectHelper.isEmpty(cacheName)) {
            cache = cacheContainer.getCache();
            cacheName = cache.getName();
        } else {
            cache = cacheContainer.getCache(cacheName);
        }
        LOGGER.trace("Cache[{}]", cacheName);
        return cache;
    }
}
