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
package org.apache.camel.quarkus.component.flink.it;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.flink.DataSetCallback;
import org.apache.camel.component.flink.DataStreamCallback;
import org.apache.camel.component.flink.FlinkConstants;
import org.apache.camel.component.flink.Flinks;
import org.apache.camel.component.flink.VoidDataSetCallback;
import org.apache.camel.component.flink.annotations.AnnotatedDataSetCallback;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.GenericTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.CloseableIterator;

@Path("/flink/processing")
@ApplicationScoped
@Consumes(MediaType.TEXT_PLAIN)
@Produces(MediaType.TEXT_PLAIN)
public class FlinkDataProcessingResource {

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate template;

    @POST
    @Path("/dataset/callback/{callback}")
    public String dataSet(@PathParam("callback") String callback, @QueryParam("factor") List<Integer> factors, String text) {
        DataSet<String> dataSet = dataSetEnvironment().fromCollection(text.lines().toList());
        Object payload = factors.isEmpty() ? null : factors.size() == 1 ? factors.get(0) : factors;
        DataSetCallback<?> dataSetCallback = switch (callback) {
        case "payload" -> new CountLines();
        case "annotated" -> new AnnotatedDataSetCallback(
                factors.isEmpty() ? new AnnotatedCountLines() : new MultiplyCount(), context);
        default -> throw new BadRequestException("Unknown callback: " + callback);
        };
        return template.requestBodyAndHeaders("flink:dataset", payload,
                Map.of(FlinkConstants.FLINK_DATASET_HEADER, dataSet,
                        FlinkConstants.FLINK_DATASET_CALLBACK_HEADER, dataSetCallback),
                String.class);
    }

    @POST
    @Path("/dataset/collect")
    public String collectDataSet(String text) {
        DataSet<String> dataSet = dataSetEnvironment().fromCollection(text.lines().toList());
        // Returning a DataSet makes Camel collect it, executing the job and deserializing its POJO results.
        DataSetCallback<DataSet<WordCount>> callback = (ds, payloads) -> {
            DataSet<String> words = ds;
            return words.map(new ToWordCount()).rebalance().groupBy("word").reduce(new SumCounts());
        };
        List<WordCount> result = template.requestBodyAndHeaders("flink:dataset", null,
                Map.of(FlinkConstants.FLINK_DATASET_HEADER, dataSet,
                        FlinkConstants.FLINK_DATASET_CALLBACK_HEADER, callback),
                List.class);
        return result.stream().map(word -> word.word + ":" + word.count).sorted().collect(Collectors.joining(","));
    }

    @POST
    @Path("/dataset/objenesis")
    public String collectDataSetUsingObjenesis(String text) {
        DataSet<String> dataSet = dataSetEnvironment().fromCollection(text.lines().toList());
        DataSetCallback<DataSet<ConstructorOnlyValue>> callback = (ds, payloads) -> {
            DataSet<String> words = ds;
            // GenericTypeInfo selects Kryo; the missing no-arg constructor requires its Objenesis fallback.
            return words.map(new ToConstructorOnlyValue())
                    .returns(new GenericTypeInfo<>(ConstructorOnlyValue.class)).rebalance();
        };
        List<ConstructorOnlyValue> result = template.requestBodyAndHeaders("flink:dataset", null,
                Map.of(FlinkConstants.FLINK_DATASET_HEADER, dataSet,
                        FlinkConstants.FLINK_DATASET_CALLBACK_HEADER, callback),
                List.class);
        return result.stream().map(value -> value.word + ":" + value.length + ":" + value.constructorInvoked)
                .sorted().collect(Collectors.joining(","));
    }

    @POST
    @Path("/dataset/sink/{callback}")
    public String dataSetSink(@PathParam("callback") String callback, String text) throws Exception {
        java.nio.file.Path directory = Files.createTempDirectory(Paths.get("target"), "flink-output-");
        java.nio.file.Path output = directory.resolve("result.txt");
        try {
            DataSet<String> dataSet = dataSetEnvironment().fromCollection(text.lines().toList());
            DataSetCallback<?> sink = switch (callback) {
            case "void" -> new VoidDataSetCallback() {
                @Override
                public void doOnDataSet(DataSet ds, Object... payloads) {
                    writeDataSet(ds, (String) payloads[0]);
                }
            };
            case "annotated" -> new AnnotatedDataSetCallback(new AnnotatedSink());
            default -> throw new BadRequestException("Unknown callback: " + callback);
            };
            template.requestBodyAndHeaders("flink:dataset", output.toString(),
                    Map.of(FlinkConstants.FLINK_DATASET_HEADER, dataSet,
                            FlinkConstants.FLINK_DATASET_CALLBACK_HEADER, sink));
            return Files.readString(output);
        } finally {
            Files.deleteIfExists(output);
            Files.deleteIfExists(directory);
        }
    }

