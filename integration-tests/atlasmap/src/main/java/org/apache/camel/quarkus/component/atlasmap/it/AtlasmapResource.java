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
package org.apache.camel.quarkus.component.atlasmap.it;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.atlasmap.core.DefaultAtlasContextFactory;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.quarkus.component.atlasmap.it.model.Person;

import static io.atlasmap.api.AtlasContextFactory.PROPERTY_ATLASMAP_CORE_VERSION;

@Path("/atlasmap")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AtlasmapResource {

    @Inject
    ProducerTemplate producerTemplate;

    @GET
    @Path("json/java2json")
    public String convertJava2JsonWithJson(Person person) {
        return producerTemplate.requestBody("atlasmap:mapping/json/atlasmapping-java-to-json.json", person, String.class);
    }

    @GET
    @Path("json/json2java")
    public Person convertJson2JavaWithJson(String json) {
        return producerTemplate.requestBody("atlasmap:mapping/json/atlasmapping-json-to-java.json", json, Person.class);
    }

    @GET
    @Path("json/xml2xml")
    public String convertXml2XmlWithJson(String xml) {
        return producerTemplate.requestBody("atlasmap:mapping/json/atlasmapping-xml-to-xml.json", xml, String.class);
    }

    @GET
    @Path("adm/xml2xml")
    public String convertXml2XmlWithAdm(String xml) {
        return producerTemplate.requestBody("atlasmap:mapping/adm/atlasmapping-xml-to-xml.adm", xml, String.class);
    }

    @GET
    @Path("json/json2xml")
    public String convertJson2XmlWithJson(String xml) {
        return producerTemplate.requestBody("atlasmap:mapping/json/atlasmapping-json-to-xml.json", xml, String.class);
    }

    @GET
    @Path("adm/json2xml")
    public String convertJson2XmlWithAdm(String xml) {
        return producerTemplate.requestBody("atlasmap:mapping/adm/atlasmapping-json-to-xml.adm", xml, String.class);
    }

    @GET
    @Path("json/xml2json")
    public String convertXml2JsonWithJson(String json) {
        return producerTemplate.requestBody("atlasmap:mapping/json/atlasmapping-xml-to-json.json", json, String.class);
    }

    @GET
    @Path("adm/xml2json")
    public String convertXml2JsonWithAdm(String json) {
        return producerTemplate.requestBody("atlasmap:mapping/adm/atlasmapping-xml-to-json.adm", json, String.class);
    }

    @GET
    @Path("json/java2xml")
    public String convertJava2XmlWithJson(Person person) {
        return producerTemplate.requestBody("atlasmap:mapping/json/atlasmapping-java-to-xml.json", person, String.class);
    }

    @GET
    @Path("json/xml2java")
    public Person convertXml2JavaWithJson(String xml) {
        return producerTemplate.requestBody("atlasmap:mapping/json/atlasmapping-xml-to-java.json", xml, Person.class);
    }

    @GET
    @Path("adm/json2json")
    public String convertJson2JsonWithJson(String json) {
        return producerTemplate.requestBody("atlasmap:mapping/adm/atlasmapping-json-to-json.adm", json, String.class);
    }

    @GET
    @Path("json/json2csv")
    public String convertJson2CsvWithJson(String json) {
        return producerTemplate.requestBody("atlasmap:mapping/json/atlasmapping-json-to-csv.json", json, String.class);
    }

    @GET
    @Path("adm/csv2json")
    public String convertCsv2JsonWithAdm(String csv) {
        return producerTemplate.requestBody("atlasmap:mapping/adm/atlasmapping-csv-to-json.adm", csv, String.class);
    }

    @GET
    @Path("json/csv2json")
    public String convertCsv2JsonWithJson(String csv) {
        return producerTemplate.requestBody("atlasmap:mapping/json/atlasmapping-csv-to-json.json", csv, String.class);
    }

    @GET
    @Path("json/csv2xml")
    public String convertCsv2XmlWithJson(String csv) {
        return producerTemplate.requestBody("atlasmap:mapping/json/atlasmapping-csv-to-xml.json", csv, String.class);
    }

    @GET
    @Path("json/xml2csv")
    public String convertXml2CsvWithJson(String xml) {
        return producerTemplate.requestBody("atlasmap:mapping/json/atlasmapping-xml-to-csv.json", xml, String.class);
    }

    @GET
    @Path("version")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        DefaultAtlasContextFactory factory = DefaultAtlasContextFactory.getInstance();
        Map<String, String> properties = factory.getProperties();
        return properties.get(PROPERTY_ATLASMAP_CORE_VERSION);
    }
}
