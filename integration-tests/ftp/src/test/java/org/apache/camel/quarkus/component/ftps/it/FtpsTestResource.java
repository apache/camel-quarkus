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
package org.apache.camel.quarkus.component.ftps.it;

import java.nio.file.Path;

import org.apache.camel.quarkus.component.ftp.it.FtpTestResource;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;

public class FtpsTestResource extends FtpTestResource {

    public FtpsTestResource() {
        super("ftps");
    }

    @Override
    protected ListenerFactory createListenerFactory(int port) {
        //do not create a factory if keystore file does not exists
        //because the test is disabled, but the test resource is "activated", this condition prevents the failure
        //for the FtpTest
        if (!Path.of(FtpsTest.CERTIFICATE_KEYSTORE_FILE).toFile().exists()) {
            return null;
        }

        SslConfigurationFactory sslConfigFactory = new SslConfigurationFactory();
        sslConfigFactory.setKeystoreFile(Path.of(FtpsTest.CERTIFICATE_KEYSTORE_FILE).toFile());
        sslConfigFactory.setKeystoreType("PKCS12");
        sslConfigFactory.setKeystorePassword("password");
        sslConfigFactory.setSslProtocol("TLSv1.3");

        ListenerFactory factory = super.createListenerFactory(port);
        factory.setSslConfiguration(sslConfigFactory.createSslConfiguration());
        return factory;
    }
}
