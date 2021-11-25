#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

## 
# Inspired from https://github.com/quarkusio/quarkus/tree/main/integration-tests/kafka-oauth-keycloak/
#
# Generating the certificates and keystore

## Creating a self-signed CA certificate and truststore

```bash
./gen-ca.sh
```

This creates `crt.ca` and adds the certificate to the keystore `ca-truststore.p12`.

## Creating a server certificate and add it to keystore

```bash
./gen-keycloak-certs.sh
```

This creates server certificate for Keycloak, signs it and adds it to keystore `keycloak.server.keystore.p12`.

## Cleanup

```bash
rm ca.srl
rm ca.crt
rm ca.key
rm cert-file
rm cert-signed
```