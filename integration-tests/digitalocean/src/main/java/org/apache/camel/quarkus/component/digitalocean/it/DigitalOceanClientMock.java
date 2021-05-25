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
package org.apache.camel.quarkus.component.digitalocean.it;

import com.myjeeva.digitalocean.impl.DigitalOceanClient;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.protocol.HttpContext;

public class DigitalOceanClientMock extends DigitalOceanClient {

    public DigitalOceanClientMock(String authToken, String apiHost) {
        super(authToken);
        this.apiHost = apiHost;
        this.httpClient = HttpClients.custom()
                .setRoutePlanner(new DefaultRoutePlanner(DefaultSchemePortResolver.INSTANCE) {
                    @Override
                    public HttpRoute determineRoute(HttpHost host, HttpRequest request, HttpContext context)
                            throws HttpException {
                        // Override DigitalOceanClient forcing HTTPS
                        HttpHost httpSchemeHost = new HttpHost(host.getHostName(), host.getPort(), "http");
                        return super.determineRoute(httpSchemeHost, request, context);
                    }
                })
                .build();
    }

}
