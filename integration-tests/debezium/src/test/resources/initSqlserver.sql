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
-- by using system property, sql agent is started, but it could happen that it is not running yet, which will fail
-- during setting of cdc for table. We are waiting for max 20 seconds if service is running
DECLARE @Counter INT
SET @Counter=1
WHILE (SELECT dss.[status] FROM   sys.dm_server_services dss WHERE  dss.[servicename] LIKE N'SQL Server Agent (%') != 4 AND @Counter <= 20
BEGIN
	WAITFOR DELAY '00:00:01'
	SET @Counter  = @Counter  + 1
END

INSERT INTO Test.COMPANY (name, city) VALUES ('init', 'init');
EXEC sys.sp_cdc_enable_table @source_schema=N'Test', @source_name=N'COMPANY', @role_name = NULL,@filegroup_name=N'PRIMARY', @supports_net_changes=0;



