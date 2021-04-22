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
package org.apache.camel.quarkus.component.google.storage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.google.cloud.storage.Storage;

@ApplicationScoped
public class GoogleStorageEnforcer {

    //Quarkiverse StorageProducer creates storage based on properties.
    //Because of https://github.com/apache/camel-quarkus/issues/1387, storage is removed and not applied to component.
    //UnremovableBeanBuildItem does not fix this.
    //Injecting all storages makes Quarkiverse StorageProducer to be used.
    //In case, that user defines multiple clients, all clients have to be injected, otherwise exception "Ambiguous dependencies" is thrown.
    @Inject
    Instance<Storage> storages;
}
