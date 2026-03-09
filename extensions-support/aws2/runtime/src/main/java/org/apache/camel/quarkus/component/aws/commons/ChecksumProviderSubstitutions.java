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
package org.apache.camel.quarkus.component.aws.commons;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import software.amazon.awssdk.checksums.SdkChecksum;
import software.amazon.awssdk.checksums.internal.ChecksumProvider;
import software.amazon.awssdk.checksums.spi.ChecksumAlgorithm;

import static org.apache.camel.quarkus.component.aws.commons.AwsChecksumSubstitutions.CRT_CRC64NVME_PATH;
import static org.apache.camel.quarkus.component.aws.commons.AwsChecksumSubstitutions.CRT_MODULE;
import static org.apache.camel.quarkus.component.aws.commons.AwsChecksumSubstitutions.CRT_XXHASH_PATH;

final class AwsChecksumSubstitutions {
    static final String CRT_CRC64NVME_PATH = "software.amazon.awssdk.crt.checksums.CRC64NVME";
    static final String CRT_XXHASH_PATH = "software.amazon.awssdk.crt.checksums.XXHash";
    static final String CRT_MODULE = "software.amazon.awssdk.crt:aws-crt";
}

/**
 * Avoids references to types that are only available if aws-crt is on the classpath.
 */
@TargetClass(value = ChecksumProvider.class, onlyWith = ChecksumProviderSubstitutions.AwsCrtIsAbsent.class)
final class ChecksumProviderSubstitutions {
    @Substitute
    static SdkChecksum crc64NvmeCrtImplementation() {
        throw new RuntimeException(
                "Could not load " + CRT_CRC64NVME_PATH + ". Add dependency on '" + CRT_MODULE
                        + "' module to enable CRC64NVME feature.");
    }

    @Substitute
    static SdkChecksum createCrtCrc32C() {
        return null;
    }

    @Substitute
    static SdkChecksum crtXxHash(ChecksumAlgorithm algorithm) {
        throw new RuntimeException(
                String.format("Could not load %s for algorithm: %s. Add dependency on '%s' module.", CRT_XXHASH_PATH,
                        algorithm.algorithmId(), CRT_MODULE));
    }

    public static final class AwsCrtIsAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Thread.currentThread().getContextClassLoader().loadClass(CRT_CRC64NVME_PATH);
                return false;
            } catch (ClassNotFoundException e) {
                return true;
            }
        }
    }
}
