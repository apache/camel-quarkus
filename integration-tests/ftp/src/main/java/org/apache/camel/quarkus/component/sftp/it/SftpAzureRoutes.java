package org.apache.camel.quarkus.component.sftp.it;

import java.util.Iterator;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class SftpAzureRoutes extends RouteBuilder {

    @ConfigProperty(name = "azure.storage.account-name")
    String azureAccountName;
    @ConfigProperty(name = "azure.storage.account-key")
    String azureAccessKey;
    @ConfigProperty(name = "azure.storage.container-name")
    String azureContainer;

    @Produces
    @Named("azureBlobServiceClient")
    BlobServiceClient azureBlobServiceClient() {
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(azureAccountName, azureAccessKey);
        String uri = String.format("https://%s.blob.core.windows.net", azureAccountName);
        return new BlobServiceClientBuilder()
                .endpoint(uri)
                .credential(credential)
                .buildClient();
    }

    @Override
    public void configure() throws Exception {

        from("sftp:admin@localhost:{{camel.sftp.test-port}}/sftp?password=admin&localWorkDirectory=target")
                .routeId("sftpToAzure")
                .errorHandler(deadLetterChannel("seda:error"))
                .convertBodyTo(byte[].class)
                .log(LoggingLevel.INFO, "File ${file:name} (${file:size} bytes) is fetched from SFTP.")
                .process(exchange -> {
                    Message message = exchange.getIn();

                    Map<String, Object> headers = message.getHeaders();
                    Iterator<Map.Entry<String, Object>> iterator = headers.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<String, Object> pair = iterator.next();
                        if (pair.getKey().equals("CamelFileNameOnly")) {
                            message.setHeader(BlobConstants.BLOB_NAME, pair.getValue().toString());
                            break;
                        }
                    }
                })
                .to(String.format(
                        "azure-storage-blob://%s/%s?blobName=blob&operation=uploadBlockBlob&serviceClient=#azureBlobServiceClient",
                        azureAccountName,
                        azureContainer))
                .log(LoggingLevel.INFO, "Uploaded to Azure.");

        from("seda:error")
                .routeId("mailError")
                .log(LoggingLevel.ERROR, exceptionMessage().toString());
    }
}
