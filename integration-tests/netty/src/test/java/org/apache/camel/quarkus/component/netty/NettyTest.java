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
package org.apache.camel.quarkus.component.netty;

import java.io.*;
import java.net.Socket;

import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class NettyTest {

    @Test
    public void testPoem() throws IOException {
        final String poem = "Epitaph in Kohima, India marking the WWII Battle of Kohima and Imphal, Burma Campaign - Attributed to John Maxwell Edmonds";
        final String expectedResponse = "When You Go Home, Tell Them Of Us And Say, For Your Tomorrow, We Gave Our Today.";

        try (
            final Socket socket = new Socket("localhost", 8994);
            final PrintWriter outputWriter = new PrintWriter(socket.getOutputStream(), true);
            final BufferedReader inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ) {
            outputWriter.println(poem);
            String response = inputReader.readLine();
            Assertions.assertTrue(response.equalsIgnoreCase(expectedResponse), "Response did not match expected response");
        }



    }

}

