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
package org.apache.camel.quarkus.core.deployment.spi;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.quarkus.core.UploadAttacher;

/**
 * Holds the {@link UploadAttacher} {@link RuntimeValue}.
 * <p>
 * There are two producers for this item:
 * <ul>
 * <li>The default one producing a no-op {@link UploadAttacher}
 * <li>
 * <li>The "real" one available in {@code camel-quarkus-attachments}
 * extension</li>
 * <ul>
 * The "real" one is used only if the {@code camel-quarkus-attachments}
 * extension is present in the class path.
 */
public final class UploadAttacherBuildItem extends SimpleBuildItem {
    private final RuntimeValue<UploadAttacher> instance;

    public UploadAttacherBuildItem(RuntimeValue<UploadAttacher> instance) {
        this.instance = instance;
    }

    public RuntimeValue<UploadAttacher> getInstance() {
        return instance;
    }
}
