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
package org.apache.camel.quarkus.component.saga.it.lra;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.Header;

@ApplicationScoped
@Named("lraService")
@RegisterForReflection
public class LraService {

    private Boolean completed, xmlCompleted;

    public void sleep(@Header("timeout") long timeout) throws InterruptedException {
        Thread.sleep(timeout);
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setXmlCompleted(boolean xmlCompleted) {
        this.xmlCompleted = xmlCompleted;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void complete() {
        this.completed = true;
    }

    public void xmlComplete() {
        this.xmlCompleted = true;
    }

    public boolean isXmlCompleted() {
        return xmlCompleted;
    }
}
