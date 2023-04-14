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
package org.apache.camel.quarkus.component.ldap.it;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.ldap.LdapHelper;

@Path("/ldap")
@ApplicationScoped
public class LdapResource {

    @Inject
    CamelContext camelContext;

    /**
     * Extracts the LDAP connection parameters passed from the test and creates a
     * {@link javax.naming.directory.DirContext} from them.
     * The DirContext is then bound into the CamelContext for use in the LDAP route.
     * 
     * @param  options
     * @throws Exception
     */
    @Path("/configure")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void configure(Map<String, String> options) throws Exception {
        String host = options.get("host");
        String port = options.get("port");
        boolean useSSL = Boolean.valueOf(options.get("ssl"));
        String trustStoreFilename = options.get("trustStore");
        String trustStorePassword = options.get("trustStorePassword");

        DirContext dirContext = createLdapContext(host, port, useSSL, trustStoreFilename, trustStorePassword);
        camelContext.getRegistry().bind("ldapserver", dirContext);
    }

    @Path("/search")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(@QueryParam("q") String filter) throws Exception {
        ProducerTemplate producer = camelContext.createProducerTemplate();
        List<SearchResult> results = producer.requestBody("direct:start", filter, List.class);
        return Response.ok(convertSearchResults(results)).build();
    }

    @Path("/safeSearch")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response safeSearch(@QueryParam("q") String unsafeFilter) throws Exception {
        String filter = String.format("(ou=%s)", LdapHelper.escapeFilter(unsafeFilter));
        ProducerTemplate producer = camelContext.createProducerTemplate();
        List<SearchResult> results = producer.requestBody("direct:start", filter, List.class);

        return Response.ok(convertSearchResults(results)).build();
    }

    private DirContext createLdapContext(String host, String port, boolean useSSL, String trustStoreFilename,
            String trustStorePassword)
            throws Exception {
        String scheme = useSSL ? "ldaps" : "ldap";
        Hashtable<String, Object> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, String.format("%s://%s:%s", scheme, host, port));
        env.put(Context.SECURITY_AUTHENTICATION, "none");

        if (useSSL) {
            CustomSSLSocketFactory.setTrustStore(trustStoreFilename, trustStorePassword);
            env.put("java.naming.ldap.factory.socket", CustomSSLSocketFactory.class.getName());
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }

        return new InitialDirContext(env);
    }

    /**
     * Converts the list of {@link javax.naming.directory.SearchResult} objects into
     * a structure that Jackson can
     * serialize into JSON.
     * 
     * @param  searchResults
     * @return
     * @throws Exception
     */
    private List<Map<String, String>> convertSearchResults(List<SearchResult> searchResults) throws Exception {
        List<Map<String, String>> results = new ArrayList<>();

        for (SearchResult searchResult : searchResults) {
            Map<String, String> resultMap = new HashMap<>();
            NamingEnumeration<? extends Attribute> attrs = searchResult.getAttributes().getAll();
            while (attrs.hasMore()) {
                Attribute attr = attrs.next();
                resultMap.put(attr.getID(), attr.get().toString());
            }
            results.add(resultMap);
        }

        return results;
    }
}
