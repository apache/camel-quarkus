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
package org.apache.camel.quarkus.component.mongodb.runtime;

import java.util.List;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.*;
import com.mongodb.connection.ClusterDescription;
import org.apache.camel.quarkus.core.LazyProxy;
import org.bson.Document;
import org.bson.conversions.Bson;

public class MongoClientProxy extends LazyProxy<MongoClient> implements MongoClient {

    public MongoClientProxy() {
    }

    public MongoClient getDelegate() {
        return super.getDelegate();
    }

    @Override
    public MongoDatabase getDatabase(String databaseName) {
        return getDelegate().getDatabase(databaseName);
    }

    @Override
    public ClientSession startSession() {
        return getDelegate().startSession();
    }

    @Override
    public ClientSession startSession(ClientSessionOptions options) {
        return getDelegate().startSession(options);
    }

    @Override
    public void close() {
        getDelegate().close();
    }

    @Override
    public MongoIterable<String> listDatabaseNames() {
        return getDelegate().listDatabaseNames();
    }

    @Override
    public MongoIterable<String> listDatabaseNames(ClientSession clientSession) {
        return getDelegate().listDatabaseNames(clientSession);
    }

    @Override
    public ListDatabasesIterable<Document> listDatabases() {
        return getDelegate().listDatabases();
    }

    @Override
    public ListDatabasesIterable<Document> listDatabases(ClientSession clientSession) {
        return getDelegate().listDatabases(clientSession);
    }

    @Override
    public <TResult> ListDatabasesIterable<TResult> listDatabases(Class<TResult> tResultClass) {
        return getDelegate().listDatabases(tResultClass);
    }

    @Override
    public <TResult> ListDatabasesIterable<TResult> listDatabases(ClientSession clientSession, Class<TResult> tResultClass) {
        return getDelegate().listDatabases(clientSession, tResultClass);
    }

    @Override
    public ChangeStreamIterable<Document> watch() {
        return getDelegate().watch();
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> tResultClass) {
        return getDelegate().watch(tResultClass);
    }

    @Override
    public ChangeStreamIterable<Document> watch(List<? extends Bson> pipeline) {
        return getDelegate().watch(pipeline);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> pipeline, Class<TResult> tResultClass) {
        return getDelegate().watch(pipeline, tResultClass);
    }

    @Override
    public ChangeStreamIterable<Document> watch(ClientSession clientSession) {
        return getDelegate().watch(clientSession);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, Class<TResult> tResultClass) {
        return getDelegate().watch(clientSession, tResultClass);
    }

    @Override
    public ChangeStreamIterable<Document> watch(ClientSession clientSession, List<? extends Bson> pipeline) {
        return getDelegate().watch(clientSession, pipeline);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<TResult> tResultClass) {
        return getDelegate().watch(clientSession, pipeline, tResultClass);
    }

    @Override
    public ClusterDescription getClusterDescription() {
        return getDelegate().getClusterDescription();
    }
}
