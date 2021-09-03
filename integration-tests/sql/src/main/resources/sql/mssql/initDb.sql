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
CREATE TABLE camel (id int NOT NULL IDENTITY PRIMARY KEY,species varchar(50));

-- for consumer
DROP TABLE projectsViaClasspath
CREATE TABLE projectsViaClasspath (id int NOT NULL, project varchar(25), license varchar(5), processed BIT);
DROP TABLE projectsViaFile
CREATE TABLE projectsViaFile (id int NOT NULL, project varchar(25), license varchar(5), processed BIT);
DROP TABLE projectsViaSql
CREATE TABLE projectsViaSql (id int NOT NULL, project varchar(25), license varchar(5), processed BIT);

-- idempotent repo
DROP TABLE CAMEL_MESSAGEPROCESSED
CREATE TABLE CAMEL_MESSAGEPROCESSED (processorName varchar(255), messageId varchar(100), createdAt datetime)

-- aggregation repo
DROP TABLE aggregation
CREATE TABLE aggregation (id varchar(255), exchange Image, version bigint);

DROP TABLE aggregation_completed
CREATE TABLE aggregation_completed (id varchar(255), exchange Image, version bigint);