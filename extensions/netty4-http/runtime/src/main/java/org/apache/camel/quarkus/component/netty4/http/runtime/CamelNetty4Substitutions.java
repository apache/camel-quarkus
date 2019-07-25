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
package org.apache.camel.quarkus.component.netty4.http.runtime;

import org.apache.camel.component.netty4.NettyHelper;
import org.apache.camel.util.concurrent.CamelThreadFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

@TargetClass(className = "org.apache.camel.component.netty4.NettyServerBootstrapConfiguration")
final class Target_org_apache_camel_component_netty4_NettyServerBootstrapConfiguration {

    @Substitute
    public boolean isNativeTransport() {
        return false;
    }

}

@TargetClass(className = "org.apache.camel.component.netty4.NettyWorkerPoolBuilder")
final class Target_org_apache_camel_component_netty4_NettyWorkerPoolBuilder {

    @Alias
    private String name = "NettyWorker";
    @Alias
    private String pattern;
    @Alias
    private int workerCount;
    @Alias
    private volatile EventLoopGroup workerPool;

    @Substitute
    public EventLoopGroup build() {
        int count = workerCount > 0 ? workerCount : NettyHelper.DEFAULT_IO_THREADS;
        workerPool = new NioEventLoopGroup(count, new CamelThreadFactory(pattern, name, false));
        return workerPool;
    }
}

@TargetClass(className = "org.apache.camel.component.netty4.NettyServerBossPoolBuilder")
final class Target_org_apache_camel_component_netty4_NettyServerBossPoolBuilder {

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

class CamelNetty4Substitutions {

}
