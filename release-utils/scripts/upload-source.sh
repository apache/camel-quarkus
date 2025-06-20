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

if [[ "$(curl -k -L -s -o /dev/null -w "%{http_code}" ${sourcesUrl})" != "200" ]]; then
  echo "Failed to access ${sourcesUrl}. Is the ${version} staging repository closed?"
  exit 1
fi

mkdir ${version}/
cd ${version}/

wget ${sourcesUrl}/camel-quarkus-${version}-src.zip -O apache-camel-quarkus-${version}-src.zip
wget ${sourcesUrl}/camel-quarkus-${version}-src.zip.asc -O apache-camel-quarkus-${version}-src.zip.asc
sha512sum -b apache-camel-quarkus-${version}-src.zip > apache-camel-quarkus-${version}-src.zip.sha512

wget ${sourcesUrl}/camel-quarkus-${version}-cyclonedx.json -O apache-camel-quarkus-${version}-sbom.json
wget ${sourcesUrl}/camel-quarkus-${version}-cyclonedx.json.asc -O apache-camel-quarkus-${version}-sbom.json.asc
sha512sum -b apache-camel-quarkus-${version}-sbom.json > apache-camel-quarkus-${version}-sbom.json.sha512

wget ${sourcesUrl}/camel-quarkus-${version}-cyclonedx.xml -O apache-camel-quarkus-${version}-sbom.xml
wget ${sourcesUrl}/camel-quarkus-${version}-cyclonedx.xml.asc -O apache-camel-quarkus-${version}-sbom.xml.asc
sha512sum -b apache-camel-quarkus-${version}-sbom.xml > apache-camel-quarkus-${version}-sbom.xml.sha512

svn import ${version}/ https://dist.apache.org/repos/dist/dev/camel/camel-quarkus/${version}/ -m "Import camel-quarkus ${version} release"

rm -rf ${version}/
