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
package org.apache.camel.quarkus.component.nsq.it;

import org.jboss.logging.Logger;

/**
 * Quarkus is not able to display logs in JVM mode when running NSQ surefire tests.
 * This class intent to workaround this issue.
 */
public class NsqLogger {

    public static void log(Logger logger, String format, Object... args) {
        String log = String.format(format, args);
        System.out.println(log);
        logger.info(log);
    }

    public static void log(org.slf4j.Logger logger, String format, Object... args) {
        String log = String.format(format, args);
        System.out.println(log);
        logger.info(log);
    }

}
