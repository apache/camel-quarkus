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

# Generates the keystore of CryptoResource for the bcfips profile, where @TestCertificates cannot be used,
# as its certificate generator needs the non-FIPS BouncyCastle. Not run by the build, run it from this directory
# to regenerate the committed src/main/resources/crypto-keystore.p12

set -e
set -x

keySize=2048
days=10000
alias="crypto"
password="crypto-keystore-password"
encryptionAlgo="aes-256-cbc"

workDir="target/openssl-work"
destinationDir="src/main/resources"

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

openssl genrsa -out "$workDir/$alias.key" $keySize
openssl req -x509 -new -subj "/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=$alias" -key "$workDir/$alias.key" -days $days -out "$workDir/$alias.crt"
openssl pkcs12 -export -in "$workDir/$alias.crt" -inkey "$workDir/$alias.key" -name "$alias" -out "$destinationDir/$alias-keystore.p12" -passout pass:"$password" -keypbe "$encryptionAlgo" -certpbe "$encryptionAlgo"

# Fail early if the JDK cannot read the keystore
"$keytool" -list -keystore "$destinationDir/$alias-keystore.p12" -storetype PKCS12 -storepass "$password"
