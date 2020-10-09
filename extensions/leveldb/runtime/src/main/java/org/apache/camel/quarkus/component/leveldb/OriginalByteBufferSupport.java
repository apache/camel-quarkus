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
package org.apache.camel.quarkus.component.leveldb;

import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;

import com.google.common.base.Throwables;

/**
 * Unmap support was changed because of jdk9+ (see
 * https://github.com/dain/leveldb/commit/39b6e0c38045281fba5f6532c52dc06905890cad)
 * Current version of levelDB is using MethodHandle, which is not supported by GraalVM (see
 * https://github.com/oracle/graal/issues/2761)
 * Original way of using Method (instead of MethodHandle) is working in native mode,
 * therefore this class contains code from levelDB class `ByteBufferSupport` from the time before mentioned change and
 * is used via substitutions.
 * Issue https://github.com/apache/camel-quarkus/issues/1908 is reported to remove class once it is possible.
 */
public final class OriginalByteBufferSupport {
    private static final Method getCleaner;
    private static final Method clean;

    static {
        try {
            getCleaner = Class.forName("java.nio.DirectByteBuffer").getDeclaredMethod("cleaner");
            getCleaner.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }

        try {
            Class<?> returnType = getCleaner.getReturnType();
            if (Runnable.class.isAssignableFrom(returnType)) {
                clean = Runnable.class.getMethod("run");
            } else {
                clean = returnType.getMethod("clean");
            }
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private OriginalByteBufferSupport() {
    }

    public static void unmap(MappedByteBuffer buffer) {
        try {
            Object cleaner = getCleaner.invoke(buffer);
            clean.invoke(cleaner);
        } catch (Exception ignored) {
            throw Throwables.propagate(ignored);
        }
    }
}
