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
package org.apache.camel.quarkus.component.smb.deployment;

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
 * Required due to net.engio.mbassy.dispatch.el.ElFilter referencing javax.el packages.
 *
 * TODO: Remove this class if this issue is resolved: https://github.com/bennidi/mbassador/issues/169
 */
public class JakartaEnablement {
    private static final List<String> CLASSES_NEEDING_TRANSFORMATION = List.of(
            "net.engio.mbassy.dispatch.el.ElFilter",
            "net.engio.mbassy.dispatch.el.ElFilter$ExpressionFactoryHolder",
            "net.engio.mbassy.dispatch.el.StandardELResolutionContext");

    @BuildStep
    void transformToJakarta(BuildProducer<BytecodeTransformerBuildItem> transformers) {
        if (QuarkusClassLoader.isClassPresentAtRuntime("jakarta.el.ELContext")) {
            JakartaTransformer tr = new JakartaTransformer();
            for (String className : CLASSES_NEEDING_TRANSFORMATION) {
                final BytecodeTransformerBuildItem item = new BytecodeTransformerBuildItem.Builder()
                        .setCacheable(true)
                        .setContinueOnFailure(false)
                        .setClassToTransform(className)
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
        private static final Map<String, String> renames = Map.of("javax" + ".el", "jakarta.el");

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
