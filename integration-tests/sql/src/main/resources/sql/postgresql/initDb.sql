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

DROP TABLE IF EXISTS camel
CREATE TABLE camel (id serial PRIMARY KEY, species VARCHAR ( 50 ) NOT NULL)

-- for consumer
DROP TABLE IF EXISTS projectsViaSql
CREATE TABLE projectsViaSql (id integer primary key, project varchar(25), license varchar(5), processed BOOLEAN);
DROP TABLE IF EXISTS projectsViaClasspath
CREATE TABLE projectsViaClasspath (id integer primary key, project varchar(25), license varchar(5), processed BOOLEAN);
DROP TABLE IF EXISTS projectsViaFile
CREATE TABLE projectsViaFile (id integer primary key, project varchar(25), license varchar(5), processed BOOLEAN);

-- idempotent repo
DROP TABLE IF EXISTS CAMEL_MESSAGEPROCESSED
CREATE TABLE CAMEL_MESSAGEPROCESSED ( processorName VARCHAR(255), messageId VARCHAR(100), createdAt TIMESTAMP )

-- aggregation repo
DROP TABLE IF EXISTS aggregation
CREATE TABLE aggregation (id varchar(255) NOT NULL, exchange BYTEA NOT NULL, version BIGINT NOT NULL, constraint aggregation_pk PRIMARY KEY (id));
DROP TABLE IF EXISTS aggregation_completed
CREATE TABLE aggregation_completed (id varchar(255) NOT NULL, exchange BYTEA NOT NULL, version BIGINT NOT NULL, constraint aggregation_completed_pk PRIMARY KEY (id));

CREATE OR REPLACE FUNCTION ADD_NUMS(a integer,b integer) RETURNS integer AS 'BEGIN RETURN a + b; END;' LANGUAGE plpgsql