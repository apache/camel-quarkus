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
package org.apache.camel.quarkus.core.runtime.graal;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

import org.apache.camel.CamelContext;
import org.apache.camel.Producer;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.LoadBalancerDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.transformer.TransformerDefinition;
import org.apache.camel.model.validator.ValidatorDefinition;
import org.apache.camel.reifier.ProcessorReifier;
import org.apache.camel.reifier.dataformat.DataFormatReifier;
import org.apache.camel.reifier.loadbalancer.LoadBalancerReifier;
import org.apache.camel.reifier.transformer.TransformerReifier;
import org.apache.camel.reifier.validator.ValidatorReifier;
import org.apache.camel.support.IntrospectionSupport.ClassInfo;
import org.apache.camel.support.LRUCacheFactory;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;


class CamelSubstitutions {
}

@TargetClass(className = "com.sun.beans.WeakCache")
@Substitute
final class Target_com_sun_beans_WeakCache<K, V> {

    @Substitute
    private Map<K, Reference<V>> map = new WeakHashMap<>();

    @Substitute
    public Target_com_sun_beans_WeakCache() {
    }

    @Substitute
    public V get(K key) {
        Reference<V> reference = this.map.get(key);
        if (reference == null) {
            return null;
        }
        V value = reference.get();
        if (value == null) {
            this.map.remove(key);
        }
        return value;
    }

    @Substitute
    public void put(K key, V value) {
        if (value != null) {
            this.map.put(key, new WeakReference<V>(value));
        } else {
            this.map.remove(key);
        }
    }

    @Substitute
    public void clear() {
        this.map.clear();
    }

}

@TargetClass(className = "java.beans.Introspector")
final class Target_java_beans_Introspector {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Target_com_sun_beans_WeakCache<Class<?>, Method[]> declaredMethodCache = new Target_com_sun_beans_WeakCache<>();

}

@TargetClass(className = "org.apache.camel.support.IntrospectionSupport")
final class Target_org_apache_camel_support_IntrospectionSupport {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Map<Class<?>, ClassInfo> CACHE = LRUCacheFactory.newLRUWeakCache(256);

}

@TargetClass(className = "org.apache.camel.util.HostUtils")
final class Target_org_apache_camel_util_HostUtils {

    @Substitute
    private static InetAddress chooseAddress() throws UnknownHostException {
        return InetAddress.getByName("0.0.0.0");
    }
}

@TargetClass(className = "org.apache.camel.reifier.ProcessorReifier")
final class Target_org_apache_camel_reifier_ProcessorReifier {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Map<Class<?>, Function<ProcessorDefinition<?>, ProcessorReifier<? extends ProcessorDefinition<?>>>> PROCESSORS = null;

}

@TargetClass(className = "org.apache.camel.reifier.dataformat.DataFormatReifier")
final class Target_org_apache_camel_reifier_dataformat_DataFormatReifier {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Map<Class<?>, Function<DataFormatDefinition, DataFormatReifier<? extends DataFormatDefinition>>> DATAFORMATS = null;

}

@TargetClass(className = "org.apache.camel.reifier.loadbalancer.LoadBalancerReifier")
final class Target_org_apache_camel_reifier_loadbalancer_LoadBalancerReifier {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Map<Class<?>, Function<LoadBalancerDefinition, LoadBalancerReifier<? extends LoadBalancerDefinition>>> LOAD_BALANCERS = null;

}

@TargetClass(className = "org.apache.camel.reifier.transformer.TransformerReifier")
final class Target_org_apache_camel_reifier_transformer_TransformerReifier {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Map<Class<?>, Function<TransformerDefinition, TransformerReifier<? extends TransformerDefinition>>> TRANSFORMERS = null;

}

@TargetClass(className = "org.apache.camel.reifier.validator.ValidatorReifier")
final class Target_org_apache_camel_reifier_validator_ValidatorReifier {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Map<Class<?>, Function<ValidatorDefinition, ValidatorReifier<? extends ValidatorDefinition>>> VALIDATORS = null;

}
