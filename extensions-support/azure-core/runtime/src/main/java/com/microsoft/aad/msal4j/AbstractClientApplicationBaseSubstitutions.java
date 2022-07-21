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
package com.microsoft.aad.msal4j;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "com.microsoft.aad.msal4j.AbstractClientApplicationBase", onlyWith = AbstractClientApplicationBaseSubstitutions.Msal4jIsPresent.class)
public final class AbstractClientApplicationBaseSubstitutions {

    /**
     * Cuts out instantiation of AcquireTokenByInteractiveFlowSupplier which leads to references of classes
     * in package com.sun.net.httpserver. Since GraalVM 2.22.0, this package is not on the module path by default.
     *
     * An additional option is required for native-image in order to compile the application successfully
     * -J--add-modules=jdk.httpserver.
     *
     * Given that interactive authentication is of little value in a production application (since in this case it requires
     * the launching of a web browser and some human intervention to examine the resulting web page), it should be safe to
     * disable AcquireTokenByInteractiveFlowSupplier.
     */
    @Substitute
    private AuthenticationResultSupplier getAuthenticationResultSupplier(MsalRequest msalRequest) {
        AuthenticationResultSupplier supplier;
        if (msalRequest instanceof DeviceCodeFlowRequest) {
            supplier = new AcquireTokenByDeviceCodeFlowSupplier(PublicClientApplication.class.cast(this),
                    (DeviceCodeFlowRequest) msalRequest);
        } else if (msalRequest instanceof SilentRequest) {
            supplier = new AcquireTokenSilentSupplier(AbstractClientApplicationBase.class.cast(this),
                    (SilentRequest) msalRequest);
        } else if (msalRequest instanceof InteractiveRequest) {
            throw new IllegalArgumentException("InteractiveRequest is not supported on GraalVM");
        } else if (msalRequest instanceof ClientCredentialRequest) {
            supplier = new AcquireTokenByClientCredentialSupplier(ConfidentialClientApplication.class.cast(this),
                    (ClientCredentialRequest) msalRequest);
        } else if (msalRequest instanceof OnBehalfOfRequest) {
            supplier = new AcquireTokenByOnBehalfOfSupplier(ConfidentialClientApplication.class.cast(this),
                    (OnBehalfOfRequest) msalRequest);
        } else {
            supplier = new AcquireTokenByAuthorizationGrantSupplier(AbstractClientApplicationBase.class.cast(this), msalRequest,
                    null);
        }
        return supplier;
    }

    static final class Msal4jIsPresent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Thread.currentThread().getContextClassLoader().loadClass("com.microsoft.aad.msal4j.Credential");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
