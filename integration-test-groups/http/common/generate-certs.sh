#!/bin/bash
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
set -x

invocationDir="$(pwd)"
workDir="target/openssl-work"
destinationDir="src/main/resources/jsse"
keySize=2048
days=10000
extFile="$(pwd)/v3.ext"
encryptionAlgo="aes-256-cbc"

if [[ -n "${JAVA_HOME}" ]] ; then
  keytool="$JAVA_HOME/bin/keytool"
elif ! [[ -x "$(command -v keytool)" ]] ; then
  echo 'Error: Either add keytool to PATH or set JAVA_HOME' >&2
  exit 1
else
  keytool="keytool"
fi

if ! [[ -x "$(command -v openssl)" ]] ; then
  echo 'Error: openssl is not installed.' >&2
  exit 1
fi

mkdir -p "$workDir"
mkdir -p "$destinationDir"

# Certificate authority
openssl genrsa -out "$workDir/ca.key" $keySize
openssl req -x509 -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=ca' -key "$workDir/ca.key" -nodes -out "$workDir/ca.pem" -days $days -extensions v3_req
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=ca' -x509 -key "$workDir/ca.key" -days $days -out "$workDir/ca.crt"

for actor in localhost; do
  # Generate keys
  openssl genrsa -out "$workDir/$actor.key" $keySize

  # Generate certificates
  openssl req -new -subj "/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=$actor" -key "$workDir/$actor.key"  -out "$workDir/$actor.csr"
  openssl x509 -req -in "$workDir/$actor.csr" -extfile "$extFile" -CA "$workDir/ca.pem" -CAkey "$workDir/ca.key" -CAcreateserial -days $days -out "$workDir/$actor.crt"

  # Export keystores
  openssl pkcs12 -export -in "$workDir/$actor.crt" -inkey "$workDir/$actor.key" -certfile "$workDir/ca.crt" -name "$actor" -out "$destinationDir/$actor-keystore.pkcs12" -passout pass:"${actor}-keystore-password" -keypbe "$encryptionAlgo" -certpbe "$encryptionAlgo"
done


# Truststore
"$keytool" -import -file "$workDir/localhost.crt" -alias localhost -noprompt -keystore "$destinationDir/client-truststore.pkcs12" -storepass "client-truststore-password"
"$keytool" -import -file "$workDir/ca.crt"     -alias ca     -noprompt -keystore "$destinationDir/client-truststore.pkcs12" -storepass "client-truststore-password"
