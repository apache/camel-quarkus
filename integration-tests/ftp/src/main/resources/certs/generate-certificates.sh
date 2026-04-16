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

#
# Script to generate OpenSSH certificate files for SFTP integration tests
#
# This script creates:
# USER CERTIFICATES:
# - test-key-rsa.key: RSA private key (2048 bits)
# - test-key-rsa-cert.pub: OpenSSH user certificate signed by user CA
#
# HOST CERTIFICATES:
# - host-ca.pub: Host CA public key (for @cert-authority in known_hosts)
# - host-key-rsa.key: RSA host private key (2048 bits)
# - host-key-rsa-cert.pub: OpenSSH host certificate signed by host CA
#
# The certificates are valid for 52 weeks and can be used for testing
# certificate-based authentication with the FTP component.
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "==================================================================="
echo "Generating OpenSSH Certificates for FTP Integration Tests"
echo "==================================================================="
echo ""

echo "Cleaning up existing files..."
rm -f user-ca user-ca.pub test-key-rsa.key test-key-rsa.key.pub test-key-rsa-cert.pub
rm -f host-ca host-ca.pub host-key-rsa.key host-key-rsa.key.pub host-key-rsa-cert.pub
echo "Cleaned up existing files"

echo ""
echo "--- USER CERTIFICATE GENERATION ---"
echo ""

echo "Generating user CA key pair..."
ssh-keygen -t rsa -b 2048 -f user-ca -N "" -C "user-ca" > /dev/null 2>&1
echo "Created user-ca and user-ca.pub"

echo "Generating user RSA key pair..."
ssh-keygen -t rsa -b 2048 -f test-key-rsa.key -N "" -C "test-rsa@test" > /dev/null 2>&1
echo "Created test-key-rsa.key and test-key-rsa.key.pub"

echo "Signing user public key with user CA to create certificate..."
ssh-keygen -s user-ca \
  -I "test-user" \
  -n admin \
  -V +520w \
  test-key-rsa.key.pub > /dev/null 2>&1
echo "Created test-key-rsa.key-cert.pub"

echo "Renaming user certificate to test-key-rsa-cert.pub..."
mv test-key-rsa.key-cert.pub test-key-rsa-cert.pub
echo "Renamed to test-key-rsa-cert.pub"

echo ""
echo "--- HOST CERTIFICATE GENERATION ---"
echo ""

echo "Generating host CA key pair..."
ssh-keygen -t ed25519 -f host-ca -N "" -C "host-ca" > /dev/null 2>&1
echo "Created host-ca and host-ca.pub"

echo "Generating host RSA key pair..."
ssh-keygen -t rsa -b 2048 -f host-key-rsa.key -N "" -C "sftp-server@localhost" > /dev/null 2>&1
echo "Created host-key-rsa.key and host-key-rsa.key.pub"

echo "Signing host public key with host CA to create host certificate..."
ssh-keygen -s host-ca \
  -I "sftp-server" \
  -h \
  -n localhost \
  -V +520w \
  host-key-rsa.key.pub > /dev/null 2>&1
echo "Created host-key-rsa.key-cert.pub"

echo "Renaming host certificate to host-key-rsa-cert.pub..."
mv host-key-rsa.key-cert.pub host-key-rsa-cert.pub
echo "Renamed to host-key-rsa-cert.pub"

echo ""
echo "Cleaning up temporary files..."
rm -f user-ca user-ca.pub test-key-rsa.key.pub
rm -f host-ca host-key-rsa.key.pub
echo "Removed CA private keys and public keys"
