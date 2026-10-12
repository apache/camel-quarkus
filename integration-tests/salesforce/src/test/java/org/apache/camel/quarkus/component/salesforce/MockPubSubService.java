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
package org.apache.camel.quarkus.component.salesforce;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.google.protobuf.ByteString;
import com.salesforce.eventbus.protobuf.ConsumerEvent;
import com.salesforce.eventbus.protobuf.FetchRequest;
import com.salesforce.eventbus.protobuf.FetchResponse;
import com.salesforce.eventbus.protobuf.ProducerEvent;
import com.salesforce.eventbus.protobuf.PubSubGrpc;
import com.salesforce.eventbus.protobuf.SchemaInfo;
import com.salesforce.eventbus.protobuf.SchemaRequest;
import com.salesforce.eventbus.protobuf.TopicInfo;
import com.salesforce.eventbus.protobuf.TopicRequest;
import io.grpc.stub.StreamObserver;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;

/**
 * A minimal stand-in for the Salesforce Pub / Sub API, which delivers a single TestEvent__e event to each subscriber.
 */
public class MockPubSubService extends PubSubGrpc.PubSubImplBase {
    public static final String CREATED_BY = "0055g00000AbCdEfGH";
    public static final long CREATED_DATE = 1760000000000L;
    public static final String TEST_FIELD_VALUE = "Camel Quarkus Pub / Sub test";

    private static final String SCHEMA_ID = "test-event-schema";
    // Matches the schema of the generated com.sforce.eventbus.TestEvent__e
    private static final String SCHEMA_JSON = """
            {
              "type": "record",
              "name": "TestEvent__e",
              "namespace": "com.sforce.eventbus",
              "fields": [
                {"name": "CreatedDate", "type": "long", "doc": "CreatedDate:DateTime"},
                {"name": "CreatedById", "type": "string", "doc": "CreatedBy:EntityId"},
                {"name": "Test_Field__c", "type": ["null", "string"], "doc": "Data:Text", "default": null}
              ]
            }
            """;

    @Override
    public void getTopic(TopicRequest request, StreamObserver<TopicInfo> responseObserver) {
        responseObserver.onNext(TopicInfo.newBuilder()
                .setTopicName(request.getTopicName())
                .setCanPublish(true)
                .setCanSubscribe(true)
                .setSchemaId(SCHEMA_ID)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getSchema(SchemaRequest request, StreamObserver<SchemaInfo> responseObserver) {
        responseObserver.onNext(SchemaInfo.newBuilder()
                .setSchemaId(SCHEMA_ID)
                .setSchemaJson(SCHEMA_JSON)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<FetchRequest> subscribe(StreamObserver<FetchResponse> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(FetchRequest request) {
                ByteString replayId = ByteString.copyFromUtf8("1");
                ConsumerEvent event = ConsumerEvent.newBuilder()
                        .setReplayId(replayId)
                        .setEvent(ProducerEvent.newBuilder()
                                .setId("test-event")
                                .setSchemaId(SCHEMA_ID)
                                .setPayload(ByteString.copyFrom(createPayload())))
                        .build();

                // Report the remaining requested events as pending, so that the client does not ask for more
                responseObserver.onNext(FetchResponse.newBuilder()
                        .addEvents(event)
                        .setLatestReplayId(replayId)
                        .setPendingNumRequested(request.getNumRequested())
                        .build());
            }

            @Override
            public void onError(Throwable t) {
                // Noop
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    private static byte[] createPayload() {
        Schema schema = new Schema.Parser().parse(SCHEMA_JSON);
        GenericRecord record = new GenericRecordBuilder(schema)
                .set("CreatedDate", CREATED_DATE)
                .set("CreatedById", CREATED_BY)
                .set("Test_Field__c", TEST_FIELD_VALUE)
                .build();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder(out, null);
            new GenericDatumWriter<GenericRecord>(schema).write(record, encoder);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
