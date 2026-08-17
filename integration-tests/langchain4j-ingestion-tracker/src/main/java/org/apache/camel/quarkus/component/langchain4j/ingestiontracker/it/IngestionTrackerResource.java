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
package org.apache.camel.quarkus.component.langchain4j.ingestiontracker.it;

import java.util.List;

import javax.sql.DataSource;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.apache.camel.quarkus.component.support.langchain4j.tracker.IngestionTracker;
import org.apache.camel.quarkus.component.support.langchain4j.tracker.IngestionTracker.TrackerRow;
import org.apache.camel.quarkus.component.support.langchain4j.tracker.jdbc.JdbcIngestionTracker;

/**
 * Exercises {@link JdbcIngestionTracker} against a real datasource. A REST resource rather than a
 * directly injected test field because {@code @QuarkusIntegrationTest} runs the application as a
 * separate process and cannot use {@code @Inject}.
 */
@ApplicationScoped
@Path("/ingestion-tracker")
public class IngestionTrackerResource {

    @Inject
    DataSource dataSource;

    private IngestionTracker tracker;

    void init(@Observes StartupEvent startup) {
        tracker = new JdbcIngestionTracker(dataSource);
        tracker.ensureSchema();
    }

    @GET
    @Path("/{pipeline}")
    public List<TrackerRow> listDocuments(@PathParam("pipeline") String pipeline) {
        return tracker.listDocuments(pipeline);
    }

    @GET
    @Path("/{pipeline}/{docId}")
    public TrackerRow read(@PathParam("pipeline") String pipeline, @PathParam("docId") String docId) {
        return tracker.read(pipeline, docId).orElseThrow(NotFoundException::new);
    }

    @POST
    @Path("/{pipeline}/{docId}/intent")
    public TrackerRow writeIntent(@PathParam("pipeline") String pipeline, @PathParam("docId") String docId,
            @QueryParam("fingerprint") String fingerprint, @QueryParam("contentHash") String contentHash,
            @QueryParam("intendedCount") int intendedCount, @QueryParam("origin") String origin) {
        tracker.writeIntent(pipeline, docId, fingerprint, contentHash, intendedCount, origin);
        return read(pipeline, docId);
    }

    @POST
    @Path("/{pipeline}/{docId}/commit")
    public TrackerRow commit(@PathParam("pipeline") String pipeline, @PathParam("docId") String docId,
            @QueryParam("fingerprint") String fingerprint, @QueryParam("contentHash") String contentHash,
            @QueryParam("segmentCount") int segmentCount) {
        tracker.commit(pipeline, docId, fingerprint, contentHash, segmentCount);
        return read(pipeline, docId);
    }

    @POST
    @Path("/{pipeline}/{docId}/refresh-fingerprint")
    public TrackerRow refreshFingerprint(@PathParam("pipeline") String pipeline, @PathParam("docId") String docId,
            @QueryParam("fingerprint") String fingerprint) {
        tracker.refreshFingerprint(pipeline, docId, fingerprint);
        return read(pipeline, docId);
    }

    @POST
    @Path("/{pipeline}/{docId}/fail")
    public TrackerRow markFailed(@PathParam("pipeline") String pipeline, @PathParam("docId") String docId,
            @QueryParam("fingerprint") String fingerprint) {
        tracker.markFailed(pipeline, docId, fingerprint);
        return read(pipeline, docId);
    }

    @POST
    @Path("/{pipeline}/{docId}/tombstone")
    public TrackerRow tombstone(@PathParam("pipeline") String pipeline, @PathParam("docId") String docId) {
        tracker.tombstone(pipeline, docId);
        return read(pipeline, docId);
    }

    @POST
    @Path("/{pipeline}/{docId}/unsuppress")
    public TrackerRow unsuppress(@PathParam("pipeline") String pipeline, @PathParam("docId") String docId) {
        tracker.unsuppress(pipeline, docId);
        return read(pipeline, docId);
    }

    @POST
    @Path("/{pipeline}/{docId}/pin")
    public TrackerRow pin(@PathParam("pipeline") String pipeline, @PathParam("docId") String docId) {
        tracker.pin(pipeline, docId);
        return read(pipeline, docId);
    }

    @POST
    @Path("/{pipeline}/{docId}/unpin")
    public TrackerRow unpin(@PathParam("pipeline") String pipeline, @PathParam("docId") String docId) {
        tracker.unpin(pipeline, docId);
        return read(pipeline, docId);
    }

    @DELETE
    @Path("/{pipeline}/{docId}")
    public void deleteRow(@PathParam("pipeline") String pipeline, @PathParam("docId") String docId) {
        tracker.deleteRow(pipeline, docId);
    }
}
