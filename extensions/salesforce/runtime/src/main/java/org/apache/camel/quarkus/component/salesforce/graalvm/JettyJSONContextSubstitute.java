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
package org.apache.camel.quarkus.component.salesforce.graalvm;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.List;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.cometd.common.JettyJSONContext;
import org.eclipse.jetty.util.ajax.JSON;

@TargetClass(JettyJSONContext.class)
final class JettyJSONContextSubstitute {

    @Alias
    private List adapt(Object object) {
        return null;
    }

    @Substitute
    public List parse(String json) throws ParseException {
        try {
            //the type of the field `_messagesParser` is  `FieldJSON`, which is a private class.
            // Therefore, I can not alias the field and it has to be gained reflectively
            Field messagesParserField = JettyJSONContext.class.getDeclaredField("_messagesParser");
            messagesParserField.setAccessible(true);
            JSON messagesParser = (JSON) messagesParserField.get(this);
            Object object = messagesParser.parse(new JSON.StringSource(json));
            return adapt(object);
        } catch (Exception x) {
            throw (ParseException) new ParseException(json, -1).initCause(x);
        }
    }
}
