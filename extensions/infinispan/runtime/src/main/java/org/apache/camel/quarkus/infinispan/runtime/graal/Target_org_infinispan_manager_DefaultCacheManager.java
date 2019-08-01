package org.apache.camel.quarkus.infinispan.runtime.graal;

import java.io.IOException;
import java.io.InputStream;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.manager.DefaultCacheManager;

@TargetClass(DefaultCacheManager.class)
final class Target_org_infinispan_manager_DefaultCacheManager {

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager() {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(Configuration defaultConfiguration) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(Configuration defaultConfiguration, boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(GlobalConfiguration globalConfiguration) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(GlobalConfiguration globalConfiguration, boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(GlobalConfiguration globalConfiguration,
            Configuration defaultConfiguration) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(GlobalConfiguration globalConfiguration,
            Configuration defaultConfiguration,
            boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(String configurationFile) throws IOException {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(String configurationFile, boolean start) throws IOException {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(InputStream configurationStream) throws IOException {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(InputStream configurationStream, boolean start)
            throws IOException {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }

    @Substitute
    public Target_org_infinispan_manager_DefaultCacheManager(ConfigurationBuilderHolder holder, boolean start) {
        throw new RuntimeException("DefaultCacheManager not supported in native image mode");
    }
}