    @POST
    @Path("/datastream/collect")
    public String collectDataStream(String text) throws Exception {
        try (StreamExecutionEnvironment env = streamEnvironment()) {
            DataStream<String> stream = env.fromData(text.lines().toList());
            DataStreamCallback<List<Tuple2<String, Integer>>> callback = (ds, payloads) -> {
                DataStream<String> words = ds;
                DataStream<Tuple2<String, Integer>> transformed = words.filter(new NonEmpty()).map(new ToTuple()).rebalance();
                List<Tuple2<String, Integer>> values = new ArrayList<>();
                try (CloseableIterator<Tuple2<String, Integer>> iterator = transformed.executeAndCollect()) {
                    iterator.forEachRemaining(values::add);
                }
                return values;
            };
            List<Tuple2<String, Integer>> result = template.requestBodyAndHeaders("flink:datastream", null,
                    Map.of(FlinkConstants.FLINK_DATASTREAM_HEADER, stream,
                            FlinkConstants.FLINK_DATASTREAM_CALLBACK_HEADER, callback),
                    List.class);
            return result.stream().map(tuple -> tuple.f0 + ":" + tuple.f1).sorted().collect(Collectors.joining(","));
        }
    }

    private static ExecutionEnvironment dataSetEnvironment() {
        ExecutionEnvironment env = Flinks.createExecutionEnvironment();
        env.getConfiguration().set(CoreOptions.TMP_DIRS, "target");
        env.setParallelism(1);
        return env;
    }

    private static StreamExecutionEnvironment streamEnvironment() {
        Configuration configuration = new Configuration();
        configuration.set(CoreOptions.TMP_DIRS, "target");
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(configuration);
        env.setParallelism(1);
        env.setRuntimeMode(RuntimeExecutionMode.BATCH);
        return env;
    }

    private static void writeDataSet(DataSet<?> dataSet, String path) {
        try {
            dataSet.writeAsText(path);
            dataSet.getExecutionEnvironment().execute("Camel DataSet sink");
        } catch (Exception e) {
            throw new RuntimeCamelException(e);
        }
    }

    public static class CountLines implements DataSetCallback<Long> {
        @Override
        public Long onDataSet(DataSet ds, Object... payloads) {
            try {
                long result = ds.count();
                for (Object payload : payloads) {
                    if (payload != null) {
                        result *= (Integer) payload;
                    }
                }
                return result;
            } catch (Exception e) {
                throw new RuntimeCamelException(e);
            }
        }
    }

    public static class AnnotatedCountLines {
        @org.apache.camel.component.flink.annotations.DataSetCallback
        public long count(DataSet<String> dataSet) throws Exception {
            return dataSet.count();
        }
    }

    public static class MultiplyCount {
        @org.apache.camel.component.flink.annotations.DataSetCallback
        public long count(DataSet<String> dataSet, int first, int second) throws Exception {
            return dataSet.count() * first * second;
        }
    }

    public static class AnnotatedSink {
        @org.apache.camel.component.flink.annotations.DataSetCallback
        public void write(DataSet<String> dataSet, String path) {
            writeDataSet(dataSet, path);
        }
    }

    // Public fields and the no-arg constructor exercise Flink's reflective POJO serializer.
    public static class WordCount {
        public String word;
        public int count;

        public WordCount() {
        }

        public WordCount(String word, int count) {
            this.word = word;
            this.count = count;
        }
    }

    public static class ToWordCount implements MapFunction<String, WordCount> {
        @Override
        public WordCount map(String value) {
            return new WordCount(value, 1);
        }
    }

    public static class ConstructorOnlyValue {
        private String word;
        private int length;
        // Kryo does not serialize this flag, so false on collection proves the constructor was bypassed.
        private transient boolean constructorInvoked;

        public ConstructorOnlyValue(String word) {
            this.word = word;
            this.length = word.length();
            this.constructorInvoked = true;
        }
    }

    public static class ToConstructorOnlyValue implements MapFunction<String, ConstructorOnlyValue> {
        @Override
        public ConstructorOnlyValue map(String value) {
            return new ConstructorOnlyValue(value);
        }
    }

    public static class SumCounts implements ReduceFunction<WordCount> {
        @Override
        public WordCount reduce(WordCount first, WordCount second) {
            return new WordCount(first.word, first.count + second.count);
        }
    }

    public static class NonEmpty implements FilterFunction<String> {
        @Override
        public boolean filter(String value) {
            return !value.isBlank();
        }
    }

    public static class ToTuple implements MapFunction<String, Tuple2<String, Integer>> {
        @Override
        public Tuple2<String, Integer> map(String value) {
            return Tuple2.of(value.toUpperCase(Locale.ROOT), value.length());
        }
    }
}
