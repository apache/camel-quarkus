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
package org.apache.camel.quarkus.component.spring.redis.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.component.redis.RedisConfiguration;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@ApplicationScoped
public class SpringRedisProducers {

    @ConfigProperty(name = "redis.host")
    String host;

    @ConfigProperty(name = "redis.port")
    int port;

    @SuppressWarnings("unchecked")
    @Named("redisTemplate")
    RedisTemplate<String, String> produceRedisTemplate() {
        RedisConfiguration redisConfiguration = new RedisConfiguration();

        RedisStandaloneConfiguration conf = new RedisStandaloneConfiguration();
        conf.setPassword(RedisPassword.of("p4ssw0rd"));
        conf.setHostName(host);
        conf.setPort(port);
        JedisConnectionFactory connectionFactory = new JedisConnectionFactory(conf);
        redisConfiguration.setConnectionFactory(connectionFactory);

        RedisTemplate<String, String> redisTemplate = (RedisTemplate<String, String>) redisConfiguration.getRedisTemplate();
        connectionFactory.start();

        return redisTemplate;
    }
}
