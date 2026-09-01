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

if [ "$#" -lt 2 ]; then
    echo "usage: $0 upload-sources release-version nexus-staging-repository-id"
    exit 1
fi

location=$(dirname $0)
version=$1
stagingRepoId=$2
sourcesUrl=https://repository.apache.org/content/repositories/orgapachecamel-${stagingRepoId}/org/apache/camel/quarkus/camel-quarkus/${version}

if [[ "$(curl -L -s -o /dev/null -w "%{http_code}" ${sourcesUrl})" != "200" ]]; then
  echo "Failed to access ${sourcesUrl}. Is the ${version} staging repository closed?"
  exit 1
fi

# Import the release keys into a throwaway keyring, so the verification below is
# answered by the project KEYS file rather than by whatever the release manager
# happens to have in their own keyring
gpgHome=$(mktemp -d)
chmod 700 ${gpgHome}

# Absolute, so that the trap still resolves it after the cd below
stagingDir=$(pwd)/${version}

# Clear the staging directory on the way out, whether or not the run succeeded.
# Without this a failed verification leaves a partly downloaded ${version}/
# behind and the next attempt stops at mkdir with "File exists".
trap 'rm -rf "${gpgHome}" "${stagingDir}"' EXIT

gpg --homedir ${gpgHome} --quiet --import ${location}/../../KEYS

# Download an artifact with its detached signature, verify the signature, and only
# then generate the checksum that gets published alongside it. set -e aborts the
# release if any verification fails.
#
# NOTE: gpg --verify exits 0 for a good signature made by a revoked or expired
# key, reporting it only as a warning. KEYS holds expired keys by design, since
# ASF keeps the keys that signed past releases, so a release manager whose key
# expired mid-cycle would still pass here. Reject EXPKEYSIG and REVKEYSIG from
# --status-fd if that needs closing.
fetch_verify_checksum() {
  remoteName=$1
  localName=$2

  wget ${sourcesUrl}/${remoteName} -O ${localName}
  wget ${sourcesUrl}/${remoteName}.asc -O ${localName}.asc
  gpg --homedir ${gpgHome} --verify ${localName}.asc ${localName}
  sha512sum -b ${localName} > ${localName}.sha512
}

mkdir ${version}/
cd ${version}/

fetch_verify_checksum camel-quarkus-${version}-src.zip apache-camel-quarkus-${version}-src.zip
fetch_verify_checksum camel-quarkus-${version}-cyclonedx.json apache-camel-quarkus-${version}-sbom.json
fetch_verify_checksum camel-quarkus-${version}-cyclonedx.xml apache-camel-quarkus-${version}-sbom.xml

cd ../

svn import ${version}/ https://dist.apache.org/repos/dist/dev/camel/camel-quarkus/${version}/ -m "Import camel-quarkus ${version} release"

