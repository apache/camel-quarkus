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
package org.apache.camel.quarkus.component.atlasmap.graalvm;

import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.slf4j.Logger;

import static com.oracle.svm.core.annotate.RecomputeFieldValue.Kind.Reset;

public class Substitutions {
}

@TargetClass(className = "io.atlasmap.core.DefaultAtlasCompoundClassLoader")
final class DefaultAtlasCompoundClassLoader_Substitute {

    @Alias
    @RecomputeFieldValue(kind = Reset)
    private static Logger LOG = null;

    @Alias
    private Set<ClassLoader> delegates;

    @Substitute
    public Enumeration<URL> getResources(String name) {
        List<URL> answer = new LinkedList<>();
        for (ClassLoader cl : delegates) {
            try {
                Enumeration<URL> urls = cl.getResources(name);
                while (urls != null && urls.hasMoreElements()) {
                    answer.add(urls.nextElement());
                }
            } catch (Exception e) {
                LOG.debug("I/O error while looking for a resource '{}' with ClassLoader '{}': {}", name, cl);
                LOG.debug(e.getMessage(), e);
            }
        }
        return Collections.enumeration(answer);
    }

    @Substitute
    public synchronized void addAlternativeLoader(ClassLoader cl) {
        delegates.add(Thread.currentThread().getContextClassLoader());
    }

}