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
package org.apache.camel.quarkus.support.xalan.graal;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.xalan.xsltc.trax.TransformerFactoryImpl;

@TargetClass(className = "javax.xml.transform.TransformerFactory")
final class TransformerFactorySubstitution {
    @Substitute
    public static TransformerFactory newDefaultInstance() {
        return new TransformerFactoryImpl();
    }

    @Substitute
    public static TransformerFactory newInstance() throws TransformerFactoryConfigurationError {
        return newDefaultInstance();
    }
}
