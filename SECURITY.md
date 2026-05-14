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
# Security Policy

## Supported Versions

To see which versions of Apache Camel Quarkus are supported please refer to this [page](https://camel.apache.org/camel-quarkus/latest/).

## Reporting a Vulnerability

For information on how to report a new security problem please see [here](https://camel.apache.org/security/).

**Important:** Do **not** file a public GitHub issue or Jira ticket for security vulnerabilities. Only
report security issues through the private `private-security@camel.apache.org` email address as
described in the security reporting instructions.

## Security Model

Before submitting a report, please read the project's
[Security Model](docs/modules/ROOT/pages/user-guide/security-model.adoc). It documents the
security model for Camel Quarkus, including inherited security considerations from Apache Camel,

The security model defines:
- Who is trusted (committers, route authors, deployment operators vs. untrusted external message senders)
- Where the trust boundaries sit (route + configuration vs. data flowing through the route)
- Which vulnerability classes the Camel Quarkus PMC accepts
- Which categories are out of scope (route-author or operator responsibility, explicit opt-ins,
  DoS through unthrottled routes, third-party transitive CVEs not reachable through Camel Quarkus code)

Reports outside the documented scope will be closed with a reference to the security model.

### Quarkus Specific Security Concerns

For suspected vulnerabilities in `io.quarkus` or `io.quarkiverse` dependencies that are not
reachable through Camel Quarkus extension code, please refer to the
[Quarkus Security Policy](https://quarkus.io/security/) and report directly to the Quarkus
project.
