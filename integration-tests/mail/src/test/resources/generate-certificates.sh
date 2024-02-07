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


export CN=${1:-localhost}
export SUBJECT_ALT_NAMES=${2:-"DNS:localhost,IP:127.0.0.1"}
export CERT_OUTPUT_FILE=${3:-greenmail.p12}

echo "====> PWD = ${PWD}"
echo "====> CN = ${CN}"
echo "====> SUBJECT_ALT_NAMES = ${SUBJECT_ALT_NAMES}"
echo "====> CERT_OUTPUT_FILE = ${CERT_OUTPUT_FILE}"

openssl req -x509 -newkey rsa:4096 -sha256 -days 3650 -nodes -keyout greenmail.key -out greenmail.crt -subj "/CN=${CN}" -addext "subjectAltName=${SUBJECT_ALT_NAMES}"
openssl pkcs12 -export -out ${CERT_OUTPUT_FILE} -inkey greenmail.key -in greenmail.crt -password pass:changeit

rm -f *.crt *.key
