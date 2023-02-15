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
package org.apache.camel.quarkus.component.dataformats.json.jsonb;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

public class ParamType implements ParameterizedType {
    private final Type rawType;
    private final Type[] types;

    public ParamType(final Type raw, final Type... types) {
        if (raw == null) {
            final ParameterizedType userFinalType = findUserParameterizedType();
            this.rawType = userFinalType.getRawType();
            this.types = userFinalType.getActualTypeArguments();
        } else {
            this.rawType = raw;
            this.types = types;
        }
    }

    private ParameterizedType findUserParameterizedType() {
        final Type genericSuperclass = getClass().getGenericSuperclass();
        if (!ParameterizedType.class.isInstance(genericSuperclass)) {
            throw new IllegalArgumentException("raw can be null only for children classes");
        }
        final ParameterizedType pt = ParameterizedType.class.cast(genericSuperclass); // our type, then unwrap it

        final Type userType = pt.getActualTypeArguments()[0];
        if (!ParameterizedType.class.isInstance(userType)) {
            throw new IllegalArgumentException("You need to pass a parameterized type to Johnzon*Types");
        }

        return ParameterizedType.class.cast(userType);
    }

    @Override
    public Type[] getActualTypeArguments() {
        return types.clone();
    }

    @Override
    public Type getOwnerType() {
        return null;
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(types) ^ (rawType == null ? 0 : rawType.hashCode());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof ParameterizedType) {
            final ParameterizedType that = (ParameterizedType) obj;
            final Type thatRawType = that.getRawType();
            return that.getOwnerType() == null
                    && (rawType == null ? thatRawType == null : rawType.equals(thatRawType))
                    && Arrays.equals(types, that.getActualTypeArguments());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(((Class<?>) rawType).getSimpleName());
        final Type[] actualTypes = getActualTypeArguments();
        if (actualTypes.length > 0) {
            buffer.append("<");
            int length = actualTypes.length;
            for (int i = 0; i < length; i++) {
                buffer.append(actualTypes[i].toString());
                if (i != actualTypes.length - 1) {
                    buffer.append(",");
                }
            }

            buffer.append(">");
        }
        return buffer.toString();
    }
}
