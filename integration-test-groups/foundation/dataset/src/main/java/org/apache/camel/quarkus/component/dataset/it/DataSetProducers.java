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
package org.apache.camel.quarkus.component.dataset.it;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.inject.Named;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.dataset.FileDataSet;
import org.apache.camel.component.dataset.ListDataSet;
import org.apache.camel.component.dataset.SimpleDataSet;

@ApplicationScoped
public class DataSetProducers {

    @Named("customDataSet")
    public CustomDataSet customDataSet() {
        CustomDataSet customDataSet = new CustomDataSet();
        customDataSet.setOutputTransformer(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Message message = exchange.getMessage();
                String body = message.getBody(String.class);
                // Sets the body to 'Hello World'
                message.setBody(body + " World");
            }
        });
        return customDataSet;
    }

    @Named("simpleDataSet")
    public SimpleDataSet simpleDataSet() {
        return new SimpleDataSet();
    }

    @Named("simpleDataSetForConsumer")
    public SimpleDataSet simpleDataSetForConsumer() {
        return new SimpleDataSet();
    }

    @Named("simpleDataSetForException")
    public SimpleDataSet simpleDataSetForException() {
        return new SimpleDataSet(1);
    }

    @Named("simpleDataSetWithIndex")
    public SimpleDataSet simpleDataSetWithIndex() {
        return new SimpleDataSet();
    }

    @Named("simpleDataSetIndexOff")
    public SimpleDataSet simpleDataSetIndexOff() {
        return new SimpleDataSet();
    }

    @Named("simpleDataSetIndexLenient")
    public SimpleDataSet simpleDataSetIndexLenient() {
        return new SimpleDataSet();
    }

    @Named("simpleDataSetIndexStrict")
    public SimpleDataSet simpleDataSetIndexStrict() {
        return new SimpleDataSet();
    }

    @Named("simpleDataSetIndexStrictWithoutHeader")
    public SimpleDataSet simpleDataSetIndexStrictWithoutHeader() {
        return new SimpleDataSet();
    }

    @Named("listDataSet")
    public ListDataSet listDataSet() {
        List<Object> bodies = new LinkedList<>();
        bodies.add("Hello");
        bodies.add("World");
        return new ListDataSet(bodies);
    }

    @Named("listDataSetForConsumer")
    public ListDataSet listDataSetForConsumer() {
        List<Object> bodies = new LinkedList<>();
        bodies.add("Hello");
        bodies.add("World");
        return new ListDataSet(bodies);
    }

    @Named("fileDataSet")
    public FileDataSet fileDataSet() throws IOException {
        Path path = Files.createTempFile("fileDataSet", ".txt");
        Files.write(path, "Hello World".getBytes(StandardCharsets.UTF_8));
        return new FileDataSet(path.toFile());
    }

    @Named("fileDataSetDelimited")
    public FileDataSet fileDataSetDelimited() throws IOException {
        Path path = Files.createTempFile("fileDataSetDelimited", ".txt");
        Files.write(path, "Hello,World".getBytes(StandardCharsets.UTF_8));
        return new FileDataSet(path.toFile(), ",");
    }

    @Named("preloadedDataSet")
    public SimpleDataSet preloadedDataSet() {
        return new SimpleDataSet();
    }

    @Named("sedaDataSet")
    public SimpleDataSet sedaDataSet() {
        return new SimpleDataSet();
    }

    void disposeFileDataSet(@Disposes FileDataSet fileDataSet) throws IOException {
        File file = fileDataSet.getSourceFile();
        Files.deleteIfExists(file.toPath());
    }
}
