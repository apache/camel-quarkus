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
package org.apache.camel.quarkus.support.bouncycastle;

import java.security.Provider;
import java.security.Security;
import java.util.List;

import javax.crypto.Cipher;

import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.runtime.SecurityProviderUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.logging.Logger;

@Recorder
public class BouncyCastleRecorder {

    private static final Logger LOG = Logger.getLogger(BouncyCastleRecorder.class);

    public void registerBouncyCastleProvider(List<String> cipherTransformations, ShutdownContext shutdownContext) {
        Provider provider = Security.getProvider(SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_NAME);
        if (provider == null) {
            // TODO: Fix BuildStep execution order so that this is not required
            // https://github.com/apache/camel-quarkus/issues/3472
            provider = new BouncyCastleProvider();
            Security.addProvider(provider);
        }

        // Make it explicit to the static analysis that below security services should be registered as they are reachable at runtime
        for (String cipherTransformation : cipherTransformations) {
            try {
                LOG.debugf(
                        "Making it explicit to the static analysis that a Cipher with transformation %s could be used at runtime",
                        cipherTransformation);
                Cipher.getInstance(cipherTransformation, provider);
            } catch (Exception e) {
                // The cipher algorithm or padding is not present at runtime, a runtime error will be reported as usual
            }
        }

        shutdownContext.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                Security.removeProvider(SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_NAME);
                LOG.debug("Removed Bouncy Castle security provider");
            }
        });
    }
}
