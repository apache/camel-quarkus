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

import javax.xml.bind.JAXBContext;

import com.oracle.svm.core.annotate.KeepOriginal;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.RestConfiguration;

/**
 * TODO: we should not use a substitution here, so we need to find a better way to plug
 *   our own modified reifier
 */
@TargetClass(className = "org.apache.camel.reifier.rest.RestBindingReifier")
final class Target_org_apache_camel_reifier_rest_RestBindingReifier {

    @Substitute
    protected void setupJaxb(CamelContext context, RestConfiguration config, String type, String outType, DataFormat jaxb, DataFormat outJaxb) throws Exception {
        Class<?> clazz = null;
        if (type != null) {
            String typeName = type.endsWith("[]") ? type.substring(0, type.length() - 2) : type;
            clazz = context.getClassResolver().resolveMandatoryClass(typeName);
        }
        if (clazz != null) {
            JAXBContext jc = JAXBContext.newInstance(clazz);
            context.adapt(ExtendedCamelContext.class).getBeanIntrospection().setProperty(context, jaxb, "context", jc);
        }
        setAdditionalConfiguration(config, context, jaxb, "xml.in.");

        Class<?> outClazz = null;
        if (outType != null) {
            String typeName = outType.endsWith("[]") ? outType.substring(0, outType.length() - 2) : outType;
            outClazz = context.getClassResolver().resolveMandatoryClass(typeName);
        }
        if (outClazz != null) {
            JAXBContext jc = JAXBContext.newInstance(outClazz);
            context.adapt(ExtendedCamelContext.class).getBeanIntrospection().setProperty(context, outJaxb, "context", jc);
        } else if (clazz != null) {
            // fallback and use the context from the input
            JAXBContext jc = JAXBContext.newInstance(clazz);
            context.adapt(ExtendedCamelContext.class).getBeanIntrospection().setProperty(context, outJaxb, "context", jc);
        }
        setAdditionalConfiguration(config, context, outJaxb, "xml.out.");
    }

    @KeepOriginal
    private void setAdditionalConfiguration(RestConfiguration config, CamelContext context, DataFormat dataFormat, String prefix) throws Exception {
    }

}
