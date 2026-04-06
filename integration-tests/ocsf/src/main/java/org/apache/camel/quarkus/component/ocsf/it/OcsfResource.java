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
package org.apache.camel.quarkus.component.ocsf.it;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.dataformat.ocsf.OcsfConstants;
import org.apache.camel.dataformat.ocsf.model.Attack;
import org.apache.camel.dataformat.ocsf.model.DetectionFinding;
import org.apache.camel.dataformat.ocsf.model.FindingInfo;
import org.apache.camel.dataformat.ocsf.model.Metadata;
import org.apache.camel.dataformat.ocsf.model.OcsfEvent;
import org.apache.camel.dataformat.ocsf.model.Product;
import org.apache.camel.dataformat.ocsf.model.Remediation;
import org.apache.camel.dataformat.ocsf.model.ResourceDetails;
import org.apache.camel.dataformat.ocsf.model.Tactic;
import org.apache.camel.dataformat.ocsf.model.Technique;

@Path("/ocsf")
@ApplicationScoped
public class OcsfResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    ObjectMapper objectMapper;

    @Path("/marshal/event")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response marshalEvent(String message) {
        OcsfEvent event = new OcsfEvent();
        event.setClassUid(OcsfConstants.CLASS_DETECTION_FINDING);
        event.setCategoryUid(OcsfConstants.CATEGORY_FINDINGS);
        event.setActivityId(OcsfConstants.ACTIVITY_CREATE);
        event.setSeverityId(OcsfConstants.SEVERITY_HIGH);
        event.setTime(System.currentTimeMillis());
        event.setMessage(message);

        String json = producerTemplate.requestBody("direct:marshal-event", event, String.class);
        return Response.ok(json).build();
    }

    @Path("/unmarshal/event")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response unmarshalEvent(String json) {
        OcsfEvent event = producerTemplate.requestBody("direct:unmarshal-event", json, OcsfEvent.class);
        return Response.ok(event.getMessage()).build();
    }

    @Path("/marshal/finding")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response marshalFinding(String title) {
        DetectionFinding finding = new DetectionFinding();
        finding.setIsAlert(true);

        FindingInfo info = new FindingInfo();
        info.setTitle(title);
        info.setDesc("Test security finding");
        finding.setFindingInfo(info);

        String json = producerTemplate.requestBody("direct:marshal-finding", finding, String.class);
        return Response.ok(json).build();
    }

    @Path("/unmarshal/finding")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response unmarshalFinding(String json) {
        DetectionFinding finding = producerTemplate.requestBody("direct:unmarshal-finding", json, DetectionFinding.class);
        return Response.ok(finding.getFindingInfo().getTitle()).build();
    }

    @Path("/roundtrip/event")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response roundtripEvent(String message) {
        // Create event, marshal to JSON, unmarshal back to event
        OcsfEvent event = new OcsfEvent();
        event.setMessage(message);
        event.setSeverityId(OcsfConstants.SEVERITY_MEDIUM);
        event.setTime(System.currentTimeMillis());

        String json = producerTemplate.requestBody("direct:marshal-event", event, String.class);
        OcsfEvent result = producerTemplate.requestBody("direct:unmarshal-event", json, OcsfEvent.class);
        return Response.ok(result.getMessage()).build();
    }

    @Path("/roundtrip/finding")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response roundtripFinding(String title) {
        // Create finding via JSON, unmarshal to DetectionFinding, marshal back, unmarshal again
        String inputJson = String.format("""
                {
                    "finding_info": {
                        "title": "%s"
                    },
                    "is_alert": true
                }
                """, title);

        DetectionFinding finding = producerTemplate.requestBody("direct:unmarshal-finding", inputJson,
                DetectionFinding.class);
        String json = producerTemplate.requestBody("direct:marshal-finding", finding, String.class);
        DetectionFinding result = producerTemplate.requestBody("direct:unmarshal-finding", json, DetectionFinding.class);

        return Response.ok(result.getFindingInfo().getTitle()).build();
    }

    @Path("/unmarshal/unknown-properties")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response unmarshalWithUnknownProperties(String json) {
        OcsfEvent event = producerTemplate.requestBody("direct:unmarshal-event", json, OcsfEvent.class);
        Map<String, Object> additionalProps = event.getAdditionalProperties();
        if (additionalProps != null && additionalProps.containsKey("unknown_property")) {
            return Response.ok(additionalProps.get("unknown_property").toString()).build();
        }
        return Response.ok("no_unknown_properties").build();
    }

    @Path("/marshal/complex-event")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response marshalComplexEvent(String message) {
        OcsfEvent event = new OcsfEvent();
        event.setClassUid(OcsfConstants.CLASS_DETECTION_FINDING);
        event.setClassName("Detection Finding");
        event.setCategoryUid(OcsfConstants.CATEGORY_FINDINGS);
        event.setCategoryName("Findings");
        event.setActivityId(OcsfConstants.ACTIVITY_CREATE);
        event.setSeverityId(OcsfConstants.SEVERITY_HIGH);
        event.setTime(System.currentTimeMillis());
        event.setMessage(message);

        String json = producerTemplate.requestBody("direct:marshal-event", event, String.class);
        return Response.ok(json).build();
    }

    @Path("/marshal/complex-finding")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response marshalComplexFinding(String title) {
        DetectionFinding finding = new DetectionFinding();
        finding.setAdditionalProperty("activity_id", OcsfConstants.ACTIVITY_CREATE);
        finding.setAdditionalProperty("severity_id", OcsfConstants.SEVERITY_CRITICAL);
        finding.setAdditionalProperty("time", System.currentTimeMillis() / 1000);
        finding.setAdditionalProperty("class_uid", OcsfConstants.CLASS_DETECTION_FINDING);
        finding.setIsAlert(true);
        finding.setRiskLevelId(Integer.valueOf(4));
        finding.setRiskLevel("High");
        finding.setConfidence("High");
        finding.setConfidenceScore(90);

        FindingInfo info = new FindingInfo();
        info.setUid("finding-123");
        info.setTitle(title);
        info.setDesc("Test complex finding");
        finding.setFindingInfo(info);

        String json = producerTemplate.requestBody("direct:marshal-finding", finding, String.class);
        return Response.ok(json).build();
    }

    @Path("/parse/complete-finding")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response parseCompleteFinding() {
        try {
            InputStream is = getClass().getResourceAsStream("/ocsf-detection-finding-example.json");
            if (is == null) {
                return Response.serverError().entity("Resource /ocsf-detection-finding-example.json not found").build();
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            DetectionFinding finding = producerTemplate.requestBody("direct:unmarshal-finding", json,
                    DetectionFinding.class);

            Map<String, Object> result = new HashMap<>();
            result.put("is_alert", finding.getIsAlert());
            result.put("risk_level", finding.getRiskLevel());
            result.put("confidence", finding.getConfidence());
            result.put("title", finding.getFindingInfo().getTitle());

            if (finding.getFindingInfo().getAttacks() != null && !finding.getFindingInfo().getAttacks().isEmpty()) {
                Attack attack = finding.getFindingInfo().getAttacks().get(0);
                result.put("tactic_name", attack.getTactic().getName());
                result.put("technique_uid", attack.getTechnique().getUid());
            }

            if (finding.getResources() != null && !finding.getResources().isEmpty()) {
                result.put("resource_name", finding.getResources().get(0).getName());
            }

            if (finding.getRemediation() != null) {
                result.put("has_remediation", true);
            }

            return Response.ok(result).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @Path("/build/complex-finding")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response buildComplexFinding(String title) {
        DetectionFinding finding = new DetectionFinding();

        finding.setAdditionalProperty("class_uid", OcsfConstants.CLASS_DETECTION_FINDING);
        finding.setAdditionalProperty("class_name", "Detection Finding");
        finding.setAdditionalProperty("category_uid", OcsfConstants.CATEGORY_FINDINGS);
        finding.setAdditionalProperty("category_name", "Findings");
        finding.setAdditionalProperty("activity_id", OcsfConstants.ACTIVITY_CREATE);
        finding.setAdditionalProperty("activity_name", "Create");
        finding.setAdditionalProperty("severity_id", OcsfConstants.SEVERITY_HIGH);
        finding.setAdditionalProperty("severity", "High");
        finding.setAdditionalProperty("time", 1706198400L);
        finding.setAdditionalProperty("message", "Security finding detected");

        finding.setIsAlert(true);
        finding.setRiskLevel("High");
        finding.setRiskLevelId(Integer.valueOf(4));
        finding.setRiskScore(85);
        finding.setConfidence("High");
        finding.setConfidenceId(Integer.valueOf(3));
        finding.setConfidenceScore(90);

        FindingInfo info = new FindingInfo();
        info.setUid("finding-001");
        info.setTitle(title);
        info.setDesc("Test security finding with complete details");
        info.setTypes(Arrays.asList("Application/Injection", "TTPs/Initial Access"));
        finding.setFindingInfo(info);

        Attack attack = new Attack();
        Tactic tactic = new Tactic();
        tactic.setName("Initial Access");
        tactic.setUid("TA0001");
        attack.setTactic(tactic);

        Technique technique = new Technique();
        technique.setName("Exploit Public-Facing Application");
        technique.setUid("T1190");
        attack.setTechnique(technique);
        attack.setVersion("14.0");
        info.setAttacks(Arrays.asList(attack));

        Remediation remediation = new Remediation();
        remediation.setDesc("Investigate and remediate the security finding");
        remediation.setReferences(Arrays.asList("https://example.com/remediation"));
        finding.setRemediation(remediation);

        ResourceDetails resource = new ResourceDetails();
        resource.setUid("resource-001");
        resource.setName("test-resource");
        resource.setType("Test::Resource");
        finding.setResources(Arrays.asList(resource));

        Metadata metadata = new Metadata();
        metadata.setVersion("1.8.0");
        Product product = new Product();
        product.setName("Test Product");
        product.setVendorName("Test Vendor");
        metadata.setProduct(product);
        finding.setAdditionalProperty("metadata", metadata);

        String json = producerTemplate.requestBody("direct:marshal-finding", finding, String.class);
        return Response.ok(json).build();
    }

    @Path("/parse/generic-event")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response parseGenericEvent(String json) {
        OcsfEvent event = producerTemplate.requestBody("direct:unmarshal-generic", json, OcsfEvent.class);

        Map<String, Object> result = new HashMap<>();
        result.put("class_uid", event.getClassUid());
        result.put("severity_id", event.getSeverityId());
        result.put("message", event.getMessage());

        if (event.getMetadata() != null) {
            result.put("metadata_version", event.getMetadata().getVersion());
            if (event.getMetadata().getProduct() != null) {
                result.put("product_name", event.getMetadata().getProduct().getName());
            }
        }

        result.put("has_additional_properties",
                event.getAdditionalProperties() != null && !event.getAdditionalProperties().isEmpty());

        return Response.ok(result).build();
    }

    @Path("/filter/severity")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response filterBySeverity(String json) {
        try {
            // Parse JSON to read severity_id directly
            JsonNode node = objectMapper.readTree(json);
            int severityId = node.has("severity_id") ? node.get("severity_id").asInt(1) : 1;

            // Simulate AWS Security Hub pattern: route based on severity
            // severity_id: 1=Other, 2=Informational, 3=Low, 4=Medium, 5=High, 6=Critical
            if (severityId >= 4) {
                return Response.ok("high-severity").build();
            } else {
                return Response.ok("normal-severity").build();
            }
        } catch (Exception e) {
            return Response.serverError().entity("Error parsing JSON: " + e.getMessage()).build();
        }
    }

    @Path("/marshal/pretty")
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response marshalPretty(String message) {
        OcsfEvent event = new OcsfEvent();
        event.setAdditionalProperty("class_uid", OcsfConstants.CLASS_DETECTION_FINDING);
        event.setAdditionalProperty("severity_id", OcsfConstants.SEVERITY_MEDIUM);
        event.setAdditionalProperty("time", System.currentTimeMillis());
        event.setMessage(message);

        String json = producerTemplate.requestBody("direct:marshal-pretty", event, String.class);
        return Response.ok(json).build();
    }
}
