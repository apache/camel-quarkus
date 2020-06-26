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

-- Create the test database
CREATE DATABASE testDB;

USE testDB;
EXEC sys.sp_cdc_enable_db;

CREATE SCHEMA Test;

CREATE TABLE Test.COMPANY(
    NAME  varchar(255),
    CITY  varchar(255),
    PRIMARY KEY (NAME)
);
-- sql agent is started by providing of the system property, but it could happen that it is still starting during this
-- execution. In that case, this script fails during setting of cdc for table. In case of failure because of:
-- 'The error returned was 14258: Cannot perform this operation while SQLServerAgent is starting.'
-- please increase following delay accordingly
WAITFOR DELAY '00:00:10'

EXEC sys.sp_cdc_enable_table @source_schema=N'Test', @source_name=N'COMPANY', @role_name = NULL,@filegroup_name=N'PRIMARY', @supports_net_changes=0;
INSERT INTO Test.COMPANY (name, city) VALUES ('init', 'init');




