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
# - test-key-rsa.key: RSA private key (2048 bits)
# - test-key-rsa-cert.pub: OpenSSH certificate signed by a temporary CA
#
# The certificate is valid for 52 weeks and can be used for testing
# certificate-based authentication with the mina-sftp component.
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "==================================================================="
echo "Generating OpenSSH Certificates for MINA SFTP Integration Tests"
echo "==================================================================="
echo ""

echo "Cleaning up existing files..."
rm -f ca-key ca-key.pub test-key-rsa.key test-key-rsa.key.pub test-key-rsa-cert.pub
echo "Cleaned up existing files"

echo "Generating temporary CA key pair..."
ssh-keygen -t rsa -b 2048 -f ca-key -N "" -C "test-ca" > /dev/null 2>&1
echo "Created ca-key and ca-key.pub"

echo "Generating user RSA key pair..."
ssh-keygen -t rsa -b 2048 -f test-key-rsa.key -N "" -C "test-rsa@test" > /dev/null 2>&1
echo "Created test-key-rsa.key and test-key-rsa.key.pub"

echo "Signing public key with CA to create certificate..."
ssh-keygen -s ca-key \
  -I "test-user" \
  -n testuser \
  -V +520w \
  test-key-rsa.key.pub > /dev/null 2>&1
echo "Created test-key-rsa.key-cert.pub"

echo "Renaming certificate to test-key-rsa-cert.pub..."
mv test-key-rsa.key-cert.pub test-key-rsa-cert.pub
echo "Renamed to test-key-rsa-cert.pub"

echo "Cleaning up temporary files..."
rm -f ca-key ca-key.pub test-key-rsa.key.pub
echo "Removed CA keys and public key"
