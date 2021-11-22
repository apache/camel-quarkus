--
-- Licensed to the Apache Software Foundation (ASF) under one or more
-- contributor license agreements.  See the NOTICE file distributed with
-- this work for additional information regarding copyright ownership.
-- The ASF licenses this file to You under the Apache License, Version 2.0
-- (the "License"); you may not use this file except in compliance with
-- the License.  You may obtain a copy of the License at
--
--      http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

DROP TABLE camel
CREATE TABLE camel (id INT NOT NULL GENERATED ALWAYS AS IDENTITY,species VARCHAR(50) NOT NULL)

-- for consumer
DROP TABLE projectsViaClasspath
CREATE TABLE projectsViaClasspath (id INT NOT NULL, project VARCHAR(25), license VARCHAR(5), processed BOOLEAN, PRIMARY KEY (id))
DROP TABLE projectsViaSql
CREATE TABLE projectsViaSql (id INT NOT NULL, project VARCHAR(25), license VARCHAR(5), processed BOOLEAN, PRIMARY KEY (id))
DROP TABLE projectsViaFile
CREATE TABLE projectsViaFile (id INT NOT NULL, project VARCHAR(25), license VARCHAR(5), processed BOOLEAN, PRIMARY KEY (id))

-- idempotent repo
DROP TABLE CAMEL_MESSAGEPROCESSED
CREATE TABLE CAMEL_MESSAGEPROCESSED ( processorName VARCHAR(255), messageId VARCHAR(100), createdAt TIMESTAMP )

-- aggregation repo
DROP TABLE aggregation
CREATE TABLE aggregation (id VARCHAR(255) NOT NULL, exchange BLOB NOT NULL, version BIGINT NOT NULL, constraint aggregation_pk PRIMARY KEY (id))
DROP TABLE aggregation_completed
CREATE TABLE aggregation_completed (id VARCHAR(255) NOT NULL, exchange BLOB NOT NULL, version BIGINT NOT NULL, constraint aggregation_completed_pk PRIMARY KEY (id))

CREATE PROCEDURE ADD_NUMS(IN a INTEGER, IN b INTEGER, IN fileName VARCHAR(50)) PARAMETER STYLE JAVA READS SQL DATA LANGUAGE JAVA EXTERNAL NAME 'org.apache.camel.quarkus.component.sql.it.storedproc.DerbyNumberAddStoredProcedure.testProc'
