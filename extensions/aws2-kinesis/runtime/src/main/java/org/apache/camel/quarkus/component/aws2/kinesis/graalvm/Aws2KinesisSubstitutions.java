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
package org.apache.camel.quarkus.component.aws2.kinesis.graalvm;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import software.amazon.kinesis.retrieval.AggregatorUtil;
import software.amazon.kinesis.retrieval.KinesisClientRecord;
import software.amazon.kinesis.retrieval.kpl.Messages;

public class Aws2KinesisSubstitutions {
}

/**
 * Quick (ugly) fix of https://github.com/awslabs/amazon-kinesis-client/issues/1355
 */
@TargetClass(AggregatorUtil.class)
final class AggregatorUtilSubstitutions {

    @Alias
    protected byte[] calculateTailCheck(byte[] data) {
        return null;
    }

    @Alias
    protected BigInteger effectiveHashKey(String partitionKey, String explicitHashKey) throws UnsupportedEncodingException {
        return null;
    }

    @Alias
    public KinesisClientRecord convertRecordToKinesisClientRecord(final KinesisClientRecord record,
            final boolean aggregated,
            final long subSequenceNumber,
            final String explicitHashKey) {
        return null;
    }

    @Substitute
    public List<KinesisClientRecord> deaggregate(List<KinesisClientRecord> records,
            BigInteger startingHashKey,
            BigInteger endingHashKey) {
        List<KinesisClientRecord> result = new ArrayList<>();
        byte[] magic = new byte[AggregatorUtil.AGGREGATED_RECORD_MAGIC.length];
        byte[] digest = new byte[16];

        for (KinesisClientRecord r : records) {
            boolean isAggregated = true;
            long subSeqNum = 0;
            ByteBuffer bb = r.data();

            if (bb.remaining() >= magic.length) {
                bb.get(magic);
            } else {
                isAggregated = false;
            }

            if (!Arrays.equals(AggregatorUtil.AGGREGATED_RECORD_MAGIC, magic) || bb.remaining() <= 16) {
                isAggregated = false;
            }

            if (isAggregated) {
                int oldLimit = bb.limit();
                bb.limit(oldLimit - 16);
                byte[] messageData = new byte[bb.remaining()];
                bb.get(messageData);
                bb.limit(oldLimit);
                bb.get(digest);
                byte[] calculatedDigest = calculateTailCheck(messageData);

                if (!Arrays.equals(digest, calculatedDigest)) {
                    isAggregated = false;
                } else {
                    try {
                        Messages.AggregatedRecord ar = Messages.AggregatedRecord.parseFrom(messageData);
                        List<String> pks = ar.getPartitionKeyTableList();
                        List<String> ehks = ar.getExplicitHashKeyTableList();
                        long aat = r.approximateArrivalTimestamp() == null
                                ? -1 : r.approximateArrivalTimestamp().toEpochMilli();
                        try {
                            int recordsInCurrRecord = 0;
                            for (Messages.Record mr : ar.getRecordsList()) {
                                String explicitHashKey = null;
                                String partitionKey = pks.get((int) mr.getPartitionKeyIndex());
                                if (mr.hasExplicitHashKeyIndex()) {
                                    explicitHashKey = ehks.get((int) mr.getExplicitHashKeyIndex());
                                }

                                BigInteger effectiveHashKey = effectiveHashKey(partitionKey, explicitHashKey);

                                if (effectiveHashKey.compareTo(startingHashKey) < 0
                                        || effectiveHashKey.compareTo(endingHashKey) > 0) {
                                    for (int toRemove = 0; toRemove < recordsInCurrRecord; ++toRemove) {
                                        result.remove(result.size() - 1);
                                    }
                                    break;
                                }

                                ++recordsInCurrRecord;

                                KinesisClientRecord record = r.toBuilder()
                                        .data(ByteBuffer.wrap(mr.getData().toByteArray()))
                                        .partitionKey(partitionKey)
                                        .explicitHashKey(explicitHashKey)
                                        .build();
                                result.add(convertRecordToKinesisClientRecord(record, true, subSeqNum++, explicitHashKey));
                            }
                        } catch (Exception e) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Unexpected exception during deaggregation, record was:\n");
                            sb.append("PKS:\n");
                            for (String s : pks) {
                                sb.append(s).append("\n");
                            }
                            sb.append("EHKS: \n");
                            for (String s : ehks) {
                                sb.append(s).append("\n");
                            }
                            for (Messages.Record mr : ar.getRecordsList()) {
                                sb.append("Record: [hasEhk=").append(mr.hasExplicitHashKeyIndex()).append(", ")
                                        .append("ehkIdx=").append(mr.getExplicitHashKeyIndex()).append(", ")
                                        .append("pkIdx=").append(mr.getPartitionKeyIndex()).append(", ")
                                        .append("dataLen=").append(mr.getData().toByteArray().length).append("]\n");
                            }
                            sb.append("Sequence number: ").append(r.sequenceNumber()).append("\n")
                                    .append("Raw data: ")
                                    .append(jakarta.xml.bind.DatatypeConverter.printBase64Binary(messageData)).append("\n");
                            // todo log.error(sb.toString(), e);
                        }
                    } catch (InvalidProtocolBufferException e) {
                        isAggregated = false;
                    }
                }
            }

            if (!isAggregated) {
                bb.rewind();
                result.add(r);
            }
        }
        return result;
    }
}
