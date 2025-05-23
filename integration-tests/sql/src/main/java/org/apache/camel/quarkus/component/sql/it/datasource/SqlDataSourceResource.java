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
package org.apache.camel.quarkus.component.sql.it.datasource;

import java.util.Map;

import javax.sql.DataSource;

import io.quarkus.arc.ClientProxy;
import io.quarkus.arc.InjectableBean;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelContext;
import org.apache.camel.component.sql.SqlEndpoint;

@Path("/sql/datasource")
public class SqlDataSourceResource {
    @Inject
    CamelContext camelContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSqlEndpoint(@QueryParam("dataSourceRef") String dataSourceRef) {
        try {
            String uri = "sql:SELECT * FROM some_table";
            if (dataSourceRef != null) {
                uri += "?dataSource=#" + dataSourceRef;
            }

            SqlEndpoint endpoint = camelContext.getEndpoint(uri, SqlEndpoint.class);
            DataSource dataSource = endpoint.getDataSource();

            InjectableBean<?> bean = ((ClientProxy) dataSource).arc_bean();
            String beanName = bean.getName() == null ? "" : bean.getName();
            Map<String, Object> response = Map.of(
                    "name", beanName,
                    "default", bean.getQualifiers().contains(Default.Literal.INSTANCE));

            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }
}
