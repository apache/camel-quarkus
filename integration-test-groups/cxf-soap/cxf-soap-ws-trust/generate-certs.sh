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

keySize=2048
days=10000
password="password"
encryptionAlgo="aes-256-cbc"

workDir="target/openssl-work"
destinationDir="src/main/resources"
destinationTestDir="src/test/resources"

# see https://stackoverflow.com/a/54924640
export MSYS_NO_PATHCONV=1

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
openssl genrsa -out "$workDir/cxfca.key" $keySize
openssl req -x509 -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=cxfca' -key "$workDir/cxfca.key" -nodes -out "$workDir/cxfca.pem" -days $days -extensions v3_req
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=cxfca' -x509 -key "$workDir/cxfca.key" -days $days -out "$workDir/cxfca.crt"

for actor in client service sts actas; do
  # Generate keys
  openssl genrsa -out "$workDir/$actor.key" $keySize

  # Generate certificates
  openssl req -new -subj "/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=$actor" -key "$workDir/$actor.key"  -out "$workDir/$actor.csr"
  openssl x509 -req -in "$workDir/$actor.csr" -CA "$workDir/cxfca.pem" -CAkey "$workDir/cxfca.key" -CAcreateserial -days $days -out "$workDir/$actor.crt"

  # Export keystores
  openssl pkcs12 -export -in "$workDir/$actor.crt" -inkey "$workDir/$actor.key" -certfile "$workDir/cxfca.crt" -name "my${actor}key" -out "$destinationDir/${actor}store.pkcs12" -passout pass:"$password" -keypbe "$encryptionAlgo" -certpbe "$encryptionAlgo"
done

keytool -import -trustcacerts -alias mystskey     -file "$workDir/sts.crt"     -noprompt -keystore "$destinationDir/servicestore.pkcs12"  -storepass "$password"

keytool -import -trustcacerts -alias actasclient -file "$workDir/actas.crt" -noprompt -keystore "$destinationDir/stsstore.pkcs12"      -storepass "$password"
keytool -import -trustcacerts -alias myclientkey -file "$workDir/client.crt" -noprompt -keystore "$destinationDir/stsstore.pkcs12"      -storepass "$password"
keytool -import -trustcacerts -alias myservicekey -file "$workDir/service.crt" -noprompt -keystore "$destinationDir/stsstore.pkcs12"      -storepass "$password"

keytool -import -trustcacerts -alias myactaskey -file "$workDir/actas.crt" -noprompt -keystore "$destinationDir/clientstore.pkcs12"      -storepass "$password"
keytool -import -trustcacerts -alias myservicekey -file "$workDir/service.crt" -noprompt -keystore "$destinationDir/clientstore.pkcs12"   -storepass "$password"
keytool -import -trustcacerts -alias mystskey     -file "$workDir/sts.crt"     -noprompt -keystore "$destinationDir/clientstore.pkcs12"   -storepass "$password"

mv "$destinationDir/clientstore.pkcs12" "$destinationTestDir/clientstore.pkcs12"
rm "$destinationDir/actasstore.pkcs12"