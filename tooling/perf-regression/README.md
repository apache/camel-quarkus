<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
# Camel Quarkus Performance regression detection tool

This Quarkus based command line tool takes a list of Camel Quarkus versions as argument.

For each Camel Quarkus versions, it:
 + Assembles a sample base Camel Quarkus project against the specified Camel Quarkus version
 + Setup a performance test in the maven integration-test phase
 + Runs the performance test with the help of the [hyperfoil-maven-plugin](https://hyperfoil.io/)
 + Collects the mean throughput of the Camel Quarkus route

At the end of the day, a report is presented to the console, including a status about possible regressions.

Please find more details about the process in below picture:
![Performance regression detection tool process](processes-schema-app.diagrams.net.drawio.png)
