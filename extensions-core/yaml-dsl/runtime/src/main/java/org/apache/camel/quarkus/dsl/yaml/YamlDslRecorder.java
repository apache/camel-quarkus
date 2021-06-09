package org.apache.camel.quarkus.dsl.yaml;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.apache.camel.spi.CamelContextCustomizer;

@Recorder
public class YamlDslRecorder {
    public RuntimeValue<CamelContextCustomizer> setYamlDeserializationMode(String mode) {
        return new RuntimeValue<>(new CamelContextCustomizer() {
            @Override
            public void configure(CamelContext camelContext) {
                camelContext.getGlobalOptions().put(
                        YamlRoutesBuilderLoader.DESERIALIZATION_MODE,
                        mode);
            }
        });
    }
}
