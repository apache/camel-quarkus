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
package org.apache.camel.quarkus.component.aws2.mq.it;

/**
 * This class is required by the aws2-grouped module. The group-tests.groovy script copies test sources
 * from this module into the grouped module and renames all occurrences of Aws2MqTest to
 * GroupedAws2MqTest, which also renames Aws2MqTestResource to GroupedAws2MqTestResource
 * in the @QuarkusTestResource annotation. This class must therefore exist so that the grouped module compiles.
 */
public class GroupedAws2MqTestResource extends Aws2MqTestResource {
}
