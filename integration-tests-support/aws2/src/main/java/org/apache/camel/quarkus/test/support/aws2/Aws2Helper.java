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
package org.apache.camel.quarkus.test.support.aws2;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;

public class Aws2Helper {

    public static final String UNABLE_TO_LOAD_CREDENTIALS_MSG = "Unable to load credentials";

    public static boolean isDefaultCredentialsProviderDefinedOnSystem() {
        try {
            DefaultCredentialsProvider.create().resolveCredentials();
        } catch (Exception e) {
            //if message starts with "Unable to load credentials", allow testing
            if (e instanceof SdkClientException && e.getMessage() != null
                    && e.getMessage().startsWith(UNABLE_TO_LOAD_CREDENTIALS_MSG)) {
                return false;
            }
        }
        return true;
    }

    public static void setAwsSystemCredentials(String accessKey, String secretKey) {
        System.setProperty("aws.accessKeyId", accessKey);
        System.setProperty("aws.secretAccessKey", secretKey);

    }

    public static void clearAwsSystemCredentials() {
        //system properties has to be cleared after the test
        System.clearProperty("aws.accessKeyId");
        System.clearProperty("aws.secretAccessKey");
    }

}
