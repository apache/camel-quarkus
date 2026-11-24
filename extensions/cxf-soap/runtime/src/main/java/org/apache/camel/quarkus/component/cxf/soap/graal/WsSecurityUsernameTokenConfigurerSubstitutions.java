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
package org.apache.camel.quarkus.component.cxf.soap.graal;

import java.util.function.BooleanSupplier;

import javax.security.auth.callback.Callback;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;

/**
 * {@code WsSecurityUsernameTokenConfigurer} needs the optional {@code cxf-rt-ws-security} dependency. Without it the
 * class cannot be linked in native mode.
 */
@TargetClass(className = "org.apache.camel.component.cxf.jaxws.WsSecurityUsernameTokenConfigurer", onlyWith = WsSecurityUsernameTokenConfigurerSubstitutions.WsSecurityIsAbsent.class)
final class WsSecurityUsernameTokenConfigurerSubstitutions {

    @Substitute
    public void configureClient(Client client) {
        throw new IllegalStateException(WsSecurityIsAbsent.MESSAGE);
    }

    @Substitute
    public void configureServer(Server server) {
        throw new IllegalStateException(WsSecurityIsAbsent.MESSAGE);
    }

    static final class WsSecurityIsAbsent implements BooleanSupplier {
        static final String MESSAGE = "The WS-Security UsernameToken options of the CXF endpoint require the io.quarkiverse.cxf:quarkus-cxf-rt-ws-security dependency";

        @Override
        public boolean getAsBoolean() {
            try {
                Thread.currentThread().getContextClassLoader().loadClass("org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor");
                return false;
            } catch (ClassNotFoundException e) {
                return true;
            }
        }
    }
}

@TargetClass(className = "org.apache.camel.component.cxf.jaxws.WsSecurityUsernameTokenConfigurer$PasswordCallbackHandler", onlyWith = WsSecurityUsernameTokenConfigurerSubstitutions.WsSecurityIsAbsent.class)
final class PasswordCallbackHandlerSubstitutions {

    @Substitute
    public void handle(Callback[] callbacks) {
        throw new IllegalStateException(WsSecurityUsernameTokenConfigurerSubstitutions.WsSecurityIsAbsent.MESSAGE);
    }
}
