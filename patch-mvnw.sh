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


# A script to replace maven-resolver 1.9.7 artifacts with 1.9.8 ones
# This is to be able to set aether.connector.http.connectionMaxTtl
# which is supported only since 1.9.8
# This is motivated by https://github.com/apache/camel-quarkus/issues/4842
# This workaround can be removed after the upgrade to Maven 3.9.2

#set -x
set -e

cd ~/.m2/wrapper/dists/apache-maven-3.9.1-bin/*/apache-maven-3.9.1/lib
for oldJar in maven-resolver-*-1.9.7.jar; do
    if [ "$oldJar" != 'maven-resolver-*-1.9.7.jar' ]; then
        base="${oldJar/%-1.9.7.jar}"
        echo $base
        newJar="$base-1.9.8.jar"
        if [[ ! -f "$newJar" ]]
        then
            echo "Downloading $newJar"
            curl -s -O "https://repo1.maven.org/maven2/org/apache/maven/resolver/$base/1.9.8/$newJar"
        fi
        if [[ -f "$oldJar" ]]
        then
            echo "Deleting $oldJar"
            rm "$oldJar"
        fi
    fi
done
