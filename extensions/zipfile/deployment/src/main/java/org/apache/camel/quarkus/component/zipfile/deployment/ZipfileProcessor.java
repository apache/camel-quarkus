package org.apache.camel.quarkus.component.zipfile.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class ZipfileProcessor {

    private static final String FEATURE = "camel-zipfile";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

}
