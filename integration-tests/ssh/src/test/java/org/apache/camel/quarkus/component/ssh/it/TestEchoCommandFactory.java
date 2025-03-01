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
package org.apache.camel.quarkus.component.ssh.it;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.command.CommandFactory;

public class TestEchoCommandFactory implements CommandFactory {

    @Override
    public Command createCommand(ChannelSession channelSession, String command) {
        return new TestEchoCommand(command);
    }

    public static class TestEchoCommand extends EchoCommand {
        public static CountDownLatch latch = new CountDownLatch(1);

        public TestEchoCommand(String command) {
            super(command);
        }

        @Override
        public void destroy(ChannelSession channelSession) throws Exception {
            if (latch != null) {
                latch.countDown();
            }
            super.destroy(channelSession);
        }
    }

    protected static class EchoCommand implements Command, Runnable {
        private String command;
        private OutputStream out;
        private OutputStream err;
        private ExitCallback callback;
        private Thread thread;

        public EchoCommand(String command) {
            this.command = command;
        }

        @Override
        public void setInputStream(InputStream in) {
        }

        @Override
        public void setOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void setErrorStream(OutputStream err) {
            this.err = err;
        }

        @Override
        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        @Override
        public void start(ChannelSession channelSession, Environment environment) throws IOException {
            thread = new Thread(this, "EchoCommand");
            thread.start();
        }

        @Override
        public void destroy(ChannelSession channelSession) throws Exception {
            // noop
        }

        @Override
        public void run() {
            boolean succeeded = true;
            String message = null;
            try {
                if (command.equals("wrong")) {
                    err.write(("command not found: " + command).getBytes(StandardCharsets.UTF_8));
                    succeeded = false;
                } else {
                    err.write("Expected Error:".getBytes(StandardCharsets.UTF_8));
                    err.write(command.getBytes(StandardCharsets.UTF_8));
                }
                err.flush();
                out.write(command.getBytes(StandardCharsets.UTF_8));
                out.flush();
            } catch (Exception e) {
                succeeded = false;
                message = e.toString();
            } finally {
                if (succeeded) {
                    callback.onExit(0);
                } else {
                    callback.onExit(1, message);
                }
            }
        }
    }
}
