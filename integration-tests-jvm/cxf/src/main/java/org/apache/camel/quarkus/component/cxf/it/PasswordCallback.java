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
package org.apache.camel.quarkus.component.cxf.it;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@RegisterForReflection
@ApplicationScoped
@Named("passwordCallback")
public class PasswordCallback implements CallbackHandler {
    @ConfigProperty(name = "password-callback.username")
    private String username;
    @ConfigProperty(name = "password-callback.password")
    private String password;

    /**
     * Here, we attempt to get the password from the private alias/passwords map.
     */
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            try {
                String id = (String) callback.getClass().getMethod("getIdentifier").invoke(callback);
                String pass = getPassword();
                if (pass != null) {
                    callback.getClass().getMethod("setPassword", String.class).invoke(callback, pass);
                    return;
                }
            } catch (Exception ex) {
                UnsupportedCallbackException e = new UnsupportedCallbackException(callback);
                e.initCause(ex);
                throw e;
            }
        }
    }

    /**
     * Add an alias/password pair to the callback mechanism.
     */
    public void setAliasPassword(String alias, String password) {
        setUsername(alias);
        setPassword(password);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
