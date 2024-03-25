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

import java.nio.charset.StandardCharsets;

public class ReplyRCCallProgram extends RCCallProgramReplyDataStream {

    public ReplyRCCallProgram() {
    }

    @Override
    int getRC() {
        //success
        return 0x0000;
    }

    @Override
    void getParameterList(ProgramParameter[] parameterList) {
        //skip
        byte[] str = "hello".repeat(50).getBytes(StandardCharsets.UTF_8);
        //lengths are encoded in the string
        //        int lengthDataReturned = BinaryConverter.byteArrayToInt(data, 152);
        //        int lengthMessageReturned = BinaryConverter.byteArrayToInt(data, 160);
        //        int lengthHelpReturned = BinaryConverter.byteArrayToInt(data, 168);
        str[150] = 0;
        str[151] = 0;
        str[152] = 0;
        str[153] = 0;
        str[154] = 0;
        str[155] = 1;
        str[156] = 0;
        str[157] = 0;
        str[158] = 0;
        str[159] = 0;
        str[160] = 0;
        str[161] = 0;
        str[162] = 0;
        str[163] = 0;
        str[164] = 0;
        str[165] = 0;
        str[166] = 0;
        str[167] = 1;
        str[168] = 0;
        str[169] = 0;
        str[170] = 0;
        str[171] = 0;
        str[172] = 0;
        str[173] = 0;
        str[174] = 0;
        str[175] = 1;
        str[176] = 0;

        parameterList[0].setOutputData(str);
    }
}
