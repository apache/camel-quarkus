/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
final String currentVersion = project.version - '-SNAPSHOT'
final String jvmSince    = project.properties['camel.quarkus.jvmSince']
final String nativeSince = project.properties['camel.quarkus.nativeSince']

if (jvmSince != null && compareVersion(jvmSince, currentVersion) > 0) {
   throw new RuntimeException("jvmSince " + jvmSince + " is newer than " + project.version);
}

if (nativeSince != null && compareVersion(nativeSince, currentVersion) > 0) {
   throw new RuntimeException("nativeSince " + nativeSince + " is newer than " + project.version);
}

int compareVersion(String a, String b) {
    List verA = a.tokenize('.')
    List verB = b.tokenize('.')

    def commonIndices = Math.min(verA.size(), verB.size())

    for (int i = 0; i < commonIndices; ++i) {
       def numA = verA[i].toInteger()
       def numB = verB[i].toInteger()

       if (numA != numB) {
          return numA <=> numB
       }
    }

    return verA.size() <=> verB.size()
}
