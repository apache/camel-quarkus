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
package org.apache.camel.quarkus.component.mapstruct.it.mapper.dog;

import java.util.UUID;

import org.apache.camel.quarkus.component.mapstruct.it.model.Cat;
import org.apache.camel.quarkus.component.mapstruct.it.model.Dog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

// Test CDI support for ApplicationScope beans
@Mapper(imports = UUID.class, componentModel = MappingConstants.ComponentModel.JAKARTA_CDI)
public interface DogMapper {
    // Test expressions
    @Mapping(target = "dogId", source = "catId", defaultExpression = "java( UUID.randomUUID().toString() )")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "age", target = "age")
    @Mapping(target = "vocalization", constant = "bark")
    Dog catToDog(Cat cat);
}
