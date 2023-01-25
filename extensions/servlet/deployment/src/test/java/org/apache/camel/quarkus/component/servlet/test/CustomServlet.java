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
package org.apache.camel.quarkus.component.servlet.test;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;

@SuppressWarnings("serial")
@WebServlet
public class CustomServlet extends CamelHttpTransportServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) {
        /* Set this header and assert in the test that the request was served by this servlet */
        resp.setHeader("x-servlet-class-name", this.getClass().getName());
        super.service(req, resp);
    }

}
