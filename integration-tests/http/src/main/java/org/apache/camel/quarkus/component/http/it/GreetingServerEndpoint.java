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
package org.apache.camel.quarkus.component.http.it;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.jboss.logging.Logger;

@ServerEndpoint("/ahc-ws/greeting")
public class GreetingServerEndpoint {

    public static volatile boolean connected = false;
    private static final Logger LOG = Logger.getLogger(GreetingServerEndpoint.class);

    @OnOpen
    public void onOpen(Session session) {
        LOG.infof("WebSocket connection opened for session %s", session.getId());
        connected = true;
    }

    @OnClose
    public void onClose(Session session) {
        LOG.infof("WebSocket connection closed for session %s", session.getId());
        connected = false;
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        session.getAsyncRemote().sendText("Hello " + message);
    }
}
