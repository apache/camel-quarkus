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

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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

    @Path("/search/{direct}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(@PathParam("direct") String directName,
            @QueryParam("ldapQuery") String ldapQuery) throws Exception {
        return Response.ok(searchByUid(directName, ldapQuery)).build();
    }

    /**
     * Reports a single entry of the JNDI environment bound for the named dir context, so that a test can assert
     * which properties the extension actually put there. Answers {@code <absent>} when the key was not set.
     */
    @Path("/dirContextEnv/{name}/{key}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response dirContextEnv(@PathParam("name") String name, @PathParam("key") String key) {
        Hashtable<?, ?> env = camelContext.getRegistry().lookupByNameAndType(name, Hashtable.class);
        if (env == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Object value = env.get(key);
        return Response.ok(value == null ? "<absent>" : value.toString()).build();
    }

    @Path("/safeSearch")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response safeSearch(@QueryParam("ldapQuery") String ldapQuery) throws Exception {
        return Response.ok(searchByUid("http", LdapHelper.escapeFilter(ldapQuery))).build();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> searchByUid(String directName, String uid) throws Exception {
        String filter = String.format("(uid=%s)", uid);
        ProducerTemplate producer = camelContext.createProducerTemplate();
        List<SearchResult> results = producer.requestBody("direct:" + directName, filter, List.class);
        return convertSearchResults(results);
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
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();

        for (SearchResult searchResult : searchResults) {
            Map<String, String> resultMap = new HashMap<String, String>();
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
