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
package org.apache.camel.quarkus.k.support;

import java.util.Collections;
import java.util.List;

import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.StringHelper;

public final class StringSupport {
    private StringSupport() {
    }

    public static String substringBefore(String str, String separator) {
        String answer = StringHelper.before(str, separator);
        if (answer == null) {
            answer = str;
        }

        return answer;
    }

    public static String substringAfter(String str, String separator) {
        String answer = StringHelper.after(str, separator);
        if (answer == null) {
            answer = "";
        }

        return answer;
    }

    public static String substringAfterLast(String str, String separator) {
        if (ObjectHelper.isEmpty(str)) {
            return str;
        }
        if (ObjectHelper.isEmpty(separator)) {
            return "";
        }
        int pos = str.lastIndexOf(separator);
        if (pos == -1 || pos == str.length() - separator.length()) {
            return "";
        }
        return str.substring(pos + separator.length());
    }

    public static String substringBeforeLast(String str, String separator) {
        if (ObjectHelper.isEmpty(str) || ObjectHelper.isEmpty(separator)) {
            return str;
        }
        int pos = str.lastIndexOf(separator);
        if (pos == -1) {
            return str;
        }
        return str.substring(0, pos);
    }

    public static List<String> split(String input, String regex) {
        return input != null ? List.of(input.split(regex)) : Collections.emptyList();
    }
}
