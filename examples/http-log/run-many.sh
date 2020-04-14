#!/bin/sh
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

# script to quickly run 100 instances of the native compiled example
# using http port numbers from 8000 - 8100
# you can then try each instance by its number such as:
#   http://localhost:8012/camel/hello
#
# to kill all instances at once, you can use pkill command:
#   pkill camel-quarkus
#
for i in $(seq 8000 8100); do
  QUARKUS_HTTP_PORT=$i ./target/camel-quarkus-examples-http-log-*-runner > http-log-$i.log &
done