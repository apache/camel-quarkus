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

import java.io.IOException;

public class MockAS400ImplRemote extends AS400ImplRemote {

    AS400Server getConnection(int service, boolean forceNewConnection,
            boolean skipSignonServer) throws AS400SecurityException, IOException {
        return new MockAS400Server(this);
    }

    @Override
    public String getNLV() {
        return "012345678901234567890123456789";
    }
}
