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

import org.apache.commons.codec.digest.DigestUtils

import java.nio.charset.StandardCharsets
import java.nio.file.Files

new File(basedir, "dependencies.yaml").withReader {
    def deps = new groovy.yaml.YamlSlurper().parse(it)

    assert deps.dependencies.size() != 0

    for (Map<String, String> dependency: deps.dependencies) {
        dependency.checksum != null
        dependency.location != null
        dependency.id != null

        File checksum

        if ((checksum = new File("${dependency.location}.md5")).exists()) {
            assert dependency.checksum == "md5:" + Files.readString(checksum.toPath(), StandardCharsets.UTF_8)
        } else if ((checksum = new File("${dependency.location}.sha1")).exists()) {
            assert dependency.checksum == "sha1:" + Files.readString(checksum.toPath(), StandardCharsets.UTF_8)
        } else {
            def file  = new File(dependency.location)
            def bytes = Files.readAllBytes(file.toPath())

            assert dependency.checksum == "sha1:" + DigestUtils.sha1Hex(bytes)
        }
    }
}
