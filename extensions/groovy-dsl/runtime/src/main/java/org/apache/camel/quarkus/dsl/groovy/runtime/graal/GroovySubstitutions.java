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
package org.apache.camel.quarkus.dsl.groovy.runtime.graal;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Constructor;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.codehaus.groovy.control.ParserPluginFactory;

final class GroovySubstitutions {
}

@TargetClass(className = "org.codehaus.groovy.vmplugin.v8.IndyInterface")
final class SubstituteIndyInterface {

    @Substitute
    protected static void invalidateSwitchPoints() {
        throw new UnsupportedOperationException("invalidateSwitchPoints is not supported");
    }

    @Substitute
    public static Object fromCache(MutableCallSite callSite, Class<?> sender, String methodName, int callID,
            Boolean safeNavigation, Boolean thisCall, Boolean spreadCall, Object dummyReceiver, Object[] arguments) {
        throw new UnsupportedOperationException("fromCache is not supported");
    }
}

@TargetClass(className = "org.codehaus.groovy.control.SourceUnit")
final class SubstituteSourceUnit {

    @Substitute
    public void convert() {
        throw new UnsupportedOperationException("convert is not supported");
    }
}

@TargetClass(className = "org.codehaus.groovy.vmplugin.v8.Java8")
final class SubstituteJava8 {

    @Substitute
    private static Constructor<MethodHandles.Lookup> getLookupConstructor() {
        throw new UnsupportedOperationException("getLookupConstructor is not supported");
    }
}

@TargetClass(className = "org.codehaus.groovy.antlr.AntlrParserPlugin")
@Delete
final class SubstituteAntlrParserPlugin {

}

@TargetClass(className = "org.codehaus.groovy.antlr.AntlrParserPluginFactory")
@Delete
final class SubstituteAntlrParserPluginFactory {

}

@TargetClass(className = "org.codehaus.groovy.control.ParserPluginFactory")
final class SubstituteParserPluginFactory {

    @Substitute
    public static ParserPluginFactory antlr2() {
        throw new UnsupportedOperationException("antlr2 is not supported");
    }
}
