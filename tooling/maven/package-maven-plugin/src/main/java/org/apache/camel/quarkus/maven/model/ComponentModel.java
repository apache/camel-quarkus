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
package org.apache.camel.quarkus.maven.model;

public class ComponentModel extends AbstractModel {

    private String scheme;
    private String syntax;
    private String alternativeSyntax;
    private String alternativeSchemes;
    private String consumerOnly;
    private String producerOnly;

    public String getScheme() {
        return scheme;
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    public String getSyntax() {
        return syntax;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

    public String getAlternativeSyntax() {
        return alternativeSyntax;
    }

    public void setAlternativeSyntax(String alternativeSyntax) {
        this.alternativeSyntax = alternativeSyntax;
    }

    public String getAlternativeSchemes() {
        return alternativeSchemes;
    }

    public void setAlternativeSchemes(String alternativeSchemes) {
        this.alternativeSchemes = alternativeSchemes;
    }

    public String getConsumerOnly() {
        return consumerOnly;
    }

    public void setConsumerOnly(String consumerOnly) {
        this.consumerOnly = consumerOnly;
    }

    public String getProducerOnly() {
        return producerOnly;
    }

    public void setProducerOnly(String producerOnly) {
        this.producerOnly = producerOnly;
    }

    @Override
    String getDocLinkSection() {
        return "components";
    }

    @Override
    String getDocLinkDocument() {
        return scheme + "-component.html";
    }

}
