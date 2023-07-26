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
package org.apache.camel.quarkus.component.mapstruct;

import org.apache.camel.component.mapstruct.MapStructMapperFinder;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.camel.util.ObjectHelper;
import org.jboss.logging.Logger;

/**
 * Custom {@link MapStructMapperFinder} that is effectively a noop implementation, as the work of discovering
 * mappings is done at build time.
 */
public class CamelQuarkusMapStructMapperFinder extends ServiceSupport implements MapStructMapperFinder {
    private static final Logger LOG = Logger.getLogger(CamelQuarkusMapStructMapperFinder.class);

    private final int mappingsCount;
    private String mapperPackageName;

    public CamelQuarkusMapStructMapperFinder(String mapperPackageName, int mappingsCount) {
        setMapperPackageName(mapperPackageName);
        this.mappingsCount = mappingsCount;
    }

    @Override
    public void setMapperPackageName(String mapperPackageName) {
        this.mapperPackageName = mapperPackageName;
    }

    @Override
    public String getMapperPackageName() {
        return this.mapperPackageName;
    }

    @Override
    public int discoverMappings(Class<?> clazz) {
        // Discovery is done at build time so just return the count
        return mappingsCount;
    }

    @Override
    protected void doInit() throws Exception {
        if (ObjectHelper.isNotEmpty(mapperPackageName)) {
            LOG.infof("Discovered %d MapStruct type converters during build time augmentation: %s", mappingsCount,
                    mapperPackageName);
        }
    }
}
