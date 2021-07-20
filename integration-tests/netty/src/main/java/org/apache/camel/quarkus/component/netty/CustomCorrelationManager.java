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
package org.apache.camel.quarkus.component.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.camel.component.netty.NettyCamelState;
import org.apache.camel.component.netty.NettyCamelStateCorrelationManager;

public class CustomCorrelationManager implements NettyCamelStateCorrelationManager {

    private volatile NettyCamelState stateA;
    private volatile NettyCamelState stateB;
    private volatile NettyCamelState stateC;

    @Override
    public void putState(Channel channel, NettyCamelState state) {
        String body = state.getExchange().getMessage().getBody(String.class);
        if ("A".equals(body)) {
            stateA = state;
        } else if ("B".equals(body)) {
            stateB = state;
        } else if ("C".equals(body)) {
            stateC = state;
        }
    }

    @Override
    public void removeState(ChannelHandlerContext ctx, Channel channel) {
        // noop
    }

    @Override
    public NettyCamelState getState(ChannelHandlerContext ctx, Channel channel, Object msg) {
        String body = msg.toString();
        if (body.endsWith("A")) {
            stateA.getExchange().getMessage().setHeader("manager", this);
            return stateA;
        } else if (body.endsWith("B")) {
            stateB.getExchange().getMessage().setHeader("manager", this);
            return stateB;
        } else if (body.endsWith("C")) {
            stateC.getExchange().getMessage().setHeader("manager", this);
            return stateC;
        }
        return null;
    }

    @Override
    public NettyCamelState getState(ChannelHandlerContext ctx, Channel channel, Throwable cause) {
        return null;
    }
}
