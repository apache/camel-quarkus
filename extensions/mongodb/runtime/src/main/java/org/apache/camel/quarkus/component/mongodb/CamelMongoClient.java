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
package org.apache.camel.quarkus.component.mongodb;

import java.util.List;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.ClientSession;
import com.mongodb.client.ListDatabasesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;

import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * Bridges Mongo client types {@link com.mongodb.MongoClient} and {@link com.mongodb.client.MongoClient} used by the
 * Quarkus Mongo extension and the Camel MongoDB component, so that it can be looked up and used via the
 * connectionBean URI endpoint path parameter
 */
public final class CamelMongoClient extends com.mongodb.MongoClient {

    private final MongoClient delegate;

    public CamelMongoClient(MongoClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public MongoDatabase getDatabase(String databaseName) {
        return delegate.getDatabase(databaseName);
    }

    @Override
    public ClientSession startSession() {
        return delegate.startSession();
    }

    @Override
    public ClientSession startSession(ClientSessionOptions options) {
        return delegate.startSession(options);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public MongoIterable<String> listDatabaseNames() {
        return delegate.listDatabaseNames();
    }

    @Override
    public ListDatabasesIterable<Document> listDatabases(ClientSession clientSession) {
        return delegate.listDatabases(clientSession);
    }

    @Override
    public <T> ListDatabasesIterable<T> listDatabases(ClientSession clientSession, Class<T> clazz) {
        return delegate.listDatabases(clientSession, clazz);
    }

    @Override
    public <T> ListDatabasesIterable<T> listDatabases(Class<T> clazz) {
        return delegate.listDatabases(clazz);
    }

    @Override
    public ChangeStreamIterable<Document> watch() {
        return delegate.watch();
    }

    @Override
    public ChangeStreamIterable<Document> watch(List<? extends Bson> pipeline) {
        return delegate.watch(pipeline);
    }

    @Override
    public ChangeStreamIterable<Document> watch(ClientSession clientSession) {
        return delegate.watch(clientSession);
    }

    @Override
    public ChangeStreamIterable<Document> watch(ClientSession clientSession, List<? extends Bson> pipeline) {
        return delegate.watch(clientSession, pipeline);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> resultClass) {
        return delegate.watch(resultClass);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> pipeline, Class<TResult> resultClass) {
        return delegate.watch(pipeline, resultClass);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, Class<TResult> resultClass) {
        return delegate.watch(clientSession, resultClass);
    }

    @Override
    public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> pipeline,
            Class<TResult> resultClass) {
        return delegate.watch(clientSession, pipeline, resultClass);
    }
}
