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
package org.apache.camel.quarkus.component.as2.it.transport;

public class ClientResult {

    boolean dispositionNotificationMultipartReportEntity;
    boolean multipartSignedEntity;
    boolean signedEntityReceived;

    int partsCount;

    String secondPartClassName;

    public boolean isDispositionNotificationMultipartReportEntity() {
        return dispositionNotificationMultipartReportEntity;
    }

    public void setDispositionNotificationMultipartReportEntity(boolean dispositionNotificationMultipartReportEntity) {
        this.dispositionNotificationMultipartReportEntity = dispositionNotificationMultipartReportEntity;
    }

    public int getPartsCount() {
        return partsCount;
    }

    public void setPartsCount(int partsCount) {
        this.partsCount = partsCount;
    }

    public String getSecondPartClassName() {
        return secondPartClassName;
    }

    public void setSecondPartClassName(String secondPartClassName) {
        this.secondPartClassName = secondPartClassName;
    }

    public boolean isMultipartSignedEntity() {
        return multipartSignedEntity;
    }

    public void setMultipartSignedEntity(boolean multipartSignedEntity) {
        this.multipartSignedEntity = multipartSignedEntity;
    }

    public boolean isSignedEntityReceived() {
        return signedEntityReceived;
    }

    public void setSignedEntityReceived(boolean signedEntityReceived) {
        this.signedEntityReceived = signedEntityReceived;
    }
}
