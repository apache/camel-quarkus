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
package org.apache.camel.quarkus.component.support.langchain4j.tracker.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.apache.camel.quarkus.component.support.langchain4j.tracker.IngestionTracker;

/**
 * JDBC-backed {@link IngestionTracker}. Deliberately dialect-free SQL (works on PostgreSQL and H2):
 * upserts are update-then-insert rather than vendor MERGE/ON CONFLICT.
 *
 * <p>
 * <strong>Concurrency assumption: at most one writer per {@code (pipeline, documentId)} at a
 * time.</strong> The update-then-insert is not atomic (each statement auto-commits on its own,
 * there is no transaction), so two concurrent writers for the same row can both see the
 * {@code UPDATE} affect zero rows and then both attempt the {@code INSERT}, and one fails on the
 * {@code (pipeline, doc_id)} primary key. Wrapping the two statements in a transaction would not
 * remove this race without either {@code SERIALIZABLE} isolation or a dialect-specific upsert,
 * both at odds with staying dialect-free; callers (the sync pass runner processes one document
 * at a time per pipeline) are relied upon to hold this invariant instead.
 *
 * <p>
 * <strong>Status: Experimental.</strong> See {@link IngestionTracker}.
 */
public class JdbcIngestionTracker implements IngestionTracker {

    static final String TABLE = "cq_ingestion_tracker";

    private final DataSource dataSource;

