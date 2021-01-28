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
package org.apache.camel.quarkus.component.oaipmh.it;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OaipmhTestServer {

    private static final Logger LOG = LoggerFactory.getLogger(OaipmhTestServer.class);
    private static final String PASSWORD = "changeit";
    private static Map<String, String> RESPONSE_CACHE;

    private String context;
    private boolean useHttps;
    private int port;
    private Server server;

    public OaipmhTestServer(String context, int port) {
        this(context, port, false);
    }

    public OaipmhTestServer(String context, int port, boolean useHttps) {
        this.context = context;
        this.useHttps = useHttps;
        this.port = port;
    }

    private static synchronized Map<String, String> getResponseCache() throws IOException {
        if (RESPONSE_CACHE == null) {
            HashMap<String, String> responseCache = new HashMap<String, String>();

            ZipInputStream zis = new ZipInputStream(OaipmhTestServer.class.getResourceAsStream("/data.zip"));

            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    responseCache.put(entry.getName(), IOUtils.toString(zis, StandardCharsets.UTF_8));
                }
                entry = zis.getNextEntry();
            }
            RESPONSE_CACHE = Collections.unmodifiableMap(responseCache);
        }
        return RESPONSE_CACHE;
    }

    public void startServer() {
        server = new Server(port);

        if (useHttps) {
            HttpConfiguration https = new HttpConfiguration();
            https.addCustomizer(new SecureRequestCustomizer());
            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();

            String keyStorePath = OaipmhTestServer.class.getResource("/jettyKS/localhost.p12").toExternalForm();
            sslContextFactory.setKeyStorePath(keyStorePath);
            sslContextFactory.setKeyStorePassword(PASSWORD);
            sslContextFactory.setKeyManagerPassword(PASSWORD);
            ServerConnector sslConnector = new ServerConnector(
                    server,
                    new SslConnectionFactory(sslContextFactory, "http/1.1"),
                    new HttpConnectionFactory(https));

            sslConnector.setPort(port);
            server.setConnectors(new Connector[] { sslConnector });
        }

        ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContext.setContextPath("/");
        server.setHandler(servletContext);
        servletContext.addServlet(new ServletHolder(new OaipmhTestServlet(this.context)), "/*");

        try {
            server.start();
        } catch (Exception ex) {
            LOG.error("An issue prevented an OaipmhTestServer from starting, so giving up", ex);
            throw new RuntimeException("An issue prevented an OaipmhTestServer from starting", ex);
        }
    }

    public void stopServer() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ex) {
                LOG.warn("An issue prevented an OaipmhTestServer from stopping, so ignoring", ex);
            } finally {
                server = null;
            }
        }
    }

    private class OaipmhTestServlet extends HttpServlet {

        private static final long serialVersionUID = 5594945031962091041L;

        private String context;

        public OaipmhTestServlet(String context) {
            this.context = context;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String qs = req.getRequestURI() + "?" + req.getQueryString();
            String sha256Hex = DigestUtils.sha256Hex(qs);
            resp.getWriter().write(getResponseCache().get("data/" + this.context + "/" + sha256Hex + ".xml"));
        }
    }

}
