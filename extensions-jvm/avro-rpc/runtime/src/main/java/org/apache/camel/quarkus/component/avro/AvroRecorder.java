package org.apache.camel.quarkus.component.avro;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.component.avro.AvroComponent;

@Recorder
public class AvroRecorder {

    public RuntimeValue<?> createAvroComponent() {
        return new RuntimeValue<>(new AvroComponent());
    }

}
