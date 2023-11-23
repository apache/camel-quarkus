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


rm -f *.p12

export CN=${1:-localhost}
export SUBJECT_ALT_NAMES=${2:-"DNS:localhost,IP:127.0.0.1"}
export SECRET=kafkas3cret
export JKS_FILE=kafka-keystore.jks
export JKS_TRUST_FILE=kafka-truststore.jks
export CERT_FILE=localhost.crt
export PKCS_FILE=kafka-keystore.p12
export PKCS_TRUST_FILE=kafka-truststore.p12
export PEM_FILE_CERT=kafka-cert.pem
export PEM_FILE_KEY=kafka-key.pem

keytool -genkey -alias kafka-test-store -keyalg RSA -keystore ${JKS_FILE} -keysize 2048 -validity 3650 -ext "san=${SUBJECT_ALT_NAMES}" -dname CN=${CN} -keypass ${SECRET} -storepass ${SECRET}
keytool -export -alias kafka-test-store -file ${CERT_FILE} -keystore ${JKS_FILE} -keypass ${SECRET} -storepass ${SECRET}
keytool -importkeystore -srckeystore ${JKS_FILE} -srcstorepass ${SECRET} -destkeystore ${PKCS_FILE} -deststoretype PKCS12 -deststorepass ${SECRET}
keytool -keystore ${JKS_TRUST_FILE} -import -file ${CERT_FILE} -keypass ${SECRET} -storepass ${SECRET} -noprompt
keytool -importkeystore -srckeystore ${JKS_TRUST_FILE} -srcstorepass ${SECRET} -destkeystore ${PKCS_TRUST_FILE} -deststoretype PKCS12 -deststorepass ${SECRET}

rm -f *.crt *.jks
