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


set -e

PASSWORD=changeit

echo "#### Create server certificate for Keycloak"
keytool -keystore keycloak.server.keystore.p12 -storetype pkcs12 -keyalg RSA -alias keycloak -validity 3650 -genkey -storepass $PASSWORD -keypass $PASSWORD -dname CN=keycloak -ext SAN=DNS:keycloak

echo "#### Sign server certificate (export, sign, add signed to keystore)"
keytool -keystore keycloak.server.keystore.p12 -storetype pkcs12 -alias keycloak -storepass $PASSWORD -keypass $PASSWORD -certreq -file cert-file
openssl x509 -req -CA ca.crt -CAkey ca.key -in cert-file -out cert-signed -days 3650 -CAcreateserial -passin pass:$PASSWORD
keytool -keystore keycloak.server.keystore.p12 -alias CARoot -storepass $PASSWORD -keypass $PASSWORD -import -file ca.crt -noprompt
keytool -keystore keycloak.server.keystore.p12 -alias keycloak -storepass $PASSWORD -keypass $PASSWORD -import -file cert-signed -noprompt
