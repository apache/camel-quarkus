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
package org.apache.camel.quarkus.component.groovy.runtime.graal;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.BlockingQueue;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.codehaus.groovy.control.ParserPluginFactory;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.memoize.MemoizeCache;
import org.codehaus.groovy.vmplugin.v8.CacheableCallSite;
import org.codehaus.groovy.vmplugin.v8.IndyInterface;

final class GroovySubstitutions {
}

@TargetClass(className = "org.codehaus.groovy.vmplugin.v8.MethodHandleWrapper")
final class SubstituteMethodHandleWrapper {

    @Alias
    public boolean isCanSetTarget() {
        return false;
    }

    @Alias
    public MethodHandle getCachedMethodHandle() {
        return null;
    }
}

@TargetClass(className = "org.codehaus.groovy.vmplugin.v8.IndyInterface$FallbackSupplier")
final class SubstituteIndyFallbackSupplier {

    @Alias
    SubstituteIndyFallbackSupplier(CacheableCallSite callSite, Class<?> sender, String methodName, int callID,
            Boolean safeNavigation, Boolean thisCall, Boolean spreadCall, Object dummyReceiver, Object[] arguments) {

    }

    @Alias
    SubstituteMethodHandleWrapper get() {
        return null;
    }
}

@TargetClass(CacheableCallSite.class)
final class SubstituteCacheableCallSite {
    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static BlockingQueue<Runnable> CACHE_CLEANER_QUEUE = null;

    @Alias
    public SubstituteMethodHandleWrapper getAndPut(String className,
            MemoizeCache.ValueProvider<? super String, ? extends SubstituteMethodHandleWrapper> valueProvider) {
        return null;
    }

    @Alias
    public SubstituteMethodHandleWrapper put(String name, SubstituteMethodHandleWrapper mhw) {
        return null;
    }

    @Alias
    private void removeAllStaleEntriesOfLruCache() {
    }
}

@TargetClass(IndyInterface.class)
final class SubstituteIndyInterface {

    @Alias
    private static SubstituteMethodHandleWrapper NULL_METHOD_HANDLE_WRAPPER;

    @Substitute
    protected static void invalidateSwitchPoints() {
        throw new UnsupportedOperationException("invalidateSwitchPoints is not supported");
    }

    @Alias
    private static boolean bypassCache(Boolean spreadCall, Object[] arguments) {
        return false;
    }

    @Alias
    private static SubstituteMethodHandleWrapper fallback(CacheableCallSite callSite, Class<?> sender, String methodName,
            int callID, Boolean safeNavigation, Boolean thisCall, Boolean spreadCall, Object dummyReceiver,
            Object[] arguments) {
        return null;
    }

    @Substitute
    public static Object selectMethod(CacheableCallSite callSite, Class<?> sender, String methodName, int callID,
            Boolean safeNavigation, Boolean thisCall, Boolean spreadCall, Object dummyReceiver, Object[] arguments)
            throws Throwable {
        final SubstituteMethodHandleWrapper mhw = fallback(callSite, sender, methodName, callID, safeNavigation, thisCall,
                spreadCall, dummyReceiver, arguments);

        final MethodHandle defaultTarget = callSite.getDefaultTarget();
        if (defaultTarget == callSite.getTarget()) {
            // correct the stale methodhandle in the inline cache of callsite
            // it is important but impacts the performance somehow when cache misses frequently
            Object receiver = arguments[0];
            String key = receiver != null ? receiver.getClass().getName() : "org.codehaus.groovy.runtime.NullObject";
            SubstituteCacheableCallSite cs = (SubstituteCacheableCallSite) (Object) callSite;
            cs.put(key, mhw);
        }

        return mhw.getCachedMethodHandle().invokeExact(arguments);
    }

    @Substitute
    public static Object fromCache(CacheableCallSite callSite, Class<?> sender, String methodName, int callID,
            Boolean safeNavigation, Boolean thisCall, Boolean spreadCall, Object dummyReceiver, Object[] arguments)
            throws Throwable {
        SubstituteIndyFallbackSupplier fallbackSupplier = new SubstituteIndyFallbackSupplier(callSite, sender, methodName,
                callID, safeNavigation, thisCall, spreadCall, dummyReceiver, arguments);

        SubstituteMethodHandleWrapper mhw;
        if (bypassCache(spreadCall, arguments)) {
            mhw = NULL_METHOD_HANDLE_WRAPPER;
        } else {
            Object receiver = arguments[0];
            String receiverClassName = receiver != null ? receiver.getClass().getName()
                    : "org.codehaus.groovy.runtime.NullObject";
            SubstituteCacheableCallSite cs = (SubstituteCacheableCallSite) (Object) callSite;
            mhw = cs.getAndPut(receiverClassName, new MemoizeCache.ValueProvider<String, SubstituteMethodHandleWrapper>() {
                @Override
                public SubstituteMethodHandleWrapper provide(String key) {
                    SubstituteMethodHandleWrapper fbMhw = fallbackSupplier.get();
                    return fbMhw.isCanSetTarget() ? fbMhw : NULL_METHOD_HANDLE_WRAPPER;
                }
            });
        }

        if (NULL_METHOD_HANDLE_WRAPPER == mhw) {
            mhw = fallbackSupplier.get();
        }

        return mhw.getCachedMethodHandle().invokeExact(arguments);
    }
}

@TargetClass(SourceUnit.class)
final class SubstituteSourceUnit {

    @Substitute
    public void convert() {
        throw new UnsupportedOperationException("convert is not supported");
    }
}

@TargetClass(ParserPluginFactory.class)
final class SubstituteParserPluginFactory {
    @Substitute
    public static ParserPluginFactory antlr4() {
        throw new UnsupportedOperationException("Antlr4 parsing is not supported at runtime in native mode");
    }
}
