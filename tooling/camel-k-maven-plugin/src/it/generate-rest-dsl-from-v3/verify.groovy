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

new File(basedir, "document.xml").withReader {
    def document = new groovy.xml.XmlSlurper().parse(it)

    assert document.rest.@path == '/v1'
    assert document.rest.get.size() == 2
    assert document.rest.get.find { it.@id == 'listPets' }.@path == '/pets'
    assert document.rest.get.find { it.@id == 'listPets' }.to.@uri == 'direct:listPets'
    assert document.rest.get.find { it.@id == 'showPetById' }.@path == '/pets/{petId}'
    assert document.rest.get.find { it.@id == 'showPetById' }.to.@uri == 'direct:showPetById'
    assert document.rest.post.find { it.@id == 'createPets' }.@path == '/pets'
    assert document.rest.post.find { it.@id == 'createPets' }.to.@uri == 'direct:createPets'
}
