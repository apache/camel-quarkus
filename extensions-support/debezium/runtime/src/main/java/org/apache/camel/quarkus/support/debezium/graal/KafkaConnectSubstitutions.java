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
package org.apache.camel.quarkus.support.debezium.graal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This should go to a separate extension if extensions beyond Debezium start needing it.
 */
class KafkaConnectSubstitutions {
}

@TargetClass(className = "org.apache.kafka.connect.runtime.WorkerConfig")
final class WorkerConfigSubstitutions {
    @Alias
    static void validateHttpResponseHeaderConfig(String config) {
    }

}

@TargetClass(className = "org.apache.kafka.connect.runtime.WorkerConfig$ResponseHttpHeadersValidator")
final class ResponseHttpHeadersValidatorSubstitutions {
    @Substitute
    public void ensureValid(String name, Object value) {
        String strValue = (String) value;
        if (strValue == null || strValue.trim().isEmpty()) {
            return;
        }

        String[] configs = StringUtil.csvSplit(strValue); // handles and removed surrounding quotes
        Arrays.stream(configs).forEach(WorkerConfigSubstitutions::validateHttpResponseHeaderConfig);
    }
}

class StringUtil {

    /**
     * Parse a CSV string using {@link #csvSplit(List, String, int, int)}
     *
     * @param  s The string to parse
     * @return   An array of parsed values.
     */
    public static String[] csvSplit(String s) {
        if (s == null)
            return null;
        return csvSplit(s, 0, s.length());
    }

    /**
     * Parse a CSV string using {@link #csvSplit(List, String, int, int)}
     *
     * @param  s   The string to parse
     * @param  off The offset into the string to start parsing
     * @param  len The len in characters to parse
     * @return     An array of parsed values.
     */
    public static String[] csvSplit(String s, int off, int len) {
        if (s == null)
            return null;
        if (off < 0 || len < 0 || off > s.length())
            throw new IllegalArgumentException();
        List<String> list = new ArrayList<>();
        csvSplit(list, s, off, len);
        return list.toArray(new String[list.size()]);
    }

    enum CsvSplitState {
        PRE_DATA, QUOTE, SLOSH, DATA, WHITE, POST_DATA
    }

    /**
     * Split a quoted comma separated string to a list
     * <p>
     * Handle <a href="https://www.ietf.org/rfc/rfc4180.txt">rfc4180</a>-like
     * CSV strings, with the exceptions:
     * <ul>
     * <li>quoted values may contain double quotes escaped with back-slash
     * <li>Non-quoted values are trimmed of leading trailing white space
     * <li>trailing commas are ignored
     * <li>double commas result in a empty string value
     * </ul>
     *
     * @param  list The Collection to split to (or null to get a new list)
     * @param  s    The string to parse
     * @param  off  The offset into the string to start parsing
     * @param  len  The len in characters to parse
     * @return      list containing the parsed list values
     */
    public static List<String> csvSplit(List<String> list, String s, int off, int len) {
        if (list == null)
            list = new ArrayList<>();
        CsvSplitState state = CsvSplitState.PRE_DATA;
        StringBuilder out = new StringBuilder();
        int last = -1;
        while (len > 0) {
            char ch = s.charAt(off++);
            len--;

            switch (state) {
            case PRE_DATA:
                if (Character.isWhitespace(ch))
                    continue;
                if ('"' == ch) {
                    state = CsvSplitState.QUOTE;
                    continue;
                }

                if (',' == ch) {
                    list.add("");
                    continue;
                }
                state = CsvSplitState.DATA;
                out.append(ch);
                continue;
            case DATA:
                if (Character.isWhitespace(ch)) {
                    last = out.length();
                    out.append(ch);
                    state = CsvSplitState.WHITE;
                    continue;
                }

                if (',' == ch) {
                    list.add(out.toString());
                    out.setLength(0);
                    state = CsvSplitState.PRE_DATA;
                    continue;
                }
                out.append(ch);
                continue;

            case WHITE:
                if (Character.isWhitespace(ch)) {
                    out.append(ch);
                    continue;
                }

                if (',' == ch) {
                    out.setLength(last);
                    list.add(out.toString());
                    out.setLength(0);
                    state = CsvSplitState.PRE_DATA;
                    continue;
                }

                state = CsvSplitState.DATA;
                out.append(ch);
                last = -1;
                continue;
            case QUOTE:
                if ('\\' == ch) {
                    state = CsvSplitState.SLOSH;
                    continue;
                }
                if ('"' == ch) {
                    list.add(out.toString());
                    out.setLength(0);
                    state = CsvSplitState.POST_DATA;
                    continue;
                }
                out.append(ch);
                continue;

            case SLOSH:
                out.append(ch);
                state = CsvSplitState.QUOTE;
                continue;

            case POST_DATA:
                if (',' == ch) {
                    state = CsvSplitState.PRE_DATA;
                    continue;
                }
                continue;
            }
        }
        switch (state) {
        case PRE_DATA:
        case POST_DATA:
            break;
        case DATA:
        case QUOTE:
        case SLOSH:
            list.add(out.toString());
            break;

        case WHITE:
            out.setLength(last);
            list.add(out.toString());
            break;
        }

        return list;
    }

}
