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
package org.apache.camel.quarkus.component.jt400.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.KeyedDataQueue;
import com.ibm.as400.access.KeyedDataQueueEntry;
import com.ibm.as400.access.MessageQueue;
import com.ibm.as400.access.ObjectDoesNotExistException;
import com.ibm.as400.access.QueuedMessage;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;

public class Jt400TestResource implements QuarkusTestResourceLifecycleManager {
    private static final Logger LOGGER = Logger.getLogger(Jt400TestResource.class);

    public static enum RESOURCE_TYPE {
        messageQueue,
        keyedDataQue,
        lifoQueueu,
        replyToQueueu;
    }

    private static final Optional<String> JT400_CLEAR_ALL = ConfigProvider.getConfig().getOptionalValue("cq.jt400.clear-all",
            String.class);
    private static final String JT400_URL = ConfigProvider.getConfig().getValue("cq.jt400.url", String.class);
    private static final String JT400_USERNAME = ConfigProvider.getConfig().getValue("cq.jt400.username", String.class);
    private static final String JT400_PASSWORD = ConfigProvider.getConfig().getValue("cq.jt400.password", String.class);
    private static final String JT400_LIBRARY = ConfigProvider.getConfig().getValue("cq.jt400.library", String.class);
    private static final String JT400_MESSAGE_QUEUE = ConfigProvider.getConfig().getValue("cq.jt400.message-queue",
            String.class);
    private static final String JT400_REPLY_TO_MESSAGE_QUEUE = ConfigProvider.getConfig().getValue(
            "cq.jt400.message-replyto-queue",
            String.class);
    private static final String JT400_LIFO_QUEUE = ConfigProvider.getConfig().getValue("cq.jt400.lifo-queue",
            String.class);
    private static final String JT400_KEYED_QUEUE = ConfigProvider.getConfig().getValue("cq.jt400.keyed-queue", String.class);

    //depth of repetitive reads for lifo queue clearing
    private final static int CLEAR_DEPTH = 100;
    public final static String LOCK_KEY = "cq.jt400.global-lock";
    //5 minute timeout to obtain a log for the tests execution
    private final static int LOCK_TIMEOUT = 300000;

    private static AS400 as400 = new AS400(JT400_URL, JT400_USERNAME, JT400_PASSWORD);;

    @Override
    public Map<String, String> start() {
        //no need to start, as the instance already exists
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (as400 != null) {
            try {
                CLIENT_HELPER.clearAll(JT400_CLEAR_ALL.isPresent() && Boolean.parseBoolean(JT400_CLEAR_ALL.get()));
            } catch (Exception e) {
                LOGGER.debug("Clearing of the external queues failed", e);
            }
            as400.close();
        }
    }

    private static String getObjectPath(String object) {
        return String.format("/QSYS.LIB/%s.LIB/%s", JT400_LIBRARY, object);
    }

