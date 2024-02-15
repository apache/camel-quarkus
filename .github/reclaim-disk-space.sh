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

echo "Free disk space before space reclaimed"
df -h /

echo "Reclaiming disk space..."

# Unwanted development SDKs
sudo rm -rf /opt/ghc \
     rm -rf /opt/hostedtoolcache/CodeQL \
     rm -rf /opt/pipx \
     rm -rf /usr/lib/google-cloud-sdk \
     rm -rf /usr/local/.ghcup \
     rm -rf /usr/local/go \
     rm -rf /usr/local/lib/android \
     rm -rf /usr/local/lib/node_modules \
     rm -rf /usr/local/share/boost \
     rm -rf /usr/local/share/powershell \
     rm -rf /usr/share/dotnet \
     rm -rf /usr/share/miniconda \
     rm -rf /usr/share/rust \
     rm -rf /usr/share/swift

# Remove unwanted container images
CONTAINER_IMAGES_TO_REMOVE=(node)
for IMAGE in ${CONTAINER_IMAGES_TO_REMOVE[@]}; do
    if [[ $(docker images ${IMAGE} -q | wc -l) > 0 ]]; then
        docker rmi -f $(docker images ${IMAGE} -q)
    fi
done

echo "Free disk space after space reclaimed"
df -h /
