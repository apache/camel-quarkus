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
package org.apache.camel.quarkus.component.netty.http.auth;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class JaasLoginModule implements LoginModule {
    private CallbackHandler callbackHandler;

    @Override
    public void initialize(
            Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        Callback[] callbacks = new Callback[1];
        callbacks[0] = new PasswordCallback("password", false);

        try {
            callbackHandler.handle(callbacks);
            char[] tmpPassword = ((PasswordCallback) callbacks[0]).getPassword();
            String password = new String(tmpPassword);
            ((PasswordCallback) callbacks[0]).clearPassword();
            if (!"adminjaaspass".equals(password)) {
                throw new LoginException("Login denied");
            }
        } catch (Exception e) {
            LoginException le = new LoginException(e.getMessage());
            le.initCause(e);
            throw le;
        }

        return true;
    }

    @Override
    public boolean commit() {
        return true;
    }

    @Override
    public boolean abort() {
        return true;
    }

    @Override
    public boolean logout() {
        callbackHandler = null;
        return true;
    }
}
