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
package org.apache.camel.quarkus.component.file.it;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileFilter;

import static org.apache.camel.component.file.FileConstants.FILE_NAME_CONSUMED;
import static org.apache.camel.quarkus.component.file.it.FileResource.CONSUME_BATCH;
import static org.apache.camel.quarkus.component.file.it.FileResource.SORT_BY;

@ApplicationScoped
public class FileRoutes extends RouteBuilder {

    public static final String READ_LOCK_IN = "read-lock-in";
    public static final String READ_LOCK_OUT = "read-lock-out";

    private static final Set<String> IDEMPOTENT_FILES_CONSUMED = ConcurrentHashMap.newKeySet();

    @Override
    public void configure() {
        from("file://target/" + READ_LOCK_IN + "?"
                + "initialDelay=0&"
                + "move=.done&"
                + "delay=1000&"
                + "readLock=changed&"
                + "readLockMinAge=1000&"
                + "readLockMinLength=100&"
                + "readLockCheckInterval=2000&"
                + "readLockLoggingLevel=TRACE&"
                + "readLockTimeout=5000")
                .to("file://target/" + READ_LOCK_OUT);

        from("file://target/quartz?scheduler=quartz&scheduler.cron=0/1+*+*+*+*+?&repeatCount=0")
                .to("file://target/quartz/out");

        from("file://target/" + CONSUME_BATCH + "?"
                + "initialDelay=0&delay=100")
                .id(CONSUME_BATCH)
                .noAutoStartup()
                .convertBodyTo(String.class)
                .to("mock:" + CONSUME_BATCH);

        from("file://target/charsetIsoRead?initialDelay=0&delay=10&delete=true&charset=ISO-8859-1")
                .routeId("charsetIsoRead")
                .autoStartup(false)
                .convertBodyTo(String.class)
                .to("mock:charsetIsoRead");

        from("file://target/test-files/idempotent?idempotent=true&move=done/${file:name}&initialDelay=0&delay=10")
                .process(ex -> {
                    String fileNameConsumed = ex.getMessage().getHeader(FILE_NAME_CONSUMED, String.class);
                    if (IDEMPOTENT_FILES_CONSUMED.add(fileNameConsumed)) {
                        ex.getMessage().setHeader("fileNameConsumedStatusHeader", fileNameConsumed + "_was-read-once");
                    } else {
                        ex.getMessage().setHeader("fileNameConsumedStatusHeader",
                                fileNameConsumed + "_was-read-more-than-once");
                    }
                })
                .convertBodyTo(String.class).toD("mock:idempotent_${header.fileNameConsumedStatusHeader}");

        bindToRegistry("myFilter", new MyFileFilter<>());
        from(("file://target/test-files/filter?initialDelay=0&delay=10&filter=#myFilter"))
                .convertBodyTo(String.class).to("mock:filter");

        from(("file://target/sortBy?initialDelay=0&delay=10&sortBy=reverse:file:name"))
                .id(SORT_BY)
                .noAutoStartup()
                .convertBodyTo(String.class).to("mock:" + SORT_BY);

        from("direct:pollEnrich")
                .pollEnrich("file://target/pollEnrich?fileName=pollEnrich.txt");

    }

    public class MyFileFilter<T> implements GenericFileFilter<T> {
        @Override
        public boolean accept(GenericFile<T> file) {
            // we want all directories
            if (file.isDirectory()) {
                return true;
            }
            // we do not accept any files having name starting with 'skipped_'
            return !file.getFileName().startsWith("skipped_");
        }
    }
}
