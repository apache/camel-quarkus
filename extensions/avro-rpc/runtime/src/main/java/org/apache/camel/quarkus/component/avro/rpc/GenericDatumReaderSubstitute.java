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
package org.apache.camel.quarkus.component.avro.rpc;

import java.util.IdentityHashMap;
import java.util.Map;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Inject;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;

@TargetClass(value = GenericDatumReader.class)
public final class GenericDatumReaderSubstitute {

    @Inject
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private Map<Schema, Class> stringClassCache;

    @Alias
    protected Class findStringClass(Schema schema) {
        return null;
    }

    @Substitute
    private Class getStringClass(Schema s) {
        if (stringClassCache == null) {
            stringClassCache = new IdentityHashMap<>();
        }

        Class c = stringClassCache.get(s);

        if (c == null) {
            c = findStringClass(s);
            stringClassCache.put(s, c);
        }
        return c;
    }
}
