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
package org.apache.camel.quarkus.component.azure.storage.blob.it;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.changefeed.models.BlobChangefeedEvent;
import com.azure.storage.blob.changefeed.models.BlobChangefeedEventType;
import com.azure.storage.blob.models.BlobContainerItem;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.Block;
import com.azure.storage.blob.models.BlockList;
import com.azure.storage.blob.models.BlockListType;
import com.azure.storage.blob.models.PageRange;
import com.azure.storage.blob.models.PageRangeItem;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.azure.storage.blob.BlobBlock;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.apache.camel.quarkus.core.util.FileUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/azure-storage-blob")
@ApplicationScoped
public class AzureStorageBlobResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ConsumerTemplate consumerTemplate;

    @Inject
    CamelContext context;

    @ConfigProperty(name = "azure.storage.account-name")
    public String azureStorageAccountName;

    @ConfigProperty(name = "azure.blob.container.name")
    public String azureBlobContainerName;

    @Path("/blob/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createBlob(String content) throws Exception {
        Exchange exchange = producerTemplate.request("direct:create", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.getMessage().setBody(content);
            }
        });

        if (!exchange.isFailed()) {
            Message message = exchange.getMessage();
            return Response.created(new URI("https://camel.apache.org/"))
                    .entity(message.getHeader(BlobConstants.E_TAG))
                    .build();
        }

        return Response.serverError().build();
    }

    @Path("/blob/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String readBlob(
            @QueryParam("containerName") String containerName,
            @QueryParam("uri") String uri) {
        if (containerName == null) {
            containerName = azureBlobContainerName;
        }

        if (uri == null) {
            uri = "direct:read";
        }

        Map<String, Object> headers = new HashMap<>();
        headers.put(Exchange.CHARSET_NAME, StandardCharsets.UTF_8.name());
        headers.put(BlobConstants.BLOB_CONTAINER_NAME, containerName);
        return producerTemplate.requestBodyAndHeaders(uri, null, headers, String.class);
    }

    @Path("/blob/read/bytes")
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public byte[] readBlobBytes() {
        return producerTemplate.requestBodyAndHeader(
                "direct:read",
                null, Exchange.CHARSET_NAME, StandardCharsets.UTF_8.name(), byte[].class);
    }

    @Path("/blob/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public JsonObject listBlobs() {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        List<BlobItem> blobs = producerTemplate.requestBody("direct:list", null, List.class);
        blobs.stream()
                .map(blobItem -> objectBuilder.add("name", blobItem.getName()))
                .forEach(arrayBuilder::add);

        objectBuilder.add("blobs", arrayBuilder);

        return objectBuilder.build();
    }

    @Path("/blob/update")
    @PATCH
    @Consumes(MediaType.TEXT_PLAIN)
    public Response updateBlob(String message) {
        producerTemplate.sendBody("direct:update", message);
        return Response.ok().build();
    }

    @Path("/blob/delete")
    @DELETE
    public Response deleteBlob() {
        try {
            producerTemplate.sendBody("direct:delete", null);
        } catch (CamelExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BlobStorageException) {
                BlobStorageException bse = (BlobStorageException) cause;
                return Response.status(bse.getStatusCode()).build();
            }
        }
        return Response.noContent().build();
    }

    @Path("/blob/download")
    @GET
    public Response downloadBlob() {
        File file = producerTemplate.requestBody("direct:download", null, File.class);
        String downloadPath = FileUtils.nixifyPath(file.getAbsolutePath());
        return Response.ok(downloadPath).build();
    }

    @Path("/blob/download/link")
    @GET
    public Response downloadLink() {
        String link = producerTemplate.requestBody("direct:downloadLink", null, String.class);
        return Response.ok(link).build();
    }

    @Path("/block/blob/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createBlockBlob(String content) throws Exception {
        producerTemplate.sendBody("direct:uploadBlockBlob", content);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    /**
     * Note: The 'blob block' naming is retained here instead of the alternative 'block blob' naming used
     * for other operations. Both the Camel and official Azure documentation have this inconsistency.
     */
    @Path("/blob/block/list")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject readBlobBlockList(@QueryParam("blockListType") String blockListType) {
        BlockListType listType = BlockListType.valueOf(blockListType.toUpperCase());
        BlockList list = producerTemplate.requestBodyAndHeader(
                "direct:readBlobBlocks",
                null, BlobConstants.BLOCK_LIST_TYPE, listType, BlockList.class);

        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (listType.equals(BlockListType.ALL) || listType.equals(BlockListType.UNCOMMITTED)) {
            extractBlockNames(builder, list.getUncommittedBlocks(), BlockListType.UNCOMMITTED);
        }

        if (listType.equals(BlockListType.ALL) || listType.equals(BlockListType.COMMITTED)) {
            extractBlockNames(builder, list.getCommittedBlocks(), BlockListType.COMMITTED);
        }

        return builder.build();
    }

    @Path("/block/blob/stage")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Boolean stageBlockBlobs(List<String> blockContent) {
        List<BlobBlock> blocks = blockContent.stream()
                .map(String::getBytes)
                .map(ByteArrayInputStream::new)
                .map(inputStream -> {
                    try {
                        return BlobBlock.createBlobBlock(inputStream);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
        return producerTemplate.requestBody("direct:stageBlockBlob", blocks, Boolean.class);
    }

    @Path("/block/blob/commit")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Boolean commitBlockBlobs(List<String> blockNames) {
        List<Block> blocks = blockNames.stream()
                .map(name -> {
                    Block block = new Block();
                    block.setName(name);
                    return block;
                })
                .collect(Collectors.toList());

        return producerTemplate.requestBody("direct:commitBlockBlob", blocks, Boolean.class);
    }

    @Path("/append/blob/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    public Response createAppendBlob(String content) throws URISyntaxException {
        producerTemplate.sendBody("direct:createAppendBlob", content);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/append/blob/commit")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Boolean commitAppendBlob(String contentToAppend) {
        byte[] bytes = contentToAppend.getBytes(StandardCharsets.UTF_8);
        return producerTemplate.requestBody("direct:commitAppendBlob", new ByteArrayInputStream(bytes), Boolean.class);
    }

    @Path("/page/blob/create")
    @POST
    public Response createPageBlob() throws URISyntaxException {
        producerTemplate.sendBody("direct:createPageBlob", null);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/page/blob/upload")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Boolean uploadPageBlob(@QueryParam("pageStart") int start, @QueryParam("pageEnd") int end) {
        byte[] dataBytes = new byte[end + 1];
        new Random().nextBytes(dataBytes);
        InputStream dataStream = new ByteArrayInputStream(dataBytes);
        PageRange pageRange = new PageRange().setStart(start).setEnd(end);
        return producerTemplate.requestBodyAndHeader("direct:uploadPageBlob", dataStream,
                BlobConstants.PAGE_BLOB_RANGE, pageRange, Boolean.class);
    }

    @Path("/page/blob/resize")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Boolean resizePageBlob(@QueryParam("pageStart") int start, @QueryParam("pageEnd") int end) {
        PageRange pageRange = new PageRange().setStart(start).setEnd(end);
        return producerTemplate.requestBodyAndHeader("direct:resizePageBlob", null,
                BlobConstants.PAGE_BLOB_RANGE, pageRange, Boolean.class);
    }

    @Path("/page/blob/clear")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Boolean clearPageBlob(@QueryParam("pageStart") int start, @QueryParam("pageEnd") int end) {
        PageRange pageRange = new PageRange().setStart(start).setEnd(end);
        return producerTemplate.requestBodyAndHeader("direct:clearPageBlob", null,
                BlobConstants.PAGE_BLOB_RANGE, pageRange, Boolean.class);
    }

    @Path("/page/blob")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getPageBlobRanges(@QueryParam("pageStart") int start, @QueryParam("pageEnd") int end) {
        PageRange pageRange = new PageRange().setStart(start).setEnd(end);
        PagedIterable pageIterable = producerTemplate.requestBodyAndHeader("direct:getPageBlobRanges", null,
                BlobConstants.PAGE_BLOB_RANGE, pageRange, PagedIterable.class);

        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        ((Stream<PageRangeItem>) pageIterable
                .stream())
                        .map(pr -> Json.createObjectBuilder()
                                .add("offset", pr.getRange().getOffset())
                                .add("length", pr.getRange().getLength())
                                .build())
                        .forEach(arrayBuilder::add);

        objectBuilder.add("ranges", arrayBuilder.build());
        return objectBuilder.build();
    }

    @Path("/blob/container")
    @POST
    public Response createBlobContainer(@QueryParam("containerName") String containerName) throws Exception {
        producerTemplate.sendBodyAndHeader("direct:createBlobContainer", null, BlobConstants.BLOB_CONTAINER_NAME,
                containerName);
        return Response.created(new URI("https://camel.apache.org/")).build();
    }

    @Path("/blob/container")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @SuppressWarnings("unchecked")
    public JsonObject listBlobContainers() throws Exception {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        List<BlobContainerItem> containers = producerTemplate.requestBody("direct:listBlobContainers", null, List.class);
        containers.stream()
                .map(BlobContainerItem::getName)
                .filter(containerName -> containerName.startsWith("camel-quarkus"))
                .map(containerName -> Json.createObjectBuilder()
                        .add("name", containerName)
                        .build())
                .forEach(arrayBuilder::add);

        objectBuilder.add("containers", arrayBuilder.build());
        return objectBuilder.build();
    }

    @Path("/blob/container")
    @DELETE
    public void deleteBlobContainer(@QueryParam("containerName") String containerName) {
        producerTemplate.sendBodyAndHeader("direct:deleteBlobContainer", null, BlobConstants.BLOB_CONTAINER_NAME,
                containerName);
    }

    @Path("/blob/copy")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response copyBlob(@QueryParam("containerName") String containerName) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(BlobConstants.BLOB_CONTAINER_NAME, containerName);
        headers.put(BlobConstants.BLOB_NAME, AzureStorageBlobRoutes.BLOB_NAME);
        headers.put(BlobConstants.SOURCE_BLOB_CONTAINER_NAME, azureBlobContainerName);
        headers.put(BlobConstants.SOURCE_BLOB_ACCOUNT_NAME, azureStorageAccountName);
        String result = producerTemplate.requestBodyAndHeaders("direct:copy", null, headers, String.class);
        return Response.ok(result).build();
    }

    @Path("/changes")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @SuppressWarnings("unchecked")
    public boolean getChangeFeed(
            @QueryParam("startTime") String startTime,
            @QueryParam("endTime") String endTime,
            @QueryParam("etag") String eTag) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(BlobConstants.BLOB_NAME, AzureStorageBlobRoutes.BLOB_NAME);
        headers.put(BlobConstants.CHANGE_FEED_START_TIME, OffsetDateTime.parse(startTime));
        headers.put(BlobConstants.CHANGE_FEED_END_TIME, OffsetDateTime.parse(endTime));

        List<BlobChangefeedEvent> events = producerTemplate.requestBodyAndHeaders("direct:getChangeFeed", null, headers,
                List.class);

        if (events == null) {
            return false;
        }

        return events.stream()
                .filter(event -> event.getEventType() != null
                        && event.getEventType().equals(BlobChangefeedEventType.BLOB_CREATED))
                .anyMatch(event -> event.getData() != null && event.getData().getETag() != null
                        && event.getData().getETag().equals(eTag));
    }

    @Path("/consumed/blobs")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getConsumedBlobs() {
        return consumerTemplate.receiveBody("seda:blobs", 10000, String.class);
    }

    @POST
    @Path("consumer/{enable}")
    public void mangeBlobConsumer(@PathParam("enable") boolean enable) throws Exception {
        if (enable) {
            context.getRouteController().startRoute("blob-consumer");
        } else {
            context.getRouteController().stopRoute("blob-consumer");
        }
    }

    private void extractBlockNames(JsonObjectBuilder builder, List<Block> blocks, BlockListType listType) {
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        blocks.stream().map(Block::getName).forEach(arrayBuilder::add);
        builder.add(listType.toString(), arrayBuilder);
    }
}
