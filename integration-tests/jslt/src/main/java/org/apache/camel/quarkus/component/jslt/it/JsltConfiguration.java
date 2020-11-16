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
package org.apache.camel.quarkus.component.jslt.it;

import javax.inject.Named;

import com.schibsted.spt.data.jslt.Expression;
import com.schibsted.spt.data.jslt.Parser;
import com.schibsted.spt.data.jslt.filters.JsltJsonFilter;
import org.apache.camel.component.jslt.JsltComponent;

import static java.util.Collections.singleton;

import static com.schibsted.spt.data.jslt.FunctionUtils.wrapStaticMethod;

public class JsltConfiguration {

    @Named
    JsltComponent jsltWithFilter() {
        Expression filterExpression = Parser.compileString(". != null and . != {}");
        JsltJsonFilter filter = new JsltJsonFilter(filterExpression);

        JsltComponent component = new JsltComponent();
        component.setObjectFilter(filter);

        return component;
    }

    @Named
    JsltComponent jsltWithFunction() throws ClassNotFoundException {
        JsltComponent component = new JsltComponent();
        component.setFunctions(
                singleton(wrapStaticMethod("power", "org.apache.camel.quarkus.component.jslt.it.MathFunctionStub", "pow")));
        component.setAllowTemplateFromHeader(true);

        return component;
    }

    public static Ping createInfiniteRecursionObject() {
        Ping ping = new Ping();
        Pong pong = new Pong();
        ping.pong = pong;
        pong.ping = ping;
        return ping;
    }

    private static class Ping {
        @SuppressWarnings("unused")
        private Pong pong;
    }

    private static class Pong {
        @SuppressWarnings("unused")
        private Ping ping;
    }

}
