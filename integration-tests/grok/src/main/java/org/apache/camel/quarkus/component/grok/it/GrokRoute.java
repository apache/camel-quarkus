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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.grok.GrokDataFormat;
import org.apache.camel.component.grok.GrokPattern;
import org.apache.camel.spi.DataFormat;

@ApplicationScoped
public class GrokRoute extends RouteBuilder {

    @Produces
    @Named("myAnotherCustomPatternBean")
    GrokPattern myAnotherCustomPatternBean = new GrokPattern("FOOBAR_WITH_PREFIX_AND_SUFFIX", "-- %{FOOBAR}+ --");

    @Produces
    @Named("myCustomPatternBean")
    GrokPattern myCustomPatternBean = new GrokPattern("FOOBAR", "foo|bar");

    @Override
    public void configure() {

        from("direct:log").unmarshal().grok("%{COMMONAPACHELOG}").setBody(simple("ip: ${body[4][clientip]}"));
        from("direct:fooBar").unmarshal().grok("%{FOOBAR_WITH_PREFIX_AND_SUFFIX:fooBar}").setBody(simple("${body[fooBar]}"));
        from("direct:ip").unmarshal().grok("%{IP:ip}").setBody(simple("${body[0][ip]} -> ${body[3][ip]}"));

        from("direct:qs").unmarshal().grok("%{QS:qs}").setBody(simple("${body[qs]}"));
        from("direct:uuid").unmarshal().grok("%{UUID:uuid}").setBody(simple("${body[uuid]}"));
        from("direct:mac").unmarshal().grok("%{MAC:mac}").setBody(simple("${body[mac]}"));
        from("direct:path").unmarshal().grok("%{PATH:path}").setBody(simple("${body[path]}"));
        from("direct:uri").unmarshal().grok("%{URI:uri}").setBody(simple("${body[uri]}"));
        from("direct:num").unmarshal().grok("%{NUMBER:num}").setBody(simple("${body[num]}"));
        from("direct:timestamp").unmarshal().grok("%{TIMESTAMP_ISO8601:timestamp}").setBody(simple("${body[timestamp]}"));

        DataFormat flattenDf = new GrokDataFormat("%{INT:i} %{INT:i}").setFlattened(true);
        from("direct:flatten").unmarshal(flattenDf);

        DataFormat namedOnlyDf = new GrokDataFormat("%{URI:website}").setNamedOnly(true);
        from("direct:namedOnly").unmarshal(namedOnlyDf);

        DataFormat singleMatchPerLineDf = new GrokDataFormat("%{INT:i}").setAllowMultipleMatchesPerLine(false);
        from("direct:singleMatchPerLine").unmarshal(singleMatchPerLineDf).setBody(simple("${body[0][i]}-${body[1][i]}"));
    }

}
