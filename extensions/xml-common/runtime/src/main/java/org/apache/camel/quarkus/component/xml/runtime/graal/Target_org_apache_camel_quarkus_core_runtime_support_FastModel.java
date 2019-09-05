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
package org.apache.camel.quarkus.component.xml.runtime.graal;

import java.io.InputStream;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultModel;
import org.apache.camel.model.ModelHelper;
import org.apache.camel.model.RoutesDefinition;

@TargetClass(className = "org.apache.camel.quarkus.core.runtime.support.FastModel")
@Substitute
public class Target_org_apache_camel_quarkus_core_runtime_support_FastModel extends DefaultModel {

    public Target_org_apache_camel_quarkus_core_runtime_support_FastModel(CamelContext camelContext) {
        super(camelContext);
    }

    @Substitute
    public void addRouteDefinitions(InputStream is) throws Exception {
        RoutesDefinition def = ModelHelper.loadRoutesDefinition(getCamelContext(), is);
        if (def != null) {
            this.addRouteDefinitions(def.getRoutes());
        }

    }

}
