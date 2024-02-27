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

public class MockAS400 extends AS400 {

    private final MockAS400ImplRemote as400ImplRemote;

    public MockAS400(MockAS400ImplRemote as400ImplRemote) {
        this.as400ImplRemote = as400ImplRemote;
    }

    @Override
    public AS400Impl getImpl() {
        return as400ImplRemote;
    }

    @Override
    public int getCcsid() {
        //ConvTable37 depends on this value
        return 37;
    }

    @Override
    public boolean isConnected(int service) {
        //always connected
        return true;
    }

    @Override
    public void connectService(int service, int overridePort) throws AS400SecurityException, IOException {
        //connection to real i server is ignored
        setSignonInfo(-1, -1, "username");
    }

    @Override
    synchronized void signon(boolean keepConnection) throws AS400SecurityException, IOException {
        //do nothing
    }

    @Override
    public int getVRM() throws AS400SecurityException, IOException {
        return 1;
    }
}
