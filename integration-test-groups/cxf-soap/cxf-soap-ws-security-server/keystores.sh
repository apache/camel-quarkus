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

echo "*** Generate keys ***"
openssl genrsa -out alice.key 2048
openssl genrsa -out bob.key 2048

echo "*** Certificate authority ***"
echo "When prompted for certificate information, confirm default values."
openssl genrsa -out cxfca.key 2048
openssl req -x509 -new -key cxfca.key -nodes -out cxfca.pem -config cxfca-openssl.cnf -days 10000 -extensions v3_req
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=cxfca' -x509 -key cxfca.key -days 10000 -out cxfca.crt

echo "*** Generate certificates ***"
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=cxfca' -x509 -key cxfca.key -out cxfca.crt
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=alice' -key alice.key -out alice.csr
openssl x509 -req -in alice.csr -CA cxfca.pem -CAkey cxfca.key -CAcreateserial -days 10000 -out alice.crt
openssl req -new -subj '/O=apache.org/OU=eng (NOT FOR PRODUCTION)/CN=bob' -key bob.key -out bob.csr
openssl x509 -req -in bob.csr -CA cxfca.pem -CAkey cxfca.key -CAcreateserial -days 10000 -out bob.crt

echo "*** Export keystores ***"
openssl pkcs12 -export -in alice.crt -inkey alice.key -certfile cxfca.crt -name "alice" -out alice.p12 -passout pass:password -keypbe aes-256-cbc -certpbe aes-256-cbc
openssl pkcs12 -export -in bob.crt -inkey bob.key -certfile cxfca.crt -name "bob" -out bob.p12 -passout pass:password -keypbe aes-256-cbc -certpbe aes-256-cbc

echo "When prompted for password, type 'password'."
echo "When prompted whether to trust the certificate, type 'yes'."
keytool -import -trustcacerts -alias bob -file bob.crt -keystore alice.p12
keytool -import -trustcacerts -alias alice -file alice.crt -keystore bob.p12
