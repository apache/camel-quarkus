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
package org.apache.camel.quarkus.kafka.oauth.it;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.keycloak.client.KeycloakTestClient;
import io.quarkus.test.keycloak.server.KeycloakContainer;
import io.strimzi.test.container.StrimziKafkaContainer;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import static java.util.Map.entry;

import static io.strimzi.test.container.StrimziKafkaContainer.KAFKA_PORT;

/**
 * Inspired from https://github.com/quarkusio/quarkus/tree/main/integration-tests/kafka-oauth-keycloak/
 */
public class KafkaKeycloakTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = Logger.getLogger(KafkaKeycloakTestResource.class);
    private static final String REALM_JSON = "keycloak/realms/kafka-authz-realm.json";
    private StrimziKafkaContainer kafka;
    private KeycloakContainer keycloak;

    @Override
    public Map<String, String> start() {
        Map<String, String> properties = new HashMap<>();

        System.setProperty("keycloak.docker.image",
                ConfigProvider.getConfig().getValue("keycloak.container.image", String.class));

        //Start keycloak container
        keycloak = new KeycloakContainer();
        keycloak.withStartupTimeout(Duration.ofMinutes(5));
        keycloak.start();
        LOG.info(keycloak.getLogs());

        Path realmJson = null;
        try {
            URL resource = Thread.currentThread().getContextClassLoader().getResource(REALM_JSON);
            if (resource == null) {
                throw new RuntimeException("Unable to load " + REALM_JSON);
            }

            realmJson = Files.createTempFile("keycloak-auth", ".json");
            IOUtils.copy(resource, realmJson.toFile());

            KeycloakTestClient client = new KeycloakTestClient(keycloak.getServerUrl());
            client.createRealmFromPath(realmJson.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (realmJson != null) {
                try {
                    Files.deleteIfExists(realmJson);
                } catch (IOException e) {
                    // Ignored
                }
            }
        }

        //Start kafka container
        String imageName = ConfigProvider.getConfig().getValue("kafka.container.image", String.class);
        this.kafka = new StrimziKafkaContainer(imageName)
                .withBrokerId(1)
                .withKafkaConfigurationMap(Map.ofEntries(
                        entry("listener.security.protocol.map", "JWT:SASL_PLAINTEXT,BROKER1:PLAINTEXT,CONTROLLER:PLAINTEXT"),
                        entry("listener.name.jwt.oauthbearer.sasl.jaas.config",
                                getOauthSaslJaasConfig(keycloak.getInternalUrl(), keycloak.getServerUrl())),
                        entry("listener.name.jwt.plain.sasl.jaas.config",
                                getPlainSaslJaasConfig(keycloak.getInternalUrl(), keycloak.getServerUrl())),
                        entry("sasl.enabled.mechanisms", "OAUTHBEARER"),
                        entry("sasl.mechanism.inter.broker.protocol", "OAUTHBEARER"),
                        entry("oauth.username.claim", "preferred_username"),
                        entry("principal.builder.class", "io.strimzi.kafka.oauth.server.OAuthKafkaPrincipalBuilder"),
                        entry("listener.name.jwt.sasl.enabled.mechanisms", "OAUTHBEARER,PLAIN"),
                        entry("listener.name.jwt.oauthbearer.sasl.server.callback.handler.class",
                                "io.strimzi.kafka.oauth.server.JaasServerOauthValidatorCallbackHandler"),
                        entry("listener.name.jwt.oauthbearer.sasl.login.callback.handler.class",
                                "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler"),
                        entry("listener.name.jwt.plain.sasl.server.callback.handler.class",
                                "io.strimzi.kafka.oauth.server.plain.JaasServerOauthOverPlainValidatorCallbackHandler")))
                .withNetworkAliases("kafka")
                .withBootstrapServers(
                        c -> String.format("JWT://%s:%s", c.getHost(), c.getMappedPort(KAFKA_PORT)));
        this.kafka.start();
        LOG.info(this.kafka.getLogs());

        properties.put("kafka.bootstrap.servers", this.kafka.getBootstrapServers());
        properties.put("camel.component.kafka.brokers", kafka.getBootstrapServers());
        properties.put("camel.component.kafka.security-protocol", "SASL_PLAINTEXT");
        properties.put("camel.component.kafka.sasl-mechanism", "OAUTHBEARER");
        properties.put("camel.component.kafka.additional-properties[sasl.login.callback.handler.class]",
                "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler");
        properties.put("camel.component.kafka.sasl-jaas-config", getClientSaslJaasConfig(keycloak.getServerUrl()));
        return properties;
    }

    @Override
    public void stop() {
        if (kafka != null) {
            kafka.stop();
        }
        if (keycloak != null) {
            keycloak.stop();
        }
    }

    private String getClientSaslJaasConfig(String keycloakServerUrl) {
        return "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required" +
                " oauth.client.id=\"kafka-client\"" +
                " oauth.client.secret=\"kafka-client-secret\"" +
                " oauth.token.endpoint.uri=\"" + keycloakServerUrl + "/realms/kafka-authz/protocol/openid-connect/token\";";
    }

    private String getPlainSaslJaasConfig(String keycloakInternalUrl, String keycloakServerUrl) {
        return "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "oauth.jwks.endpoint.uri=\"" + keycloakInternalUrl + "/realms/kafka-authz/protocol/openid-connect/certs\" " +
                "oauth.valid.issuer.uri=\"" + keycloakServerUrl + "/realms/kafka-authz\" " +
                "oauth.token.endpoint.uri=\"" + keycloakInternalUrl + "/realms/kafka-authz/protocol/openid-connect/token\" " +
                "oauth.client.id=\"kafka\" " +
                "oauth.client.secret=\"kafka-secret\" " +
                "unsecuredLoginStringClaim_sub=\"admin\";";
    }

    private String getOauthSaslJaasConfig(String keycloakInternalUrl, String keycloakServerUrl) {
        return "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required " +
                "oauth.jwks.endpoint.uri=\"" + keycloakInternalUrl + "/realms/kafka-authz/protocol/openid-connect/certs\" " +
                "oauth.valid.issuer.uri=\"" + keycloakServerUrl + "/realms/kafka-authz\" " +
                "oauth.token.endpoint.uri=\"" + keycloakInternalUrl + "/realms/kafka-authz/protocol/openid-connect/token\" " +
                "oauth.client.id=\"kafka\" " +
                "oauth.client.secret=\"kafka-secret\";";
    }
}
