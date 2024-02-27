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
package com.ibm.as400.access;

public class ReplyDQRequestAttributesNormal extends DQRequestAttributesNormalReplyDataStream {

    private final int keyLength;

    public ReplyDQRequestAttributesNormal(int keyLength) {
        this.keyLength = keyLength;
    }

    @Override
    public int hashCode() {
        return 0x8001;
    }

    @Override
    int getType() {
        //required for keyed
        return 2;
    }

    @Override
    int getMaxEntryLength() {
        return 1;
    }

    @Override
    boolean getSaveSenderInformation() {
        return false;
    }

    @Override
    boolean getForceToAuxiliaryStorage() {
        return false;
    }

    @Override
    byte[] getDescription() {
        return new byte[5];
    }

    @Override
    int getKeyLength() {
        return keyLength;
    }
}
