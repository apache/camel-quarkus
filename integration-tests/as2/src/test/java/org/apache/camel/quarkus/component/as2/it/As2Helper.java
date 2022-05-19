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
package org.apache.camel.quarkus.component.as2.it;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.component.as2.api.AS2EncryptionAlgorithm;
import org.apache.camel.component.as2.api.AS2MediaType;
import org.apache.camel.component.as2.api.AS2MessageStructure;
import org.apache.camel.component.as2.api.AS2SignatureAlgorithm;
import org.apache.camel.quarkus.component.as2.it.transport.Request;
import org.apache.http.entity.ContentType;

public class As2Helper {

    public static final String AS2_VERSION = "1.1";
    public static final String REQUEST_URI = "/";
    public static final String SUBJECT = "Test Case";
    public static final String AS2_NAME = "878051556";
    public static final String FROM = "mrAS@example.org";
    public static final String[] SIGNED_RECEIPT_MIC_ALGORITHMS = new String[] { "sha1", "md5" };
    public static final String DISPOSITION_NOTIFICATION_TO = FROM;
    public static final String EDI_MESSAGE = "UNB+UNOA:1+005435656:1+006415160:1+060515:1434+00000000000778'\n"
            + "UNH+00000000000117+INVOIC:D:97B:UN'\n"
            + "BGM+380+342459+9'\n"
            + "DTM+3:20060515:102'\n"
            + "RFF+ON:521052'\n"
            + "NAD+BY+792820524::16++CUMMINS MID-RANGE ENGINE PLANT'\n"
            + "NAD+SE+005435656::16++GENERAL WIDGET COMPANY'\n"
            + "CUX+1:USD'\n"
            + "LIN+1++157870:IN'\n"
            + "IMD+F++:::WIDGET'\n"
            + "QTY+47:1020:EA'\n"
            + "ALI+US'\n"
            + "MOA+203:1202.58'\n"
            + "PRI+INV:1.179'\n"
            + "LIN+2++157871:IN'\n"
            + "IMD+F++:::DIFFERENT WIDGET'\n"
            + "QTY+47:20:EA'\n"
            + "ALI+JP'\n"
            + "MOA+203:410'\n"
            + "PRI+INV:20.5'\n"
            + "UNS+S'\n"
            + "MOA+39:2137.58'\n"
            + "ALC+C+ABG'\n"
            + "MOA+8:525'\n"
            + "UNT+23+00000000000117'\n"
            + "UNZ+1+00000000000778'";

    private static final String EDI_MESSAGE_CONTENT_TRANSFER_ENCODING = "7bit";

    private As2Helper() {
    }

    public static Request createPlainRequest() {
        final Map<String, Object> headers = createBaseHeaders(AS2MessageStructure.PLAIN);

        return new Request()
                .withHeaders(headers)
                .withEdiMessage(EDI_MESSAGE);
    }

    public static Request createEncryptedRequest() {
        final Map<String, Object> headers = createBaseHeaders(AS2MessageStructure.ENCRYPTED);

        return new Request()
                .withHeaders(headers)
                .withEdiMessage(EDI_MESSAGE)
                .withEncryptionAlgorithm(AS2EncryptionAlgorithm.AES128_CBC);
    }

    public static Request createMultipartSignedRequest() {
        final Map<String, Object> headers = createBaseHeaders(AS2MessageStructure.SIGNED);
        // parameter type is String[]
        headers.put("CamelAS2.signedReceiptMicAlgorithms", SIGNED_RECEIPT_MIC_ALGORITHMS);

        return new Request()
                .withHeaders(headers)
                .withEdiMessage(EDI_MESSAGE)
                .withSigningAlgorithm(AS2SignatureAlgorithm.SHA512WITHRSA);
    }

    private static Map<String, Object> createBaseHeaders(AS2MessageStructure plain) {
        final Map<String, Object> headers = new HashMap<>();
        // parameter type is String
        headers.put("CamelAS2.requestUri", REQUEST_URI);
        // parameter type is String
        headers.put("CamelAS2.subject", SUBJECT);
        // parameter type is String
        headers.put("CamelAS2.from", FROM);
        // parameter type is String
        headers.put("CamelAS2.as2From", AS2_NAME);
        // parameter type is String
        headers.put("CamelAS2.as2To", AS2_NAME);
        // parameter type is org.apache.camel.component.as2.api.AS2MessageStructure
        headers.put("CamelAS2.as2MessageStructure", plain);
        // parameter type is org.apache.http.entity.ContentType
        headers.put("CamelAS2.ediMessageContentType",
                ContentType.create(AS2MediaType.APPLICATION_EDIFACT, StandardCharsets.US_ASCII.name()));
        // parameter type is String
        headers.put("CamelAS2.ediMessageTransferEncoding", EDI_MESSAGE_CONTENT_TRANSFER_ENCODING);
        // parameter type is String
        headers.put("CamelAS2.dispositionNotificationTo", DISPOSITION_NOTIFICATION_TO);
        return headers;
    }

}
