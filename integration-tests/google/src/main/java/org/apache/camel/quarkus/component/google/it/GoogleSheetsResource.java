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
package org.apache.camel.quarkus.component.google.it;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.UpdateSpreadsheetPropertiesRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;

@Path("/google-sheets")
public class GoogleSheetsResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/create")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response createSheet(String title) throws Exception {
        SpreadsheetProperties sheetProperties = new SpreadsheetProperties();
        sheetProperties.setTitle(title);

        Spreadsheet sheet = new Spreadsheet();
        sheet.setProperties(sheetProperties);

        Spreadsheet response = producerTemplate.requestBody("google-sheets://spreadsheets/create?inBody=content", sheet,
                Spreadsheet.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(response.getSpreadsheetId())
                .build();
    }

    @Path("/read")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response readSheet(@QueryParam("spreadsheetId") String spreadsheetId) {
        try {
            Spreadsheet response = producerTemplate.requestBody("google-sheets://spreadsheets/get?inBody=spreadsheetId",
                    spreadsheetId,
                    Spreadsheet.class);
            if (response != null) {
                return Response.ok(response.getProperties().getTitle()).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (CamelExecutionException e) {
            Exception exchangeException = e.getExchange().getException();
            if (exchangeException != null && exchangeException.getCause() instanceof GoogleJsonResponseException) {
                GoogleJsonResponseException originalException = (GoogleJsonResponseException) exchangeException.getCause();
                return Response.status(originalException.getStatusCode()).build();
            }
            throw e;
        }
    }

    @Path("/update")
    @PATCH
    @Consumes(MediaType.TEXT_PLAIN)
    public Response updateSheet(@QueryParam("spreadsheetId") String spreadsheetId, String title) {
        BatchUpdateSpreadsheetRequest request = new BatchUpdateSpreadsheetRequest()
                .setIncludeSpreadsheetInResponse(true)
                .setRequests(Collections
                        .singletonList(new Request().setUpdateSpreadsheetProperties(new UpdateSpreadsheetPropertiesRequest()
                                .setProperties(new SpreadsheetProperties().setTitle(title))
                                .setFields("title"))));

        final Map<String, Object> headers = new HashMap<>();
        headers.put("CamelGoogleSheets.spreadsheetId", spreadsheetId);
        headers.put("CamelGoogleSheets.batchUpdateSpreadsheetRequest", request);
        producerTemplate.requestBodyAndHeaders("google-sheets://spreadsheets/batchUpdate", null, headers);
        return Response.ok().build();
    }

}
