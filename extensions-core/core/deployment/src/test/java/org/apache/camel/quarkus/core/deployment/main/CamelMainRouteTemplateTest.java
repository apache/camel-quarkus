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
package org.apache.camel.quarkus.core.deployment.main;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import io.quarkus.test.QuarkusUnitTest;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.quarkus.main.CamelMain;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class CamelMainRouteTemplateTest {
    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(applicationProperties(), "application.properties"));

    @Inject
    CamelMain main;

    public static Asset applicationProperties() {
        Writer writer = new StringWriter();

        Properties props = new Properties();
        props.setProperty("quarkus.banner.enabled", "false");

        try {
            props.store(writer, "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new StringAsset(writer.toString());
    }

    //@Test
    public void testRouteTemplate() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("foo", "one");
        parameters.put("greeting", "Camel");
        main.getCamelContext().addRouteFromTemplate("first", "myTemplate", parameters);

        parameters.put("foo", "two");
        parameters.put("greeting", "World");
        main.getCamelContext().addRouteFromTemplate("second", "myTemplate", parameters);

        assertThat(main.getCamelContext().getRoutes()).isNotEmpty();

        FluentProducerTemplate p = main.getCamelContext().createFluentProducerTemplate();
        String out1 = p.withBody("body1").to("direct:one").request(String.class);
        String out2 = p.withBody("body2").to("direct:two").request(String.class);
        assertThat(out1).isEqualTo("Hello Camel");
        assertThat(out2).isEqualTo("Hello World");
    }

    @Named("my-template")
    @ApplicationScoped
    public static class MyTemplate extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            routeTemplate("myTemplate").templateParameter("foo").templateParameter("greeting")
                    .description("Route saying {{greeting}}")
                    .from("direct:{{foo}}")
                    .transform(simple("Hello {{greeting}}"));
        }
    }

}
