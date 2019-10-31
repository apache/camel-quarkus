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
package org.apache.camel.quarkus.component.netty.runtime;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.camel.component.netty.NettyServerBossPoolBuilder;
import org.apache.camel.util.concurrent.CamelThreadFactory;

@TargetClass(NettyServerBossPoolBuilder.class)
final class SubstituteNettyServerBossPoolBuilder {

    @Alias
    private String name = "NettyServerBoss";
    @Alias
    private String pattern;
    @Alias
    private int bossCount = 1;
    @Alias
    private boolean nativeTransport;

    /**
     * Creates a new boss pool.
     */
    @Substitute
    public EventLoopGroup build() {
        return new NioEventLoopGroup(bossCount, new CamelThreadFactory(pattern, name, false));
    }
}
