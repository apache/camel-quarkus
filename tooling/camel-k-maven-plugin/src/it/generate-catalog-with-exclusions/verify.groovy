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

new File(basedir, "catalog.yaml").withReader {
    def catalog = new groovy.yaml.YamlSlurper().parse(it)

    assert catalog.spec.loaders['jsh'] == null
    assert catalog.spec.loaders['kts'] == null
    assert catalog.spec.loaders['js'] == null
    assert catalog.spec.loaders['groovy'] == null

    assert catalog.spec.loaders['java'] != null
    assert catalog.spec.loaders['xml'] != null
    assert catalog.spec.loaders['yaml'] != null

    assert catalog.spec.artifacts['camel-quarkus-jackson-avro'] != null
    assert catalog.spec.artifacts['camel-quarkus-csimple'] == null
    assert catalog.spec.artifacts['camel-quarkus-disruptor'] == null

    assert catalog.spec.artifacts['camel-quarkus-debug'] != null
    assert catalog.spec.artifacts['camel-quarkus-jta'] == null
    assert catalog.spec.artifacts['camel-quarkus-redis'] == null

    assert catalog.spec.runtime.capabilities['master'] == null
    assert catalog.spec.artifacts['camel-k-master'] == null
}
