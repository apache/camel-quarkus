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
package org.apache.camel.quarkus.core.converter.it;

import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConverter;
import org.apache.camel.quarkus.core.converter.it.model.MyExchangePair;
import org.apache.camel.quarkus.core.converter.it.model.MyNullablePair;
import org.apache.camel.quarkus.core.converter.it.model.MyTestPair;
import org.apache.camel.spi.TypeConverterRegistry;

@Converter
public class TestConverters {

    public static final String CONVERTER_VALUE = "converter_value";

    @Converter(allowNull = true)
    public static MyNullablePair toNullablePair(String s) {
        if ("null".equals(s)) {
            return null;
        }
        return new MyNullablePair(s);
    }

    @Converter
    public static MyTestPair toMyPair(String s) {
        if (s.split(":").length != 2) {
            return null;
        }
        return new MyTestPair(s);
    }

    @Converter
    public static MyExchangePair toMyPair(String s, Exchange exchange) {
        String converter_value = exchange.getProperty(CONVERTER_VALUE, String.class);
        if (converter_value != null && converter_value.split(":").length == 2) {
            return new MyExchangePair(converter_value);
        }
        if (s.split(":").length != 2) {
            return null;
        }
        return new MyExchangePair(s);
    }

    @Converter(fallback = true)
    public static Object convertTo(Class type, Exchange exchange, Object value, TypeConverterRegistry registry)
            throws ClassNotFoundException {
        if (value instanceof String) {
            String s = (String) value;
            String[] tokens = s.split(":");
            if (tokens.length == 3) {
                Class convertTo = Class.forName(tokens[0]);
                TypeConverter tc = registry.lookup(convertTo, String.class);
                if (tc != null) {
                    return tc.convertTo(convertTo, s.replaceFirst(tokens[0] + ":", ""));
                }
            }
        }
        return null;
    }
}
