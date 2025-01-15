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

import java.nio.file.Paths;
import java.security.Security;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.ssh.SshComponent;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.keyprovider.KeyPairProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SshRoutes extends RouteBuilder {

    @ConfigProperty(name = "quarkus.ssh.host")
    String host;
    @ConfigProperty(name = "quarkus.ssh.port")
    String port;
    @ConfigProperty(name = "ssh.username")
    String username;
    @ConfigProperty(name = "ssh.password")
    String password;

    @PostConstruct
    public void init() {
        Security.addProvider(new EdDSASecurityProvider());
    }

    @Override
    public void configure() throws Exception {
        // Route without SSL
        from("direct:exampleProducer")
                .toF("ssh://%s:%s@%s:%s", username, password, host, port);

    }

    /**
     * We need to implement some conditional configuration of the {@link SshComponent} thus we create it
     * programmatically and publish via CDI.
     *
     * @return a configured {@link SshComponent}
     */
    @Named("ssh-with-key-provider")
    SshComponent sshWithKeyProvider() throws IllegalAccessException, NoSuchFieldException, InstantiationException {
        final SshComponent sshComponent = new SshComponent();
        sshComponent.setCamelContext(getContext());
        sshComponent.getConfiguration()
                .setKeyPairProvider(new FileKeyPairProvider(Paths.get("target/certs/user01.key")));
        sshComponent.getConfiguration().setKeyType(KeyPairProvider.SSH_RSA);
        return sshComponent;
    }

    @Named("ssh-cert")
    SshComponent sshCert() throws IllegalAccessException, NoSuchFieldException, InstantiationException {
        final SshComponent sshComponent = new SshComponent();
        sshComponent.setCamelContext(getContext());
        sshComponent.getConfiguration().setKeyType(null);
        return sshComponent;
    }

}
