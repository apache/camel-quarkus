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
package org.apache.camel.quarkus.component.paho.mqtt5.it;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal MQTT 5 TCP server that reproduces the resubscribe-failure scenario from CAMEL-24511.
 *
 * <ul>
 * <li>Connection 1 (initial): CONNECT/CONNACK, SUBSCRIBE/SUBACK, then force-close to simulate connection
 * loss.</li>
 * <li>Connection 2 (auto-reconnect): CONNECT/CONNACK, but ignores SUBSCRIBE and PINGREQ. The client's
 * keep-alive timer fires, causing MqttException 32000 (connection lost) which triggers the fix.</li>
 * <li>Connection 3+ (after route restart): normal CONNECT/CONNACK, SUBSCRIBE/SUBACK, PINGREQ/PINGRESP.</li>
 * </ul>
 */
class FaultyMqtt5Broker {

    private static final Logger LOG = LoggerFactory.getLogger(FaultyMqtt5Broker.class);

    private final int port;
    private final int firstNormalConnection;
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private volatile ServerSocket serverSocket;
    private volatile ExecutorService executor;
    private volatile boolean running;

    FaultyMqtt5Broker(int port, int firstNormalConnection) {
        this.port = port;
        this.firstNormalConnection = firstNormalConnection;
    }

    void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "FaultyMqtt5Broker");
            t.setDaemon(true);
            return t;
        });
        executor.submit(this::acceptLoop);
        LOG.info("FaultyMqtt5Broker started on port {}", port);
    }

    void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // ignored
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        LOG.info("FaultyMqtt5Broker stopped");
    }

    int getConnectionCount() {
        return connectionCount.get();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                int connNum = connectionCount.incrementAndGet();
                LOG.info("Connection #{} from {}", connNum, socket.getRemoteSocketAddress());
                executor.submit(() -> handleConnection(socket, connNum));
            } catch (SocketException e) {
                if (running) {
                    LOG.warn("Accept error: {}", e.getMessage());
                }
            } catch (IOException e) {
                LOG.warn("Accept error: {}", e.getMessage());
            }
        }
    }

    private void handleConnection(Socket socket, int connNum) {
        try (socket) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            byte[] buf = new byte[1024];
            int len = in.read(buf);
            if (len <= 0 || ((buf[0] >> 4) & 0x0F) != 1) {
                return;
            }
            // CONNACK: session not present, success, no properties
            LOG.info("Conn #{}: CONNECT received, sending CONNACK", connNum);
            out.write(new byte[] { 0x20, 0x03, 0x00, 0x00, 0x00 });
            out.flush();

            if (connNum >= 2 && connNum < firstNormalConnection) {
                LOG.info("Conn #{}: Simulating resubscribe failure - not responding to SUBSCRIBE or PINGREQ", connNum);
                // Keep socket open but ignore everything.
                // The client's keep-alive timer will fire, causing MqttException 32000.
                try {
                    while (running && in.read(buf) > 0) {
                        int pt = (buf[0] >> 4) & 0x0F;
                        LOG.debug("Conn #{}: Ignoring packet type {}", connNum, pt);
                    }
                } catch (IOException ignored) {
                }
                return;
            }

            // Connection 1 and 3+: handle packets normally
            while (running) {
                len = in.read(buf);
                if (len <= 0) {
                    break;
                }
                int packetType = (buf[0] >> 4) & 0x0F;
                switch (packetType) {
                case 8: // SUBSCRIBE
                    int packetIdMsb = len > 2 ? buf[2] : 0;
                    int packetIdLsb = len > 3 ? buf[3] : 1;
                    LOG.info("Conn #{}: SUBSCRIBE received, sending SUBACK", connNum);
                    out.write(new byte[] { (byte) 0x90, 0x04, (byte) packetIdMsb, (byte) packetIdLsb, 0x00, 0x01 });
                    out.flush();
                    if (connNum == 1) {
                        Thread.sleep(2000);
                        LOG.info("Conn #{}: Simulating connection loss - closing connection", connNum);
                        return;
                    }
                    break;
                case 12: // PINGREQ
                    out.write(new byte[] { (byte) 0xD0, 0x00 });
                    out.flush();
                    break;
                case 14: // DISCONNECT
                    LOG.info("Conn #{}: DISCONNECT received", connNum);
                    return;
                default:
                    break;
                }
            }
        } catch (SocketException e) {
            LOG.debug("Conn #{}: Socket closed: {}", connNum, e.getMessage());
        } catch (Exception e) {
            LOG.warn("Conn #{}: Error: {}", connNum, e.getMessage(), e);
        }
    }
}
