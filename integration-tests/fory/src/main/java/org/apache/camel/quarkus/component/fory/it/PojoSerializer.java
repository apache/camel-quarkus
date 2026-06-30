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
package org.apache.camel.quarkus.component.fory.it;

import org.apache.fory.config.Config;
import org.apache.fory.context.ReadContext;
import org.apache.fory.context.WriteContext;
import org.apache.fory.serializer.Serializer;

/**
 * In fory >= 1.3.0, native mode requires pre-compiled serializer classes.
 * With ThreadLocalFory, each thread creates a new fory instance with a different
 * config hash (due to System.nanoTime() in the name), so auto-generated serializers
 * registered in the GraalVM registry under the build-time config hash can't be found at runtime
 */
public class PojoSerializer extends Serializer<Pojo> {
    public PojoSerializer(Config config, Class<Pojo> type) {
        super(config, type);
    }

    @Override
    public void write(WriteContext ctx, Pojo value) {
        ctx.getBuffer().writeVarInt32(value.f1());
        ctx.writeString(value.f2());
    }

    @Override
    public Pojo read(ReadContext ctx) {
        return new Pojo(ctx.getBuffer().readVarInt32(), ctx.readString());
    }
}