    public static Jt400ClientHelper CLIENT_HELPER = new Jt400ClientHelper() {

        private String key = null;
        Map<RESOURCE_TYPE, Set<Object>> toRemove = new HashMap<>();

        @Override
        public QueuedMessage peekReplyToQueueMessage(String msg) throws Exception {
            return getQueueMessage(JT400_REPLY_TO_MESSAGE_QUEUE, msg);
        }

        private QueuedMessage getQueueMessage(String queue, String msg) throws Exception {
            MessageQueue messageQueue = new MessageQueue(as400,
                    getObjectPath(queue));
            Enumeration<QueuedMessage> msgs = messageQueue.getMessages();

            while (msgs.hasMoreElements()) {
                QueuedMessage queuedMessage = msgs.nextElement();

                if (msg.equals(queuedMessage.getText())) {
                    return queuedMessage;
                }
            }
            return null;
        }

        @Override
        public void registerForRemoval(RESOURCE_TYPE type, Object value) {
            if (toRemove.containsKey(type)) {
                toRemove.get(type).add(value);
            } else {
                Set<Object> set = new HashSet<>();
                set.add(value);
                toRemove.put(type, set);
            }
        }

        @Override
        public void clearAll(boolean all) throws Exception {
            //message queue
            MessageQueue mq = new MessageQueue(as400, getObjectPath(JT400_MESSAGE_QUEUE));
            if (all) {
                mq.remove();
            } else if (toRemove.containsKey(RESOURCE_TYPE.messageQueue)) {
                clearMessageQueue(RESOURCE_TYPE.messageQueue, mq);
            }

            //lifo queue
            DataQueue dq = new DataQueue(as400, getObjectPath(JT400_LIFO_QUEUE));
            if (all) {
                for (int i = 01; i < CLEAR_DEPTH; i++) {
                    if (dq.read() == null) {
                        break;
                    }
                }
            } else if (toRemove.containsKey(RESOURCE_TYPE.lifoQueueu)) {
                for (Object entry : toRemove.get(RESOURCE_TYPE.lifoQueueu)) {
                    List<byte[]> otherMessages = new LinkedList<>();
                    DataQueueEntry dqe = dq.read();
                    while (dqe != null && !(entry.equals(dqe.getString())
                            || entry.equals(new String(dqe.getData(), StandardCharsets.UTF_8)))) {
                        otherMessages.add(dqe.getData());
                        dqe = dq.read();
                    }
                    //write back other messages in reverse order (it is a lifo)
                    Collections.reverse(otherMessages);
                    for (byte[] msg : otherMessages) {
                        dq.write(msg);
                    }
                }
            }
            //reply-to queue
            MessageQueue rq = new MessageQueue(as400, getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE));
            if (all) {
                rq.remove();
            } else if (toRemove.containsKey(RESOURCE_TYPE.replyToQueueu)) {
                clearMessageQueue(RESOURCE_TYPE.replyToQueueu, rq);
            }

            //keyed queue
            KeyedDataQueue kdq = new KeyedDataQueue(as400, getObjectPath(JT400_KEYED_QUEUE));
            if (all) {
                kdq.clear();
            } else if (toRemove.containsKey(RESOURCE_TYPE.keyedDataQue)) {
                for (Object entry : toRemove.get(RESOURCE_TYPE.keyedDataQue)) {
                    kdq.clear((String) entry);
                }
            }
        }

        private void clearMessageQueue(RESOURCE_TYPE type, MessageQueue mq) throws AS400SecurityException,
                ErrorCompletingRequestException, InterruptedException, IOException, ObjectDoesNotExistException {
            if (!toRemove.get(type).isEmpty()) {
                List<QueuedMessage> msgs = Collections.list(mq.getMessages());
                Map<String, byte[]> keys = msgs.stream().collect(Collectors.toMap(q -> q.getText(), q -> q.getKey()));
                for (Object entry : toRemove.get(type)) {
                    if (entry instanceof String) {
                        mq.remove(keys.get((String) entry));
                    } else {
                        mq.remove((byte[]) entry);
                    }
                }
            }
        }

        /**
         * Keyed dataque (FIFO) is used for locking purposes.
         *
         * - Each participant saves unique token into a key cq.jt400.global-lock
         * - Each participant the reads the FIFO queue and if the resulted string is its own unique token, execution is allowed
         * - When execution ends, the key is removed
         *
         * If the token is not its own
         * -read of the token is repeated until timeout or its own token is returned (so the second participant waits, until the
         * first participant removes its token)
         *
         * Dead lock prevention
         *
         * - part of the unique token is timestamp, if participant finds a token, which is too old, token is removed
         * - action to clear-all data removes also the locking tokens
         *
         *
         * Therefore only 1 token (thus 1 participant) is allowed to run the tests, the others have to wait
         *
         * @throws Exception
         */
        @Override
        public void lock() throws Exception {
            if (key == null) {
                key = generateKey();
                //write key into keyed queue
                KeyedDataQueue kdq = new KeyedDataQueue(as400, getObjectPath(JT400_KEYED_QUEUE));

                Assertions.assertTrue(kdq.isFIFO(), "keyed dataqueue has to be FIFO");

                kdq.write(LOCK_KEY, key);

                //added 5 seconds for the timeout, to have some spare time for removing old locks
                Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(LOCK_TIMEOUT + 5000, TimeUnit.SECONDS)
                        .until(
                                () -> {
                                    KeyedDataQueueEntry kdqe = kdq.peek(LOCK_KEY);
                                    if (kdqe == null) {
                                        //if kdqe is null, try to lock again
                                        LOGGER.debug("locked in the queueu was removed, locking again with " + key);
                                        kdq.write(LOCK_KEY, key);
                                    }
                                    String peekedKey = kdqe == null ? null : kdqe.getString();
                                    //if waiting takes more than 300s, check whether the actual lock can be removed
                                    LOGGER.debug("peeked lock " + peekedKey + "(my lock is " + key + ")");

                                    if (peekedKey != null && !key.equals(peekedKey)) {
                                        long peekedTime = Long.parseLong(peekedKey.substring(11));
                                        if (System.currentTimeMillis() - peekedTime > LOCK_TIMEOUT) {
                                            //read the key (therefore remove it)
                                            String readKey = kdq.read(LOCK_KEY).getString();
                                            System.out.println("Removed old lock " + readKey);
                                            peekedKey = kdq.peek(LOCK_KEY).getString();
                                        }
                                    }
                                    return peekedKey;
                                },
                                Matchers.is(key));
            }
        }

