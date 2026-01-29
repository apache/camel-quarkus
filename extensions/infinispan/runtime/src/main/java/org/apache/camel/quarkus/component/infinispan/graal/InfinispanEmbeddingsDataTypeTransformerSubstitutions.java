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
package org.apache.camel.quarkus.component.infinispan.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.component.infinispan.remote.transform.InfinispanEmbeddingsDataTypeTransformer;

public final class InfinispanEmbeddingsDataTypeTransformerSubstitutions {
}

// Delete InfinispanEmbeddingsDataTypeTransformer if langchain4j-embeddings is not on the classpath
@TargetClass(value = InfinispanEmbeddingsDataTypeTransformer.class, onlyWith = LangChain4jEmbeddingsAbsent.class)
@Delete
final class DeleteInfinispanEmbeddingsDataTypeTransformer {
}

final class LangChain4jEmbeddingsAbsent implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            Thread.currentThread().getContextClassLoader().loadClass("dev.langchain4j.data.embedding.Embedding");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
