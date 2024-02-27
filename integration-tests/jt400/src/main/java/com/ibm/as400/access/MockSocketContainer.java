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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

public class MockSocketContainer extends SocketContainer {

    ByteArrayOutputStream bOutput = new ByteArrayOutputStream(50);

    byte[] _data = new byte[50];

    public MockSocketContainer() {

        // https://github.com/IBM/JTOpen/blob/98e74fae6d212563a1558abce60ea5c73fcfc0c0/src/main/java/com/ibm/as400/access/ClientAccessDataStream.java#L70
        _data[6] = (byte) 0xE0;

        //sets length to 49
        _data[1] = 0;
        _data[2] = 0;
        _data[3] = '1';

        _data[4] = 0;
        _data[5] = 0;
        _data[7] = 0;
    }

    @Override
    void setProperties(Socket socket, String serviceName, String systemName, int port, SSLOptions options) throws IOException {

    }

    @Override
    void close() throws IOException {

    }

    @Override
    InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(_data);
    }

    @Override
    OutputStream getOutputStream() throws IOException {
        return bOutput;
    }

    @Override
    void setSoTimeout(int timeout) throws SocketException {

    }

    @Override
    int getSoTimeout() throws SocketException {
        return 0;
    }
}
