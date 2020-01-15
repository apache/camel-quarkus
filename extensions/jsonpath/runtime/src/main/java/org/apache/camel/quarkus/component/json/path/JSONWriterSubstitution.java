package org.apache.camel.quarkus.component.json.path;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

import net.minidev.json.reader.JsonWriter;
import net.minidev.json.reader.JsonWriterI;

@TargetClass(JsonWriter.class)
final class JSONWriterSubstitution {
    @Delete
    static public JsonWriterI<Object> beansWriterASM;
}
