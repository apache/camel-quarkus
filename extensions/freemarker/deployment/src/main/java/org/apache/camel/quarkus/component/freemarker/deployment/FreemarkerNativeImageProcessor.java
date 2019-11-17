package org.apache.camel.quarkus.component.freemarker.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import org.apache.camel.quarkus.component.freemarker.CamelFreemarkerConfig;
import org.apache.camel.support.ResourceHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class FreemarkerNativeImageProcessor {
    public static final String CLASSPATH_SCHEME = "classpath:";

    @BuildStep
    List<NativeImageResourceBuildItem> freemarkerResources(CamelFreemarkerConfig config) {
        List<NativeImageResourceBuildItem> items = new ArrayList<>(config.sources.size());

        for (String source : config.sources) {
            String scheme = ResourceHelper.getScheme(source);

            if (Objects.isNull(scheme) || Objects.equals(scheme, CLASSPATH_SCHEME)) {
                if (Objects.equals(scheme, CLASSPATH_SCHEME)) {
                    source = source.substring(CLASSPATH_SCHEME.length() + 1);
                }

                items.add(new NativeImageResourceBuildItem(source));
            }
        }

        return items;
    }
}
