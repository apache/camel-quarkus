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
package org.apache.camel.quarkus.component.zipfile.graal;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipException;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * commons-compress unconditionally references the optional zstd-jni library when reading ZSTD compressed ZIP entries.
 * When zstd-jni is not on the classpath, fail reading such entries rather than the native image build.
 */
@TargetClass(className = "org.apache.commons.compress.archivers.zip.ZipArchiveInputStream", onlyWith = ZstdJniIsAbsent.class)
final class ZipArchiveInputStreamSubstitutions {
    @Substitute
    protected InputStream createZstdInputStream(InputStream in) throws IOException {
        throw new ZipException(
                "Reading ZSTD compressed ZIP entries requires com.github.luben:zstd-jni, which is not on the classpath");
    }
}

final class ZstdJniIsAbsent implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            Thread.currentThread().getContextClassLoader().loadClass("com.github.luben.zstd.ZstdInputStream");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
