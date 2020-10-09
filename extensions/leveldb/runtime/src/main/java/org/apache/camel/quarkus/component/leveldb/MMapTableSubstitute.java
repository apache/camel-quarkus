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

import java.io.Closeable;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.Callable;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.iq80.leveldb.table.MMapTable;
import org.iq80.leveldb.util.Closeables;

/**
 * Workaround for https://github.com/oracle/graal/issues/2761
 * (see OriginalByteBufferSupport for more information)
 */
@TargetClass(value = MMapTable.class)
final class MMapTableSubstitute {

    @Alias
    protected String name;
    @Alias
    protected FileChannel fileChannel;
    @Alias
    private MappedByteBuffer data;

    @Substitute
    public Callable<?> closer() {
        return new Closer(name, fileChannel, data);
    }

    private static class Closer
            implements Callable<Void> {
        private final String name;
        private final Closeable closeable;
        private final MappedByteBuffer data;

        public Closer(String name, Closeable closeable, MappedByteBuffer data) {
            this.name = name;
            this.closeable = closeable;
            this.data = data;
        }

        public Void call() {
            OriginalByteBufferSupport.unmap(data);
            Closeables.closeQuietly(closeable);
            return null;
        }
    }

}
