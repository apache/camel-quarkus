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
package org.apache.camel.quarkus.component.cxf.soap.wsrm.it;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.io.AbstractWrappedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageLossSimulator extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LoggerFactory.getLogger(MessageLossSimulator.class.getName());
    private AtomicInteger appMessageCount = new AtomicInteger();
    private Queue<String> wsrmResults;

    public MessageLossSimulator(Queue<String> wsrmResults) {
        super("prepare-send");
        this.addBefore(MessageSenderInterceptor.class.getName());
        this.wsrmResults = wsrmResults;
    }

    public void handleMessage(Message message) throws Fault {
        String action = RMContextUtils.retrieveMAPs(message, false, true).getAction().getValue();
        if (!RMContextUtils.isRMProtocolMessage(action)) {
            if (0 == this.appMessageCount.incrementAndGet() % 2) {
                InterceptorChain chain = message.getInterceptorChain();
                ListIterator it = chain.getIterator();

                while (it.hasNext()) {
                    PhaseInterceptor<?> pi = (PhaseInterceptor) it.next();
                    if (MessageSenderInterceptor.class.getName().equals(pi.getId())) {
                        chain.remove(pi);
                        Object greetMe = message.getContent(List.class).get(0);
                        Object result;
                        try {
                            //object is generated class for service which is registered for reflection
                            result = greetMe.getClass().getMethod("getArg0").invoke(greetMe);
                        } catch (Exception e) {
                            throw new Fault(e);
                        }
                        wsrmResults.add(result + " lost by MessageLossSimulator");
                        LOG.debug("Removed MessageSenderInterceptor from interceptor chain.");
                        break;
                    }
                }

                message.setContent(OutputStream.class, new MessageLossSimulator.WrappedOutputStream(message));
                message.getInterceptorChain().add(new AbstractPhaseInterceptor<Message>("prepare-send-ending") {
                    public void handleMessage(Message message) throws Fault {
                        try {
                            message.getContent(OutputStream.class).close();
                        } catch (IOException var3) {
                            throw new Fault(var3);
                        }
                    }
                });
            }
        }
    }

    private class DummyOutputStream extends OutputStream {
        private DummyOutputStream() {
        }

        public void write(int b) throws IOException {
        }
    }

    private class WrappedOutputStream extends AbstractWrappedOutputStream {
        private Message outMessage;

        WrappedOutputStream(Message m) {
            this.outMessage = m;
        }

        protected void onFirstWrite() throws IOException {
            if (MessageLossSimulator.LOG.isDebugEnabled()) {
                Long nr = RMContextUtils.retrieveRMProperties(this.outMessage, true).getSequence().getMessageNumber();
                MessageLossSimulator.LOG.debug("Losing message {}", nr);
            }

            this.wrappedStream = MessageLossSimulator.this.new DummyOutputStream();
        }
    }
}
