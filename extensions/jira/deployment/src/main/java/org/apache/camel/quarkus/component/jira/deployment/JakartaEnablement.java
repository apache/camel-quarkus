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
package org.apache.camel.quarkus.component.jira.deployment;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import org.eclipse.transformer.action.ActionContext;
import org.eclipse.transformer.action.ByteData;
import org.eclipse.transformer.action.impl.ActionContextImpl;
import org.eclipse.transformer.action.impl.ByteDataImpl;
import org.eclipse.transformer.action.impl.ClassActionImpl;
import org.eclipse.transformer.action.impl.SelectionRuleImpl;
import org.eclipse.transformer.action.impl.SignatureRuleImpl;
import org.eclipse.transformer.util.FileUtils;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jira REST client is compiled using references to classes in the javax packages;
 * we need to transform these to fix compatibility with jakarta packages.
 * We do this by leveraging the Eclipse Transformer project during Augmentation, so
 * that end users don't need to bother.
 *
 * This class must be removed when https://ecosystem.atlassian.net/browse/JRJC-262
 * gets resolved.
 */
public class JakartaEnablement {

    private static final List<String> CLASSES_NEEDING_TRANSFORMATION = List.of(
            "com.atlassian.jira.rest.client.internal.json.JsonObjectParser",
            "com.atlassian.jira.rest.client.internal.json.IssueJsonParser",
            "com.atlassian.jira.rest.client.internal.json.RoleActorJsonParser",

            "com.atlassian.jira.rest.client.internal.async.AsynchronousAuditRestClient",
            "com.atlassian.jira.rest.client.internal.async.AsynchronousGroupRestClient",
            "com.atlassian.jira.rest.client.internal.async.AsynchronousIssueRestClient",
            "com.atlassian.jira.rest.client.internal.async.AsynchronousMetadataRestClient",
            "com.atlassian.jira.rest.client.internal.async.AsynchronousMyPermissionsRestClient",
            "com.atlassian.jira.rest.client.internal.async.AsynchronousProjectRestClient",
            "com.atlassian.jira.rest.client.internal.async.AsynchronousProjectRolesRestClient",
            "com.atlassian.jira.rest.client.internal.async.AsynchronousVersionRestClient",
            "com.atlassian.jira.rest.client.internal.async.AsynchronousUserRestClient",
            "com.atlassian.jira.rest.client.internal.async.AsynchronousSearchRestClient",
            "com.atlassian.jira.rest.client.internal.async.AsynchronousSessionRestClient",
            "com.atlassian.jira.rest.client.internal.async.AsynchronousComponentRestClient",
            "com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClient");

    @BuildStep
    void transformToJakarta(BuildProducer<BytecodeTransformerBuildItem> transformers) {
        if (QuarkusClassLoader.isClassPresentAtRuntime("jakarta.ws.rs.Path")) {
            JakartaTransformer tr = new JakartaTransformer();
            for (String classname : CLASSES_NEEDING_TRANSFORMATION) {
                final BytecodeTransformerBuildItem item = new BytecodeTransformerBuildItem.Builder()
                        .setCacheable(true)
                        .setContinueOnFailure(false)
                        .setClassToTransform(classname)
                        .setClassReaderOptions(ClassReader.SKIP_DEBUG)
                        .setInputTransformer(tr::transform)
                        .build();
                transformers.produce(item);
            }
        }
    }

    private static class JakartaTransformer {

        private final Logger logger;
        private final ActionContext ctx;
        // We need to prevent the Eclipse Transformer to adjust the "javax" packages.
        // Thus why we split the strings.
        private static final Map<String, String> renames = Map.of("javax" + ".ws.rs.core", "jakarta.ws.rs.core");

        JakartaTransformer() {
            logger = LoggerFactory.getLogger("JakartaTransformer");
            //N.B. we enable only this single transformation of package renames, not the full set of capabilities of Eclipse Transformer;
            //this might need tailoring if the same idea gets applied to a different context.
            ctx = new ActionContextImpl(logger,
                    new SelectionRuleImpl(logger, Collections.emptyMap(), Collections.emptyMap()),
                    new SignatureRuleImpl(logger, renames, null, null, null, null, null, Collections.emptyMap()));
        }

        byte[] transform(final String name, final byte[] bytes) {
            logger.debug("Jakarta EE compatibility enhancer for Quarkus: transforming " + name);
            final ClassActionImpl classTransformer = new ClassActionImpl(ctx);
            final ByteBuffer input = ByteBuffer.wrap(bytes);
            final ByteData inputData = new ByteDataImpl(name, input, FileUtils.DEFAULT_CHARSET);
            final ByteData outputData = classTransformer.apply(inputData);
            return outputData.buffer().array();
        }
    }

}
