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

package org.apache.camel.quarkus.component.leveldb.it;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.leveldb.LevelDBAggregationRepository;
import org.apache.camel.quarkus.component.leveldb.QuarkusLevelDBAggregationRepository;

public class LeveldbRouteBuilder extends RouteBuilder {
    public static final String DIRECT_START = "direct:start";
    public static final String DIRECT_BINARY = "direct:binary";
    public static final String DIRECT_START_WITH_FAILURE = "direct:startWithFailure";
    public static final String DIRECT_START_DEAD_LETTER = "direct:startDeadLetter";
    public static final String MOCK_AGGREGATED = "mock:aggregated";
    public static final String MOCK_RESULT = "mock:result";
    public static final String MOCK_DEAD = "mock:dead";
    public static final String DATA_FOLDER = "target/data";

    private static AtomicInteger counter = new AtomicInteger(0);

    @Override
    public void configure() {
        LevelDBAggregationRepository repo = new QuarkusLevelDBAggregationRepository("repo", DATA_FOLDER + "leveldb.dat");

        from(DIRECT_START)
                .aggregate(header("id"), new MyAggregationStrategy())
                .completionSize(7).aggregationRepository(repo)
                .to(MOCK_RESULT);

        LevelDBAggregationRepository repoBinary = new QuarkusLevelDBAggregationRepository("repo",
                DATA_FOLDER + "levelBinarydb.dat");

        from(DIRECT_BINARY)
                .aggregate(header("id"), new BinaryAggregationStrategy())
                .completionSize(3).aggregationRepository(repoBinary)
                .to(MOCK_RESULT);

        LevelDBAggregationRepository repoWithFailure = new QuarkusLevelDBAggregationRepository("repoWithFailure",
                DATA_FOLDER + "leveldbWithFailure.dat");

        repoWithFailure.setUseRecovery(true);
        repoWithFailure.setRecoveryInterval(500, TimeUnit.MILLISECONDS);

        from(DIRECT_START_WITH_FAILURE)
                .aggregate(header("id"), new MyAggregationStrategy())
                .completionSize(7).aggregationRepository(repoWithFailure)
                .to(MOCK_AGGREGATED)
                .process(exchange -> {
                    int count = counter.incrementAndGet();
                    if (count <= 2) {
                        throw new IllegalArgumentException("Failure");
                    }
                })
                .to(MOCK_RESULT)
                .end();

        LevelDBAggregationRepository repoDeadLetter = new QuarkusLevelDBAggregationRepository("repoDeadLetter",
                DATA_FOLDER + "leveldbDeadLetter.dat");

        repoDeadLetter.setUseRecovery(true);
        repoDeadLetter.setRecoveryInterval(500, TimeUnit.MILLISECONDS);
        repoDeadLetter.setMaximumRedeliveries(3);
        repoDeadLetter.setDeadLetterUri(MOCK_DEAD);

        from(DIRECT_START_DEAD_LETTER)
                .aggregate(header("id"), new MyAggregationStrategy())
                .completionSize(7).aggregationRepository(repoDeadLetter)
                .to(MOCK_AGGREGATED)
                .process(e -> {
                    throw new IllegalArgumentException("Failure");
                })
                .log("XXX: result exchange id ${exchangeId} with ${body}")
                .to(MOCK_RESULT)
                .end();
    }

    public static class MyAggregationStrategy implements AggregationStrategy {

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                return newExchange;
            }
            String body1 = oldExchange.getIn().getBody(String.class);
            String body2 = newExchange.getIn().getBody(String.class);

            oldExchange.getIn().setBody(body1 + "+" + body2);
            return oldExchange;
        }
    }

    public static class BinaryAggregationStrategy implements AggregationStrategy {

        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                return newExchange;
            }
            byte[] body1 = oldExchange.getIn().getBody(byte[].class);
            byte[] body2 = newExchange.getIn().getBody(byte[].class);

            //keeps longer byte[]
            oldExchange.getIn().setBody(body1.length > body2.length ? body1 : body2);
            return oldExchange;
        }
    }
}
