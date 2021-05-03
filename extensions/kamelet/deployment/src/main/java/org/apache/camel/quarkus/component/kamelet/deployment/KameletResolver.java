package org.apache.camel.quarkus.component.kamelet.deployment;

import java.io.InputStream;
import java.util.Optional;

import org.apache.camel.Ordered;

public interface KameletResolver extends Ordered {
    Optional<InputStream> resolve(String id) throws Exception;

    @Override
    default int getOrder() {
        return Ordered.LOWEST;
    }
}
