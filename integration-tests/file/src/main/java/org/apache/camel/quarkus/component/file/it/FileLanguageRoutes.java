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
package org.apache.camel.quarkus.component.file.it;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.builder.RouteBuilder;

@ApplicationScoped
public class FileLanguageRoutes extends RouteBuilder {

    public static final String FILE_LANGUAGE = "fileLanguage";
    public static final String FILE_LANG_TXT_IN = "file-lang-txt-in";
    public static final String FILE_LANG_TXT_OUT = "file-lang-txt-out";
    public static final String FILE_LANG_DATE_IN = "file-lang-date-in";
    public static final String FILE_LANG_DATE_OUT = "file-lang-date-out";

    @Override
    public void configure() {

        from("file://target/" + FILE_LANG_TXT_IN + "?fileName=${file:onlyname.noext}.txt")
                .id(FILE_LANGUAGE + "_txt")
                .to("file://target/" + FILE_LANG_TXT_OUT);

        from("file://target/" + FILE_LANG_DATE_IN)
                .id(FILE_LANGUAGE + "_date")
                .setHeader("myHeader", constant("customValue"))
                .to("file://target/" + FILE_LANG_DATE_OUT
                        + "?fileName=out-${date:now:yyyyMMdd}-${in.header.myHeader}.${file:ext}");
    }

}
