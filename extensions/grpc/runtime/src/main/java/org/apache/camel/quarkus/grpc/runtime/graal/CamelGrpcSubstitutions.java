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
package org.apache.camel.quarkus.grpc.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.CamelContext;
import org.apache.camel.component.grpc.GrpcConsumer;
import org.apache.camel.component.grpc.GrpcEndpoint;
import org.apache.camel.component.grpc.server.BindableServiceFactory;
import org.apache.camel.support.CamelContextHelper;

import static org.apache.camel.component.grpc.GrpcConstants.GRPC_BINDABLE_SERVICE_FACTORY_NAME;

final class CamelGrpcSubstitutions {
}

@TargetClass(GrpcConsumer.class)
final class SubstituteGrpcConsumer {

    @Alias
    protected GrpcEndpoint endpoint;

    @Alias
    private BindableServiceFactory factory;

    @Substitute
    private BindableServiceFactory getBindableServiceFactory() {
        // Remove unwanted references to javassist
        CamelContext context = endpoint.getCamelContext();
        if (this.factory == null) {
            BindableServiceFactory bindableServiceFactory = CamelContextHelper.lookup(context,
                    GRPC_BINDABLE_SERVICE_FACTORY_NAME, BindableServiceFactory.class);
            if (bindableServiceFactory != null) {
                this.factory = bindableServiceFactory;
            }
        }
        return this.factory;
    }
}
