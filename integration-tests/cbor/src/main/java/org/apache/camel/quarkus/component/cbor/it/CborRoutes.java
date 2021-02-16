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
package org.apache.camel.quarkus.component.cbor.it;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cbor.CBORDataFormat;
import org.apache.camel.quarkus.component.cbor.it.model.Author;
import org.apache.camel.quarkus.component.cbor.it.model.DummyObject;

public class CborRoutes extends RouteBuilder {

    @Override
    public void configure() {
        CBORDataFormat useMapDf = new CBORDataFormat();
        useMapDf.useMap();
        from("direct:marshal-map").marshal(useMapDf);
        from("direct:unmarshal-map").unmarshal(useMapDf);

        CBORDataFormat authorDf = new CBORDataFormat();
        authorDf.setUseDefaultObjectMapper(false);
        authorDf.setUnmarshalType(Author.class);
        from("direct:marshal-author").marshal(authorDf);
        from("direct:unmarshal-author").unmarshal(authorDf);

        CBORDataFormat allowJmsTypeDf = new CBORDataFormat();
        allowJmsTypeDf.setUseDefaultObjectMapper(false);
        allowJmsTypeDf.setAllowJmsType(true);
        from("direct:unmarshal-via-jms-type-header").unmarshal(allowJmsTypeDf);

        CBORDataFormat dummyObjectListDf = new CBORDataFormat();
        dummyObjectListDf.setUseDefaultObjectMapper(false);
        dummyObjectListDf.useList();
        dummyObjectListDf.setUnmarshalType(DummyObject.class);
        from("direct:unmarshal-dummy-object-list")
                .unmarshal(dummyObjectListDf)
                .split(body())
                .to("mock:unmarshal-dummy-object-list");
    }

}