    public JdbcIngestionTracker(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void ensureSchema() {
        String ddl = "CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "pipeline VARCHAR(128) NOT NULL, "
                + "doc_id VARCHAR(512) NOT NULL, "
                + "fingerprint VARCHAR(256), "
                + "content_hash VARCHAR(64), "
                + "segment_count INT DEFAULT 0 NOT NULL, "
                + "intended_count INT DEFAULT 0 NOT NULL, "
                + "status VARCHAR(16) NOT NULL, "
                + "origin VARCHAR(16) DEFAULT 'source' NOT NULL, "
                + "tombstone BOOLEAN DEFAULT FALSE NOT NULL, "
                + "pinned BOOLEAN DEFAULT FALSE NOT NULL, "
                + "updated_at TIMESTAMP, "
                + "PRIMARY KEY (pipeline, doc_id))";
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute(ddl);
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Cannot create the ingestion tracker table '" + TABLE + "'. "
                            + "mode=sync needs a working datasource — configure quarkus.datasource "
                            + "(Dev Services provides one in dev mode) or use mode=append.",
                    e);
        }
    }

    private static final String ROW_COLUMNS = "fingerprint, content_hash, segment_count, intended_count, status, "
            + "origin, tombstone, pinned";

    @Override
    public Optional<TrackerRow> read(String pipeline, String documentId) {
        String sql = "SELECT " + ROW_COLUMNS + " FROM " + TABLE + " WHERE pipeline = ? AND doc_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pipeline);
            statement.setString(2, documentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(row(pipeline, documentId, resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Tracker read failed for '" + documentId + "'", e);
        }
    }

    @Override
    public List<TrackerRow> listDocuments(String pipeline) {
        String sql = "SELECT doc_id, " + ROW_COLUMNS + " FROM " + TABLE + " WHERE pipeline = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pipeline);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<TrackerRow> rows = new ArrayList<>();
                while (resultSet.next()) {
                    String documentId = resultSet.getString(1);
                    rows.add(new TrackerRow(pipeline, documentId,
                            resultSet.getString(2), resultSet.getString(3),
                            resultSet.getInt(4), resultSet.getInt(5), resultSet.getString(6),
                            resultSet.getString(7), resultSet.getBoolean(8), resultSet.getBoolean(9)));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Tracker listing failed for pipeline '" + pipeline + "'", e);
        }
    }

    private static TrackerRow row(String pipeline, String documentId, ResultSet resultSet) throws SQLException {
        return new TrackerRow(pipeline, documentId,
                resultSet.getString(1), resultSet.getString(2),
                resultSet.getInt(3), resultSet.getInt(4), resultSet.getString(5),
                resultSet.getString(6), resultSet.getBoolean(7), resultSet.getBoolean(8));
    }

    @Override
    public void writeIntent(String pipeline, String documentId, String fingerprint, String contentHash,
            int committedCount, int intendedCount, String origin) {
        try (Connection connection = dataSource.getConnection()) {
            String update = "UPDATE " + TABLE + " SET fingerprint = ?, content_hash = ?, "
                    + "intended_count = ?, status = 'in_progress', origin = ?, updated_at = ? "
                    + "WHERE pipeline = ? AND doc_id = ?";
            int updated;
            try (PreparedStatement statement = connection.prepareStatement(update)) {
                statement.setString(1, fingerprint);
                statement.setString(2, contentHash);
                statement.setInt(3, intendedCount);
                statement.setString(4, origin);
                statement.setTimestamp(5, Timestamp.from(Instant.now()));
                statement.setString(6, pipeline);
                statement.setString(7, documentId);
                updated = statement.executeUpdate();
            }
            if (updated == 0) {
                String insert = "INSERT INTO " + TABLE
                        + " (pipeline, doc_id, fingerprint, content_hash, segment_count, intended_count, "
                        + "status, origin, updated_at) VALUES (?, ?, ?, ?, ?, ?, 'in_progress', ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(insert)) {
                    statement.setString(1, pipeline);
                    statement.setString(2, documentId);
                    statement.setString(3, fingerprint);
                    statement.setString(4, contentHash);
                    statement.setInt(5, committedCount);
                    statement.setInt(6, intendedCount);
                    statement.setString(7, origin);
                    statement.setTimestamp(8, Timestamp.from(Instant.now()));
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Tracker intent write failed for '" + documentId + "'", e);
        }
    }

    @Override
    public void tombstone(String pipeline, String documentId) {
        setFlag(pipeline, documentId, "tombstone", true);
    }

    @Override
    public void unsuppress(String pipeline, String documentId) {
        setFlag(pipeline, documentId, "tombstone", false);
    }

    @Override
    public void pin(String pipeline, String documentId) {
        setFlag(pipeline, documentId, "pinned", true);
    }

    @Override
    public void unpin(String pipeline, String documentId) {
        setFlag(pipeline, documentId, "pinned", false);
    }

    private void setFlag(String pipeline, String documentId, String column, boolean value) {
        String sql = "UPDATE " + TABLE + " SET " + column + " = ?, updated_at = ? "
                + "WHERE pipeline = ? AND doc_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, value);
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
            statement.setString(3, pipeline);
            statement.setString(4, documentId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Tracker " + column + " update failed for '" + documentId + "'", e);
        }
    }

    @Override
    public void markFailed(String pipeline, String documentId, String fingerprint) {
        try (Connection connection = dataSource.getConnection()) {
            String update = "UPDATE " + TABLE + " SET fingerprint = ?, status = 'failed', updated_at = ? "
                    + "WHERE pipeline = ? AND doc_id = ?";
            int updated;
            try (PreparedStatement statement = connection.prepareStatement(update)) {
                statement.setString(1, fingerprint);
                statement.setTimestamp(2, Timestamp.from(Instant.now()));
                statement.setString(3, pipeline);
                statement.setString(4, documentId);
                updated = statement.executeUpdate();
            }
            if (updated == 0) {
                String insert = "INSERT INTO " + TABLE
                        + " (pipeline, doc_id, fingerprint, status, origin, updated_at) "
                        + "VALUES (?, ?, ?, 'failed', 'source', ?)";
                try (PreparedStatement statement = connection.prepareStatement(insert)) {
                    statement.setString(1, pipeline);
                    statement.setString(2, documentId);
                    statement.setString(3, fingerprint);
                    statement.setTimestamp(4, Timestamp.from(Instant.now()));
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Tracker dead-letter update failed for '" + documentId + "'", e);
        }
    }

    @Override
    public void deleteRow(String pipeline, String documentId) {
        String sql = "DELETE FROM " + TABLE + " WHERE pipeline = ? AND doc_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pipeline);
            statement.setString(2, documentId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Tracker row deletion failed for '" + documentId + "'", e);
        }
    }

    @Override
    public void commit(String pipeline, String documentId, String fingerprint, String contentHash,
            int segmentCount) {
        String sql = "UPDATE " + TABLE + " SET fingerprint = ?, content_hash = ?, segment_count = ?, "
                + "intended_count = ?, status = 'done', updated_at = ? WHERE pipeline = ? AND doc_id = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fingerprint);
            statement.setString(2, contentHash);
            statement.setInt(3, segmentCount);
            statement.setInt(4, segmentCount);
            statement.setTimestamp(5, Timestamp.from(Instant.now()));
            statement.setString(6, pipeline);
            statement.setString(7, documentId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Tracker commit failed for '" + documentId + "'", e);
        }
    }

    @Override
    public void refreshFingerprint(String pipeline, String documentId, String fingerprint) {
        String sql = "UPDATE " + TABLE + " SET fingerprint = ?, updated_at = ? "
                + "WHERE pipeline = ? AND doc_id = ? AND status = 'done'";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fingerprint);
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
            statement.setString(3, pipeline);
            statement.setString(4, documentId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Tracker fingerprint refresh failed for '" + documentId + "'", e);
        }
    }
}
