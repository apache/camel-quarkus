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

/**
 * Class is not present in the dependency (because of classifier), but is referenced from some classes. (but not
 * accessed)
 */
public class AS400ImplNative {

    static byte[] signonNative(byte[] userId) throws NativeException {
        throw new UnsupportedOperationException();
    }

    static void swapToNative(byte[] userId, byte[] bytes, byte[] swapToPH, byte[] swapFromPH) throws NativeException {
        throw new UnsupportedOperationException();
    }

    static void swapBackNative(byte[] swapToPH, byte[] swapFromPH) throws NativeException {
        throw new UnsupportedOperationException();
    }
}
