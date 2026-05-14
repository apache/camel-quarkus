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
package org.apache.camel.quarkus.component.rocketmq.graal;

import java.io.IOException;
import java.lang.reflect.Type;

import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.support.moneta.MonetaCodec;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Removes the optional javax.money dependency from fastjson MonetaCodec so that
 * NativeImageAllowIncompleteClasspathBuildItem is not required.
 */
@TargetClass(MonetaCodec.class)
final class Target_MonetaCodec implements ObjectSerializer, ObjectDeserializer {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    public static MonetaCodec instance = new MonetaCodec();

    @Substitute
    public Target_MonetaCodec() {
    }

    @Substitute
    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features)
            throws IOException {
        throw new UnsupportedOperationException("javax.money support is not available in native mode");
    }

    @Substitute
    @Override
    public <T> T deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
        throw new UnsupportedOperationException("javax.money support is not available in native mode");
    }

    @Substitute
    @Override
    public int getFastMatchToken() {
        return 0;
    }
}
