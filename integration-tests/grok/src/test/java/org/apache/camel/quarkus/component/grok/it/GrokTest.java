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
package org.apache.camel.quarkus.component.grok.it;

import io.krakens.grok.api.exception.GrokException;
import io.quarkus.test.junit.QuarkusTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class GrokTest {

    private static final String LOGS = ""
            + "64.242.88.10 - - [07/Mar/2004:16:05:49 -0800] \"GET /twiki/bin/edit/Main/Double_bounce_sender?topicparent=Main.ConfigurationVariables HTTP/1.1\" 401 12846\n"
            + "64.242.88.11 - - [07/Mar/2004:16:06:51 -0800] \"GET /twiki/bin/rdiff/TWiki/NewUserTemplate?rev1=1.3&rev2=1.2 HTTP/1.1\" 200 4523\n"
            + "64.242.88.12 - - [07/Mar/2004:16:10:02 -0800] \"GET /mailman/listinfo/hsdivision HTTP/1.1\" 200 6291\n"
            + "64.242.88.13 - - [07/Mar/2004:16:11:58 -0800] \"GET /twiki/bin/view/TWiki/WikiSyntax HTTP/1.1\" 200 7352\n"
            + "64.242.88.14 - - [07/Mar/2004:16:20:55 -0800] \"GET /twiki/bin/view/Main/DCCAndPostFix HTTP/1.1\" 200 5253\n";

    //@Test
    public void grokLogShouldCaptureFifthIp() {
        given().body(LOGS).get("/grok/log").then().statusCode(200).body(is("ip: 64.242.88.14"));
    }

    //@Test
    public void grokFooBarShouldCaptureCenterFoosAndBars() {
        final String fooBar = "bar foobar bar -- barbarfoobarfoobar -- barbar";
        given().body(fooBar).get("/grok/fooBar").then().statusCode(200).body(is("-- barbarfoobarfoobar --"));
    }

    //@Test
    public void grokSpaceDelimitedIpsShouldCaptureFirstAndFourthIps() {
        final String ips = "178.21.82.201 178.21.82.202 178.21.82.203 178.21.82.204";
        given().body(ips).get("/grok/ip").then().statusCode(200).body(is("178.21.82.201 -> 178.21.82.204"));
    }

    //@Test
    public void grokMixDelimitedIpsShouldCaptureFirstAndFourthIps() {
        final String ips = "178.21.82.101 178.21.82.102\n178.21.82.103\r\n178.21.82.104";
        given().body(ips).get("/grok/ip").then().statusCode(200).body(is("178.21.82.101 -> 178.21.82.104"));
    }

    //@Test
    public void grokQsShouldCaptureQuotedString() {
        final String qs = "this is some \"quoted string\".";
        given().body(qs).get("/grok/qs").then().statusCode(200).body(is("quoted string"));
    }

    //@Test
    public void grokUuidShouldCaptureUuidAtEnd() {
        final String uuid = "some 123e4567-e89b-12d3-a456-426614174000";
        given().body(uuid).get("/grok/uuid").then().statusCode(200).body(is("123e4567-e89b-12d3-a456-426614174000"));
    }

    //@Test
    public void grokMacShouldCaptureMacAddressAtEnd() {
        final String mac = "some:invalid:prefix:of:eth0:02:00:4c:4f:4f:50";
        given().body(mac).get("/grok/mac").then().statusCode(200).body(is("02:00:4c:4f:4f:50"));
    }

    //@Test
    public void grokPathShouldCaptureMntRelativePath() {
        final String path = "The file with path /home/user/../../mnt has been deleted";
        given().body(path).get("/grok/path").then().statusCode(200).body(is("/home/user/../../mnt"));
    }

    //@Test
    public void grokUriShouldCaptureCamelSiteUri() {
        final String uri = "the site is at https://camel.apache.org/";
        given().body(uri).get("/grok/uri").then().statusCode(200).body(is("https://camel.apache.org/"));
    }

    //@Test
    public void grokNumberShouldCapture123() {
        final String number = "number is 123.";
        given().body(number).get("/grok/num").then().statusCode(200).body(is("123"));
    }

    //@Test
    public void grokTimestampShouldCaptureMay26th() {
        final String timestamp = "This test was created at 2019-05-26T10:54:15Z test convert";
        given().body(timestamp).get("/grok/timestamp").then().statusCode(200).body(is("2019-05-26T10:54:15Z"));
    }

    //@Test
    public void grokFlattenShouldReturnGrokExceptionClassName() {
        final String expected = GrokException.class.getName();
        given().body("1 2").get("/grok/flatten").then().statusCode(200).body(is(expected));
    }

    //@Test
    public void grokNamedOnlyShouldNotCaptureUnamedExpressions() {
        final String body = "https://github.com/apache/camel";
        given().body(body).get("/grok/namedOnly").then().statusCode(200).body(is("false-false+false"));
    }

    //@Test
    public void grokSingleMathPerLineShouldCapture1AndThen3() {
        given().body("1 2 \n 3").get("/grok/singleMatchPerLine").then().statusCode(200).body(is("1-3"));
    }

}
