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
package org.apache.camel.quarkus.core.deployment;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class LanguageExpressionContentHandler extends DefaultHandler {

    private final String languageName;

    private final BiConsumer<String, Boolean> expressionConsumer;
    private boolean inExpression = false;
    private final StringBuilder expressionBuilder = new StringBuilder();
    private final Deque<Map.Entry<String, Attributes>> path = new ArrayDeque<>();

    public LanguageExpressionContentHandler(String languageName, BiConsumer<String, Boolean> expressionConsumer) {
        super();
        this.languageName = languageName;
        this.expressionConsumer = expressionConsumer;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts)
            throws SAXException {
        if (inExpression) {
            throw new IllegalStateException("Unexpected element '" + localName + "' under '" + languageName
                    + "'; only text content is expected");
        }
        if (languageName.equals(localName)) {
            inExpression = true;
        } else {
            path.push(new SimpleImmutableEntry<String, Attributes>(localName, atts));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (languageName.equals(localName)) {
            final String expressionText = expressionBuilder.toString();
            final boolean predicate = isPredicate();
            expressionConsumer.accept(expressionText, predicate);
            expressionBuilder.setLength(0);
            inExpression = false;
        } else {
            path.pop();
        }
    }

    private boolean isPredicate() {
        Entry<String, Attributes> parent = path.peek();
        if (parent != null) {
            return hasSimplePredicateChild(parent.getKey(), attributeName -> {
                final Attributes attribs = parent.getValue();
                if (attribs != null) {
                    return attribs.getValue(attributeName);
                }
                return null;
            });
        }
        return false;
    }

    /**
     * Inspired by {@link org.apache.camel.parser.XmlRouteParser#isSimplePredicate(Node)}.
     *
     * @param  name
     * @param  getAttributeFunction
     * @return
     */
    public static boolean hasSimplePredicateChild(String name, Function<String, String> getAttributeFunction) {

        if (name == null) {
            return false;
        }
        if (name.equals("completionPredicate") || name.equals("completion")) {
            return true;
        }
        if (name.equals("onWhen") || name.equals("when") || name.equals("handled") || name.equals("continued")) {
            return true;
        }
        if (name.equals("retryWhile") || name.equals("filter") || name.equals("validate")) {
            return true;
        }
        // special for loop
        if (name.equals("loop") && "true".equalsIgnoreCase(getAttributeFunction.apply("doWhile"))) {
            return true;
        }
        return false;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (inExpression) {
            expressionBuilder.append(ch, start, length);
        }
    }

}