        @Override
        public void unlock() throws Exception {
            Assertions.assertEquals(key,
                    new KeyedDataQueue(as400, getObjectPath(JT400_KEYED_QUEUE)).read(LOCK_KEY).getString());
            //clear key
            key = null;
        }

        private String generateKey() {
            return RandomStringUtils.randomAlphanumeric(10).toLowerCase(Locale.ROOT) + ":" + System.currentTimeMillis();
        }

        @Override
        public String dumpQueues() throws Exception {
            StringBuilder sb = new StringBuilder();

            sb.append("\n* MESSAGE QUEUE\n");
            sb.append("\t" + Collections.list(new MessageQueue(as400, getObjectPath(JT400_MESSAGE_QUEUE)).getMessages())
                    .stream().map(mq -> mq.getText()).sorted().collect(Collectors.joining(", ")));

            sb.append("\n* INQUIRY QUEUE\n");
            sb.append("\t" + Collections
                    .list(new MessageQueue(as400, getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE)).getMessages())
                    .stream().map(mq -> mq.getText()).sorted().collect(Collectors.joining(", ")));

            sb.append("\n* LIFO QUEUE\n");
            DataQueue dq = new DataQueue(as400, getObjectPath(JT400_LIFO_QUEUE));
            DataQueueEntry dqe;
            List<byte[]> lifoMessages = new LinkedList<>();
            List<String> lifoTexts = new LinkedList<>();
            do {
                dqe = dq.read();
                if (dqe != null) {
                    lifoTexts.add(dqe.getString() + " (" + new String(dqe.getData(), StandardCharsets.UTF_8) + ")");
                    lifoMessages.add(dqe.getData());
                }
            } while (dqe != null);

            //write back other messages in reverse order (it is a lifo)
            Collections.reverse(lifoMessages);
            for (byte[] msg : lifoMessages) {
                dq.write(msg);
            }
            sb.append(lifoTexts.stream().collect(Collectors.joining(", ")));

            sb.append("\n* KEYED DATA QUEUE\n");
            KeyedDataQueue kdq = new KeyedDataQueue(as400, getObjectPath(JT400_KEYED_QUEUE));
            KeyedDataQueueEntry kdqe = kdq.peek(LOCK_KEY);
            sb.append("\tlock: " + (kdqe == null ? "null" : kdqe.getString()));
            return sb.toString();
        }

        public void sendInquiry(String msg) throws Exception {
            new MessageQueue(as400, getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE)).sendInquiry(msg,
                    getObjectPath(JT400_REPLY_TO_MESSAGE_QUEUE));
        }
    };

}

interface Jt400ClientHelper {

    void registerForRemoval(Jt400TestResource.RESOURCE_TYPE type, Object value);

    QueuedMessage peekReplyToQueueMessage(String msg) throws Exception;

    void sendInquiry(String msg) throws Exception;

    //------------------- clear listeners ------------------------------

    void clearAll(boolean all) throws Exception;

    //----------------------- locking

    void lock() throws Exception;

    void unlock() throws Exception;

    String dumpQueues() throws Exception;

}
