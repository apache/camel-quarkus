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
package org.apache.camel.quarkus.component.openstack.graal;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.openstack4j.core.transport.internal.OSBadBooleanDeserializer;

/**
 * TODO: Remove this https://github.com/apache/camel-quarkus/issues/5604
 *
 * Mostly a replica of the original OSBadBooleanDeserializer.deserialize but with references to
 * deprecated & removed Jackson methods replaced.
 */
@TargetClass(OSBadBooleanDeserializer.class)
final class OSBadBooleanDeserializerSubstitutions {

    @Substitute
    public Boolean deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.VALUE_TRUE) {
            return Boolean.TRUE;
        }
        if (t == JsonToken.VALUE_FALSE) {
            return Boolean.FALSE;
        }
        // [JACKSON-78]: should accept ints too, (0 == false, otherwise true)
        if (t == JsonToken.VALUE_NUMBER_INT) {
            // 11-Jan-2012, tatus: May be outside of int...
            if (jp.getNumberType() == JsonParser.NumberType.INT) {
                return (jp.getIntValue() == 0) ? Boolean.FALSE : Boolean.TRUE;
            }
            return Boolean.valueOf(_parseBooleanFromNumber(jp, ctxt));
        }
        if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        // And finally, let's allow Strings to be converted too
        if (t == JsonToken.VALUE_STRING) {
            String text = jp.getText().trim();
            if ("true".equalsIgnoreCase(text)) {
                return Boolean.TRUE;
            }
            if ("false".equalsIgnoreCase(text)) {
                return Boolean.FALSE;
            }
            if (text.length() == 0) {
                return null;
            }
            throw ctxt.weirdStringException(text, Boolean.class, "only \"true\" or \"false\" recognized");
        }

        ctxt.handleUnexpectedToken(Boolean.class, ctxt.getParser());
        // Otherwise, no can do:
        throw JsonMappingException.from(ctxt.getParser(),
                String.format("Cannot deserialize instance of %s out of %s token",
                        ClassUtil.nameOf(Boolean.class), t));
    }

    @Alias
    protected final boolean _parseBooleanFromNumber(JsonParser jp, DeserializationContext ctxt) {
        return true;
    }
}
