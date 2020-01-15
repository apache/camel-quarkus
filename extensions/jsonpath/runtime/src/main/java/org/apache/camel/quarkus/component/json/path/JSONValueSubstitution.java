package org.apache.camel.quarkus.component.json.path;

import java.io.IOException;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import static net.minidev.json.JSONValue.defaultWriter;

import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;
import net.minidev.json.reader.JsonWriter;
import net.minidev.json.reader.JsonWriterI;

@TargetClass(JSONValue.class)
final class JSONValueSubstitution {

    @SuppressWarnings("unchecked")
    @Substitute
    public static void writeJSONString(Object value, Appendable out, JSONStyle compression) throws IOException {
        if (value == null) {
            out.append("null");
            return;
        }
        Class<?> clz = value.getClass();
        @SuppressWarnings("rawtypes")
        JsonWriterI w = defaultWriter.getWrite(clz);
        if (w == null) {
            if (clz.isArray())
                w = JsonWriter.arrayWriter;
            else {
                w = defaultWriter.getWriterByInterface(value.getClass());
                if (w == null) {
                    String format = "No suitable Jsonwriter found for class \"%s\", \"net.minidev.json.reader.BeansWriterASM\" is not supported in native mode.";
                    throw new UnsupportedOperationException(String.format(format, clz.getName()));
                }
            }
            defaultWriter.registerWriter(w, clz);
        }
        w.writeJSONString(value, out, compression);
    }
}
