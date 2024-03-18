package org.apache.camel.quarkus.component.qdrant.it;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

public class QdrantTestResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOG = LoggerFactory.getLogger(QdrantTestResource.class);
    private static final String QDRANT_IMAGE = ConfigProvider.getConfig().getValue("qdrant.container.image", String.class);
    private static final int QDRANT_GRPC_PORT = 6334;

    private GenericContainer<?> qdrantContainer;

    @Override
    public Map<String, String> start() {
        LOG.info(TestcontainersConfiguration.getInstance().toString());

        Map<String, String> properties = new HashMap<>();

        GenericContainer<?> container = new GenericContainer<>(QDRANT_IMAGE)
                .withExposedPorts(QDRANT_GRPC_PORT)
                //.withNetworkAliases("basicAuthContainer")
                //.withCommand("-DV", "--user", BASIC_AUTH_USERNAME, "--pass", BASIC_AUTH_PASSWORD)
                .withLogConsumer(new Slf4jLogConsumer(LOG)/*.withPrefix("basicAuthContainer")*/)
                .waitingFor(Wait.forLogMessage(".*Actix runtime found; starting in Actix runtime.*", 1));

        container.start();

        String basicAuthIp = container.getHost();
        Integer basicAuthPort = container.getMappedPort(QDRANT_GRPC_PORT);

        properties.put("camel.component.qdrant.host", basicAuthIp);
        properties.put("camel.component.qdrant.port", basicAuthPort.toString());

        LOG.info("Properties: {}", properties);

        return properties;
    }

    @Override
    public void stop() {
        try {
            if (qdrantContainer != null) {
                qdrantContainer.stop();
            }
        } catch (Exception ex) {
            LOG.error("An issue occurred while stopping Qdrant container", ex);
        }
    }

}
