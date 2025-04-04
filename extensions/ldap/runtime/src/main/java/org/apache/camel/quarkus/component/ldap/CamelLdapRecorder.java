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
package org.apache.camel.quarkus.component.ldap;

import java.util.Hashtable;

import javax.naming.Context;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.CamelContext;

@Recorder
public class CamelLdapRecorder {

    public void createDirContexts(RuntimeValue<CamelContext> contextRuntimeValue, final CamelLdapConfig config) {
        CamelContext context = contextRuntimeValue.getValue();

        config.dirContexts().keySet().forEach(contextName -> {

            CamelLdapConfig.LdapDirContextConfig dirConfig = config.dirContexts().get(contextName);

            Hashtable<String, Object> env = new Hashtable<String, Object>();
            dirConfig.initialContextFactory().ifPresent(v -> env.put(Context.INITIAL_CONTEXT_FACTORY, v));
            dirConfig.providerUrl().ifPresent(v -> env.put(Context.PROVIDER_URL, v));
            env.put(Context.SECURITY_AUTHENTICATION, dirConfig.securityAuthentication());
            dirConfig.securityProtocol().ifPresent(v -> env.put(Context.SECURITY_PROTOCOL, v));
            dirConfig.socketFactory().ifPresent(v -> env.put("java.naming.ldap.factory.socket", v));

            //additional options
            dirConfig.additionalOptions().entrySet().forEach(e -> env.put(e.getKey(), e.getValue()));

            context.getRegistry().bind(contextName, env);
        });
    }
}
