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
package org.apache.camel.quarkus.component.kubernetes.it;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.kubernetes.KubernetesConstants;
import org.apache.camel.component.kubernetes.KubernetesOperations;

@Path("/kubernetes/job")
@ApplicationScoped
public class KubernetesJobResource {

    @Inject
    ProducerTemplate producerTemplate;

    @Path("/{namespace}/{jobName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Job getJob(
            @PathParam("namespace") String namespace,
            @PathParam("jobName") String jobName) {

        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-job");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_JOB_NAME, jobName);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.GET_JOB_OPERATION);
        return producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Job.class);
    }

    @Path("/{namespace}/{jobName}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response createJob(
            @PathParam("namespace") String namespace,
            @PathParam("jobName") String jobName,
            Job job) throws Exception {

        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-job");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_JOB_NAME, jobName);
        headers.put(KubernetesConstants.KUBERNETES_JOB_SPEC, job.getSpec());
        headers.put(KubernetesConstants.KUBERNETES_JOB_LABELS, Map.of("app", jobName));
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.CREATE_JOB_OPERATION);

        Job createdJob = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Job.class);
        return Response
                .created(new URI("https://camel.apache.org/"))
                .entity(createdJob)
                .build();
    }

    @Path("/{namespace}/{jobName}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateJob(
            @PathParam("namespace") String namespace,
            @PathParam("jobName") String jobName,
            Job job) {

        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-job");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_JOB_NAME, jobName);
        headers.put(KubernetesConstants.KUBERNETES_JOB_SPEC, job.getSpec());
        headers.put(KubernetesConstants.KUBERNETES_JOB_LABELS, Map.of("app", jobName));
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.UPDATE_JOB_OPERATION);

        Job updatedJob = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, Job.class);
        return Response.ok().entity(updatedJob).build();
    }

    @Path("/{namespace}/{jobName}")
    @DELETE
    public Response deleteJob(
            @PathParam("namespace") String namespace,
            @PathParam("jobName") String jobName) {

        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-job");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_JOB_NAME, jobName);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.DELETE_JOB_OPERATION);
        producerTemplate.requestBodyAndHeaders("direct:start", null, headers);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @Path("/{namespace}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listJobs(@PathParam("namespace") String namespace) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-job");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.LIST_JOB);
        List<Job> jobList = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, List.class);
        return Response.ok().entity(jobList).build();
    }

    @Path("/labels/{namespace}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response listJobsByLabels(
            @PathParam("namespace") String namespace,
            Map<String, String> labels) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("componentName", "kubernetes-job");
        headers.put(KubernetesConstants.KUBERNETES_NAMESPACE_NAME, namespace);
        headers.put(KubernetesConstants.KUBERNETES_JOB_LABELS, labels);
        headers.put(KubernetesConstants.KUBERNETES_OPERATION, KubernetesOperations.LIST_JOB_BY_LABELS_OPERATION);
        List<Job> jobList = producerTemplate.requestBodyAndHeaders("direct:start", null, headers, List.class);
        return Response.ok().entity(jobList).build();
    }
}
