package org.apache.camel.quarkus.component.kamelet.deployment;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Build item used by kamelet providers to plug their own way of resolving kamelets giving a name. This could be
 * leveraged by a future camel-quarkus-kamelet-catalog extension to resolve kamelets as they may have a different naming
 * structure or location int the classpath.
 */
public final class KameletResolverBuildItem extends MultiBuildItem {
    private final KameletResolver resolver;

    public KameletResolverBuildItem(KameletResolver resolver) {
        this.resolver = resolver;
    }

    public KameletResolver getResolver() {
        return this.resolver;
    }
}
