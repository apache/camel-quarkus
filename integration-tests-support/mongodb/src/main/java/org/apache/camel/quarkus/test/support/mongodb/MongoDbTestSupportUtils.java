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
package org.apache.camel.quarkus.test.support.mongodb;

public class MongoDbTestSupportUtils {
    private MongoDbTestSupportUtils() {
        // Utility class
    }

    public static String getMongoScriptExecutable(String imageName) {
        int major = getMongoMajorVersion(imageName);
        if (major < 6) {
            return "mongo";
        }
        return "mongosh";
    }

    static int getMongoMajorVersion(String imageName) {
        String[] imageNameParts = imageName.split(":");
        if (imageNameParts.length == 1) {
            // Assume it's an image name without a tag. E.g 'latest'.
            return 999;
        }

        String[] versionParts = imageNameParts[1].split("\\.");
        if (versionParts.length == 0) {
            throw new IllegalArgumentException("Invalid image version: " + imageName);
        }

        return Integer.parseInt(versionParts[0]);
    }
}
