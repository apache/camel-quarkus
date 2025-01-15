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

keyType="ed25519"

destinationDir="src/main/resources/edDSA"
testDestinationDir="src/test/resources/edDSA"

# see https://stackoverflow.com/a/54924640
export MSYS_NO_PATHCONV=1

if ! [[ -x "$(command -v openssl)" ]] ; then
  echo 'Error: openssl is not installed.' >&2
  exit 1
fi

mkdir -p "$destinationDir"
mkdir -p "$testDestinationDir"

# Ed25519  private key
#openssl genpkey -algorithm ed25519   -out "$destinationDir/key_ed25519.pem"
ssh-keygen -t $keyType -o -a 100 -N "" -f "$testDestinationDir/key_ed25519.pem" -C "test@localhost"

# Ed25519  public key
#openssl pkey -in "$destinationDir/key_ed25519.pem" -pubout -out "$destinationDir/key_ed25519.pem.pub"
ssh-keygen -y -f "$testDestinationDir/key_ed25519.pem" > "$testDestinationDir/key_ed25519.pem.pub"

#generate known-hosts
echo -n "127.0.0.1 $(sed 's/\(.*\) \([^ ]*\)$/\1/' "$testDestinationDir/key_ed25519.pem.pub")" >> "$destinationDir/known_hosts"


