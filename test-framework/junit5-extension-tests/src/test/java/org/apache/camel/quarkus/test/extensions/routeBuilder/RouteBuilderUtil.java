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
package org.apache.camel.quarkus.test.extensions.routeBuilder;

import java.util.logging.Level;

import io.quarkus.test.ContinuousTestingTestUtils;
import io.quarkus.test.QuarkusDevModeTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class RouteBuilderUtil {

    static QuarkusDevModeTest createTestModule(Class testClass, Class<?>... archiveClasses) {
        QuarkusDevModeTest retVal = new QuarkusDevModeTest()
                .setArchiveProducer(() -> {
                    JavaArchive ja = ShrinkWrap.create(JavaArchive.class)
                            .addClasses(archiveClasses)
                            .add(new StringAsset(
                                    ContinuousTestingTestUtils.appProperties("camel-quarkus.junit5.message=Sheldon")),
                                    "application.properties");
                    return ja;
                })
                .setTestArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class).addClasses(testClass))
                .setLogRecordPredicate(record -> record.getLevel().equals(Level.WARNING));

        return retVal;
    }

}
