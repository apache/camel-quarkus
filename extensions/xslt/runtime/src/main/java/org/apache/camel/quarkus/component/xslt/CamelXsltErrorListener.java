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
package org.apache.camel.quarkus.component.xslt;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelXsltErrorListener implements ErrorListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelXsltErrorListener.class);

    @Override
    public void warning(TransformerException e) throws TransformerException {
        LOGGER.warn(e.getMessage(), e);
    }

    @Override
    public void error(TransformerException e) throws TransformerException {
        LOGGER.error(e.getMessage(), e);
    }

    @Override
    public void fatalError(TransformerException e) throws TransformerException {
        LOGGER.error(e.getMessage(), e);
    }
}
