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
package org.apache.camel.quarkus.test.support.google;

import org.testcontainers.containers.GenericContainer;

public interface GoogleTestEnvCustomizer<C extends GenericContainer> {

    /**
     * Create specific container for the google service.
     * This method is called before `customize` method.
     *
     * @return The container to be started.
     */
    C createContainer();

    /**
     * Customize the given {@link GoogleCloudContext}
     * If the container was created by `createContainer` method, it is already running.
     *
     * @param envContext the {@link GoogleCloudContext} to customize
     */
    void customize(GoogleCloudContext envContext);

}
