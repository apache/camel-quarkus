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

import com.fasterxml.jackson.databind.Module;
import org.apache.camel.component.leveldb.LevelDBAggregationRepository;
import org.apache.camel.component.leveldb.LevelDBFile;
import org.apache.camel.component.leveldb.serializer.JacksonLevelDBSerializer;

public class QuarkusLevelDBAggregationRepository extends LevelDBAggregationRepository {

    public QuarkusLevelDBAggregationRepository() {
        initSerializer(null);
    }

    public QuarkusLevelDBAggregationRepository(String repositoryName) {
        super(repositoryName);
        initSerializer(null);
    }

    public QuarkusLevelDBAggregationRepository(String repositoryName, String persistentFileName) {
        super(repositoryName, persistentFileName);
        initSerializer(null);
    }

    public QuarkusLevelDBAggregationRepository(String repositoryName, LevelDBFile levelDBFile) {
        super(repositoryName, levelDBFile);
        initSerializer(null);
    }

    //constructor with module

    public QuarkusLevelDBAggregationRepository(Module module) {
        JacksonLevelDBSerializer serializer = new JacksonLevelDBSerializer(module);
        initSerializer(module);
    }

    public QuarkusLevelDBAggregationRepository(String repositoryName, Module module) {
        super(repositoryName);
        initSerializer(module);
    }

    public QuarkusLevelDBAggregationRepository(String repositoryName, String persistentFileName, Module module) {
        super(repositoryName, persistentFileName);
        initSerializer(module);
    }

    public QuarkusLevelDBAggregationRepository(String repositoryName, LevelDBFile levelDBFile, Module module) {
        super(repositoryName, levelDBFile);
        initSerializer(module);
    }

    private void initSerializer(Module module) {
        JacksonLevelDBSerializer serializer = new JacksonLevelDBSerializer(module);
        setSerializer(serializer);
    }
}
