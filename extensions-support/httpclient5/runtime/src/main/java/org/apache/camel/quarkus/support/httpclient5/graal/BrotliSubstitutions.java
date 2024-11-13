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
package org.apache.camel.quarkus.support.httpclient5.graal;

import java.io.IOException;
import java.io.InputStream;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.hc.client5.http.entity.BrotliDecompressingEntity;
import org.apache.hc.client5.http.entity.BrotliInputStreamFactory;

/**
 * Remove references to optional brotli:dec dependency.
 */
final class BrotliSubstitutions {
}

@TargetClass(value = BrotliInputStreamFactory.class, onlyWith = { BrotliAbsentBooleanSupplier.class })
final class SubstituteBrotliInputStreamFactory {
    @Substitute
    public InputStream create(InputStream inputStream) throws IOException {
        throw new UnsupportedOperationException(
                "Cannot create BrotliInputStream. Add org.brotli:dec to the application classpath.");
    }
}

@TargetClass(value = BrotliDecompressingEntity.class, onlyWith = { BrotliAbsentBooleanSupplier.class })
final class SubstituteBrotliDecompressingEntity {
    @Substitute
    public static boolean isAvailable() {
        return false;
    }
}
