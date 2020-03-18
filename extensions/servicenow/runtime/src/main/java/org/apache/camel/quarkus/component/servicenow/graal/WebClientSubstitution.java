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
package org.apache.camel.quarkus.component.servicenow.graal;

import java.util.ArrayList;
import java.util.List;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.bus.extension.Extension;
import org.apache.cxf.bus.extension.ExtensionRegistry;
import org.apache.cxf.bus.managers.ClientLifeCycleManagerImpl;
import org.apache.cxf.bus.managers.ConduitInitiatorManagerImpl;
import org.apache.cxf.bus.managers.PhaseManagerImpl;
import org.apache.cxf.endpoint.ClientLifeCycleManager;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.http.HTTPTransportFactory;

@TargetClass(WebClient.class)
final public class WebClientSubstitution {

    @Substitute
    static JAXRSClientFactoryBean getBean(String baseAddress, String configLocation) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        // configLocation is always null and no need to create SpringBusFactory.
        CXFBusFactory bf = new CXFBusFactory();

        // It can not load the extensions from the bus-extensions.txt dynamically.
        // So have to set all of necessary ones here.
        List<Extension> extensions = new ArrayList<>();
        Extension http = new Extension();
        http.setClassname(HTTPTransportFactory.class.getName());
        http.setDeferred(true);
        extensions.add(http);
        ExtensionRegistry.addExtensions(extensions);

        Bus bus = bf.createBus();
        bus.setExtension(new PhaseManagerImpl(), PhaseManager.class);
        bus.setExtension(new ClientLifeCycleManagerImpl(), ClientLifeCycleManager.class);
        bus.setExtension(new ConduitInitiatorManagerImpl(bus), ConduitInitiatorManager.class);

        bean.setBus(bus);
        bean.setAddress(baseAddress);
        return bean;
    }
}
