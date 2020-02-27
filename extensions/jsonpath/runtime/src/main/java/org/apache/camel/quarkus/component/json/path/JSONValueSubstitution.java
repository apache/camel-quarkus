/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.json.path;

import java.io.IOException;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import net.minidev.json.JSONStyle;
import net.minidev.json.JSONValue;
import net.minidev.json.reader.JsonWriter;
import net.minidev.json.reader.JsonWriterI;

import static net.minidev.json.JSONValue.defaultWriter;

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
