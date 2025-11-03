<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->
# Changelog

## 3.27.1

* [3.27.x] Bump io.quarkiverse.langchain4j:quarkus-langchain4j-bom from 1.2.0.CR2 to 1.2.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7768
* [3.27.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7780
* [3.27.x] Ban io.grpc:grpc-netty-shaded by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7790
* [3.27.x] fixed test password kafka sasl ssl test by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7787
* [3.27.x] Set ES_JAVA_OPTS for ElasticSearch container JVM only tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7794
* [3.27.x] Upgrade Camel to 4.14.1 + Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7802
* [3.27.x] Bump quarkiverse-minio.version from 3.8.5 to 3.8.6 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7809
* [3.27.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7824
* [3.27.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7847
* [3.27.x] Close the class path resource properly after reading from it in BeanioProcessor by @ppalaga in https://github.com/apache/camel-quarkus/pull/7859
* [3.27.x] Disable NATS TLS testing due to #7771 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7888
* [3.27.x] Prevent Swagger Java CodeGen prefixing src/main/java to the generated model source output path by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7919
* [3.27.x] Upgrade Camel to 4.14.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7921

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.27.0...3.27.1

## 3.29.0

* Remove not needed Agroal dependency from Jasypt tests by @llowinge in https://github.com/apache/camel-quarkus/pull/7758
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.8.4 to 3.10.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7759
* Generated sources regen for SBOM by @github-actions[bot] in https://github.com/apache/camel-quarkus/pull/7760
* Bump org.apache.maven.plugins:maven-compiler-plugin from 3.14.0 to 3.14.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7765
* Bump io.quarkiverse.langchain4j:quarkus-langchain4j-bom from 1.2.0.CR2 to 1.2.0.CR3 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7764
* Bump org.apache.maven.plugins:maven-javadoc-plugin from 3.11.3 to 3.12.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7763
* Next is 3.29.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7762
* Bump io.quarkiverse.langchain4j:quarkus-langchain4j-bom from 1.2.0.CR3 to 1.2.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7767
* Add langchain4j-embeddings native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7769
* Bump org.apache.maven.plugins:maven-scm-plugin from 2.1.0 to 2.2.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7777
* Bump cq-plugin.version from 4.20.0 to 4.20.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7776
* Add retry logic to Debezium Oracle container startup by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7772
* Avoid using docker.io for images pulled by Quarkus dev services by @ppalaga in https://github.com/apache/camel-quarkus/pull/7774
* Use mirror.gcr.io for testing OpenTelemetry by @ppalaga in https://github.com/apache/camel-quarkus/pull/7775
* Test camel-quarkus-activemq against Artemis broker by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7778
* Set ElasticSearch container ES_JAVA_OPTS and disable disk-based shard allocation thresholds by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7779
* Generated sources regen for SBOM by @github-actions[bot] in https://github.com/apache/camel-quarkus/pull/7782
* Update Maven wrapper distribution URL to Maven 3.9.11 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7781
* Ban io.grpc:grpc-netty-shaded by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7783
* Add quarkus-netty to google-storage extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7791
* Fixes #7785 -respect min size of test password for KafkaSaslSslTest by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7786
* Bump quarkiverse-groovy.version from 3.26.3 to 3.28.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7788
* Set ES_JAVA_OPTS for ElasticSearch container JVM only tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7793
* Bump org.codehaus.mojo:exec-maven-plugin from 3.5.1 to 3.6.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7795
* Bump quarkiverse-cxf.version from 3.27.0 to 3.27.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7797
* Upgrade Maven wrapper from 3.2.0 to 3.3.4 by @apupier in https://github.com/apache/camel-quarkus/pull/7800
* [Quarkus CXF 3.27.0] Fail the build if there are overlaps between our BOM and Quarkus BOM by @ppalaga in https://github.com/apache/camel-quarkus/pull/7738
* Upgrade Camel to 4.14.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7784
* Updates for dependency convergence CI check by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7804
* Bump quarkiverse-groovy.version from 3.28.1 to 3.28.2 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7805
* Bump quarkiverse-minio.version from 3.8.5 to 3.8.6 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7807
* Bump org.codehaus.mojo:exec-maven-plugin from 3.6.0 to 3.6.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7810
* Bump io.swagger.codegen.v3:swagger-codegen-generators from 1.0.57 to 1.0.58 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7811
* Camel 4.15.0 upgrade by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7808
* Bump com.microsoft.graph:microsoft-graph from 6.53.0 to 6.54.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7814
* Remove unecessary code, as the image is already set in application.pr… by @llowinge in https://github.com/apache/camel-quarkus/pull/7815
* fix: doc groovy-xml invalid format by @gansheer in https://github.com/apache/camel-quarkus/pull/7817
* Add LangChain4j EmbeddingStore support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7818
* Bump cq-plugin.version from 4.20.1 to 4.20.3 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7822
* Remove superfluous System.out.println calls by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7823
* Generated sources regen for SBOM by @github-actions[bot] in https://github.com/apache/camel-quarkus/pull/7825
* Auto close dependency convergence issue when the workflow is successful by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7827
* Use AsciiDoc attribute for langchain4j BOM version in extension code snippets by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7829
* Enlarge timeout for Master Openshift IT by @llowinge in https://github.com/apache/camel-quarkus/pull/7828
* Fix Dev UI fetching of Camel console JSON data by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7831
* Bump io.swagger.codegen.v3:swagger-codegen-generators from 1.0.58 to 1.0.59 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7833
* Bump quarkiverse-groovy.version from 3.28.2 to 3.28.3 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7834
* Upgrade Quarkus LangChain4j to 1.2.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7835
* Fix SNAPSHOT deploy CI build by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7837
* Add tests and support for langchain4j-agent custom tools by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7836
* Fix path to qwc-camel-core.js in Micrometer & MicroProfile Fault Tolerance Dev UI pages by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7840
* Upgrade Quarkus to 3.29.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7842
* Bump quarkiverse-jsch.version from 3.1.0 to 3.1.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7844
* Bump io.quarkiverse.jgit:quarkus-jgit-bom from 3.6.0 to 3.6.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7845
* Update Dev UI documentation with the correct path to qwc-camel-core.js by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7846
* Refactor Splunk test container by @llowinge in https://github.com/apache/camel-quarkus/pull/7848
* Bump org.jolokia:jolokia-agent-jvm from 2.3.0 to 2.4.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7849
* Generated sources regen for SBOM by @github-actions[bot] in https://github.com/apache/camel-quarkus/pull/7850
* Clean up usage of deprecated Quarkus configuration options by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7852
* Bump org.codehaus.mojo:exec-maven-plugin from 3.6.1 to 3.6.2 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7853
* Bump org.apache.maven.plugins:maven-antrun-plugin from 3.1.0 to 3.2.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7855
* Close the class path resource properly after reading from it in BeanioProcessor by @ppalaga in https://github.com/apache/camel-quarkus/pull/7858
* Upgrade Quarkus to 3.29.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7863
* Fixes #7860: more logging and tweaking timouts for jt400 real tests by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7861
* Configure com.google.protobuf.JavaFeaturesProto for runtime initialization in native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7866
* Add docling & keycloak JVM only extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7869
* Replace deprecated WeaviateVectorDb.Headers by WeaviateVectorDbHeaders by @apupier in https://github.com/apache/camel-quarkus/pull/7871
* Replace deprecated Aws DefaultCredentialsProvider.create() by @apupier in https://github.com/apache/camel-quarkus/pull/7873

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.27.0...3.29.0

## 3.20.3

* [3.20.x] Fix SplunkTest for running with remote docker test container by @llowinge in https://github.com/apache/camel-quarkus/pull/7515
* [3.20.x] Upgrade Quarkus to 3.20.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7528
* [3.20.x] Register SimpleSearchTerm for reflection by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7552
* [3.20.x] Fallback to resolving bean names from @Identifier for RuntimeBeanRepository.findByTypeWithName by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7561
* [3.20.x] Wiremock Olingo4 test by @llowinge in https://github.com/apache/camel-quarkus/pull/7626
* [3.20.x] Avoid configuring JasyptPropertiesParser unless encrypted properties are detected by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7636
* [3.20.x] Use 4.10.x for components doc xrefs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7664
* [3.20.x] Add type check for non-synthetic beans in RuntimeBeanRepository.getReferenceByName by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7690
* [3.20.x] Upgrade Quarkus to 3.20.3 + backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7751
* [3.20.x] Upgrade Camel to 4.10.7 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7770
* [3.20.x] Ban io.grpc:grpc-netty-shaded by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7789
* [3.20.x] Upgrade quarkus-minio to 3.8.6 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7857

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.20.2...3.20.3

## 3.26.0

## What's Changed
* fix(azure-storage): Shared key credentials type requires credentials … by @avano in https://github.com/apache/camel-quarkus/pull/7544
* [fixes #7547] Register SimpleSearchTerm for reflection, as Camel conf… by @llowinge in https://github.com/apache/camel-quarkus/pull/7549
* Generated sources regen for SBOM by @github-actions[bot] in https://github.com/apache/camel-quarkus/pull/7550
* Fix some deprecated API usage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7548
* Next is 3.26.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7551
* Avoid using LaunchMode static method by @gsmet in https://github.com/apache/camel-quarkus/pull/7553
* Document and apply workaround for Groovy native compilation issues #7361 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7554
* Mail-microsoft-oauth: clear messages after the test by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7556
* Bump ibm.mq.client.version from 9.4.2.0 to 9.4.3.0 by @vkasala in https://github.com/apache/camel-quarkus/pull/7557
* Add changelog for 3.25.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7560
* Fallback to resolving bean names from @Identifier for RuntimeBeanRepository.findByTypeWithName by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7559
* Bump com.microsoft.graph:microsoft-graph from 6.47.0 to 6.48.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7564
* Deprecate / remove some redundant config and test modules by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7566
* Move main-devmode test module to integration-tests-jvm by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7570
* Stabilize throttle test by @llowinge in https://github.com/apache/camel-quarkus/pull/7563
* Fix disabled test in CoreMainTest by @llowinge in https://github.com/apache/camel-quarkus/pull/7571
* Add note to @Disabled query test in InfinispanTest by @llowinge in https://github.com/apache/camel-quarkus/pull/7573
* Generated sources regen for SBOM by @github-actions[bot] in https://github.com/apache/camel-quarkus/pull/7577
* Enable disabled As2Test#clientMultipartSignedTest by @llowinge in https://github.com/apache/camel-quarkus/pull/7576
* Upgrade Jolokia to 2.3.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7578
* Upgrade Quarkus Jackson JQ to 2.3.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7579
* [fix #2407] Enable Bindy native test FixedLengthWithLocaleIT by @llowinge in https://github.com/apache/camel-quarkus/pull/7580
* Enlarge timeouts for FileTest by @llowinge in https://github.com/apache/camel-quarkus/pull/7581
* Bump quarkiverse-cxf.version from 3.23.1 to 3.25.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7582
* Bump quarkiverse-groovy.version from 3.24.4 to 3.25.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7583
* Apply RoutesDiscoveryConfig deprecation only to enabled() method by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7584
* Avoid using nested Map for UpdateExtensionDocPageMojo.componentLinkOverrides parameter by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7591
* Enable InfinispanTest#query test by @llowinge in https://github.com/apache/camel-quarkus/pull/7590
* Fix DB2 test for Podman by @llowinge in https://github.com/apache/camel-quarkus/pull/7594
* Enable GrpcTest#forwardOnError test by @llowinge in https://github.com/apache/camel-quarkus/pull/7575
* Bump actions/download-artifact from 4.3.0 to 5.0.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7595
* Remove workaround for Camel Quarkus main by @llowinge in https://github.com/apache/camel-quarkus/pull/7596
* [fixes #7585] Wiremock Olingo4 test by @llowinge in https://github.com/apache/camel-quarkus/pull/7597
* Enable Infinispan client native tests by @llowinge in https://github.com/apache/camel-quarkus/pull/7598
* Enable Tika tests by @llowinge in https://github.com/apache/camel-quarkus/pull/7599
* Remove workaround for Google BigQuery test by @llowinge in https://github.com/apache/camel-quarkus/pull/7600
* Bump com.microsoft.graph:microsoft-graph from 6.48.0 to 6.49.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7604
* Restore FOP native mode support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7602
* Remove redundant debezium-sqlserver & debezium-mysql test profiles by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7603
* Avoid usage of VertxHttpConfig runtime configuration for vertx-websocket build time by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7606
* Generated sources regen for SBOM by @github-actions[bot] in https://github.com/apache/camel-quarkus/pull/7611
* Fix typo in config property quarkus.camel.dev-ui.update-internal -> quarkus.camel.dev-ui.update-interval by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7612
* Bump quarkiverse-groovy.version from 3.25.0 to 3.25.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7609
* Remove workaround management dump by @llowinge in https://github.com/apache/camel-quarkus/pull/7610
* [relates #6083] Enable test for JMX in Camel Debug by @llowinge in https://github.com/apache/camel-quarkus/pull/7613
* [relates #4084] Remove not needed Spring deps from SQL by @llowinge in https://github.com/apache/camel-quarkus/pull/7614
* Deprecate camel-quarkus-langchain4j extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7605
* [resolves #6593] Remove workaround from Swagger by @llowinge in https://github.com/apache/camel-quarkus/pull/7615
* Fix warning about missing 'java.naming.factory.initial' when camel de… by @llowinge in https://github.com/apache/camel-quarkus/pull/7616
* Remove not needed explicit versions by @llowinge in https://github.com/apache/camel-quarkus/pull/7617
* Remove workaround for CAMEL-18143 by @llowinge in https://github.com/apache/camel-quarkus/pull/7618
* Remove not needed Infinispan exclusions which was needed for differen… by @llowinge in https://github.com/apache/camel-quarkus/pull/7619
* Bump actions/checkout from 4.2.2 to 5.0.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7621
* [relates #4089] Enable FTPS tests by @llowinge in https://github.com/apache/camel-quarkus/pull/7623
* Move CamelGraphQLConfig phase to BUILD_TIME by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7624
* Use maven container image properties when referencing image names by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7625
* Bump com.microsoft.graph:microsoft-graph from 6.49.0 to 6.50.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7627
* Rebalance native CI tests group 8 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7569
* Remove native mode workarounds for Google Cloud extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7628
* Evenly distribute modules across alternate JDK test groups by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7629
* Remove redundant log format override in AS2 extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7630
* Upgrade Quarkus to 3.26.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7631
* Upgrade Cassandra Quarkus to 1.3.0-rc1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7633
* Avoid configuring JasyptPropertiesParser unless encrypted properties are detected by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7635
* Bump quarkiverse-langchain4j.version from 1.1.0 to 1.1.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7622
* Bump cq-plugin.version from 4.17.10 to 4.18.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7641
* Update Dev UI dependencies to relocated GAVs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7639
* Generated sources regen for SBOM by @github-actions[bot] in https://github.com/apache/camel-quarkus/pull/7642
* Add section for observability-services extension to observability documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7643
* Remove spring-rabbitmq limitations by @llowinge in https://github.com/apache/camel-quarkus/pull/7648
* Enable ContinuousDevTest by @llowinge in https://github.com/apache/camel-quarkus/pull/7646
* Remove workaround for FOP extension by @llowinge in https://github.com/apache/camel-quarkus/pull/7647
* Remove Google Pubsub Micrometer BOM workaround by @llowinge in https://github.com/apache/camel-quarkus/pull/7649
* Move RUNTIME_INIT CamelContext customizations to CamelContextCustomizers by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7650
* Sync LangChain4j extension status with their Camel component metadata by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7652
* Upgrade Cassandra Quarkus to 1.3.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7653
* Bump org.apache.maven.plugins:maven-javadoc-plugin from 3.11.2 to 3.11.3 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7654
* Fix AsciiDoc attribute substitution in MapStruct and Jasypt documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7655
* Upgrade camel to 4.14.0 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7644
* Remove LevelDB workaround by @llowinge in https://github.com/apache/camel-quarkus/pull/7656
* Add langchain4j-agent JVM only extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7657
* Bump quarkiverse-groovy.version from 3.25.1 to 3.25.3 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7658
* Rebalance native CI tests group 4 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7659
* Fix order of applying system properties to camel main by @llowinge in https://github.com/apache/camel-quarkus/pull/7660
* Upgrade Quarkus to 3.26.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7662
* Fixed Oracle native failure in grouped tests by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7665
* Remove unnecessary comments and disabled annotations in jdbc tests by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7666
* Bump quarkiverse-cxf.version from 3.25.0 to 3.26.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7668
* Demote Hazelcast & OptaPlanner extensions to JVM only by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7661

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.25.0...3.26.0

## 3.25.0

* Add workflow to test LTS branches with Quarkus LTS SNAPSHOTs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7454
* Better  test coverage - usage, examples, operations by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7452
* Bump org.xmlunit:xmlunit-core from 2.10.2 to 2.10.3 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7456
* Next is 3.25.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7457
* Generated sources regen for SBOM by @github-actions[bot] in https://github.com/apache/camel-quarkus/pull/7464
* Rework upload-source.sh to fetch sources and signatures from repository.apache.org by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7458
* Remove SNAPSHOT builds for camel-main & quarkus-main branches by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7459
* Bump quarkiverse-groovy.version from 3.23.3 to 3.24.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7462
* Bump io.swagger.codegen.v3:swagger-codegen-generators from 1.0.56 to 1.0.57 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7463
* Make it clear which steps within the release guide are not applicable to patch releases by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7465
* Bump io.quarkiverse.jgit:quarkus-jgit-bom from 3.5.1 to 3.5.2 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7467
* Rename fury component link to fory by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7470
* Add PGP key for Jiri Ondrusek by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7471
* Make component doc xref links overridable via Maven configuration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7472
* Add changelog for 3.24.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7475
* Always enable dev console service discovery by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7474
* Bump quarkiverse-groovy.version from 3.24.0 to 3.24.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7478
* Bump net.revelc.code.formatter:formatter-maven-plugin from 2.26.0 to 2.27.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7479
* Change back to parent directory before svn import by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7480
* Generated sources regen for SBOM by @github-actions[bot] in https://github.com/apache/camel-quarkus/pull/7482
* fixes #7460 Azure-storage-datalake: cover methods of authentication by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7481
* Add changelog for 3.15.4 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7484
* Support Quarkus Dev UI extension config editing by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7486
* Bump quarkiverse-groovy.version from 3.24.1 to 3.24.2 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7488
* Bump org.apache.maven.plugins:maven-gpg-plugin from 3.2.7 to 3.2.8 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7487
* Bump quarkiverse-jsch.version from 3.0.15 to 3.1.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7492
* Bump io.quarkiverse.jgit:quarkus-jgit-bom from 3.5.2 to 3.6.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7493
* Generated sources regen for SBOM by @github-actions[bot] in https://github.com/apache/camel-quarkus/pull/7494
* Add changelog for 3.20.2 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7495
* Remove Azure test certificate by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7498
* Avoid false positive alerts from gitleaks by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7499
* Extend mail-microsoft-oauth test coverage by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7490
* Bump com.microsoft.graph:microsoft-graph from 6.43.0 to 6.44.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7502
* Upgrade camel to 4.13.0 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7497
* Bump com.microsoft.graph:microsoft-graph from 6.44.0 to 6.45.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7503
* Bump quarkiverse-fory.version from 0.4.0 to 0.4.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7504
* mail-microsoft-oauth native support by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7506
* Bump quarkiverse-mybatis.version from 2.4.0 to 2.4.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7508
* Import langchain4j-bom to align all dev.langchain4j dependencies by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7509
* Fix SplunkTest for running with remote docker test container by @llowinge in https://github.com/apache/camel-quarkus/pull/7511
* Generated sources regen for SBOM by @github-actions[bot] in https://github.com/apache/camel-quarkus/pull/7514
* Move all classpath resource glob tests to a dedicated test method by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7512
* Dependency convergence check fixes after Camel 4.13.0 upgrade by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7516
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.8.2 to 3.8.3 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7518
* Add initial Dev UI pages for Camel consoles by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7519
* Move dataformat runtime configuration logic into Camel Quarkus core by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7520
* Upgrade Quarkus to 3.25.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7521
* Do not use Runtime configuration during deployment by @radcortez in https://github.com/apache/camel-quarkus/pull/7524
* Generated sources regen for SBOM by @github-actions[bot] in https://github.com/apache/camel-quarkus/pull/7526
* Fix Dev UI nav link by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7527
* Bump quarkiverse-langchain4j.version from 1.1.0.CR1 to 1.1.0.CR2 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7513
* Bump com.microsoft.graph:microsoft-graph from 6.45.0 to 6.46.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7523
* Bump quarkiverse-groovy.version from 3.24.2 to 3.24.4 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7525
* Fix Kafka component Quarkus dev services discovery in dev & test mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7531
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 3.6.1 to 3.9.1 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7538
* Bump com.microsoft.graph:microsoft-graph from 6.46.0 to 6.47.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7537
* Bump quarkiverse-langchain4j.version from 1.1.0.CR2 to 1.1.0 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7536
* Upgrade Quarkus to 3.25.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7540
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.8.3 to 3.8.4 by @dependabot[bot] in https://github.com/apache/camel-quarkus/pull/7542

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.24.0...3.25.0

## 3.20.2

* [3.20.x] Upgrade Quarkus Amazon Services to 3.3.3 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7325
* [3.20.x] Add ignore option also for service bus test by @tveskrna in https://github.com/apache/camel-quarkus/pull/7334
* [3.20.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7341
* [3.20.x] Rework CallbackUtil.MockExtensionContext to not implement JUnit ExtensionContext by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7351
* [3.20.x] Use more correct convention with configuring datasource by @llowinge in https://github.com/apache/camel-quarkus/pull/7359
* [3.20.x] Upgrade Quarkus to 3.20.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7362
* [3.20.x] Do not exclude findbugs from wss4j-ws-security-common in the BOM because it transitively depends on quarkus-grpc-common that requires it at runtime by @ppalaga in https://github.com/apache/camel-quarkus/pull/7368
* [3.20.x] Upgrade to cq-maven-plugin 4.17.9 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7377
* [3.20.x] Improve handling of findSingleByType where multiple beans exist witho… by @zhfeng in https://github.com/apache/camel-quarkus/pull/7385
* [3.20.x] Deprecate Jolokia /q/jolokia management endpoint by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7390
* [3.20.x] Upgrade Camel to 4.10.5 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7407
* [3.20.x] Remove static modifier from CamelJolokiaRestrictor.ALLOWED_DOMAINS field by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7423
* [3.20.x] Upgrade Camel to 4.10.6 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7468

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.20.1...3.20.2

## 3.15.4

* Upgrade Quarkus to 3.15.4 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7121
* [3.15.x] fixes #7056 Option for disabling identity tests except key-vault (Azure) by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7138
* [3.15.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7147
* Update documentation for missing camel extensions that are knative co… by @zbendhiba in https://github.com/apache/camel-quarkus/pull/7173
* [3.15.x] Fix Beanio tests for Windows by @llowinge in https://github.com/apache/camel-quarkus/pull/7187
* [3.15.x] Ban auto value annotations 3.15.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7192
* [3.15.x] Upgrade Camel to 4.8.6 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7207
* Fix printing sensitive ENVs information from SSH test by @llowinge in https://github.com/apache/camel-quarkus/pull/7231
* [3.15.x] Upgrade Camel to 4.8.7 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7354
* [3.15.x] Upgrade Quarkus to 3.15.5 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7363
* [3.15.x] Add note about HTTP endpoint paths when using rest-openapi contract first by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7364
* [3.15.x] Remove SmallRye Fault Tolerance dependency overrides in microprofile-fault-tolerance test module by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7372
* [3.15.x] Upgrade Camel to 4.8.8 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7469


**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.15.3...3.15.4

## 3.24.0

* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7395
* Bump org.xmlunit:xmlunit-core from 2.10.1 to 2.10.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/7394
* Bump quarkiverse-cxf.version from 3.23.0 to 3.23.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7399
* Bump quarkiverse-minio.version from 3.8.3 to 3.8.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/7400
* Bump org.codehaus.mojo:exec-maven-plugin from 3.5.0 to 3.5.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7401
* fixes #7397 Debezium tests -  refactor to use grouped approach by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7398
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7406
* Upgrade camel to 4.12.0 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7403
* Bump org.apache.maven.plugins:maven-clean-plugin from 3.4.1 to 3.5.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7409
* Add changelog for 3.23.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7412
* Next is 3.24.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7411
* Debezium oracle connector by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7402
* Upgrade OptaPlanner Quarkus to 10.0.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7413
* Update PQC extension jvmSince version to 3.24.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7415
* Remove azure-core-http-vertx from camel-quarkus-bom by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7416
* Tidy debezium-grouped test modules by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7417
* Remove static modifier from CamelJolokiaRestrictor.ALLOWED_DOMAINS field by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7419
* Bump quarkiverse-groovy.version from 3.22.2 to 3.23.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7421
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.8.0 to 3.8.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/7422
* Bump org.codehaus.mojo:build-helper-maven-plugin from 3.6.0 to 3.6.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7424
* Bump quarkiverse-groovy.version from 3.23.0 to 3.23.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7425
* Dependency convergence check workflow fixes and additional GAV exclusions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7429
* Support resolving beans by name and qualifiers by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7431
* Add support to langchain4j extensions for multiple chat model configuration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7433
* Add documentation for autowiring behaviour with default beans by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7434
* Make extension-support metadata consistent with other extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7435
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7428
* Upgrade Quarkus to 3.24.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7436
* Bump org.wiremock:wiremock-standalone from 3.13.0 to 3.13.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7437
* Add Langchain4j Tools native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7438
* Add Python JVM Only extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7439
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7444
* Bump com.unboundid:unboundid-ldapsdk from 7.0.2 to 7.0.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/7443
* azure-storage-datalake native support by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7442
* Bump quarkiverse-groovy.version from 3.23.1 to 3.23.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/7447
* Add langchain4j-tokenizer native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7449
* Add langchain4j-web-search native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7451
* Upgrade Quarkus to 3.24.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7453

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.23.0...3.24.0

## 3.23.0

* Next is 3.23.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7300
* Bump org.wiremock:wiremock-standalone from 3.12.1 to 3.13.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7301
* Bump quarkiverse-groovy.version from 3.21.3 to 3.22.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7302
* Bump actions/download-artifact from 4.2.1 to 4.3.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7303
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7308
* Bump org.jolokia:jolokia-agent-jvm from 2.2.8 to 2.2.9 by @dependabot in https://github.com/apache/camel-quarkus/pull/7314
* Fixes #7211 Enable ssh eddsa test for RHEL by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7311
* fixes #7317 Longer interval for azureServiceBus.scheduled by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7318
* Fix dynamic instantiation of Azure HttpResponseException types in native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7320
* Bump quarkiverse-mybatis.version from 2.3.2 to 2.4.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7321
* Add changelog for 3.22.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7323
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 3.4.0 to 3.5.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7313
* Temporarily disable wildcard package scan test in the Quarkus Platform #7312 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7324
* Bump quarkiverse-groovy.version from 3.22.0 to 3.22.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7326
* Avoid producing duplicate synthetic beans for @EndpointInject and @Produce by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7327
* Remove redundant Splunk component name ternary expression by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7328
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7329
* Add changelog for 3.20.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7332
* Simplify running SQL test against external database servers by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7333
* fixes #7337: add ignore option also for service bus test by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7338
* fixes #7335 EdDSA test should be disabled on RHEL8 in native by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7336
* Fix dependency convergence error io.opentelemetry:opentelemetry-semconv by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7339
* Bump quarkiverse-minio.version from 3.8.1 to 3.8.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/7344
* Test CamelQuarkusTestSupport in the Quarkus Platform by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7347
* Add GitHub workflow to perform dependency convergence checks by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7246
* Rework CallbackUtil.MockExtensionContext to not implement JUnit ExtensionContext by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7350
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7352
* Remove duplicate camel-dfdl dependency declaration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7355
* Bump quarkiverse-groovy.version from 3.22.1 to 3.22.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/7357
* Use more correct convention with configuring datasource by @llowinge in https://github.com/apache/camel-quarkus/pull/7358
* Run camel-quarkus-junit5 tests in the Quarkus Platform by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7360
* Remove redundant BaseMainSupport method overrides by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7365
* Upgrade Quarkus to 3.23.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7366
* Do not exclude findbugs from wss4j-ws-security-common in the BOM because it transitively depends on quarkus-grpc-common that requires it at runtime by @ppalaga in https://github.com/apache/camel-quarkus/pull/7367
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 3.5.0 to 3.6.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7356
* Improve handling of findSingleByType where multiple beans exist without any @Default qualifier by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7370
* Upgrade to cq-maven-plugin 4.17.9 by @ppalaga in https://github.com/apache/camel-quarkus/pull/7375
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7379
* Remove rest-assured groovy exclusions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7378
* Fixes #7373 extend saga coverage by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7374
* Bump org.xmlunit:xmlunit-core from 2.10.0 to 2.10.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7381
* Fix #7383 to override shouldRun of CodeGenProvider by @zhfeng in https://github.com/apache/camel-quarkus/pull/7384
* Deprecate Jolokia /q/jolokia management endpoint by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7389
* Let Quarkus BOM 3.23.0+ manage io.perfmark:perfmark-api by @ppalaga in https://github.com/apache/camel-quarkus/pull/7388
* Upgrade Quarkus to 3.23.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7392
* Upgrade to Quarkus CXF 3.23.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/7393

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.22.0...3.23.0

## 3.20.1

* [3.20.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7168
* [3.20.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7178
* Update documentation for missing camel extensions that are knative co… by @zbendhiba in https://github.com/apache/camel-quarkus/pull/7184
* [3.20.x] Use hyphenated anchor links for config properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7183
* [3.20.x] Ban auto value annotations 3.20.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7191
* Revert "Use hyphenated anchor links for config properties" by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7194
* [3.20.x] Upgrade Camel to 4.10.3 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7182
* [3.20.x] Use hyphenated anchor links for config properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7204
* [3.20.x] Upgrade Quarkus Amazon Services to 3.3.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7210
* Backports from main by @zbendhiba in https://github.com/apache/camel-quarkus/pull/7221
* [3.20.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7244
* Upgrade to Quarkus CXF 3.20.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/7245
* [3.20.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7258
* [3.20] Backports by @ppalaga in https://github.com/apache/camel-quarkus/pull/7268
* [3.20.x] Backports by @ppalaga in https://github.com/apache/camel-quarkus/pull/7277
* [3.20.x] Backports by @ppalaga in https://github.com/apache/camel-quarkus/pull/7290
* [3.20.x] Upgrade to Quarkus CXF 3.20.2 by @ppalaga in https://github.com/apache/camel-quarkus/pull/7306
* [3.20.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7322
* [3.20.x] Upgrade Camel to 4.10.4 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7309

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.20.0...3.20.1

## 3.22.0

* Configure extension capabilities for langchain4j-embeddings by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7158
* Bump net.revelc.code.formatter:formatter-maven-plugin from 2.25.0 to 2.26.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7159
* Bump org.amqphub.quarkus:quarkus-qpid-jms-bom from 2.7.1 to 2.8.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7161
* Bump quarkiverse-mybatis.version from 2.2.4 to 2.3.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7164
* Bump com.mycila:license-maven-plugin from 4.6 to 5.0.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7160
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7165
* Next is 3.22.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7167
* Add squash and merge recommendation to Dependabot branch sync workflow by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7171
* Upgrade Quarkus to 3.21.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7169
* Support Camel default route resource locations by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7134
* Update documentation for missing camel extensions that are knative co… by @zbendhiba in https://github.com/apache/camel-quarkus/pull/7172
* Bump quarkiverse-mybatis.version from 2.3.0 to 2.3.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7177
* Bump org.jolokia:jolokia-agent-jvm from 2.2.6 to 2.2.7 by @dependabot in https://github.com/apache/camel-quarkus/pull/7175
* Bump cq-plugin.version from 4.17.1 to 4.17.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/7176
* Bump quarkiverse-cxf.version from 3.20.0.CR1 to 3.21.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7174
* Add azure-files JVM only extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7179
* Use hyphenated anchor links for config properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7180
* Add changelog for 3.20.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7186
* Ban com.google.auto.value:auto-value-annotations by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7190
* Revert "Use hyphenated anchor links for config properties" by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7193
* Bump quarkiverse-groovy.version from 3.19.2 to 3.21.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7195
* Upgrade Debezium to 3.0.8.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7198
* Use hyphenated anchor links for config properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7199
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7202
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.7.1 to 3.8.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7200
* Bump quarkiverse-pooled-jms.version from 2.7.0 to 2.8.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7201
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 3.3.1 to 3.4.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7205
* Bump maven-surefire-plugin.version from 3.5.2 to 3.5.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/7206
* Avoid setting null config property overrides in test profiles by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7208
* Disable dev services for Kubernetes tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7209
* Reduce log noise from FTP tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7213
* fixes #7211: disable SshTest#testProducerWithEdDSAKeyType for RHEL8(9) by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7212
* Bump quarkiverse-cxf.version from 3.21.0 to 3.21.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7215
* Upgrade Camel to 4.11.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7203
* fixed #7217 ldap uses certificate-generator by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7218
* Smooks: EDI isn't supported in Camel Quarkus Smooks by @zbendhiba in https://github.com/apache/camel-quarkus/pull/7220
* feat(extension): Add camel-dfdl JVM only extension by @igarashitm in https://github.com/apache/camel-quarkus/pull/7222
* Bump quarkiverse-groovy.version from 3.21.0 to 3.21.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7223
* Upgrade Debezium to 3.0.10.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7224
* Clean up PubNub WireMock responses by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7225
* fix of #7226: ldap config optimization + tests refactor by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7227
* Bump org.jolokia:jolokia-agent-jvm from 2.2.7 to 2.2.8 by @dependabot in https://github.com/apache/camel-quarkus/pull/7228
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7229
* Fix jms-artemis-ra module name by @tveskrna in https://github.com/apache/camel-quarkus/pull/7230
* Align langchain4j-tokenizer.adoc to reflect the component .adoc from Camel documentation by @oscerd in https://github.com/apache/camel-quarkus/pull/7233
* Langchain4j-Tokenizer link is component and not others by @oscerd in https://github.com/apache/camel-quarkus/pull/7234
* Revert "Langchain4j-Tokenizer link is component and not others" by @oscerd in https://github.com/apache/camel-quarkus/pull/7235
* Revert "Align langchain4j-tokenizer.adoc to reflect the component .adoc from Camel documentation by @oscerd in https://github.com/apache/camel-quarkus/pull/7236
* Bump cq-plugin.version from 4.17.2 to 4.17.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/7237
* Remove logging of testcontainers configuration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7238
* Handle potential for bcel to be present for xalan native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7239
* Do not manage com.squareup.okhttp3:mockwebserver by @zhfeng in https://github.com/apache/camel-quarkus/pull/7240
* Bump actions/setup-java from 4.7.0 to 4.7.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7242
* Bump quarkiverse-mybatis.version from 2.3.1 to 2.3.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/7243
* Manage and align com.fasterxml:aalto-xml by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7241
* Bump cq-plugin.version from 4.17.3 to 4.17.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/7250
* Bump quarkiverse-groovy.version from 3.21.1 to 3.21.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/7251
* Upgrade quarkus-jsch to 3.0.15 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7255
* Document groovy extension limitations for property placeholders in native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7256
* Update IBM MQ versions by @vkasala in https://github.com/apache/camel-quarkus/pull/7249
* fixes #6771 DataFormat endpoints ignore camel.dataformat.* properties by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7247
* Use Pinecone emulator for integration testing by @jonomorris in https://github.com/apache/camel-quarkus/pull/7248
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7260
* Fix #7254 to support named entityManagerFactory in camel-quarkus-jpa by @zhfeng in https://github.com/apache/camel-quarkus/pull/7261
* fixes #7262: removal of unecessary dependencies to Quarkus AmazonServices by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7263
* Manage smooks transitives by @ppalaga in https://github.com/apache/camel-quarkus/pull/7267
* fixes #7275 Jasypt tests fail on FIPS machine by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7276
* Drop Camel http from Knative consumers list by @zbendhiba in https://github.com/apache/camel-quarkus/pull/7273
* Bump quarkiverse-groovy.version from 3.21.2 to 3.21.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/7279
* Bump cq-plugin.version from 4.17.4 to 4.17.5 by @dependabot in https://github.com/apache/camel-quarkus/pull/7278
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7281
* Update quarkus to 3.22.0.CR1 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7271
* Bump to quarkus-jgit 3.5.1 by @gastaldi in https://github.com/apache/camel-quarkus/pull/7265
* Use smooks-version property from camel-dependencies instead of syncing from camel-parent by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7282
* fixes #7269: bump com.ibm.cloud:sdk-core to 9.23.1 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7284
* Add missing QuarkusTestResource annotations to k8s ConfigMap & Secret reload tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7288
* Fixes #7286: aws2-s3 tests fail on FIPS by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7287
* Fix #7280 to check defualt bean only with @Default qualifier by @zhfeng in https://github.com/apache/camel-quarkus/pull/7289
* Upgrade Quarkus to 3.22.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7292
* Upgrade to Quarkus CXF 3.22.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/7295

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.20.0...3.22.0

## 3.20.0

* Next is 3.20.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7036
* Bump org.apache.maven.plugins:maven-clean-plugin from 3.4.0 to 3.4.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7033
* Bump actions/cache from 4.2.0 to 4.2.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7035
* Bump quarkiverse-freemarker.version from 1.1.0 to 1.2.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7037
* Bump quarkiverse-groovy.version from 3.18.2 to 3.19.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7038
* Bump com.azure:azure-core-http-vertx from 1.0.1 to 1.0.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/7039
* Bump actions/upload-artifact from 4.6.0 to 4.6.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7040
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7041
* Add tests for Kubernetes property resolution and context reloading by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7046
* Bump quarkiverse-cxf.version from 3.18.1 to 3.19.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7048
* Bump peter-evans/create-pull-request from 7.0.6 to 7.0.7 by @dependabot in https://github.com/apache/camel-quarkus/pull/7047
* Add changelog for 3.19.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7049
* Set a default Jolokia agent description by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7052
* Enable Jolokia Camel restrictor allowed MBean domains to be configurable by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7053
* Option for disabling identity tests except key-vault by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7057
* Upgrade Quarkus to 3.19.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7058
* Bump actions/download-artifact from 4.1.8 to 4.1.9 by @dependabot in https://github.com/apache/camel-quarkus/pull/7059
* Clean up usage of deprecated APIs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7060
* Bump actions/cache from 4.2.1 to 4.2.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/7065
* Bump quarkiverse-pooled-jms.version from 2.6.0 to 2.7.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7063
* Bump quarkiverse-groovy.version from 3.19.0 to 3.19.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7064
* Bump io.quarkiverse.micrometer.registry:quarkus-micrometer-registry-jmx from 3.2.4 to 3.3.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7066
* Bump org.apache.maven.plugins:maven-deploy-plugin from 3.1.3 to 3.1.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/7067
* Add Jolokia native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7062
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.6.4 to 3.7.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7070
* Bump org.apache.maven.plugins:maven-install-plugin from 3.1.3 to 3.1.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/7071
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7072
* Ensure AsciiDoc in config table description column is rendered properly by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7073
* Miscellaneous documentation tidy-ups by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7075
* Bump io.quarkiverse.micrometer.registry:quarkus-micrometer-registry-jmx from 3.3.0 to 3.3.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7079
* Bump org.jolokia:jolokia-agent-jvm from 2.2.2 to 2.2.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/7081
* Replace codegen usage of deprecated Swagger Schema.required attribute with Schema.requiredMode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7083
* Restore Micrometer JMX tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7084
* Bump org.wiremock:wiremock-standalone from 3.12.0 to 3.12.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7080
* Upgrade Camel to 4.10.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7069
* Bump peter-evans/create-pull-request from 7.0.7 to 7.0.8 by @dependabot in https://github.com/apache/camel-quarkus/pull/7089
* Smb: extend coverage with test using path parameter by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7087
* Fix formatting for Jolokia expose-container-port config docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7090
* Upgrade Quarkus to 3.19.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7091
* Kubernetes extension and test fixes for Camel 4.10.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7092
* Add support for SimpleLanguageFunctionFactory by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7094
* Bump org.apache.maven.plugins:maven-compiler-plugin from 3.11.0 to 3.14.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7096
* Bump org.jolokia:jolokia-agent-jvm from 2.2.3 to 2.2.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/7097
* Add note about HTTP endpoint paths when using rest-openapi contract first by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7098
* Disable FopTest on GitHub CI for Windows by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7095
* Verify expected json-validator error messages by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7099
* Added azure-servicebus mock testing by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7082
* Fix Jolokia client-principal config code snippet by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7100
* Handle ipv6 addresses for JolokiaRequestRedirectHandler by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7103
* Bump quarkiverse-groovy.version from 3.19.1 to 3.19.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/7101
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7109
* Upgrade Camel to 4.10.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7110
* Add changelog for 3.15.3 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7113
* Use quarkus-rest-client instead of quarkus-rest in Jira extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7115
* cxf-soap-grouped native build occasionally runs out of memory by @ppalaga in https://github.com/apache/camel-quarkus/pull/7116
* Refactored azure-servicebus emulator to not use docker compose by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7117
* Bump org.jolokia:jolokia-agent-jvm from 2.2.4 to 2.2.6 by @dependabot in https://github.com/apache/camel-quarkus/pull/7118
* Fix population of catalog models when extendClassPathCatalog is in use by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7122
* Bump cq-plugin.version from 4.17.0 to 4.17.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7126
* Bump quarkiverse-jsch.version from 3.0.13 to 3.0.14 by @dependabot in https://github.com/apache/camel-quarkus/pull/7124
* Bump com.azure:azure-core-http-vertx from 1.0.2 to 1.0.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/7123
* Fix get beans name from the camel quarkus runtime catalog by @zhfeng in https://github.com/apache/camel-quarkus/pull/7127
* Add option to disable sanity checks in individual modules by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7129
* Avoid comparing CDI client proxies in Fault Tolerance configuration testing by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7130
* Fix dependency convergence failures with camel-quarkus-google-secret-manager by @zhfeng in https://github.com/apache/camel-quarkus/pull/7128
* Azure-servicebus: fixed intermittent failures by switching to another img by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7131
* Upgrade Quarkus to 3.20.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7133
* Upgrade Quarkus Amazon Services to 3.3.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7135
* Upgrade to Quarkus CXF 3.20.0.CR1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/7136
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.7.0 to 3.7.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7137
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7139
* Add basic Smooks component and DataFormat tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7141
* perf-regression: Upgrade hyperfoil-maven-plugin to 0.27.1 by @aldettinger in https://github.com/apache/camel-quarkus/pull/7143
* Update jt400 README notes for mocked testing by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7144
* Groovy: extend test coverage by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7145
* Align json-smart with Camel by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7146
* Ensure docs component xref point to Camel 4.10.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7149
* Fix potential Jolokia java.net.BindException in dev mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7150
* Bump actions/download-artifact from 4.1.9 to 4.2.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7151
* Upgrade Quarkus to 3.20.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7152
* delete unecessary banned guava : listenablefuture dependency by @zbendhiba in https://github.com/apache/camel-quarkus/pull/7153
* Bump actions/upload-artifact from 4.6.1 to 4.6.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/7155
* Bump actions/download-artifact from 4.2.0 to 4.2.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7156
* Bump quarkiverse-minio.version from 3.7.7 to 3.8.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7154
* Sync gax-httpjson version from google-cloud-pubsub by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7157

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.19.0...3.20.0

## 3.15.3

* [3.15.x] Update generated files after release by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6908
* [3.15.x] Extend ssh coverage + native by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7019
* [3.15.x] Fix build time DefaultCamelContext creation when camel-quarkus-opentelemetry is present by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7025
* [3.15.x] Upgrade Camel to 4.8.4 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7061
* [3.15.x] Upgrade Camel to 4.8.5 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7108

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.15.2...3.15.3

## 3.19.0

* Next is 3.19.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6932
* Add observability-services extension to main branch by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6936
* Fixes #6690, #6933 - google-secret-manager extend coverage and native  by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6935
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6937
* Add note to OpenTelemetry extension docs stating that the otel agent is not required by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6944
* fixes #6941: fixed load of ssh keyProvider from test resources by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6943
* Add changelog for 3.18.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6945
* Bump io.swagger.codegen.v3:swagger-codegen-generators from 1.0.55 to 1.0.56 by @dependabot in https://github.com/apache/camel-quarkus/pull/6948
* Bump actions/setup-java from 4.6.0 to 4.7.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6956
* Add azure-servicebus native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6959
* fixes #6934: google-pubsub support by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6939
* Bump quarkiverse-cxf.version from 3.18.0 to 3.18.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6961
* Make tika extension work as per the vanilla Camel component by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6963
* Bump org.wiremock:wiremock-standalone from 3.9.2 to 3.11.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6964
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6965
* Full support of aws-secret-manager by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6953
* Update kubernetes.adoc by @rhaetor in https://github.com/apache/camel-quarkus/pull/6957
* Test observability-services otel tracing and JMX metrics by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6969
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.6.1 to 3.6.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6971
* Bump quarkiverse-groovy.version from 3.17.2 to 3.18.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6970
* Migrate extension configuration to @ConfigMapping by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6975
* Bump com.azure:azure-core-http-vertx from 1.0.0-beta.24 to 1.0.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6981
* Bump quarkiverse-groovy.version from 3.18.0 to 3.18.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6982
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.6.2 to 3.6.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/6983
* Bump cq-plugin.version from 4.16.1 to 4.16.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6980
* fixes #6660: azure-key-vault refresh context coverage by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6972
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6987
* Add Jolokia JVM only extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6988
* Fix missing and incorrect copyright notices by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6990
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.6.3 to 3.6.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/6991
* Upgrade Camel to 4.10.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6986
* fixes #6992 make azure-key-vault readme clearer by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6993
* Enable azure-servicebus produceConsumeWithCustomClients test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6994
* Azure-key-vault better coverage for identity credentials by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6985
* Upgrade Quarkus to 3.19.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6998
* Add Kubernetes integration tests for Pod, ConfigMap and Job resources by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6999
* Bump org.wiremock:wiremock-standalone from 3.11.0 to 3.12.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7002
* Add more detail to Kubernetes extension configuration documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7003
* Add camel-console management endpoint by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7005
* Restore solr extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7006
* Bump com.azure:azure-core-http-vertx from 1.0.0 to 1.0.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/7008
* Bump quarkiverse-groovy.version from 3.18.1 to 3.18.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/7007
* Add camel-kubernetes deployment, secret, customresource tests by @avano in https://github.com/apache/camel-quarkus/pull/7010
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/7013
* Add tests for Kubernetes component consumers by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7014
* Add test for Kubernetes component with autowiring disabled by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7015
* Fixes apache#7004: making azure-key-valt refresh test stable. by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/7009
* Bump net.revelc.code.formatter:formatter-maven-plugin from 2.24.1 to 2.25.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/7017
* Use NativeMonitoringBuildItem to automatically enable native monitoring features by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7018
* Move common Swagger native configuration to support-swagger extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7020
* Use only an embedded server for SSH testing by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7021
* Add a helper method for extension build time DefaultCamelContext creation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7024
* Align dependencies shared with Quarkus Google Cloud Services for libraries-bom 26.50.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7028
* Remove GitHub pull request template by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6995
* Upgrade Quarkus to 3.19.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7030
* Link to Camel 4.10.x documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/7032

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.18.0...3.19.0

## 3.18.0

* Bump com.unboundid:unboundid-ldapsdk from 7.0.1 to 7.0.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6848
* Next is 3.18.0-SNAPSHOT by @ppalaga in https://github.com/apache/camel-quarkus/pull/6850
* Bump actions/cache from 4.1.2 to 4.2.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6852
* Bump quarkiverse-groovy.version from 3.17.0 to 3.17.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6853
* Load Java DSL routes in a predicatable order by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6854
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6859
* Bump cq-plugin.version from 4.15.0 to 4.15.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6858
* Bump quarkiverse-cxf.version from 3.17.2 to 3.17.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/6857
* Remove redundant dependency overrides for org.testcontainers:cassandra by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6861
* Bump cq-plugin.version from 4.15.2 to 4.16.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6863
* Bump org.apache.maven.plugins:maven-javadoc-plugin from 3.11.1 to 3.11.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6862
* [camel-main] fixes #6492 - removed DoubleRoutesPerClassTest by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6582
* Upgrade Debezium to 3.0.4.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6864
* Bump quarkiverse-groovy.version from 3.17.1 to 3.17.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6865
* Camel Quarkus 3.17.0 - changelog by @zbendhiba in https://github.com/apache/camel-quarkus/pull/6868
* Add test for AI service resolution by name #6866 by @aldettinger in https://github.com/apache/camel-quarkus/pull/6867
* Upgrade to quarkus 3.17.4 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/6869
* Bump cq-plugin.version from 4.16.0 to 4.16.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6870
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6872
* fixed typos in testing.adoc by @Ainges in https://github.com/apache/camel-quarkus/pull/6873
* Bump io.swagger.codegen.v3:swagger-codegen-generators from 1.0.54 to 1.0.55 by @dependabot in https://github.com/apache/camel-quarkus/pull/6874
* Bump actions/upload-artifact from 4.4.3 to 4.5.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6875
* Bump actions/setup-java from 4.5.0 to 4.6.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6876
* langchain4j: fix documentation by @aldettinger in https://github.com/apache/camel-quarkus/pull/6877
* Fix #6720 to add camel-quarkus-fory by @zhfeng in https://github.com/apache/camel-quarkus/pull/6878
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6879
* Bump peter-evans/create-pull-request from 7.0.5 to 7.0.6 by @dependabot in https://github.com/apache/camel-quarkus/pull/6883
* Upgrade Debezium to 3.0.5.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6885
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6887
* Bump org.apache.maven.plugins:maven-remote-resources-plugin from 3.2.0 to 3.3.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6886
* [#6888] Use @ApplicationScoped instead of @SessionScoped by @llowinge in https://github.com/apache/camel-quarkus/pull/6893
* Switch from docker.io to gcr.io for container images by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6895
* Fix generated BrotliInputStream constructor by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6897
* Bump quarkiverse-langchain4j.version from 0.22.0 to 0.23.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6894
* langchain4j: add support for AI service resolution by bean name #6866 by @aldettinger in https://github.com/apache/camel-quarkus/pull/6899
* Bump actions/upload-artifact from 4.5.0 to 4.6.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6901
* Rework Kamelet build time code to avoid bytecode serialization issues by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6902
* Bump quarkiverse-langchain4j.version from 0.23.0 to 0.23.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/6900
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6903
* Upgrade Quarkus to 3.18.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6910
* Upgrade Debezium to 3.0.7.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6913
* Langchain4j: remove useless dependency by @aldettinger in https://github.com/apache/camel-quarkus/pull/6915
* Fix usage of deprecated locale configuration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6914
* Disable kubernetes-client dev services by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6916
* Fix #6905 to add a test for using artemis jca connector by @zhfeng in https://github.com/apache/camel-quarkus/pull/6906
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6918
* fixes #6909 ssh: extend test coverage by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6912
* Minor updates to release guide sources dist promotion by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6919
* Add changelog for 3.15.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6920
* Fix #6922 to add a JMS Component customizer if Artemis JMS RA is avai… by @zhfeng in https://github.com/apache/camel-quarkus/pull/6923
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.6.0 to 3.6.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6924
* Validate pull request number from downloaded archive in synchronize-dependabot-branch workflow before attempting to use it by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6921
* Add workaround for microprofile-fault-tolerance incompatibilities by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6925
* Upgrade Quarkus to 3.18.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6928
* Add Smooks JVM only extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6929

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.17.0...3.18.0

## 3.15.2

* [3.15.x] Update generated files after release by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6715
* [3.15.x] Backport the fixes for camel-quarkus-rest-openapi codegen by @zhfeng in https://github.com/apache/camel-quarkus/pull/6728
* [3.15.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6729
* [3.15.x] Fix #6736 no need to add RegisterForReflection annotation on array type Class by @zhfeng in https://github.com/apache/camel-quarkus/pull/6738
* [3.15.x] backports by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6742
* [3.15.x] Fixes #6747 - opentelemetry - ensure that the sequence of recorded spans by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6749
* [3.15.x] Upgrade FHIR core to 6.4.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6756
* [3.15.x] Fix #6754 to remove exclusion of findbugs when depends on quarkus-grpc-common by @zhfeng in https://github.com/apache/camel-quarkus/pull/6757
* [3.15.x] backports by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6758
* [3.15.x] override artemis image name by @zhfeng in https://github.com/apache/camel-quarkus/pull/6767
* [3.15.x] backports by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6776
* [3.15.x] Upgrade to Quarkus CXF 3.15.3 by @ppalaga in https://github.com/apache/camel-quarkus/pull/6782
* [3.15.x] Add to copy allow-findbugs.xsl since flatten-bom needs it even quickly build by @zhfeng in https://github.com/apache/camel-quarkus/pull/6784
* [3.15.x] backports by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6795
* [3.15.x] Enable Mongo tests to run with container image versions < 7.x by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6798
* [3.15.x] Upgrade Quarkus to 3.15.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6800
* [3.15.x] fixes #6688: google-secret-manager: extend test coverage by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6817
* [3.15.x] Replace camel-kamelets-catalog dependency with camel-kamelets in Kamelet extension docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6826
* [3.15.x] Upgrade Camel to 4.8.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6837
* [3.15.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6855
* [3.15.x] Upgrade Camel to 4.8.3 + Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6898
* [3.15.x] Upgrade quarkus-artemis to 3.5.1 by @zhfeng in https://github.com/apache/camel-quarkus/pull/6904
* [3.15.x] Upgrade Quarkus to 3.15.3 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6907

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.15.1...3.15.2

## 3.17.0

* Downgrade maven-release-plugin to 3.0.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6699
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6704
* Bump org.apache.maven.plugins:maven-plugin-plugin from 3.15.0 to 3.15.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6703
* Next is 3.17.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6706
* Fix #6701 to support OpenAPI spec with yaml format by @zhfeng in https://github.com/apache/camel-quarkus/pull/6707
* Restore capability to disable automatic startup of the Camel Quarkus runtime by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6708
* Simplify splunk-hec test SSL setup by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6710
* Upgrade Quarkus Amazon Services to 2.19.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6714
* Use jackson extension instead of plain component dependency in MongoDB extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6717
* Fix #6716 to introduce ignore-unknown-properties property by @zhfeng in https://github.com/apache/camel-quarkus/pull/6719
* Fix incorrect exporter endpoint configuration property in OpenTelemetry extension documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6723
* Move Quarkus Qpid JMS configuration content to AMQP extension usage section by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6725
* Add changelog for 3.16.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6727
* Introduce additionProperties in rest-openapi codegen and fix some kno… by @zhfeng in https://github.com/apache/camel-quarkus/pull/6726
* Upgrade Quarkus to 3.16.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6724
* Bump quarkiverse-jgit.version from 3.3.1 to 3.3.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6718
* Work around issues for avro-jackson compatibility with Avro 1.12.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6722
* Upgrade Debezium to 3.0.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6731
* Remove named DataSource configuration in OpenTelemetryTestResource by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6732
* Bump quarkiverse-jsch.version from 3.0.11 to 3.0.12 by @dependabot in https://github.com/apache/camel-quarkus/pull/6735
* Fix #6736 no need to add RegisterForReflection annotation on array ty… by @zhfeng in https://github.com/apache/camel-quarkus/pull/6737
* Add changelog for 3.15.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6740
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6741
* Bump org.apache.maven.plugins:maven-javadoc-plugin from 3.10.1 to 3.11.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6745
* Bump maven-surefire-plugin.version from 3.5.1 to 3.5.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6746
* Ensure that the sequence of recorded spans is correct by using bean:* by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6748
* Align SubstituteIntrospectionSupport.CACHE type with that of the original class by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6750
* Remove redundant HostUtils substitutions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6751
* Use target directory for Narayana object store location by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6752
* Fix #6754 to remove exlusion of findbugs when depends on quarkus-grpc… by @zhfeng in https://github.com/apache/camel-quarkus/pull/6755
* Bump quarkiverse-langchain4j.version from 0.20.3 to 0.21.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6759
* Bump io.swagger.codegen.v3:swagger-codegen-generators from 1.0.53 to 1.0.54 by @dependabot in https://github.com/apache/camel-quarkus/pull/6760
* Bump quarkiverse-groovy.version from 3.15.0 to 3.16.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6762
* Update to support override artemis devservices image name by @zhfeng in https://github.com/apache/camel-quarkus/pull/6763
* Upgrade Quarkus to 3.16.2 by @zhfeng in https://github.com/apache/camel-quarkus/pull/6764
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6765
* Documentation for 'Fop native failures due to pdfbox 3 upgrade' #1 by @fugerit79 in https://github.com/apache/camel-quarkus/pull/6744
* Fix #6702 to add custom openapi spec locations by @zhfeng in https://github.com/apache/camel-quarkus/pull/6769
* Adding profile for using SunPKCS11-NSS-FIPS provider by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6773
* Use camel.version property value for catalog camelVersion metadata by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6774
* Remove dependency on optional dependency org.brotli:dec by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6778
* Upgrade Quarkus to 3.17.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6779
* Bump cq-plugin.version from 4.14.2 to 4.15.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6783
* Exclude unwanted gRPC services from build time discovery by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6785
* Fix typo in core docs form -> from by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6786
* Add an example for using a specific transaction policy to JTA extension docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6787
* Bump quarkiverse-jackson-jq.version from 2.1.0 to 2.2.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6788
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6793
* Add changelog for 3.8.4 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6794
* Enable Mongo tests to run with container image versions < 7.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6796
* Add an explanation of how to handle ContextNotActiveException to CDI documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6797
* Bump quarkiverse-jgit.version from 3.3.2 to 3.3.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/6802
* Upgrade Quarkus Amazon Services to 2.20.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6803
* Remove tests and documentation for Quarkus Amazon Services integration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6804
* Filter AdviceWithRouteBuilder types from build time route discovery by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6809
* Upgrade Quarkus to 3.17.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6810
* JIRA extension : update the Rest Client by @zbendhiba in https://github.com/apache/camel-quarkus/pull/6812
* Bump quarkiverse-groovy.version from 3.16.1 to 3.17.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6815
* Avoid producing OpenTelemetryTracer bean if quarkus.otel.sdk.disabled= true by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6818
* fixes #6688: google-secret-manager: extend test coverage by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6799
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6819
* Add capability to ignore specific example projects in CI workflows by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6820
* Bump quarkiverse-cxf.version from 3.16.1 to 3.17.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6822
* Update Maven wrapper distribution URL to Maven 3.9.9 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6823
* Upgrade Quarkus to 3.17.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6824
* Replace camel-kamelets-catalog dependency with camel-kamelets in Kamelet extension docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6825
* Bump quarkiverse-langchain4j.version from 0.21.0 to 0.22.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6827
* Bump org.cyclonedx:cyclonedx-maven-plugin from 2.9.0 to 2.9.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6829
* Upgrade Quarkus to 3.17.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6830
* Move test support modules to integration-tests-support folder by @ppalaga in https://github.com/apache/camel-quarkus/pull/6833
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6835
* LangChain4j: Fix AI services that could not be resolved by interface … by @aldettinger in https://github.com/apache/camel-quarkus/pull/6839
* Bump quarkiverse-cxf.version from 3.17.0 to 3.17.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6841
* Upgrade camel to 4.9.0 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6836
* Use quarkus-bom managed async-http-client by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6840
* Revert "Workaround + fixed generator/resolver of bean catalog in maven-plugin" by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6843
* Add boot clock to CamelContext to capture boot time by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6844
* Upgrade Quarkus to 3.17.3 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6845
* Upgrade azure-core-http-vertx to 1.0.0-beta.24 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6846
* Upgrade to Quarkus CXF 3.17.2 by @ppalaga in https://github.com/apache/camel-quarkus/pull/6847

## 3.8.4

* [3.8.x] Update generated files after 3.8.3 release by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6270
* [3.8.x][fips] Jdbc -grouped - added fips profile for mysql by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6267
* [3.8.x] Fix #6301 to resort the beans if priority is same the default one win… by @zhfeng in https://github.com/apache/camel-quarkus/pull/6307
* [3.8.x] Workaround with registering OperatingSystemMXBeanSupport at RuntimeIn… by @zhfeng in https://github.com/apache/camel-quarkus/pull/6310
* [3.8.x] Move all enforcer invocations to the full profile by @ppalaga in https://github.com/apache/camel-quarkus/pull/6312
* [3.8.x] Avoid dependency misconvergence when extensions are used together by @ppalaga in https://github.com/apache/camel-quarkus/pull/6325
* [3.8.x] Fix MockBackendUtils native mode config resolution of CAMEL_QUARKUS_START_MOCK_BACKEND by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6347
* [3.8.x] Changed default location of generated certificates to target/certs by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6363
* Upgrade to cq-maven-plugin 4.6.11 by @ppalaga in https://github.com/apache/camel-quarkus/pull/6380
* [3.8.x] Upgrade Quarkus to 3.8.6 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6391
* Upgrade to Quarkus CXF 3.8.6 by @ppalaga in https://github.com/apache/camel-quarkus/pull/6396
* [3.8.x] FTP: Use restrictions to bypass cert setup issues by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6437
* [3.8.x] Fix paths to FHIR DSTU_2_1 & DSTU3 properties files by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6459
* [3.8.x] Upgrade to Quarkus CXF 3.8.7 by @ppalaga in https://github.com/apache/camel-quarkus/pull/6588
* [3.8.x] Upgrade Camel to 4.4.4 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6670
* [3.8.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6730
* [3.8.x] Backports + Upgrade FHIR core to 6.4.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6768
* [3.8.x] Backport release improvements by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6770

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.8.3...3.8.4

## 3.15.1

* [3.15.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6505
* [3.15.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6540
* [3.15.x] Upgrade Quarkus to 3.15.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6551
* [3.15.x] Workaround + fixed generator/resolver of bean catalog in maven-plugin by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6557
* [3.15.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6568
* [3.15.x] Upgrade to Quarkus CXF 3.15.2 by @ppalaga in https://github.com/apache/camel-quarkus/pull/6587
* [3.15.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6592
* [3.15.x] Remove incorrect assumptions about kamelets-catalog dependency scope from the extension documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6595
* [3.15.x] Mark .wasm files as binary in .gitattributes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6597
* [3.15.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6603
* [3.15.x] Fix and extend aws secret manager vault integration tests for real instance by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6617
* [3.15.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6622
* [3.15.x] Upgrade hapi-fhir-core dependencies to 6.3.23 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6626
* [3.15.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6634
* [3.15.x] Link to new cluster service extensions from extensions where the functionality used to reside by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6638
* [3.15.x] Increase MockEndpoint assertion wait timeout for JMS resequence test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6644
* [3.15.x] Disable SMB and Kudu on FIPS by @llowinge in https://github.com/apache/camel-quarkus/pull/6646
* [3.15.x] Fix Azure tests by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6649
* [3.15.x] Backport splunk by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6650
* [3.15.x] backports  by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6656
* [3.15.x] backports  by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6668
* [3.15.x] Add a brief explanation of yaml-io usage to the extension documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6675
* [3.15.x] Removal of crypto bcfips 3.15.x by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6678
* [3.15.x] Add a basic test for pipes by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6692
* [3.15.x] Upgrade Camel to 4.8.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6700
* [3.15] Fix #6701 to support OpenAPI spec with yaml format by @zhfeng in https://github.com/apache/camel-quarkus/pull/6712
* [3.15.x] Simplify splunk-hec test SSL setup by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6713

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.15.0...3.15.1

## 3.16.0

* Next is 3.16.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6490
* Avoid UDP port clashes in tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6491
* CamelQuarkusTestSupport restrictions during migration by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6494
* Add antora.yml to CI workflow ignored paths by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6495
* Bump quarkiverse-jsch.version from 3.0.10 to 3.0.11 by @dependabot in https://github.com/apache/camel-quarkus/pull/6496
* Bump org.amqphub.quarkus:quarkus-qpid-jms-bom from 2.6.1 to 2.7.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6497
* Switch to io.smallrye.certs:smallrye-certificate-generator-junit5 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6500
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 2.16.2 to 2.18.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6502
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6503
* Update heading in 3.15.0 migration guide by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6504
* Force Dependabot to get dependency metadata only from Maven Central by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6506
* Bump cq-plugin.version from 4.11.0 to 4.12.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6501
* Bump quarkiverse-langchain4j.version from 0.17.2 to 0.18.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6507
* Bump org.apache.maven.plugins:maven-remote-resources-plugin from 3.1.0 to 3.2.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6508
* Bump quarkiverse-groovy.version from 3.14.1 to 3.15.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6509
* Bump org.apache.maven.plugins:maven-scm-plugin from 2.0.0 to 2.1.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6510
* Bump org.apache.maven.plugins:maven-deploy-plugin from 3.1.1 to 3.1.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/6511
* Bump org.apache.maven.plugins:maven-jar-plugin from 3.3.0 to 3.4.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6512
* Bump org.apache.maven.plugins:maven-assembly-plugin from 3.6.0 to 3.7.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6513
* Bump org.apache.maven.plugins:maven-source-plugin from 3.2.1 to 3.3.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6515
* Bump org.apache.maven.plugins:maven-javadoc-plugin from 3.5.0 to 3.10.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6514
* Bump org.apache.maven.plugins:maven-install-plugin from 3.1.1 to 3.1.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/6518
* Bump org.apache.maven.plugins:maven-clean-plugin from 3.2.0 to 3.4.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6519
* Bump org.apache.maven.plugins:maven-plugin-plugin from 3.9.0 to 3.15.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6520
* Bump org.apache.maven.plugins:maven-release-plugin from 3.0.1 to 3.1.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6521
* Bump maven-surefire-plugin.version from 3.1.2 to 3.5.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6522
* Bump org.apache.maven.plugins:maven-gpg-plugin from 3.1.0 to 3.2.6 by @dependabot in https://github.com/apache/camel-quarkus/pull/6524
* Bump org.amqphub.quarkus:quarkus-qpid-jms-bom from 2.7.0 to 2.7.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6525
* Bump quarkiverse-cxf.version from 3.15.0 to 3.15.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6526
* Bump org.apache.maven.plugins:maven-shade-plugin from 3.5.0 to 3.6.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6527
* Upgrade Debezium to 2.7.3.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6529
* Add commons-collections4 to FHIR extension to fix native compilation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6533
* Add missing quarkus-netty dependency to Dropbox extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6535
* Add jakarta.jms-api to SJMS extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6537
* Add missing camel-quarkus-support-aws2 dependency to aws-bedrock & aws2-eventbridge extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6539
* Configure maven-deploy-plugin to retry on failure by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6541
* Avoid including redundant modules when deploying SNAPSHOTs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6542
* Add Camel Quarkus LangChain4j extension #6534 by @aldettinger in https://github.com/apache/camel-quarkus/pull/6544
* Bump actions/setup-java from 4.3.0 to 4.4.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6545
* Bump quarkiverse-minio.version from 3.7.6 to 3.7.7 by @dependabot in https://github.com/apache/camel-quarkus/pull/6546
* Bump actions/checkout from 4.1.7 to 4.2.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6549
* Bump org.cyclonedx:cyclonedx-maven-plugin from 2.8.1 to 2.8.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6548
* Bump com.mycila:license-maven-plugin from 4.5 to 4.6 by @dependabot in https://github.com/apache/camel-quarkus/pull/6547
* Upgrade Quarkus to 3.15.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6550
* Add changelog for 3.15.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6552
* Upgrade Quarkus Langchain4j to 0.19.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6555
* Fix camel annotated parameters that were not usable as template varia… by @aldettinger in https://github.com/apache/camel-quarkus/pull/6558
* Workaround + fixed generator/resolver of bean catalog in maven-plugin by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6554
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.4.2 to 3.5.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6561
* Bump org.apache.maven.plugins:maven-gpg-plugin from 3.2.6 to 3.2.7 by @dependabot in https://github.com/apache/camel-quarkus/pull/6562
* Bump quarkiverse-langchain4j.version from 0.19.0.CR1 to 0.19.0.CR3 by @dependabot in https://github.com/apache/camel-quarkus/pull/6563
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6564
* Rework client SSL configuration in hashicorp-vault tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6567
* Deprecated bits related to development of AbstractTestSupport by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6566
* Bump quarkiverse-pooled-jms.version from 2.5.0 to 2.6.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6570
* Bump quarkiverse-langchain4j.version from 0.19.0.CR3 to 0.20.0.CR1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6569
* Make BindToRegistry work outside of RouteBuilder classes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6571
* Bump quarkiverse-langchain4j.version from 0.20.0.CR1 to 0.20.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6572
* Bump org.apache.maven.plugins:maven-javadoc-plugin from 3.10.0 to 3.10.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6574
* Make Camel Tracer beans unremovable by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6578
* Bump quarkiverse-langchain4j.version from 0.20.1 to 0.20.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/6580
* Fix build time processing of Kamelets with bean definitions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6583
* Upgrade Debezium to 3.0.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6585
* Upgrade quarkus-jackson-jq to 2.1.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6586
* Upgrade to Quarkus CXF 3.15.2 by @ppalaga in https://github.com/apache/camel-quarkus/pull/6589
* Bump cq-plugin.version from 4.12.0 to 4.13.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6590
* Mark .wasm files as binary in .gitattributes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6596
* Bump actions/cache from 4.0.2 to 4.1.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6599
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6600
* Move swagger-codegen dependencies to rest-openapi deployment module by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6594
* Add tests for context reloading by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6598
* Bump actions/checkout from 4.2.0 to 4.2.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6605
* Bump actions/upload-artifact from 4.4.0 to 4.4.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6606
* Bump maven-surefire-plugin.version from 3.5.0 to 3.5.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6607
* Link to Camel 4.8.x documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6611
* Fix and extend aws secret manager vault integration tests for real in… by @llowinge in https://github.com/apache/camel-quarkus/pull/6609
* Bump org.cyclonedx:cyclonedx-maven-plugin from 2.8.2 to 2.9.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6616
* Bump actions/cache from 4.1.0 to 4.1.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6615
* Bump actions/upload-artifact from 4.4.1 to 4.4.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6614
* fixes #6608 - deprecated groovy-dsl as is deprecated in Camel by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6612
* Set sponsor field in extension metadata by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6620
* Bump actions/upload-artifact from 4.4.2 to 4.4.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/6621
* Upgrade hapi-fhir-core dependencies to 6.3.23 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6625
* Bump cq-plugin.version from 4.13.0 to 4.13.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6627
* Bump quarkiverse-jgit.version from 3.1.3 to 3.2.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6628
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6629
* Increase BeanIO test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6631
* Remove netty-tcnative-boringssl-static from support-azure-core extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6635
* Link to new cluster service extensions from extensions where the functionality used to reside by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6636
* Bump quarkiverse-jgit.version from 3.2.2 to 3.3.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6637
* Fix Azure tests by @llowinge in https://github.com/apache/camel-quarkus/pull/6639
* Increase MockEndpoint assertion wait timeout for JMS resequence test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6641
* langchain4j: document camel parameter binding usage by @aldettinger in https://github.com/apache/camel-quarkus/pull/6643
* Disable SMB and Kudu on FIPS by @llowinge in https://github.com/apache/camel-quarkus/pull/6645
* Upgrade Quarkus to 3.16.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6648
* Splunk, splunk-hec SSL tests by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6314
* Bump io.swagger.codegen.v3:swagger-codegen-generators from 1.0.52 to 1.0.53 by @dependabot in https://github.com/apache/camel-quarkus/pull/6652
* Add header details and examples to Qute documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6654
* Skip tests for unsupported services by @avano in https://github.com/apache/camel-quarkus/pull/6640
* langchain4j: fix link to camel core parameter binding usage by @orpiske in https://github.com/apache/camel-quarkus/pull/6659
* Fix Camel header documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6662
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6665
* Mysql testResource instead of devservices in FIPS by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6658
* FHIR documentation & test improvements by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6666
* Fix check for ServiceRemoveEvent observers by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6667
* Bump cq-plugin.version from 4.13.1 to 4.14.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6671
* Add a brief explanation of yaml-io usage to the extension documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6674
* Add BeanIO native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6672
* Fixes #6676, removal of classifier bcfips from the crypto dependencies by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6677
* Bump actions/cache from 4.1.1 to 4.1.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6679
* Bump org.codehaus.mojo:exec-maven-plugin from 3.4.1 to 3.5.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6680
* Bump org.wiremock:wiremock-standalone from 3.9.1 to 3.9.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6681
* Add a basic test for pipes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6683
* Transform the configuration options documentation from vanila JavaDoc to @asciidoclet by @ppalaga in https://github.com/apache/camel-quarkus/pull/6684
* Bump quarkiverse-jgit.version from 3.3.0 to 3.3.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6685
* Bump actions/checkout from 4.2.1 to 4.2.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6686
* Upgrade Quarkus to 3.16.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6687
* Upgrade Camel to 4.8.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6689
* Upgrade Quarkus CXF to 3.16.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6693
* Bump quarkiverse-cxf.version from 3.16.0 to 3.16.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6696
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.5.0 to 3.6.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6697
* Bump actions/setup-java from 4.4.0 to 4.5.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6694
* Bump cq-plugin.version from 4.14.1 to 4.14.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6695

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.15.0...3.16.0

## 3.15.0

* Clarify the performance regression steps in the release procedure by @aldettinger in https://github.com/apache/camel-quarkus/pull/6373
* Next is 3.15.0-SNAPSHOT by @aldettinger in https://github.com/apache/camel-quarkus/pull/6372
* Increase azure-eventhubs test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6371
* Recommend writing the release announcement before sending the platform PR by @ppalaga in https://github.com/apache/camel-quarkus/pull/6375
* Extend Spring-rabbitmq coverage by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6377
* Upgrade Debezium to 2.7.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6378
* Upgrade Quarkus CXF to 3.14.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6379
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6385
* Bump cq-plugin.version from 4.10.1 to 4.10.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6384
* Ban com.sun.xml.bind:jaxb-core and jaxb-impl to avoid clash with org.glassfish.jaxb:jaxb-core and jaxb-runtime by @ppalaga in https://github.com/apache/camel-quarkus/pull/6382
* Bump quarkiverse-freemarker.version from 1.0.0 to 1.1.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6383
* Fix the staging repository template name in the release procedure by @aldettinger in https://github.com/apache/camel-quarkus/pull/6386
* Fix hashicorp-vault extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6255
* Combine knative tests into a single module by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6387
* Remove camel-k-runtime extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6388
* Update the project changelog with 3.14.0 release by @aldettinger in https://github.com/apache/camel-quarkus/pull/6389
* Replace quarkus-test-artemis with dev services container by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6390
* Revert back to QuarkusTestResource by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6394
* Rebalance some test categories after recent module removal by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6392
* Bump quarkiverse-groovy.version from 3.12.1 to 3.13.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6397
* Bump quarkiverse-langchain4j.version from 0.17.0 to 0.17.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6398
* Fix the camel-quarkus-examples release procedure to handle multi-modu… by @aldettinger in https://github.com/apache/camel-quarkus/pull/6395
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6400
* Upgrade Quarkus to 3.14.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6401
* Add hashicorp-vault native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6403
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.4.1 to 3.4.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6404
* Bump quarkiverse-langchain4j.version from 0.17.1 to 0.17.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6405
* Github Actions Security Best practices: Pin Actions to Full lenght C… by @oscerd in https://github.com/apache/camel-quarkus/pull/6406
* Github Actions Security Best practices: Pin Actions to Full lenght C… by @oscerd in https://github.com/apache/camel-quarkus/pull/6407
* Github Actions Security Best practices: Pin Actions to Full lenght C… by @oscerd in https://github.com/apache/camel-quarkus/pull/6408
* Github Actions Security Best practices: Pin Actions to Full lenght C… by @oscerd in https://github.com/apache/camel-quarkus/pull/6409
* Github Actions Security Best practices: Pin Actions to Full lenght C… by @oscerd in https://github.com/apache/camel-quarkus/pull/6410
* Github Actions Security Best practices: Pin Actions to Full lenght C… by @oscerd in https://github.com/apache/camel-quarkus/pull/6411
* Github Actions Security Best practices: Pin Actions to Full lenght C… by @oscerd in https://github.com/apache/camel-quarkus/pull/6412
* Github Actions Security Best practices: Pin Actions to Full lenght C… by @oscerd in https://github.com/apache/camel-quarkus/pull/6413
* Test hashicorp-vault with HTTPS scheme by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6414
* [Tests] FTP: Use restrictions to bypass cert setup issues by @avano in https://github.com/apache/camel-quarkus/pull/6415
* Avoid overwriting existing Camel GAV catalog metadata by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6416
* Bump peter-evans/create-pull-request from 6.1.0 to 7.0.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6417
* Use quarkus-micrometer-registry-prometheus instead of the plain io.micrometer dependency by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6419
* Upgrade Quarkus to 3.14.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6421
* Enable vertx-websocket extension to handle Quarkus TLS Registry configuration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6420
* Fix NPE in SupportSwaggerProcessor when sources do not include a package statement by @apupier in https://github.com/apache/camel-quarkus/pull/6424
* Fix and add tests camel flink extension on JVM mode  by @svkcemk in https://github.com/apache/camel-quarkus/pull/6422
* Removed workaround for strimzi kafka container and JDK 17 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6426
* Bump net.revelc.code:impsort-maven-plugin from 1.11.0 to 1.12.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6427
* Bump quarkiverse-groovy.version from 3.13.0 to 3.14.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6428
* Bump peter-evans/create-pull-request from 7.0.0 to 7.0.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6429
* Extend validator test coverage by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6431
* Use target as the temporary directory location for the Flink mini cluster by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6432
* Bump quarkiverse-mybatis.version from 2.2.3 to 2.2.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/6434
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6436
* Fix paths to FHIR DSTU_2_1 & DSTU3 properties files by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6442
* Bump actions/setup-java from 4.2.2 to 4.3.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6443
* Deprecate kotlin extension #6444 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6445
* Suppress camel component doc xref for kotlin-dsl extension due to #6448 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6449
* Upgrade Quarkus to 3.15.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6450
* Upgrade Debezium to 2.7.2.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6451
* Add migration guide to 3.15.0 release by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6452
* Fix wiremock dependency name in Depandabot configuration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6453
* Bump peter-evans/create-pull-request from 7.0.1 to 7.0.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6454
* Upgrade WireMock to 3.9.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6456
* Bump quarkiverse-jsch.version from 3.0.9 to 3.0.10 by @dependabot in https://github.com/apache/camel-quarkus/pull/6458
* Upgrade IBM JMS Client to 9.4.0.5 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6460
* Bump quarkiverse-jgit.version from 3.1.2 to 3.1.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/6461
* Bump cq-plugin.version from 4.10.2 to 4.11.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6462
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6463
* Upgrade Camel to 4.8.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6439
* Activate Camel dev profile when running in development mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6464
* Discover Datasonnet libraries at build time by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6465
* Enable testing with Azure Event Hubs Emulator by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6466
* Use quarkus-bom managed bcprov-jdk18on instead of bcprov-ext-jdk18on by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6467
* Reflect correct usage of CXF SOAP in routes in tests by @llowinge in https://github.com/apache/camel-quarkus/pull/6457
* Remove azure-eventhubs shared access configuration for AZURE_IDENTITY credentials test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6468
* Add new langchain4j-(tokenizer|tools|web-search) extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6469
* Miscellaneous documentation tidy-ups by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6474
* Miscellaneous dependency upgrades & tidy-ups by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6473
* Bump quarkiverse-groovy.version from 3.14.0 to 3.14.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6475
* Bump quarkiverse-minio.version from 3.7.5 to 3.7.6 by @dependabot in https://github.com/apache/camel-quarkus/pull/6476
* Bump peter-evans/create-pull-request from 7.0.2 to 7.0.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/6477
* Bump io.swagger.codegen.v3:swagger-codegen-generators from 1.0.51 to 1.0.52 by @dependabot in https://github.com/apache/camel-quarkus/pull/6478
* Remove redundant disableXmlReifiers build step by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6480
* Align test container images with Camel & Quarkus by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6481
* Bump com.azure:azure-core-http-vertx from 1.0.0-beta.20 to 1.0.0-beta.21 by @dependabot in https://github.com/apache/camel-quarkus/pull/6482
* Remove kotlin-dsl reference from defining camel routes guide by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6483
* Upgrade Quarkus to 3.15.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6484
* Add Camel service inclusion pattern for tokenizer by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6485
* Bump peter-evans/create-pull-request from 7.0.3 to 7.0.5 by @dependabot in https://github.com/apache/camel-quarkus/pull/6486
* Upgrade Quarkus CXF to 3.15.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6487

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.14.0...3.15.0

## 3.14.0

* Migrate from deprecated QuarkusTestResource to WithTestResource by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6295
* [Cassandra] Increase stack size for test container by @avano in https://github.com/apache/camel-quarkus/pull/6296
* Next is 3.14.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6297
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6298
* Fix incorrect endpoint paths in REST producer test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6299
* Add changelog for 3.13.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6305
* Strip label prefix in auto label workflow before performing GraphQL search query by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6306
* Fix #6301 to re-sort the beans by @zhfeng in https://github.com/apache/camel-quarkus/pull/6304
* cxf-soap SSL tests fail with Quarkus CXF 3.13.0  by @ppalaga in https://github.com/apache/camel-quarkus/pull/6303
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.3.0 to 3.4.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6308
* GithHub Workflow tidy ups by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6309
* Bump hamcrest.version from 2.2 to 3.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6313
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.4.0 to 3.4.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6316
* Bump com.azure:azure-core-http-vertx from 1.0.0-beta.19 to 1.0.0-beta.20 by @dependabot in https://github.com/apache/camel-quarkus/pull/6315
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6317
* Upgrade CycloneDX Maven plugin to version 2.8.1 by @oscerd in https://github.com/apache/camel-quarkus/pull/6318
* Move all enforcer invocations to the full profile by @ppalaga in https://github.com/apache/camel-quarkus/pull/6311
* Fix #6300 to remove @AfterEach by @zhfeng in https://github.com/apache/camel-quarkus/pull/6319
* Avoid deploying redundant modules during the release by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6320
* Migrate to com.mysql:mysql-connector-j for debezium-mysql by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6321
* Bump cq-plugin.version from 4.9.1 to 4.9.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6323
* Bump quarkiverse-pooled-jms.version from 2.4.0 to 2.5.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6322
* Avoid dependency misconvergence when extensions are used together by @ppalaga in https://github.com/apache/camel-quarkus/pull/6324
* Bump org.codehaus.mojo:exec-maven-plugin from 3.3.0 to 3.4.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6326
* Restore Aws2KinesisFirehoseTest by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6330
* Bump quarkiverse-langchain4j.version from 0.16.4 to 0.17.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6332
* Mark modules using legacy @ConfigRoot as such by @gsmet in https://github.com/apache/camel-quarkus/pull/6331
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 2.16.0 to 2.16.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6333
* Fix autolabel workflow label search by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6335
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6336
* Extracion of crypto-pgp and making crypto work on FIPS by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6241
* Reinstate Aws2KinesisFirehoseTest against localstack by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6337
* Fix MockBackendUtils native mode config resolution of CAMEL_QUARKUS_START_MOCK_BACKEND by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6340
* Bump org.codehaus.mojo:exec-maven-plugin from 3.4.0 to 3.4.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6343
* Move mock-backend support classes into camel-quarkus-integration-test-support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6345
* Add capability to set the localstack log level by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6346
* Upgrade Quarkus to 3.14.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6349
* Adapt update-extension-doc-page mojo to Quarkus 3.14.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/6351
* bump quarkiverse-jsch.version from 3.0.8 to 3.0.9 by @dependabot in https://github.com/apache/camel-quarkus/pull/6352
* bump cq-plugin.version from 4.9.2 to 4.10.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6353
* Bump quarkiverse-tika.version from 2.0.2 to 2.0.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/6358
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6359
* Temporarily remove duration & memory size summary links from generated docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6361
* Using filesystem instead of classpath for certificates from target/certs by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6356
* Bump cq-plugin.version from 4.10.0 to 4.10.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6366
* Add missing Duration and MemSize type notes to the docs by @ppalaga in https://github.com/apache/camel-quarkus/pull/6365
* Upgrade to Quarkus 3.14.0 by @aldettinger in https://github.com/apache/camel-quarkus/pull/6370
* added camel-quarkus-javascript by @mweissdigchg in https://github.com/apache/camel-quarkus/pull/6369

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.13.0...3.14.0

## 3.13.0

* Next is 3.13.0-SNAPSHOT by @aldettinger in https://github.com/apache/camel-quarkus/pull/6215
* Add default value of modePackage in camel-quarkus-rest-openapi by @zhfeng in https://github.com/apache/camel-quarkus/pull/6217
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6219
* Update changelog for 3.12.0 release by @aldettinger in https://github.com/apache/camel-quarkus/pull/6220
* Clean release procedure by @aldettinger in https://github.com/apache/camel-quarkus/pull/6221
* Improve the testing guide by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6222
* Miscellaneous test framework tidy ups by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6223
* Bump me.escoffier.certs:certificate-generator-junit5 from 0.6.0 to 0.7.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6225
* Fix release procedure by @aldettinger in https://github.com/apache/camel-quarkus/pull/6226
* Document that ConsumerTemplate, ProducerTemplate & Registry can be injected into CDI beans by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6227
* Set -Xmx4600m for Jenkins build jobs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6231
* Upgrade to maven-enforcer-plugin 3.5.0 and cq-maven-plugin 4.9.0 and fix dependency convergence issues by @ppalaga in https://github.com/apache/camel-quarkus/pull/6228
* Ensure additional caffeine cache classes are registered for reflection if stats are enabled by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6233
* Bump quarkiverse-groovy.version from 3.11.0 to 3.12.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6235
* Bump net.revelc.code:impsort-maven-plugin from 1.10.0 to 1.11.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6236
* Bump quarkus-mybatis to 2.2.3 by @zhfeng in https://github.com/apache/camel-quarkus/pull/6237
* Perf tool updates by @aldettinger in https://github.com/apache/camel-quarkus/pull/6240
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6242
* Fix Gradle dev mode execution for gRPC extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6243
* Use correct table naming pattern common for more db types by @llowinge in https://github.com/apache/camel-quarkus/pull/6246
* Add azure-key-vault native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6249
* Bump quarkiverse-minio.version from 3.7.3 to 3.7.5 by @dependabot in https://github.com/apache/camel-quarkus/pull/6252
* Bump cq-plugin.version from 4.9.0 to 4.9.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6251
* Remove OpenAPI V2 integration test from camel-k-maven-plugin by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6253
* Use container host instead of localhost for ElasticSearch tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6257
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6258
* Add changelog for 3.8.3 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6259
* Document Jandex requirement when adding routes defined in external JARs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6261
* Add error handling section to platform-http extension docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6260
* Log the Camel Quarkus version on startup by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6216
* Remove prefix from shell code snippets by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6262
* Mention JDK 17 as the minimum required version in first steps guide by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6263
* [fips] Jdbc-grouped - added fips profile for mysql by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6266
* Bump quarkiverse-langchain4j.version from 0.15.1 to 0.16.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6269
* Bump quarkiverse-groovy.version from 3.12.0 to 3.12.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6274
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6275
* Disabled jdbc/db2 test for native in FIPS by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6272
* Bump quarkiverse-langchain4j.version from 0.16.2 to 0.16.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/6276
* Revert "Auto generated changes for dependabot commit 309e723fec9adfd9… by @aldettinger in https://github.com/apache/camel-quarkus/pull/6277
* Upgrade camel to 4.7.0 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6264
* Upgrade Quarkus to 3.13.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6280
* Add beans to the catalog by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6281
* Configure elasticsearch-rest-client using component options by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6282
* Upgrade azure-core-http-vertx to 1.0.0-beta.19 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6283
* Upgrade quarkus-langchain4j to 0.16.4 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6284
* Replace aws2-kinesis native substitutions with bytecode transformation #6238 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6286
* Remove redundant microprofile-fault-tolerance dependencies from foundation-grouped tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6287
* Disable commons-pool2 JMX MBean registration in native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6292
* Upgrade Quarkus to 3.13.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6293
* Improve Quarkus LangChain4j version alignment #6288 by @aldettinger in https://github.com/apache/camel-quarkus/pull/6289

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.12.0...3.13.0

## 3.8.3

* [3.8.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6064
* [3.8.x] Use more correct convention with configuring datasource by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6101
* [3.8] Upgrade to Quarkus CXF 3.8.4 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6124
* [3.8.x] Upgrade Quarkus to 3.8.5 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6156
* [3.8.x] Upgrade upload & download GitHub actions to v4 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6162
* [3.8.x] Fixed flaky aws2 cw test with real service by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6174
* [3.8.x] Fix flaky storage queue crud test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6184
* [3.8.x] Added some small logs to Aws2KinesisFirehoseTest for better debugging by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6189
* [3.8.x] [Slack] Decrease delay when getting messages (#6197) by @zhfeng in https://github.com/apache/camel-quarkus/pull/6201
* [3.8.x] Missing fixes for fips testing by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6209
* [3.8.x] Test support certificate by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6213
* [3.8.x] Upgrade Camel to 4.4.3 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6232
* [3.8.x] Upgrade to maven-enforcer-plugin 3.5.0 and cq-maven-plugin 4.9.0 and fix dependency convergence issues by @ppalaga in https://github.com/apache/camel-quarkus/pull/6234
* [3.8.x] Fix Gradle dev mode execution for gRPC extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6244
* [3.8.x] Use correct table naming pattern common for more db types by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6247
* [3.8.x] Upgrade Quarkus CXF to 3.8.5 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6250

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.8.2...3.8.3

## 3.12.0

* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6120
* Bump net.revelc.code:impsort-maven-plugin from 1.9.0 to 1.10.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6122
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 2.15.0 to 2.16.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6121
* Upgrade azure-core-http-vertx to 1.0.0-beta.18 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6123
* Remove hard coded version from langchain4j-ollama by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6129
* Remove unused version properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6131
* Bump net.revelc.code.formatter:formatter-maven-plugin from 2.23.0 to 2.24.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6135
* Use more robust type check in test report action for test case names by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6134
* MySql test does not work on FIPS enabled system - native by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6137
* Next is 3.12.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6138
* Fix release guide command for regenerating files post SNAPSHOT version bump by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6139
* Supporting GrapghQL by adding more tests by @spatnity in https://github.com/apache/camel-quarkus/pull/6132
* Add langchain4j-chat native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6140
* Fix #5824 to add camel trace config properties by @zhfeng in https://github.com/apache/camel-quarkus/pull/6130
* Remove duplicated dependency from tests-support-kafka by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6141
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6144
* Test support certicate by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6116
* Add traceProcessors option to OpenTelemetry extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6146
* Enable relocations profile when building Quarkus by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6149
* Fix auto label workflow GraphQL query by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6150
* Bump quarkiverse-minio.version from 3.7.1 to 3.7.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/6152
* Update Maven wrapper distribution URL to Maven 3.9.7 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6153
* Bump net.revelc.code.formatter:formatter-maven-plugin from 2.24.0 to 2.24.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6161
* Bump quarkiverse-cxf.version from 3.11.0 to 3.11.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6160
* Add missing HTTP client dependency to azure-key-vault extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6158
* Remove redundant integration test READMEs relating to certificate generation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6166
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6168
* Increase elasticsearch-rest-client test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6165
* Use certificate-generator for gRPC integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6167
* Fixed flaky aws2 cw test with real service by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6170
* Bump quarkiverse-langchain4j.version from 0.14.2 to 0.15.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6175
* Add elasticsearch-rest-client native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6176
* Upgrade Debezium to 2.6.2.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6178
* Upgrade Quarkus to 3.12.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6181
* Added aws2-kinesis-firehose test service dependency STS by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6183
* [Azure] Fix flaky storage queue crud test by @avano in https://github.com/apache/camel-quarkus/pull/6180
* Bump quarkiverse-groovy.version from 3.10.0 to 3.11.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6187
* Added more logging to Aws2KinesisFirehoseTest because of flaky tests by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6186
* Add httpclient5 support extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6188
* Bump quarkiverse-minio.version from 3.7.2 to 3.7.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/6192
* Bump quarkiverse-jgit.version from 3.1.0 to 3.1.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6193
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6194
* Use LazySecretKeysHandler for Jasypt Config by @radcortez in https://github.com/apache/camel-quarkus/pull/6191
* Bump quarkiverse-jsch.version from 3.0.7 to 3.0.8 by @dependabot in https://github.com/apache/camel-quarkus/pull/6200
* [Slack] Decrease delay when getting messages by @avano in https://github.com/apache/camel-quarkus/pull/6197
* Using WireMock in weather extension integration tests #6047 by @spatnity in https://github.com/apache/camel-quarkus/pull/6202
* Upgrade Quarkus Jgit version to 3.1.2 by @aldettinger in https://github.com/apache/camel-quarkus/pull/6203
* Bump com.unboundid:unboundid-ldapsdk from 7.0.0 to 7.0.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/6205
* Add Pinecone extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6206
* Update Maven wrapper distribution URL to Maven 3.9.8 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6207
* Auto label issues relating to flaky tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6208
* Upgrade Quarkus to 3.12.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6210
* Upgrade to Quarkus CxF 3.12.0 by @aldettinger in https://github.com/apache/camel-quarkus/pull/6211

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.11.0...3.12.0

## 3.11.0

* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 2.13.1 to 2.14.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6038
* Next is 3.11.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6039
* Remove references to redundant yarn build arguments by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6040
* Migrate from deprecated quarkus.package.type to quarkus.native.enabled by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6044
* [LRA] Fix test when running with DOCKER_HOST by @avano in https://github.com/apache/camel-quarkus/pull/6042
* [CXF] Fix exception message expectation on windows by @avano in https://github.com/apache/camel-quarkus/pull/6043
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6048
* Bump quarkiverse-groovy.version from 3.9.3 to 3.10.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6053
* Add xml-jaxb extension back to management extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6051
* Move slow Splunk tests to separate test categories by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6052
* Add changelog for 3.10.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6055
* Remove superfluous usage of System.out.println by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6060
* Register @PropertyInject classes for reflection by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6056
* MySql test does not work on FIPS enabled system by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6063
* Add changelog for 3.8.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6068
* Langchain-chat extension by @spatnity in https://github.com/apache/camel-quarkus/pull/6066
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6069
* Enforce usage of Swagger Jakarta compatible dependencies by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6074
* Increase Xmx to 4600m for initial-mvn-install job by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6077
* Upgrade Camel to 4.6.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6071
* Align container image versions with Camel & Quarkus by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6078
* Add GitHub Action to report test failures in the workflow summary by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6076
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6079
* Update license-maven-plugin to 4.5 by @zhfeng in https://github.com/apache/camel-quarkus/pull/6082
* Document available configuration methods for the Infinispan extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6084
* Add debugging to test-summary-report action by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6085
* Bump quarkiverse-pooled-jms.version from 2.3.1 to 2.4.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6086
* Migrate from camel.main.debugging to camel.debug config prefix by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6087
* Disabled Crypto tests in FIPS by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6089
* Split MicroProfile integration tests into separate modules by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6092
* Auto label issues relating to FIPS by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6093
* Upgrade Quarkus to 3.11.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6094
* Increase Xmx to 3500m for Validate PR workflow by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6095
* Clean up leftovers from removed non-camel-main based runtime by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6099
* Use more correct convention with configuring datasource by @llowinge in https://github.com/apache/camel-quarkus/pull/6098
* Pin test report action junit2json package version to 3.1.7 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6100
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6105
* Use a consistent release version for CompilationProvider by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6107
* Upgrade jolokia to 2.0.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6110
* Bump org.codehaus.mojo:build-helper-maven-plugin from 3.5.0 to 3.6.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6112
* Add support for openapi-contract-first development by @zhfeng in https://github.com/apache/camel-quarkus/pull/6109
* Add tests for supervised routes with MicroProfile health by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6108
* Fix test report action handling of error & failure objects by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6113
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 2.14.0 to 2.15.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6070
* Bump org.codehaus.mojo:exec-maven-plugin from 3.2.0 to 3.3.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6114
* Add contract first development section in rest-openapi doc by @zhfeng in https://github.com/apache/camel-quarkus/pull/6115
* kafka-ssl in fips by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6091
* Upgrade Quarkus to 3.11.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6117
* Upgrade to Quarkus CXF 3.11.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/6119
* Bump me.escoffier.certs:certificate-generator-junit5 from 0.5.0 to 0.6.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/6118

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.10.0...3.11.0

## 3.8.2

* [3.8.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5892
* [3.8.x] Register Mixin classes for reflection (#5898) by @zhfeng in https://github.com/apache/camel-quarkus/pull/5900
* [3.8.x] Manage com.sun.xml.fastinfoset:FastInfoset by @zhfeng in https://github.com/apache/camel-quarkus/pull/5906
* jt400 com.ibm.as400.access.AS400 should be registered for runtime reinit by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5913
* [3.8.x] Fix potential UnsatisfiedLinkError for Azure extensions in native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5919
* Separation of jt400 test module into mocked one integration one. (#5915) by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5920
* JT400: Use better name of workspace in the readme.adoc by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5929
* [3.8.x] Upgrade QCXF to 3.8.2 by @zhfeng in https://github.com/apache/camel-quarkus/pull/5933
* [3.8.x] Remove quarkus-cxf-rt-features-logging since it is deprecated (#5937) by @zhfeng in https://github.com/apache/camel-quarkus/pull/5941
* [3.8.x] Reinstate Kudu tablet server host resolution workaround by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5943
* [3.8.x] JT400 Inquiry test by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5955
* [3.8.x] Removed jsch container and netty increased proxy connections by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5963
* [3.8.x] Improve the testability against Quarkus Platform BOMs by @ppalaga in https://github.com/apache/camel-quarkus/pull/5960
* [3.8.x] Http: use FIPS complaiant keystore and truststore by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5969
* Upgrade to Quarkus CXF 3.8.3 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5988
* [3.8.x] jdbc-db2: fails in fips environment #5993 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5996
* [3.8.x] Jt400: tests are not cleaning after themselves and parallel run fails by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6003
* [3.8.x] Dependency management improvements by @ppalaga in https://github.com/apache/camel-quarkus/pull/6006
* Upgrade to cq-maven-plugin 4.6.8 by @ppalaga in https://github.com/apache/camel-quarkus/pull/6008
* [3.8.x] Upgrade Quarkus to 3.8.4 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6010
* [3.8.x] Cxf-soap: tests are not working in FIPS environment by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5982
* [3.8.x] JT400 tests can not be run in parallel #6018 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6022
* [3.8.x] Jt400: possible missing resource in the native by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6031
* [3.8.x] Backport by @zhfeng in https://github.com/apache/camel-quarkus/pull/6045
* [3.8.x] Upgrade Camel to 4.4.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6046

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.8.1...3.8.2

## 3.10.0

* update release plugin version by @aldettinger in https://github.com/apache/camel-quarkus/pull/5910
* Next is 3.10.0-SNAPSHOT by @aldettinger in https://github.com/apache/camel-quarkus/pull/5909
* jt400AS400 should be registered for runtime reinitialization by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5908
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5918
* Separation of jt400 test module into mocked one integration one. by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5915
* Fix potential UnsatisfiedLinkError for Azure extensions in native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5916
* Next is 3.10.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5925
* JT400: Use better name of workspace in the readme.adoc by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5928
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 2.12.1 to 2.13.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/5917
* Bump org.cyclonedx:cyclonedx-maven-plugin from 2.7.11 to 2.8.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5930
* Update CHANGELOG.md for Camel Quarkus 3.9.0 release by @aldettinger in https://github.com/apache/camel-quarkus/pull/5931
* quarkus master split by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/5922
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5935
* Upgrade camel to 4.5.0 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5921
* Fix docs symbolic link to camel-quarkus-qute.json by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5940
* Remove quarkus-cxf-rt-features-logging since it is deprecated by @zhfeng in https://github.com/apache/camel-quarkus/pull/5937
* Fix doc links for components with multiple URI schemes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5942
* Reinstate Kudu tablet server host resolution workaround by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5938
* Upgrade cq-maven-plugin to 4.7.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5939
* Bump quarkiverse-groovy.version from 3.8.0 to 3.9.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/5936
* Link to Camel SNAPSHOT docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5944
* Adjust UpdateExtensionDocPageMojo to filter on supported catalog model kinds by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5947
* Add Micrometer naming strategy and route policy level configuration options by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5951
* Enable CSimple tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5952
* Promote Qdrant extension to native #5815 by @aldettinger in https://github.com/apache/camel-quarkus/pull/5948
* Add tests for FastCamelContext JSON schema lookups by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5956
* Remove jsch container DOCKER_MODS configuration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5958
* JT400 Inquiry test by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5954
* [NettyHttp] Increase proxy connection timeout and make it configurable by @avano in https://github.com/apache/camel-quarkus/pull/5959
* Improve the testability against Quarkus Platform BOMs by @ppalaga in https://github.com/apache/camel-quarkus/pull/5961
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5974
* Camel 4.5 : create langchain-embeddings extension by @zbendhiba in https://github.com/apache/camel-quarkus/pull/5972
* Temporarily rename any langchain doc xrefs to langchain4j by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5977
* Fix auto milestone workflow title regex by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5976
* Http: use FIPS complaiant keystore and truststore by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5968
* Remove findbugs exclusion from quarkus-grpc-common in gRPC extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5978
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.2.1 to 3.3.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5984
* Restore file tests original class naming by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5985
* Create AWS bedrock extensions by @oscerd in https://github.com/apache/camel-quarkus/pull/5987
* Fix various ClassNotFoundExceptions for spring-redis extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5989
* Tidy openapi-java tests after removal of support for OpenAPI V2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5991
* Tidy logging for openapi-java expose option build time code by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5992
* jdbc-db2: fails in fips environment #5993 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5994
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5997
* Upgrade upload & download GitHub actions to v4 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5998
* Jt400: tests are not cleaning after themselves and parallel run fails by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6001
* Dependency management improvements by @ppalaga in https://github.com/apache/camel-quarkus/pull/6007
* Use xml-io-dsl instead of xml-jaxb when using the management extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6002
* Document how to generate a new example project by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6004
* Upgrade Quarkus to 3.10.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6005
* Upgrade to Quarkus CXF 3.10.0.CR1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/6009
* Cxf-soap: tests are not working in FIPS environment by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5980
* Enable xchange tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6011
* Add Wasm extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6012
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/6014
* Fix nightly CI workflow alternate JDK steps by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6015
* Create Milvus Extension by @oscerd in https://github.com/apache/camel-quarkus/pull/6020
* Fixup Milvus JVM only state by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6023
* Removed Qdrant leftover test in JVM Integration tests by @oscerd in https://github.com/apache/camel-quarkus/pull/6024
* Bump quarkiverse-groovy.version from 3.9.1 to 3.9.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/6026
* JT400 tests can not be run in parallel #6018 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6019
* Make Jasypt SmallRye Config integration operate only on runtime properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6017
* Activate format profile as part of CI checks by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6027
* Jt400: possible missing resource in the native by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6030
* Fix AsciiDoc attribute substitution in MapStruct and Jasypt documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6032
* Upgrade Quarkus to 3.10.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6033
* Remove workaround for Google BigQuery host resolution by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6034
* Upgrade to Quarkus CXF 3.10.0 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/6035
* Exclude faulttolerance package from MicroProfile integration test module javadoc by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/6036

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.9.0...3.10.0

## 3.9.0

* Avoid updating quarkus.version in antora.yml on the quarkus-main branch by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5787
* Update release guide by @aldettinger in https://github.com/apache/camel-quarkus/pull/5790
* Next is 3.9.0-SNAPSHOT by @aldettinger in https://github.com/apache/camel-quarkus/pull/5789
* Add JUnit conditions for FIPS mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5791
* Remove maven-deploy-plugin configuration deprecated since maven 3 by @aldettinger in https://github.com/apache/camel-quarkus/pull/5793
* Add a Jasypt test profile for FIPS by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5794
* feat(camel-k): add options to override some aspects of a route by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/5792
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5797
* update quarkus metadata #5803 by @aldettinger in https://github.com/apache/camel-quarkus/pull/5805
* Bump org.codehaus.mojo:exec-maven-plugin from 3.1.1 to 3.2.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5796
* Move com.squareup.okhttp3:mockwebserver into camel-quarkus-bom by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5806
* Use correct quarkus-extension-maven-plugin in beanio extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5807
* Upgrade quarkus-amazon-services to 2.12.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5811
* Avoid hard coded use of localhost in Google cloud extension tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5808
* Update release changelog and associated process step by @aldettinger in https://github.com/apache/camel-quarkus/pull/5817
* Upgrade Quarkus to 3.8.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5818
* jt400 mock coverage + native fixes by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5812
* Add jolokia and okhttp to dependabot config by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5820
* Manage software.amazon.awssdk:endpoints-spi by @ppalaga in https://github.com/apache/camel-quarkus/pull/5821
* Clean up camel-k extension leftovers by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/5802
* Remove xerces:xercesImpl from the BOM by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5822
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.1.4 to 3.2.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5826
* Bump quarkiverse-groovy.version from 3.7.1 to 3.8.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5825
* Upgrade cq-maven-plugin to 4.5.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5827
* Rebalance native test category group-13 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5829
* Add multipart configuration options to servlet extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5328
* Remove registration of Servlet classes with AdditionalBeanBuildItem by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5831
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5833
* Bump quarkiverse-pooled-jms.version from 2.3.0 to 2.3.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/5836
* Servlet extension improvements by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5837
* Upgrade cq-maven-plugin to 4.6.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5838
* Migrate file watch tests to new harness #3584 by @aldettinger in https://github.com/apache/camel-quarkus/pull/5840
* Bump quarkiverse-jgit.version from 3.0.6 to 3.0.7 by @dependabot in https://github.com/apache/camel-quarkus/pull/5841
* Bump quarkiverse-jsch.version from 3.0.6 to 3.0.7 by @dependabot in https://github.com/apache/camel-quarkus/pull/5842
* Bump quarkiverse-mybatis.version from 2.2.1 to 2.2.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/5845
* Bump org.amqphub.quarkus:quarkus-qpid-jms-bom from 2.5.0 to 2.6.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5846
* Upgrade Quarkus to 3.8.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5848
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.2.0 to 3.2.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/5854
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5856
* Fix typo in servlet name configuration code snippet by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5857
* Exclude CloudEvents transformer services unless camel-quarkus-cloudevents is on the classpath by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5862
* Move main-xml-io-with-beans tests into main-xml-io by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5863
* Add profile to debug camel-k-maven-plugin integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5864
* Bump com.unboundid:unboundid-ldapsdk from 6.0.11 to 7.0.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5865
* Remove redundant use of oss-snapshots profile on push to quarkus-main by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5867
* Remove obsolete vertx-grpc exclusions since they potentially cause issues if quarkus-grpc is present by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5868
* Upgrade Quarkus to 3.9.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5869
* Speed up Windows, quarkus-main & camel-main builds by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5871
* Avoid potential NPE when handling Jasypt password prefixes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5875
* Upgrade to Quarkus CXF 3.8.1 by @zhfeng in https://github.com/apache/camel-quarkus/pull/5877
* Upgrade Camel to 4.4.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5878
* Upgrade quarkus-jgit to 3.1.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5879
* Upgrade Quarkus to 3.9.0.CR2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5881
* Enable java-joor-dsl & jsh-dsl tests on Windows by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5882
* jt400: extend test coverage by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5883
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5886
* Fixup adding com.squareup.okhttp3:mockwebserver to camel-quarkus-bom by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5888
* Remove redundant parentFirstArtifact configuration from js-dsl extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5889
* Align Debezium & QPid JMS versions with the Quarkus Platform by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5890
* Add changelog for 3.8.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5894
* Register Mixin classes for reflection by @zhfeng in https://github.com/apache/camel-quarkus/pull/5898
* Explicit blog post annoucement step in the release procedure by @aldettinger in https://github.com/apache/camel-quarkus/pull/5901
* Upgrade Quarkus to 3.9.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5902
* Upgrade to Quarkus CXF 3.9.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5904
* Manage com.sun.xml.fastinfoset:FastInfoset by @zhfeng in https://github.com/apache/camel-quarkus/pull/5905

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.8.0...3.9.0

## 3.8.1

* release: fix qute version by @aldettinger in https://github.com/apache/camel-quarkus/pull/5788
* [3.8.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5795
* update quarkus metadata #5803 by @aldettinger in https://github.com/apache/camel-quarkus/pull/5804
* [3.8.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5810
* [3.8.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5819
* [3.8.x] Manage software.amazon.awssdk:endpoints-spi by @ppalaga in https://github.com/apache/camel-quarkus/pull/5823
* [3.8.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5828
* [3.8.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5839
* [3.8.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5847
* [3.8.x] Upgrade Quarkus to 3.8.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5852
* Fix typo in servlet name configuration code snippet by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5858
* [3.8.x] Upgrade Quarkus CXF to 3.8.1 by @zhfeng in https://github.com/apache/camel-quarkus/pull/5876
* [3.8.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5880
* jt400: extend test coverage by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5884
* [3.8.x] Upgrade Quarkus to 3.8.3 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5885

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.8.0...3.8.1

## 3.8.0

* Next is 3.8.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5681
* Fix Jasypt docs indentation level by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5684
* Bump quarkiverse-jackson-jq.version from 2.0.1 to 2.0.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/5686
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5687
* Add changelog for 3.7.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5688
* Bump quarkiverse-cxf.version from 2.7.0 to 2.7.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/5697
* Increase xj test coverage by @zhfeng in https://github.com/apache/camel-quarkus/pull/5703
* Increase kudu extension test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5699
* Upgrade Quarkus to 3.7.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5705
* Bump peter-evans/create-pull-request from 5 to 6 by @dependabot in https://github.com/apache/camel-quarkus/pull/5706
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.1.3 to 3.1.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/5707
* CXF-SOAP: Cover possible regression prior CXF fix causing indefinitive hang by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5685
* Bump quarkiverse-jsch.version from 3.0.5 to 3.0.6 by @dependabot in https://github.com/apache/camel-quarkus/pull/5710
* Add manual saga tests by @zhfeng in https://github.com/apache/camel-quarkus/pull/5714
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5718
* file: migrate batch test to non flaky harness #3584 by @aldettinger in https://github.com/apache/camel-quarkus/pull/5715
* Fixing SMB test failure in Quarkus Platform by @spatnity in https://github.com/apache/camel-quarkus/pull/5720
* Improve message generated by Dependabot branch sync workflow by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5724
* Correcting path in FhirR5Processor.java and upgrading docker image ve… by @spatnity in https://github.com/apache/camel-quarkus/pull/5728
* Set explicit path for regenerated mail test certificates and add debug logging by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5729
* Jasypt dev UI by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5711
* Infinispan test suite integration changes by @karesti in https://github.com/apache/camel-quarkus/pull/5719
* Make alternative-jdk job a matrix build by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5726
* Splunk-hec native support by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5730
* Enable MasterOpenShiftTest by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5732
* Upgrade Quarkus to 3.7.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5733
* Bump quarkiverse-groovy.version from 3.6.4 to 3.7.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5735
* Unshade Google and Micrometer related packages from kudu-client by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5727
* Restore quarkus.runner Maven property to master-openshift tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5740
* Add release guide notes for enabling platform tests that have been fixed in the latest release by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5741
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5744
* Add Elasticsearch Low Level Rest Client by @zbendhiba in https://github.com/apache/camel-quarkus/pull/5738
* Make the majority of KuberenetesClusterService configuration overridable at runtime by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5746
* Use eclipse-temurin:17-jdk as openssl got removed in the ubi image by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5748
* Extend keytool-maven-plugin generated certificate validity by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5749
* Rework JasyptSecureExtensionConfigTest configuration for native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5752
* Upgrade Quarkus to 3.8.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5756
* Assert the correct number of expected messages in DataSetTest.simpleDataSetConsumer by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5758
* Free more disk space on GitHub actions runners by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5759
* Disable JasyptSecureExtensionConfigTest.secureDirectComponentTimeout on GitHub CI by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5762
* Camel 4.4.0 upgrade by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5747
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5767
* Add component and endpoint documentation for Qute extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5764
* Remove ssr-dom-shim dependency override by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5768
* Add spring-rabbitmq as an alternative to rabbitmq in the 3.2.x migration guide by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5771
* Test configuration secured by Jasypt with custom profiles by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5772
* file: migrate the sortBy test to non flaky harness #3584 by @aldettinger in https://github.com/apache/camel-quarkus/pull/5765
* JVM support of BeanIO, OpenSearch and MailMicrosoftOAuth by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5769
* Upgrade okhttp to 4.12.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5770
* Remove BigQuery workaround for Arrow Netty incompatibility by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5773
* Enable TwilioTest.phoneCall by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5774
* Link to Camel 4.4.x documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5775
* release-guide: fix typo by @aldettinger in https://github.com/apache/camel-quarkus/pull/5780
* Remove ssr-dom-shim dependency override (again) by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5778
* Post Camel 4.4.0 upgrade tidy-ups by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5779
* feat: introduce a CamelModelReifierFactoryBuildItem spi to inject a custom ModelReifierFactory by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/5783
* Upgrade Quarkus to 3.8.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5784
* Upgrade to Quarkus CXF 3.8.0 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5786
* release: upgrade third party quarkus extensions by @aldettinger in https://github.com/apache/camel-quarkus/pull/5785
* Jt400: make native work by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5782

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.7.0...3.8.0

## 3.7.0

* Next is 3.7.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5551
* Add steps to publish SBOM artifacts to the release guide by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5552
* Make HazelcastInstanceTest wait and verify new cluster member is shutdown by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5553
* Bump org.codehaus.mojo:build-helper-maven-plugin from 3.4.0 to 3.5.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5555
* Bump quarkiverse-groovy.version from 3.5.0 to 3.5.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/5556
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5557
* Upgrade quarkus-amazon-services-bom to 2.7.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5558
* Fix native issues with REST DSL param arrayType and allowableValues by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5560
* Remove redundant Apache Arrow io.netty classes from google-biqquery extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5562
* Bump org.amqphub.quarkus:quarkus-qpid-jms-bom from 2.4.0 to 2.5.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5563
* Bump quarkiverse-pooled-jms.version from 2.2.0 to 2.3.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5567
* Add changelog for 3.6.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5570
* Add release scripts in Camel Quarkus for uploading and promoting sour… by @oscerd in https://github.com/apache/camel-quarkus/pull/5571
* Enable Dependabot updates of GitHub actions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5572
* Use native builder image pull strategy 'missing' to reduce interactions with quay.io by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5573
* Bump actions/setup-java from 3 to 4 by @dependabot in https://github.com/apache/camel-quarkus/pull/5574
* Bump peter-evans/create-pull-request from 4 to 5 by @dependabot in https://github.com/apache/camel-quarkus/pull/5575
* Bump actions/github-script from 6 to 7 by @dependabot in https://github.com/apache/camel-quarkus/pull/5576
* Bump actions/checkout from 1 to 4 by @dependabot in https://github.com/apache/camel-quarkus/pull/5577
* Convert KotlinFeature from Kotlin to Java source by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5579
* Add CI workflow steps to test on JDK 21 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5581
* Bump com.unboundid:unboundid-ldapsdk from 6.0.10 to 6.0.11 by @dependabot in https://github.com/apache/camel-quarkus/pull/5580
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5585
* Bump quarkiverse-groovy.version from 3.5.2 to 3.6.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5587
* Auto update Antora graalvm-docs-version attribute by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5588
* Update CHANGELOG with 3.2.3 by @zhfeng in https://github.com/apache/camel-quarkus/pull/5590
* Switch to org.wiremock:wiremock-standalone by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5593
* Free more disk space on GitHub actions runner by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5594
* Remove workaround for quarkusio/quarkus#36952 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5595
* Fix disablement of InfinispanTest.query on JUnit 5.10.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5596
* Switch to enabling JFR support with quarkus.native.monitoring config property by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5597
* Fix incorrect Maven args variable name used for example projects build by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5598
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5602
* Upgrade Quarkus to 3.6.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5599
* Update Maven wrapper distribution URL to Maven 3.9.6 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5605
* Bump quarkiverse-groovy.version from 3.6.0 to 3.6.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/5606
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.1.2 to 3.1.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/5612
* Bump quarkiverse-jgit.version from 3.0.5 to 3.0.6 by @dependabot in https://github.com/apache/camel-quarkus/pull/5613
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 2.7.2 to 2.7.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/5614
* Upgarde Quarkus to 3.6.4 by @zhfeng in https://github.com/apache/camel-quarkus/pull/5616
* file: migrate read lock test to new harness #3584 by @aldettinger in https://github.com/apache/camel-quarkus/pull/5618
* Bump actions/upload-artifact from 3 to 4 by @dependabot in https://github.com/apache/camel-quarkus/pull/5608
* Bump quarkiverse-groovy.version from 3.6.1 to 3.6.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/5621
* Upgrade Camel to 4.3.0  by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5607
* Simplify greenmail container certificate setup by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5622
* Reference Camel SNAPSHOT docs as 4.3.0 is not published on the website by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5624
* Switch to quay.io/strimzi container images for Kafka testing by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5627
* Revert upload / download artifact GitHub action upgrade by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5628
* Replace tinyproxy container image with an embedded HTTP proxy server by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5632
* Use Quarkus Derby DevServices for SQL integration test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5634
* Remove Salesforce PubSubApiConsumer POJO class loading workaround by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5637
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5610
* Add support for s390x architecture to gRPC codegen by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5640
* Clean up remaining reference to deprecated quarkus.opentelemetry config by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5642
* Fix transformer service inclusion path by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5643
* Quarkus extension for smbComponent by @spatnity in https://github.com/apache/camel-quarkus/pull/5644
* Add option to skip sanity-checks script execution by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5648
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5649
* Bump org.cyclonedx:cyclonedx-maven-plugin from 2.7.10 to 2.7.11 by @dependabot in https://github.com/apache/camel-quarkus/pull/5652
* Upgrade Quarkus to 3.7.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5653
* Adds a rebase step into CI integration-tests-alternative-jdk by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5655
* Upgrade to Quarkus CXF 2.7.0.CR2 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5657
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 2.7.3 to 2.10.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/5659
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5660
* Remove Camel Facebook extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5662
* Bump quarkus-mybatis to 2.2.1 by @zhfeng in https://github.com/apache/camel-quarkus/pull/5664
* Fix #5667 to add native support for camel-quarkus-xj by @zhfeng in https://github.com/apache/camel-quarkus/pull/5669
* Fix #5663 with the icon url by @zhfeng in https://github.com/apache/camel-quarkus/pull/5670
* Add Jasypt native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5671
* Regenerate extension metadata by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5672
* Upgrade Quarkus to 3.7.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5673
* Upgrade Debezium to 2.5.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5674
* Disable JasyptSecureExtensionConfigTest.secureDirectComponentTimeout by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5676
* Upgrade to Quarkus CXF 2.7.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5677
* Use /bin/bash for release-util sign.sh script by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5680


**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.6.0...3.7.0

## 3.2.3

* [3.2.x] Update generated files after release by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5483
* [3.2.x] Set infinispan-quarkus-client test client-intelligence to BASIC on all platforms by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5489
* [3.2.x] Upgrade Quarkus to 3.2.8.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5492
* [3.2.x] Infinispan test: fix broken immutable list by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5507
* [3.2.x] Clean up usage of hard coded hosts that use containers by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5513
* [3.2.x] Upgrade camel 4.0.3 by @zhfeng in https://github.com/apache/camel-quarkus/pull/5525
* [3.2.x] Upgrade Quarkus to 3.2.9.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5527
* [3.2.x] Ftp regenerate certificate + extend validity (#5523) by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5529
* [3.2.x] Upgrade quarkus-artemis to 3.0.3 by @zhfeng in https://github.com/apache/camel-quarkus/pull/5532
* [3.2.x] Upgrade quarkus-pooled-jms to 2.1.1 by @zhfeng in https://github.com/apache/camel-quarkus/pull/5533
* [3.2.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5543
* [3.2.x] Upgrade to Quarkus CXF 2.2.6 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5547
* [3.2.x] Remove redundant Apache Arrow io.netty classes from google-biqquery e… by @zhfeng in https://github.com/apache/camel-quarkus/pull/5564

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.2.2...3.2.3

## 3.6.0

* Next is 3.6.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5445
* Update Maven wrapper distribution URL to Maven 3.9.5 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5446
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5448
* Fix package path to XmlSlurper in camel-k-maven-plugin integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5451
* Add changelog for 3.5.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5454
* Bump quarkiverse-groovy.version from 3.4.0 to 3.5.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5455
* Fix build order for camel-quarkus-camel-k-deployment test dependencies by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5456
* Upgrade to Quarkus CXF 2.5.0  by @ppalaga in https://github.com/apache/camel-quarkus/pull/5457
* Remove duplicate quarkus-resteasy dependency from camel-quarkus-integration-test-azure-servicebus by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5458
* Document how users can upgrade to new Camel Quarkus releases without a Quarkus Platform release by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5460
* Bump quarkiverse-mybatis.version from 2.1.0 to 2.2.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5459
* Update Jira tests to work with the latest container image by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5465
* Fix #5453 to introduce camel-spring-redis extension by @zhfeng in https://github.com/apache/camel-quarkus/pull/5466
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5469
* Bump org.cyclonedx:cyclonedx-maven-plugin from 2.7.9 to 2.7.10 by @dependabot in https://github.com/apache/camel-quarkus/pull/5475
* Only manage io.quarkiverse.minio:quarkus-minio-native by @zhfeng in https://github.com/apache/camel-quarkus/pull/5470
* Automatically register beans with methods annotated with @Handler for reflection by @spatnity in https://github.com/apache/camel-quarkus/pull/5472
* Review Camel service include patterns by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5476
* Platform-http test fails in FIPS environment by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5463
* Register To*Stream classes for reflection by @turing85 in https://github.com/apache/camel-quarkus/pull/5477
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5484
* Add changelog for 3.2.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5487
* Set infinispan-quarkus-client test client-intelligence to BASIC on all platforms by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5488
* file: migrate charset test to non flaky test harness #3584 by @aldettinger in https://github.com/apache/camel-quarkus/pull/5480
* Re-enable SimpleIT by @zhfeng in https://github.com/apache/camel-quarkus/pull/5491
* Add debug logging to gRPC extension class generation build steps by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5494
* Upgrade to Quarkus 3.5.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5495
* Add --fail-at-end to functional-extension-tests Maven executions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5496
* Bump quarkiverse-tika.version from 2.0.2 to 2.0.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/5498
* Revert "Bump quarkiverse-tika.version from 2.0.2 to 2.0.3" by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5499
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5500
* Add support for Salesforce pub / sub API by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5503
* Upgrade to 4.2.0 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5501
* Clean up usage of hard coded hosts that use containers by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5509
* Use Apache Camel icon for extension catalog by @ppalaga in https://github.com/apache/camel-quarkus/pull/5514
* Upgrade Quarkus to 3.6.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5512
* Improve container setup for kafka-oauth test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5516
* Quarkus 3.6.0.CR1 upgrade post tidy ups by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5517
* Bump org.codehaus.mojo:exec-maven-plugin from 3.1.0 to 3.1.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/5519
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5520
* Re-enable SnakeYAML JVM test and native profile  by @zhfeng in https://github.com/apache/camel-quarkus/pull/5524
* Ftp regenerate certificate + extend validity by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5523
* Bump quarkiverse-jsch.version from 3.0.4 to 3.0.5 by @dependabot in https://github.com/apache/camel-quarkus/pull/5526
* Fix intermittent failure of QuartzQuarkusSchedulerAutowiredWithSchedulerBeanTest by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5530
* Fix intermittent failure of debug integration test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5531
* Fix MailTest.testAttachments test on Windows by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5537
* Upgrade Quarkus to 3.6.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5539
* Enable Mail & Kafka test certificates to be regenerated for the docker host name or ip address by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5540
* Upgrade Debezium to 2.3.3.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5542
* Upgrade quarkus-amazon-services to 2.5.3 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5545
* Upgrade to Quarkus CXF 2.6.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5546
* Downgrade calculator-ws container version to 1.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5550

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.5.0...3.6.0

## 3.2.2

* [3.2.x] Remove Google Cloud native build limitation for RunReachabilityHandlersConcurrently option by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5405
* Backports 3.2.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5413
* Splunk docker image for testing should be 9.0 (not 9.1) by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5424
* Sql: Test SqlTest#testDefaultErrorCode fails with mssql by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5435
* Ftp fails in the FIPS because of the not supported key. by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5434
* Upgrade Camel to 4.0.2 by @zhfeng in https://github.com/apache/camel-quarkus/pull/5468
* [3.2.x] Only manage io.quarkiverse.minio:quarkus-minio-native by @zhfeng in https://github.com/apache/camel-quarkus/pull/5471
* [3.2.x] Register To*Stream classes for reflection by @zhfeng in https://github.com/apache/camel-quarkus/pull/5479
* [3.2.x] Upgrade Quarkus to 3.2.7.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5481

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.2.1...3.2.2

## 3.5.0

* Test Prometheus metrics with CXF SOAP client and service by @ppalaga in https://github.com/apache/camel-quarkus/pull/5297
* Next is 3.5.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/5307
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5313
* Upgrade to Quarkus CXF 2.4.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5311
* Updates to LDAP tests and usage docs by @djcoleman in https://github.com/apache/camel-quarkus/pull/5310
* Exclude vertx-grpc dependencies from Google BigQuery and Google PubSub extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5315
* ldap: fixup query name parameter in itest by @aldettinger in https://github.com/apache/camel-quarkus/pull/5316
* Bump quarkiverse-pooled-jms.version from 2.1.0 to 2.2.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5320
* Bump quarkiverse-jgit.version from 3.0.4 to 3.0.5 by @dependabot in https://github.com/apache/camel-quarkus/pull/5321
* Improve implementation of GooglePubsubTest.testOrdering by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5322
* Update jms doc to add some usages about IBM MQ Client by @zhfeng in https://github.com/apache/camel-quarkus/pull/5317
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.1.0 to 3.1.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/5325
* Update graalvm-docs-version in antora.yml by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5327
* Remove redundant usage.adoc from xml-jaxp extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5329
* Upgrade Quarkus to 3.4.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5338
* Add xslt prefix to grouped XML test sub-modules so that the intent is clear by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5339
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5341
* Bump com.unboundid:unboundid-ldapsdk from 6.0.9 to 6.0.10 by @dependabot in https://github.com/apache/camel-quarkus/pull/5331
* file: migrate file creation test to new harness #3584 by @aldettinger in https://github.com/apache/camel-quarkus/pull/5340
* Upgrade to Quarkus CXF 2.4.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5355
* Create basic Azure Servicebus tests by @ldrozdo in https://github.com/apache/camel-quarkus/pull/5333
* Mongodb: native build fails because of CredentialsProviderFinder by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5336
* Add basic test coverage for xml-jaxp type converters by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5346
* Add Camel service inclusion pattern for periodic-task by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5348
* Upgrade Camel to 4.0.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5354
* Xslt-saxon: native build fails because of BrotliInputSreamFactory by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5351
* Regen by @zhfeng in https://github.com/apache/camel-quarkus/pull/5357
* Update CHANGELOG with 3.2.0 & 3.4.0 releases by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5358
* Regenerate extension metadata by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5364
* Improve descriptions for xml-jaxp & yaml-io extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5370
* Aws2 kinesis: native build fails because of missing netty by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5362
* Remove guide link metadata from camel-k extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5365
* Kafka: native build fails because of missing vertx by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5367
* Salesforce: native build fails because of missing netty by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5371
* Revert Force Oracle devservices image to gvenzl/oracle-free:23.2-slim by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5374
* Add note for jvmstat to management extension native JMX documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5376
* Bump com.mycila:license-maven-plugin from 4.2 to 4.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/5377
* Add missing quarkus-netty dependency to gRPC extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5383
* Upgrade github actions to latest versions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5385
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5387
* feat(native): support Native Sources by @squakez in https://github.com/apache/camel-quarkus/pull/5380
* Fix package name for xml-jaxp integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5388
* Speed up examples CI build step by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5386
* Allow extensions to inject kamelets resources by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/5392
* generalize kubernetes version label updates to all examples in release procedure by @aldettinger in https://github.com/apache/camel-quarkus/pull/5394
* jta: fix dataSource bean reference in documentation by @aldettinger in https://github.com/apache/camel-quarkus/pull/5395
* Disable Quarkus integration-tests module due to quarkusio/quarkus#36245 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5397
* Bump quarkiverse-groovy.version from 3.2.2 to 3.4.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5401
* Change description of this project which is presented in google result by @oscerd in https://github.com/apache/camel-quarkus/pull/5402
* Remove Google Cloud native build limitation for RunReachabilityHandlersConcurrently option by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5403
* Bump org.seleniumhq.selenium:htmlunit-driver from 4.12.0 to 4.13.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5406
* Camel-k-runtime tests is failing in Quarkus platform #5319 by @spatnity in https://github.com/apache/camel-quarkus/pull/5404
* perf-tool: upgrade hyperfoil version by @aldettinger in https://github.com/apache/camel-quarkus/pull/5408
* Use ImageMode enum in XmlJaxbRecorder instead of graal-sdk APIs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5410
* Remove redundant cxf.version property by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5409
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5412
* Combine container-license-acceptance.txt in jdbc-grouped test module by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5414
* perf: Upgrade to Java 17 #5417 by @aldettinger in https://github.com/apache/camel-quarkus/pull/5418
* Upgrade Camel to 4.1.0 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5415
* Upgrade Quarkus to 3.5.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5420
* Sql: Test SqlTest#testDefaultErrorCode fails with mssql by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5416
* Splunk docker image for testing should be 9.0 (not 9.1) by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5423
* Bump io.quarkiverse.artemis:quarkus-artemis-bom from 3.1.1 to 3.1.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/5421
* Remove erroneous SpoolRule reflective class configuration from camel-k-runtime extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5425
* Reformat generated files by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5426
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5428
* Add gRPC codegen protoc support for ppc64le architecture by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5431
* Support user TypeConverter as CDI beans by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5436
* Ftp fails in the FIPS because of the not supported key. by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5433
* Fix #5437 to avoid FastCamelContext creating TypeConverter by @zhfeng in https://github.com/apache/camel-quarkus/pull/5439
* Upgrade Quarkus to 3.5.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5440

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.4.0...3.5.0

## 3.2.1

* [3.2.x] Reinstate auto update of antora.yml camel-docs-version attribute by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5239
* [3.2.x] Fix camel website build errors related to CQ by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5257
* [3.2.x] Exclude com.google.auto.value:auto-value-annotations from gRPC and Google Pubsub extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5264
* [3.2.x] Upgrade to Quarkus CXF 2.2.3 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5266
* [3.2.x] Update supported since metadata from 3.0.0 to 3.2.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5272
* [3.2.x] Remove redundant camel-quarkus-bom import in knative runtime module by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5276
* Backports 3.2.x by @zbendhiba in https://github.com/apache/camel-quarkus/pull/5283
* Backport 3.2.x by @zhfeng in https://github.com/apache/camel-quarkus/pull/5291
* [3.2.x] Javax replaced with jakarta in adoc files #5293 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5295
* [3.2.x] Upgrade Quarkus to 3.2.6.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5305
* Backports from main by @zbendhiba in https://github.com/apache/camel-quarkus/pull/5308
* Update jms doc to add some usages about IBM MQ Client by @zhfeng in https://github.com/apache/camel-quarkus/pull/5342
* [3.2.x] Upgrade to Quarkus CXF 2.2.4 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5347
* Mongodb: native build fails because of CredentialsProviderFinder by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5337
* Backport 3.2.x by @zhfeng in https://github.com/apache/camel-quarkus/pull/5344
* Xslt-saxon: native build fails because of BrotliInputSreamFactory by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5352
* Backport 3.2.x by @zhfeng in https://github.com/apache/camel-quarkus/pull/5356
* [3.2.x] Regen by @zhfeng in https://github.com/apache/camel-quarkus/pull/5359
* Aws2 kinesis: native build fails because of missing netty by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5363
* Kafka: native build fails because of missing vertx by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5368
* Salesforce: native build fails because of missing netty by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5372
* [3.2.x] Improve descriptions for xml-jaxp & yaml-io extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5373
* [3.2.x] Revert "Force Oracle devservices image to gvenzl/oracle-free:23.2-slim by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5375
* [3.2.x] Add missing quarkus-netty dependency to gRPC extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5384
* [3.2.x] Fix package name for xml-jaxp integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5389
* [3.2.x] Improve implementation of GooglePubsubTest.testOrdering by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5391
* jta: fix dataSource bean reference in documentation (#5395) by @aldettinger in https://github.com/apache/camel-quarkus/pull/5396
* [3.2.x] Upgrade to Quarkus CXF 2.2.5 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5399

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.2.0...3.2.1

## 3.4.0

* Next is 3.3.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/5231
* Onboard Camel K Runtime fixups by @ppalaga in https://github.com/apache/camel-quarkus/pull/5219
* Reinstate auto update of antora.yml camel-docs-version attribute by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5238
* Upgrade to Quarkus 3.3.1 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/5236
* Update migration guide version for 3.2.0 LTS by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5248
* Fix resolution of postgres.container.image config property in OpenTelemetry tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5245
* Fix resolution of postgres.container.image config property in Quartz Clustered tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5246
* Manage platform participant dependencies by @zbendhiba in https://github.com/apache/camel-quarkus/pull/5250
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5252
* Fix camel website build errors related to CQ by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5255
* Bump quarkiverse-jgit.version from 3.0.2 to 3.0.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/5256
* Exclude com.google.auto.value:auto-value-annotations from gRPC and Google Pubsub extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5259
* Bump quarkiverse-minio.version from 3.1.0.Final to 3.3.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/5262
* Bump quarkiverse-jsch.version from 3.0.3 to 3.0.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/5263
* Upgrade to Quarkus CXF 2.3.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5261
* Set native-image-xmx for camel-k-runtime integration tests for GitHub CI by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5265
* Update supported since metadata from 3.0.0 to 3.2.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5267
* Test REST to SOAP bridge scenario by @ppalaga in https://github.com/apache/camel-quarkus/pull/5271
* Do not run camel-k-maven-plugin tests if skipTests = true by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5273
* Remove redundant camel-quarkus-bom import in knative runtime module by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5274
* Bump org.seleniumhq.selenium:htmlunit-driver from 4.11.0 to 4.12.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5275
* Upgrade Quarkus to 3.4.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5270
* Remove quarkus.camel.native.resources.*-patterns config properties  #5251 by @spatnity in https://github.com/apache/camel-quarkus/pull/5280
* Next is 3.4.0-SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5281
* Telegram webhook extra doc by @zbendhiba in https://github.com/apache/camel-quarkus/pull/5282
* Bump quarkiverse-jgit.version from 3.0.3 to 3.0.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/5285
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5286
* file: migrate quartz scheduled file polling to new test harness #3584 by @aldettinger in https://github.com/apache/camel-quarkus/pull/5284
* Upgrade quarkus-pooled-jms to 2.1.0 by @zhfeng in https://github.com/apache/camel-quarkus/pull/5287
* Remove redundant quarkus-virtual-threads dependency from knative extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5288
* Fix #5212 to add XA test for IBMMQ client by @zhfeng in https://github.com/apache/camel-quarkus/pull/5223
* Javax replaced with jakarta in adoc files #5293 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5294
* Revert "Fix #5180 to skip quarkus build on jms-ibmmq-client integration tests by @zhfeng in https://github.com/apache/camel-quarkus/pull/5290
* Add Generated SBOM to release as artifacts by @oscerd in https://github.com/apache/camel-quarkus/pull/5296
* Telegram integration tests: add the possibility to disable running webhook test by @zbendhiba in https://github.com/apache/camel-quarkus/pull/5299
* Correcting failing checks when localstack is upgraded to 2.2.0 by @spatnity in https://github.com/apache/camel-quarkus/pull/5303
* Upgrade Quarkus to 3.4.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5304

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.2.0...3.4.0

## 3.2.0

* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5130
* Update 3.x migration guide removed extensions table by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5131
* Add missing graal-sdk dependency to ical runtime module by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5132
* Add xslt-saxon native support by @zhfeng in https://github.com/apache/camel-quarkus/pull/5133
* Upgrade async-http-client to 3.0.0.Beta2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5134
* Align Apache HTTP Client 5.x with Camel by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5135
* Remove redundant autowiring workaround in aws2-cw tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5137
* Tidy iCal extension native workaround for absence of com.github.erosbjson-sKema by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5138
* Restore AS2 testing by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5140
* Upgrade Quarkus to 3.2.3.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5141
* Add instructions for auto release note generation to release docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5143
* Bump org.seleniumhq.selenium:htmlunit-driver from 4.10.0 to 4.11.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5146
* Bump quarkiverse-jsch.version from 3.0.2 to 3.0.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/5145
* Register known jakarta.mail exception classes for reflection by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5148
* Fix #5149 to manage com.ibm.icu:icu4j by @zhfeng in https://github.com/apache/camel-quarkus/pull/5150
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5151
* Manage test container versions in the root project pom.xml by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5152
* Set encoding to UTF-8 when writing microprofile-config.properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5157
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 2.4.2 to 2.4.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/5158
* Reclaim disk space before running integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5159
* Ref#4772 Introduce group testing for different jdbc db types by @ldrozdo in https://github.com/apache/camel-quarkus/pull/5116
* Bump quarkiverse-pooled-jms.version from 2.0.1 to 2.0.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/5171
* Add elasticsearch tests by @llowinge in https://github.com/apache/camel-quarkus/pull/5170
* Add org.mapstruct:mapstruct-processor to the BOM by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5172
* Extend test coverage of gRPC extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5168
* Unban com.google.code.findbugs:jsr305 for gRPC extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5175
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5176
* Camel 4.0.0 upgrade by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5169
* Upgrade to Quarkus CXF 2.2.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5181
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 2.4.3 to 2.4.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/5182
* Fix #5180 to skip quarkus build on jms-ibmmq-client integration tests by @zhfeng in https://github.com/apache/camel-quarkus/pull/5183
* Fix compareVersion in sanity-checks.groovy #5165 by @zhfeng in https://github.com/apache/camel-quarkus/pull/5166
* Introduce custom  CodeGenProvider for gRPC extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5185
* Remove integration-tests/jdbc module by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5186
* chore: regen for mapstruct and xslt-saxon by @zhfeng in https://github.com/apache/camel-quarkus/pull/5179
* Exclude unwanted vertx-grpc transitive dependencies form quarkus-grpc-common by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5188
* Expand xslt-saxon test coverage by @zhfeng in https://github.com/apache/camel-quarkus/pull/5173
* Expand Splunk test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5192
* Fix dependency convergence error for google-auth-library-oauth2-http by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5195
* Bump io.quarkiverse.micrometer.registry:quarkus-micrometer-registry-jmx from 3.1.2 to 3.2.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/5198
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5200
* file: migrate pollEnrich to the non-flaky test harness #3584 by @aldettinger in https://github.com/apache/camel-quarkus/pull/5197
* Add yaml-io extension  by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5193
* Downgrade async-http-client to 2.12.3. Relates #5201 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5202
* Fix usage of deprecated Quarkus SSL config properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5203
* Fix quarkus-maven-plugin groupId & version for gRPC itest module by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5204
* Remove changelog GitHub action by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5213
* Upgrade to Quarkus CXF 2.2.2 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5211
* Micrometer: Message History factory and JMX MicrometerMessageHistory is not covered by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5210
* Populate FastComponentNameResolver component names from included component service paths by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5215
* Add missing dependencies to cli-connector extension and include dev-console service pattern by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5220
* Add missing logging dependency in elasticsearch extension by @zbendhiba in https://github.com/apache/camel-quarkus/pull/5222
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5226
* Use defined images for dev services in tests by @llowinge in https://github.com/apache/camel-quarkus/pull/5224
* Upgrade to Quarkus 3.2.5.Final by @zbendhiba in https://github.com/apache/camel-quarkus/pull/5228
* Upgrade Amazon services to 2.4.5 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/5229

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.0.0-RC2...3.2.0

## 3.0.0-RC2

* Restore native profiles to kubernetes-client tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5047
* Remove skip of maven-enforcer-plugin for Google extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5049
* Re-enable google-bigquery and add a work around by @zhfeng in https://github.com/apache/camel-quarkus/pull/5041
* Bump quarkus-micrometer-registry-jmx from 3.0.2 to 3.1.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/5055
* Bump quarkus-amazon-services-bom from 2.3.3 to 2.4.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5054
* Add sync tag for Groovy version property by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5052
* Improve native support of camel-quarkus-debug by @essobedo in https://github.com/apache/camel-quarkus/pull/5060
* Set the official snapshot repositories by @essobedo in https://github.com/apache/camel-quarkus/pull/5062
* chore: fix various build/compilation warnings by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/5053
* Ref #5056: Replace the deprecated RecorderContext#classProxy by @essobedo in https://github.com/apache/camel-quarkus/pull/5066
* Fix #5068 to add a pooling test with quarkus-qpid-jms by @zhfeng in https://github.com/apache/camel-quarkus/pull/5069
* Restrict downloading of com.atlassian dependencies to packages.atlassian.com by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5065
* Bump htmlunit-driver from 4.9.1 to 4.10.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5063
* Bump os-maven-plugin from 1.7.0 to 1.7.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/5064
* Use NativeImageFeatureBuildItem instead of deprecated AutomaticFeature annotation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5074
* Register additional JDK classes for serialization required by Nitrite by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5075
* chore: Add git diff when there are uncommitted changes by @essobedo in https://github.com/apache/camel-quarkus/pull/5076
* Ref #5067: Make expression extractor supports properties by @essobedo in https://github.com/apache/camel-quarkus/pull/5079
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5085
* Ref #5056: Improve the replacement of RecorderContext#classProxy by @essobedo in https://github.com/apache/camel-quarkus/pull/5077
* Enable Twilio test after the component was fixed via Camel 4.0.0-M2 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5078
* NettyHttpJaasTestResource reads config.jaas from disk  by @ppalaga in https://github.com/apache/camel-quarkus/pull/5084
* Re-add activemq extension after it was added back in Camel 4.0.0-RC1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5089
* Velocity $foreach.index, $foreach.count and $foreach.hasNext do not work in native mode by @ppalaga in https://github.com/apache/camel-quarkus/pull/5090
* Add a note to the contributor guide about keeping junit & Co. in Maven test scope by @ppalaga in https://github.com/apache/camel-quarkus/pull/5091
* [ #3087] Divide HTTP tests into separate modules by @avano in https://github.com/apache/camel-quarkus/pull/5073
* Replace org.graalvm.nativeimage:svm with org.graalvm.sdk:graal-sdk which has a status of public API by @ppalaga in https://github.com/apache/camel-quarkus/pull/5038
* Build Camel on the nightly build instead of relying on the SNAPSHOT repo by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5061
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5097
* Bump quarkus-amazon-services-bom from 2.4.0 to 2.4.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/5093
* Bump quarkiverse-jsch.version from 3.0.1 to 3.0.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/5094
* Bump quarkiverse-tika.version from 2.0.0 to 2.0.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/5095
* Bump quarkus-qpid-jms-bom from 2.3.0 to 2.4.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5096
* Remove pdfbox dependency excludes from quarkus-tika by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5098
* Bump quarkiverse-jgit.version from 3.0.1 to 3.0.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/5101
* Manage io.dropwizard.metrics dependencies used by camel-quarkus extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5100
* Simplify MicrometerTest.testDumpAsJson results extraction by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5102
* Add missing graal-sdk dependency declaration to extensions that use GraalVM APIs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5103
* Added atlassion-groupId for the correct functionality with the 'io.atlassian.fugue' by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5104
* Upgrade Quarkus to 3.2.1.final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5105
* Fix hamcrest GAV in bom-test by @claudio4j in https://github.com/apache/camel-quarkus/pull/5106
* Build Camel from source for camel-main branch builds by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5112
* Do not produce JmxMeterRegistry bean in native mode testing by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5113
* Upgrade Quarkus to 3.2.2.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5114
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5115
* Bump quarkiverse-groovy.version from 3.2.1 to 3.2.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/5120
* Bump io.quarkiverse.amazonservices:quarkus-amazon-services-bom from 2.4.1 to 2.4.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/5124
* SNMPv3 test coverage by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5122
* Add MapStruct native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5123
* Ref #5026: Delay the init of ICMPHelper on non Linux OS by @essobedo in https://github.com/apache/camel-quarkus/pull/5125
* Disallow invalid host/port values on the vertx-websocket server consumer by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5127
* Configure native-image-xmx for kubernetes and master-openshift native tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5128
* Bump quarkiverse-mybatis.version from 2.0.0 to 2.1.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5129
* Camel 4.0.0 rc2 upgrade by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5119

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.0.0-RC1...3.0.0-RC2

## 3.0.0-RC1

* Bump quarkus-amazon-services-bom from 2.2.0 to 2.3.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4930
* Fix #4793 to update MyBatisConsumerTest and keep awaitility in the test scope by @zhfeng in https://github.com/apache/camel-quarkus/pull/4932
* [IBM-MQ] Add tests for IBM MQ client by @avano in https://github.com/apache/camel-quarkus/pull/4918
* Ban com.github.spotbugs:spotbugs-annotations by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4933
* Micrometer: InstrumentedThreadPoolFactory by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4935
* Restore master itest platform exclusion by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4936
* Bump quarkus-qpid-jms-bom from 2.1.0 to 2.2.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4938
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4939
* Added link to get started with Camel in general. by @spatnity in https://github.com/apache/camel-quarkus/pull/4941
* Upgrade Debezium to 2.2.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4942
* Bump quarkiverse-cxf.version from 2.0.4 to 2.1.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4937
* Revert "file: disabling idempotent test in order to experiment around flakiness by @aldettinger in https://github.com/apache/camel-quarkus/pull/4946
* Cxf-soap: Extend test coverage with Converter scenario #4652 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4653
* Bump quarkiverse-jsch.version from 3.0.0 to 3.0.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/4948
* Fix broken formatting on the CXF extension page by @ppalaga in https://github.com/apache/camel-quarkus/pull/4949
* Bump quarkiverse-minio.version from 3.0.2 to 3.1.0.Final by @dependabot in https://github.com/apache/camel-quarkus/pull/4952
* Bump formatter-maven-plugin from 2.22.0 to 2.23.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4953
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4954
* Use correct package for cxf-soap-server tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4958
* Ref #4960: Upgrade Groovy to 4.0.12 by @essobedo in https://github.com/apache/camel-quarkus/pull/4963
* Ref #4959: Delegate complexity to quarkus-groovy by @essobedo in https://github.com/apache/camel-quarkus/pull/4965
* Use GitHub SCM URL because https://gitbox.apache.org/repos/asf?p=camel-quarkus.git redirects to GitHub anyway by @ppalaga in https://github.com/apache/camel-quarkus/pull/4966
* Fix formatting in the release guide by @ppalaga in https://github.com/apache/camel-quarkus/pull/4970
* Be more precise about the WS standards coverage, add ,subs="attributes+" to code snippets on the CXF extension page by @ppalaga in https://github.com/apache/camel-quarkus/pull/4969
* Bump unboundid-ldapsdk from 6.0.8 to 6.0.9 by @dependabot in https://github.com/apache/camel-quarkus/pull/4972
* Expand micrometer test coverage #4582 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4951
* Bump quarkus-micrometer-registry-jmx from 0.2.0 to 3.0.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/4974
* Add a profile for analyzing possible dependency conflicts between Camel and Quarkus by @ppalaga in https://github.com/apache/camel-quarkus/pull/4976
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4977
* Add GitHub Actions workflows to label issues and assign a milestone by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4978
* Bump wiremock from 3.0.0-beta-8 to 3.0.0-beta-9 by @dependabot in https://github.com/apache/camel-quarkus/pull/4982
* camel-quarkus-crypto: Added test to sign/verify raw keys by @djcoleman in https://github.com/apache/camel-quarkus/pull/4980
* Temporarily disable auto milestone on PR merge due to permission issues by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4985
* Add test coverage for MDC logging by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4967
* JAXB integration tests failing fix if locale different from EN #4135 by @spatnity in https://github.com/apache/camel-quarkus/pull/4986
* Bump quarkus-amazon-services-bom from 2.3.0 to 2.3.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/4987
* Upgrade hazelcast-quarkus-client to 4.0.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4990
* Bump quarkiverse-jgit.version from 3.0.0 to 3.0.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/4991
* file: rewrite filter and idempotent tests from scratch for clarity by @aldettinger in https://github.com/apache/camel-quarkus/pull/4993
* Bump quarkus-artemis-bom from 3.0.0 to 3.0.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/4994
* Bump wiremock from 3.0.0-beta-9 to 3.0.0-beta-10 by @dependabot in https://github.com/apache/camel-quarkus/pull/4995
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4996
* Restore issue auto milestone workflow by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4997
* Re-enable jira tests by @zhfeng in https://github.com/apache/camel-quarkus/pull/5000
* Remove Spring dependencies from Jira extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5001
* Add names to auto milestone workflows by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5002
* Restore FOP integration test native profile by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5003
* Prevent user added issue labels from being incorrectly removed by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5005
* Upgrade Quarkus to 3.2.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5004
* Disable google-storage native profile due to #5010 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5011
* Upgrade quarkus-amazon-services-bom to 2.3.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5012
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5013
* [closes #4956] Extend netty-http test coverage by @avano in https://github.com/apache/camel-quarkus/pull/4999
* Upgrade to quarkus-cxf 2.2.0.CR1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5015
* Fix #5016 to add a IBMMQ pooling test with quarkus-pooled-jms by @zhfeng in https://github.com/apache/camel-quarkus/pull/5017
* Micrometer: Custom registry coverage is missing #5018 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5019
* Fix image links for CI status badges by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5022
* Remove reserve-network-port execution for integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5021
* Ref #2800: Remove the addition of affinity for mac os by @essobedo in https://github.com/apache/camel-quarkus/pull/5025
* Upgrade Quarkus to 3.2.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5027
* Micrometer: SimpleMeterRegistry is not created if no other registry is defined #5023 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5024
* Micrometer: It is not necessary to produce JMXRegistry for the test #5030 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5031
* Upgrade to quarkus-cxf 2.2.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/5032
* Re-enable google-pubsub and google-storage and add a workaround by @zhfeng in https://github.com/apache/camel-quarkus/pull/5033
* Bump quarkus-qpid-jms-bom from 2.2.0 to 2.3.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/5039
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/5040
* Add native support for camel-quarkus-management by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5037
* Upgrade Camel to 4.0.0-RC1 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/5036
* Add native support for camel-quarkus-debug by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/5043
* Bump quarkus-amazon-services-bom from 2.3.2 to 2.3.3 by @dependabot in https://github.com/apache/camel-quarkus/pull/5046
* Ref #5044 - Upgrade Quarkus Groovy to 3.2.1 by @essobedo in https://github.com/apache/camel-quarkus/pull/5045

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.0.0-M2...3.0.0-RC1

## 3.0.0-M2

* Fix metrics introduction in observability doc by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4660
* Upgrade Quarkus to 3.0.0.Alpha6 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4659
* camel-quarkus-management: Added tests for managed beans by @djcoleman in https://github.com/apache/camel-quarkus/pull/4663
* Remove redundant Azure IdentityClientBase runtime initialized class config by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4666
* Make AWS test class names in grouped testing unique by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4665
* Fix #4667 to use quay.io/jbosstm/lra-coordinator by @zhfeng in https://github.com/apache/camel-quarkus/pull/4668
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4669
* Use QUARKUS_HTTP_PORT env in LraTestResource by @zhfeng in https://github.com/apache/camel-quarkus/pull/4670
* Auto synchronize dependabot branches with generated changes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4645
* Add icon-url attribute to quarkus-extension.yaml pointing to Camel logo by @ppalaga in https://github.com/apache/camel-quarkus/pull/4673
* Revert "Temporary workaround for #4603 IllegalAnnotationsException Two classes by @ppalaga in https://github.com/apache/camel-quarkus/pull/4678
* Init Migrating to Camel Quarkus 3.0 guide by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4680
* Update checkout action configuration to pick up dependabot branch changes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4682
* Bump quarkiverse-mybatis.version from 2.0.0.CR1 to 2.0.0.CR2 by @dependabot in https://github.com/apache/camel-quarkus/pull/4674
* Upgrade Quarkus to 3.0.0.Beta1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4687
* Update Migration guide : add missing removed dependencies by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4688
* Enable back Telegram mp3, mp4 and pdf integration tests by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4692
* Add back DataSonnet by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4690
* Fix xref links in 3.0.0 migration guide by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4696
* Fix #4694 remove and replace some methods in ReflectiveClassBuildItem by @zhfeng in https://github.com/apache/camel-quarkus/pull/4695
* Ref #4596: Expand JDBC tests - named parameters and samples by @ldrozdo in https://github.com/apache/camel-quarkus/pull/4655
* Update jvmSince and nativeSince from 2.17.0 to 3.0.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4700
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4701
* Update SNAPSHOT deploy and Sonarcloud builds to use JDK 17 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4702
* Fix (jira): Add jira model classes and jackson joda datatype (main branch) by @claudio4j in https://github.com/apache/camel-quarkus/pull/4691
* Add Elasticsearch and Mapstruct in JVM only and drop Elasticsearch-rest and Dozer by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4697
* Complete vertx-http test coverage #4658 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4708
* Test CXF client with HTTP BASIC authentication  by @ppalaga in https://github.com/apache/camel-quarkus/pull/4709
* Cxf-soap: Extend test coverage with Ssl scenario #4679 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4681
* vertx-http: remove deprecated serviceCall eip by @aldettinger in https://github.com/apache/camel-quarkus/pull/4719
* Fix #4710 to register reflection for FastStringBuffer and resource bundle for XMLErrorResources by @zhfeng in https://github.com/apache/camel-quarkus/pull/4720
* Upgrade Quarkus to 3.0.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4724
* Upgrade to Quarkus Cassandra 1.2.0-alpha1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4612
* Start JDBC polling after database initialization #4723 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4728
* Upgrade to quarkus-cxf 2.0.0.Alpha5, Add back XML Security Sign by @ppalaga in https://github.com/apache/camel-quarkus/pull/4729
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4734
* Ref #4716: java-joor-dsl - Add RegisterForReflection support by @essobedo in https://github.com/apache/camel-quarkus/pull/4726
* Bump quarkus-artemis-bom from 3.0.0.Alpha7 to 3.0.0.CR1 by @dependabot in https://github.com/apache/camel-quarkus/pull/4733
* Ref #4731: java-joor-dsl - Add support of inner classes by @essobedo in https://github.com/apache/camel-quarkus/pull/4732
* Ref #4447: java-joor-dsl - Improve the test coverage by @essobedo in https://github.com/apache/camel-quarkus/pull/4737
* Add back Tika component by @zhfeng in https://github.com/apache/camel-quarkus/pull/4739
* Upgrade CycloneDX Maven Plugin to version 2.7.6 by @oscerd in https://github.com/apache/camel-quarkus/pull/4743
* Add missing @Component annotation to QuarkusVertxWebsocketComponent by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4742
* Upgrade Quarkus to 3.0.0.CR2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4744
* Fix maven connection time out by @zhfeng in https://github.com/apache/camel-quarkus/pull/4740
* Add GitHub actions directory to license-maven-plugin exclusions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4748
* Update camel-quarkus-tika resource by @zhfeng in https://github.com/apache/camel-quarkus/pull/4754
* Fix #4043 to group xml tests by @zhfeng in https://github.com/apache/camel-quarkus/pull/4753
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4756
* Ref #4749: java-joor-dsl - Add templated route support to native mode by @essobedo in https://github.com/apache/camel-quarkus/pull/4755
* Upgrade to quarkus-cxf 2.0.0.Alpha7 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4757
* #4291 avoid In/Out Message soap headers conflict by @ppalaga in https://github.com/apache/camel-quarkus/pull/4761
* Bump quarkiverse-tika.version from 2.0.0.CR2 to 2.0.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4763
* Bump quarkiverse-jsch.version from 2.0.3 to 3.0.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4764
* file: disabling idempotent test in order to experiment around flakiness by @aldettinger in https://github.com/apache/camel-quarkus/pull/4762
* Bump quarkus-amazon-services-bom from 2.0.0.Alpha1 to 2.0.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4767
* Bump quarkiverse-jgit.version from 2.3.2 to 3.0.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4765
* Ref#4596 Cover different db types for jdbc by @ldrozdo in https://github.com/apache/camel-quarkus/pull/4751
* Fix #4717 to expand camel-quarkus-mybatis test coverage by @zhfeng in https://github.com/apache/camel-quarkus/pull/4769
* Upgrade to Quarkus 3.0.0.Final by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4774
* Cxf fixes by @llowinge in https://github.com/apache/camel-quarkus/pull/4771
* Test also GITHUB_BASE_REF to checkout the right examples branch by @ppalaga in https://github.com/apache/camel-quarkus/pull/4776
* Bump quarkus-amazon-services-bom from 2.0.0 to 2.0.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/4777
* Fix #4781 Intermittent failure of MyBatisConsumerTest by @zhfeng in https://github.com/apache/camel-quarkus/pull/4782
* Bump quarkiverse-mybatis.version from 2.0.0.CR2 to 2.0.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4787
* Fix Spring integration test by @jbonofre in https://github.com/apache/camel-quarkus/pull/4788
* Test OpenTelemetry extension integration with opentelemetry-jdbc by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4790
* Fix OpenTelemetry test assertions argument order by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4792
* Restore maven.wagon.http.retryHandler.count to CQ_MAVEN_ARGS by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4795
* release: complete release process to be more explicit by @aldettinger in https://github.com/apache/camel-quarkus/pull/4800
* Enabling Google BigQuery, Google PubSub, Zendesk  and Stax integration tests by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4791
* Add OpenTelemetry documentation for CDI bean instrumentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4802
* Add a limitation doc about pooling support for camel-quarkus-amqp by @zhfeng in https://github.com/apache/camel-quarkus/pull/4809
* Bump quarkiverse-freemarker.version from 1.0.0.Alpha2 to 1.0.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4804
* Bump quarkus-artemis-bom from 3.0.0.CR1 to 3.0.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4814
* Snmp: Extend test coverage #4797 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4813
* Add camel-cli-connector extension. by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4812
* Upgrade to quarkus-cxf 2.0.1, test for #4746 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4803
* Upgrade cassandra-quarkus to 1.2.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4815
* Bump quarkiverse-pooled-jms.version from 2.0.0.CR2 to 2.0.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4818
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4819
* Upgrade quarkus-qpid-jms to 2.0.0 by @zhfeng in https://github.com/apache/camel-quarkus/pull/4810
* Improve handling of optional FHIR schematron and hapi-fhir-server dependencies by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4817
* Upgrade Cyclonedx Maven Plugin to version 2.7.7 by @oscerd in https://github.com/apache/camel-quarkus/pull/4824
* Fix handling of Quarkus quartz scheduler autowiring by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4826
* Added ldap tests, updated docs and promoted to native. by @djcoleman in https://github.com/apache/camel-quarkus/pull/4822
* Upgrade to Quarkus 3.0.1.Final by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4835
* Remove org.apache.cxf:cxf-codegen plugin  #4839 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4840
* camel-quarks-ldap: Fixed bug in CI tests from incorrect IP address resolution by @djcoleman in https://github.com/apache/camel-quarkus/pull/4845
* Bump quarkus-qpid-jms-bom from 2.0.0 to 2.1.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4848
* Snmp: Extend coverage for supported versions #4843 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4844
* MinIO: Extend test coverage #4707 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4828
* Bump quarkiverse-pooled-jms.version from 2.0.0 to 2.0.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/4847
* Snmp: Extend coverage of some smaller features #4850 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4854
* Upgrade to Maven Wrapper 3.2.0 and Maven 3.9.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4857
* Upgrade to cq-maven-plugin 4.1.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4829
* Test that all componets are present in the generated Catalog, Permanently remove extensions depending on components removed from Camel 4 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4852
* Upgrade to quarkus-cxf 2.0.2, Stop managing woodstox directly, rather by @ppalaga in https://github.com/apache/camel-quarkus/pull/4859
* Bump quarkus-amazon-services-bom from 2.0.1 to 2.1.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/4849
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4862
* Fixup Upgrade to Maven Wrapper 3.2.0 and Maven 3.9.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4863
* Limit aether.connector.http.connectionMaxTtl to 120 seconds by @ppalaga in https://github.com/apache/camel-quarkus/pull/4864
* simple("${exchange.getMessage().getBody()}") causes a MethodNotFoundException in native mode by @ppalaga in https://github.com/apache/camel-quarkus/pull/4861
* Update documentation references for javax packages to jakarta by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4869
* [MapStruct] Add tests by @avano in https://github.com/apache/camel-quarkus/pull/4838
* Snmp: reworked tests to avoid flaky failures by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4867
* Snmp: tiny timeout change in test to help stability by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4872
* Upgrade Camel to 4.0.0-M3 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4868
* Bump quarkiverse-jackson-jq.version from 2.0.0.Alpha to 2.0.1 by @dependabot in https://github.com/apache/camel-quarkus/pull/4877
* Restore camel-kubernetes related native testing by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4874
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4878
* Improve test coverage for vertx-websocket by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4875
* Remove openapi-java extensions limitations doc section as apiContextdListing was removed in Camel by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4879
* Upgrade CycloneDX Maven Plugin to version 2.7.8 by @oscerd in https://github.com/apache/camel-quarkus/pull/4883
* Test CXF client with a method referencing class with runtime initialization  by @ppalaga in https://github.com/apache/camel-quarkus/pull/4880
* Bump quarkiverse-minio.version from 3.0.0.Alpha3 to 3.0.2 by @dependabot in https://github.com/apache/camel-quarkus/pull/4888
* Upgrade to quay.io/l2x6/calculator-ws:1.2 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4887
* Snmp: cover snmp v3 for POLL operation #4881 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4882
* Be more specific when to use a ConsumerTemplate in extension tests by @ppalaga in https://github.com/apache/camel-quarkus/pull/4889
* Document CXF SOAP usage, configuration and limitations by @ppalaga in https://github.com/apache/camel-quarkus/pull/4886
* Upgrade to Maven 3.9.2, Remove the local upgrade of maven-resolver used by mvnw by @ppalaga in https://github.com/apache/camel-quarkus/pull/4890
* Upgrade to quarkus-cxf 2.0.3 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4891
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4898
* Recommend using wsdl2Java -validate while wsdlvalidator is not supported by @ppalaga in https://github.com/apache/camel-quarkus/pull/4901
* Bump quarkiverse-cxf.version from 2.0.3 to 2.0.4 by @dependabot in https://github.com/apache/camel-quarkus/pull/4897
* Bump quarkus-amazon-services-bom from 2.1.1 to 2.2.0 by @dependabot in https://github.com/apache/camel-quarkus/pull/4903
* Miscellaneous dependency alignment / removal by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4904
* Restore erroneously removed camel-quartz dependency exclusions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4909
* Upgrade Quarkus to 3.1.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4908
* Upgrade to Optaplanner 9.37.Final by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4912
* fix contributor links by @onderson in https://github.com/apache/camel-quarkus/pull/4915
* Upgrade CycloneDX Maven plugin to version 2.7.9 by @oscerd in https://github.com/apache/camel-quarkus/pull/4917
* Upgrade Maven plugins and remove redundant plugin version properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4906
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4922
* Upgrade Quarkus to 3.1.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4925
* Remove "none" WSDLs as they are not needed by @llowinge in https://github.com/apache/camel-quarkus/pull/4921
* Add sync tag for retrofit version by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4928
* Remove optional org.jruby.joni:joni from json-validator extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4926
* Dependency cleanup part 2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4929
* Prepare tests for Quarkus Platform by @ppalaga in https://github.com/apache/camel-quarkus/pull/4896

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/3.0.0-M1...3.0.0-M2

## 2.13.3

* [2.13.x] Exclude io.fabric8:zjsonpatch from test artifacts to avoid version misconvergence by @ppalaga in https://github.com/apache/camel-quarkus/pull/4349
* Upgrade to Quarkiverse CXF 1.5.10 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4361
* [2.13] Check extension pages with strict option by @ppalaga in https://github.com/apache/camel-quarkus/pull/4352
* Backport jms doc 2.13.x by @zhfeng in https://github.com/apache/camel-quarkus/pull/4366
* Added conditional logic for supported content. by @Gerry-Forde in https://github.com/apache/camel-quarkus/pull/4391
* Backport doc fixes by @aldettinger in https://github.com/apache/camel-quarkus/pull/4397
* [2.13.x] Upgrade to Camel 3.18.4 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4336
* Test with Camel 3.18.5 staging repository and Quarkus 2.13.7 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4435
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4495
* test - Define routes in YAML DSL using beans declared in Java by @AnetaCadova in https://github.com/apache/camel-quarkus/pull/4576
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4586
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4671
* Fix (jira): Add jira model classes and jackson joda datatype to @BuildStep (2.13.x) by @claudio4j in https://github.com/apache/camel-quarkus/pull/4677
* Complete vertx-http test coverage #4658 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4714
* [2.13.x] Upgrade Camel to 3.18.6 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4718
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4747
* [2.13.x] Fixup Test also GITHUB_BASE_REF to checkout the right examples branch by @ppalaga in https://github.com/apache/camel-quarkus/pull/4775
* Backports 2.13.x by @zhfeng in https://github.com/apache/camel-quarkus/pull/4779
* [Backport 2.13.x] Mybatis by @zhfeng in https://github.com/apache/camel-quarkus/pull/4778

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.13.2...2.13.3

## 3.0.0-M1

* Fix camel-console documentation xref by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4425
* fix the release doc by @aldettinger in https://github.com/apache/camel-quarkus/pull/4430
* Restore ability to test command mode runners by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4427
* Next is 2.17.0-SNAPSHOT by @aldettinger in https://github.com/apache/camel-quarkus/pull/4429
* Test aws-secrets-manager extension with Localstack #3741 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3815
* Sql test using derby doesn't start dev service and shows class loading issue if stored procedure is called by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3370
* Ref #4426: Support CSimple expressions with all DSLs by @essobedo in https://github.com/apache/camel-quarkus/pull/4428
* Fix #4122 to use io.quarkiverse.cxf:quarkus-cxf-saaj by @zhfeng in https://github.com/apache/camel-quarkus/pull/4431
* Fix #4128 add a dependabot to upgrade quarkiverse versions by @zhfeng in https://github.com/apache/camel-quarkus/pull/4433
* Add test for REST DSL returning CORS headers for OPTIONS request by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4438
* Clean up redundant properties from root pom.xml by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4439
* fix the master-openshift itest flakiness #4387 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4445
* testing defaultCredentialProvider with local stack #440 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4404
* Pass Object class type to BeanManager.getReferece to avoid bean resolution issues #4444 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4446
* Ref #2083: jOOR language native support by @essobedo in https://github.com/apache/camel-quarkus/pull/4440
* perf-regression: add --use-mandrel-native-builder option #4436 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4448
* Fix #4454 to set jms properties with ActiveMQ connections in native build by @zhfeng in https://github.com/apache/camel-quarkus/pull/4455
* Aws2-sqs: Test with real account fails. #4389 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4450
* Ref #4452: Allow to disable build time compilation of jOOR expressions by @essobedo in https://github.com/apache/camel-quarkus/pull/4453
* Support registry lookups by name for beans annotated with io.smallrye.common.annotation.Identifier by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4449
* Ref #1748: OGNL language native support by @essobedo in https://github.com/apache/camel-quarkus/pull/4451
* Verify that the specific log message we're interested in is not present for RouteBuilderWarningWithoutProducedBuilderTest by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4456
* Fixup SQL extension documentation and native testing with alternate JDBC drivers by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4460
* Ref #3088: Split JSON dataformats test by @ldrozdo in https://github.com/apache/camel-quarkus/pull/4457
* Re-enable filter test #3584 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4471
* file: fix warning by @aldettinger in https://github.com/apache/camel-quarkus/pull/4476
* Provide SBOM for Camel-Quarkus project by @oscerd in https://github.com/apache/camel-quarkus/pull/4461
* Add quarkus-qpid-jms for dependency version checking by @zhfeng in https://github.com/apache/camel-quarkus/pull/4480
* Update dependabot by @zhfeng in https://github.com/apache/camel-quarkus/pull/4483
* Improve yaml-dsl documentation and add simple test scenarios by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4489
* Improve documentation, add Kubernetes section by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4521
* Upgrade to Camel 4.0.0 and Quarkus 3.0.0.Alpha3 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4504
* Mail test fails in native mode #4519 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4530
* Upgrade quarkus-pooled-jms to 2.0.0.CR1 and re-enable jms-artemis-tests by @zhfeng in https://github.com/apache/camel-quarkus/pull/4533
* perf-regression: restore PerfRegressionIT to test against 3.0.0-SNAPSOT by @aldettinger in https://github.com/apache/camel-quarkus/pull/4535
* Upgrade to Quarkus Minio 3.0.0.Alpha3 and quarkus-qpid-jms-bom 2.0.0.Alpha3 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4536
* Fix JAXB and Salesforce native compilation issues by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4539
* Fix references to removed extensions and reference Camel latest docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4549
* Ref #4523/#4443: Get rid of jOOR workarounds used for native mode by @essobedo in https://github.com/apache/camel-quarkus/pull/4542
* Fix NPE instantiating Azure HttpHeaderName by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4543
* Remove unwanted references to grpc-netty-shaded from OpenTelemetry extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4545
* Re-enable saxon tests by @zhfeng in https://github.com/apache/camel-quarkus/pull/4547
* Provide a Github action for generating SBOM by @oscerd in https://github.com/apache/camel-quarkus/pull/4554
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4558
* Fix vertx-websocket URIs in tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4556
* camel-quarkus-integration-test-aws2 fail to compile (native) with Camel4 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4552
* Fix camel-nitrite native tests by @zhfeng in https://github.com/apache/camel-quarkus/pull/4551
* Remove redundant CI workflow steps by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4544
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4561
* bump quarkus-artemis-bom from 3.0.0.Alpha3 to 3.0.0.Alpha6 by @dependabot in https://github.com/apache/camel-quarkus/pull/4559
* Fix compilation of gRPC service stubs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4548
* Upgrade Quarkus to 3.0.0.Alpha4 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4563
* xml-io: Remove the use of XMLRoutesDefinitionLoader deprecated class by @aldettinger in https://github.com/apache/camel-quarkus/pull/4565
* Adapt FHIR tests after upgrade to 6.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4562
* Use XML DSL to define templated route in integration test by @osmman in https://github.com/apache/camel-quarkus/pull/4567
* Aws2: Add testing of defaultCredentialProvider to each extension #4442 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4474
* Revert "Disable crypto test temporarily #4510" by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4571
* xml-io: document the possibility to add all charsets in native mode by @aldettinger in https://github.com/apache/camel-quarkus/pull/4573
* Restore ability for camel.main.javaRoutesIncludePattern to be overridable at runtime by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4575
* create itest project for xml-io + bean #4579 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4580
* Update qute.json to 3.0.0-SNAPSHOT by @zhfeng in https://github.com/apache/camel-quarkus/pull/4581
* Fix contributor guide by @aldettinger in https://github.com/apache/camel-quarkus/pull/4583
* Split json dataformats to different modules by @ldrozdo in https://github.com/apache/camel-quarkus/pull/4578
* [backport to main] test - Define routes in YAML DSL using beans declared in Java  by @AnetaCadova in https://github.com/apache/camel-quarkus/pull/4577
* Multiple test methods do not work when extending CamelQuarkusTestSupport by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4568
* Restore camel-quarkus-stax testing by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4584
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4587
* Upgrade github-api to 1.313 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4590
* Ref #4393: Groovy DSL - Get rid of --report-unsupported-elements-at-runtime by @essobedo in https://github.com/apache/camel-quarkus/pull/4589
* Minimize exclusions in the BOM, remove superfluous exclusions in extensions by @ppalaga in https://github.com/apache/camel-quarkus/pull/4593
* Upgrade to Quarkiverse CXF 2.0.0  by @ppalaga in https://github.com/apache/camel-quarkus/pull/4594
* Move Qute component camel-package-maven-plugin execution phase to process-classes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4595
* Upgrade quarkus-jackson-jq to 2.0.0.Alpha by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4599
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4601
* Zendesk test cannot be compiled to native with Camel 4 and Quarkus 3  by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4592
* Remove workaround for quarkus.http.test-ssl-port being ignored by REST Assured by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4604
* Micrometer test coverage - @Counted by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4600
* Improve Telegram integration tests by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4608
* Split infinispan testing into separate modules for the quarkus and camel managed clients by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4607
* Exclude banned xml-apis from org.seleniumhq.selenium:htmlunit-driver by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4610
* Upgrade to cq-maven-plugin 3.5.3: flatten faster, sync-versions in by @ppalaga in https://github.com/apache/camel-quarkus/pull/4611
* Aws2-cw: remove  io.quarkus:quarkus-jaxp from aws2-cw #4614 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4615
* Bump to CycloneDX Maven Plugin 2.7.5 by @oscerd in https://github.com/apache/camel-quarkus/pull/4618
* Upgrade to cq-maven-plugin 3.5.4 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4613
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4623
* Upgrade Quarkus to 3.0.0.Alpha5 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4627
* Bump quarkus-artemis to 3.0.0.Alpha7 and quarkus-pooled-jms to 2.0.0.CR2 by @zhfeng in https://github.com/apache/camel-quarkus/pull/4636
* Generated sources regen for SBOM by @github-actions in https://github.com/apache/camel-quarkus/pull/4643
* Upgrade to Quarkiverse CXF 2.0.0.Alpha4 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4644
* camel-quarkus-language: Added language, resource and options tests by @djcoleman in https://github.com/apache/camel-quarkus/pull/4641
* Ref #4596: Create jdbc tests for generated keys and other headers by @ldrozdo in https://github.com/apache/camel-quarkus/pull/4646
* Fix #4635 replace with ReflectiveClassBuildItem.builder() by @zhfeng in https://github.com/apache/camel-quarkus/pull/4648
* Register atlasmap.properties as a native image resource by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4649
* Upgrade Quarkiverse Freemarker to 1.0.0.Alpha2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4650

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.16.0...3.0.0-M1

## 2.16.0

* Upgrade to quarkus-cxf 1.7.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4331
* Next is 2.16.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4333
* Fix name of bean in the CDI documentation by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4335
* Update index.adoc by @y-luis-rojo in https://github.com/apache/camel-quarkus/pull/4341
* Workaround Datasonnet integration tests fail in native mode on Mandrel by @ppalaga in https://github.com/apache/camel-quarkus/pull/4343
* Exclude io.fabric8:zjsonpatch from test artifacts to avoid version misconvergence by @ppalaga in https://github.com/apache/camel-quarkus/pull/4348
* Depend on quarkus-minio-native instead of quarkus-minio to be able to create Minio clients programmatically by @ppalaga in https://github.com/apache/camel-quarkus/pull/4340
* Check extension pages with strict option by @ppalaga in https://github.com/apache/camel-quarkus/pull/4351
* Added extra content to the Control Bus extension for supported languages. by @Gerry-Forde in https://github.com/apache/camel-quarkus/pull/4354
* Upgrade to Quarkiverse CXF 1.7.2 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4360
* Make at least the quick profile compatible with mvnd 1.0.0-m1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4355
* fix qute package declaration by @claudio4j in https://github.com/apache/camel-quarkus/pull/4364
* Upgrade to 3.20 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4350
* Upgrade cassandra-quarkus-client to 1.1.3 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4368
* Post camel 3.20.0 upgrade cleanup by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4369
* DoBeforeEach does not work with Advice #4362 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4370
* Tika: Enable test testImagePng #4371 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4372
* Register azure-identity IdentityClientBase for runtime initialization by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4373
* Deprecate hdfs and hbase component #3763 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4375
* Remove dependency on camel-health from camel-quarkus-core by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4376
* Add link to supported data formats in dataformat extension docs introduction by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4378
* Fix unterminated listing block breaking the website build by @orpiske in https://github.com/apache/camel-quarkus/pull/4380
* Ref #4358: Java jOOR DSL native support by @essobedo in https://github.com/apache/camel-quarkus/pull/4359
* Poll telegram stop-location endpoint to avoid sporadic failures by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4383
* Ref #4379: Groovy DSL native support by @essobedo in https://github.com/apache/camel-quarkus/pull/4385
* Upgrade Camel to 3.20.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4386
* Reenable Olingo4 native integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4388
* perf-regression: try resolving 'java.home' system property first in itest #4023 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4390
* Add extra support content for JMS pooling usage. by @Gerry-Forde in https://github.com/apache/camel-quarkus/pull/4396
* Test framework - warn if global RouteBuilder may be wrong by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4382
* perf-regression: write the report to disk #4263 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4399
* Aws2: Allow testing of useDefaultCredentialsProvider #4346 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4395
* Upgrade Quarkus to 2.16.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4402
* Expand AWS CW test coverage #4196 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4345
* Splunk: tests are failing after the upgrade of Splunk in Camel #4085 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4124
* Disable MasterOpenShiftIT due to #4387 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4405
* Add platform-http test for retrieving an HTTP header with no value by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4406
* Telegram: Testing subscribing and unsubscribing to Webhook by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4410
* Ref #4392: Kotlin DSL native support by @essobedo in https://github.com/apache/camel-quarkus/pull/4403
* Ref #4416: Make the csimple extension agnostic to build systems by @essobedo in https://github.com/apache/camel-quarkus/pull/4417
* Ref #4407: js-dsl - Improve the interoperability with Java code by @essobedo in https://github.com/apache/camel-quarkus/pull/4409
* Upgrade Quarkus to 2.16.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4419
* Ref #4413: JavaShell DSL support by @essobedo in https://github.com/apache/camel-quarkus/pull/4414
* Restore azure-grouped native testing by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4422
* Add camel-console extension by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4411
* Camel Quarkus 2.16.0 pre-release tasks by @aldettinger in https://github.com/apache/camel-quarkus/pull/4421

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.15.0...2.16.0

## 2.13.2

* 2.13.x backports + Quarkus 2.13.4.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4261
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4271
* Platform-http : add integration tests for reverse proxy feature  by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4273
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4277
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4289
* Google-bigquery: Fixed sqlCrudOperations in branch 2.13.x by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4290
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4297
* [2.13.x] Upgrade Quarkus to 2.13.5.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4299
* Google-bigquery test sqlCrudOperations fails with real account on 2.13.x by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4305
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4310
* [2.13.x] Upgrade to quarkus-cxf 1.5.8 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4318
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4316
* Fix Opentelemetry port numbers in the documentation by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4324
* Fix #4317 to update jms documentation for pooling and XA support (#4323) by @zhfeng in https://github.com/apache/camel-quarkus/pull/4325
* [2.13.x] Upgrade to quarkus-cxf 1.5.9; ban org.apache.geronimo.javamail:geronimo-javamail_1.4_mail by @ppalaga in https://github.com/apache/camel-quarkus/pull/4332
* Fix name of bean in the CDI documentation by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4338
* [2.13.x] backports by @ppalaga in https://github.com/apache/camel-quarkus/pull/4344

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.13.1...2.13.2

## 2.15.0

* Fix #4250 to add JmsArtemisXATest by @zhfeng in https://github.com/apache/camel-quarkus/pull/4251
* Upgarde quarkus-artemis to 2.0.1 and quarkus-pooled-jms to 1.0.6 by @zhfeng in https://github.com/apache/camel-quarkus/pull/4253
* Update OpenTelemetry exporter documentation in line with changes in Quarkus 2.14.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4256
* Fix GitHub actions deprecation warnings by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4249
* Google-bigquery: Enable sqlCrudOperations test once Camel version is 3.18.3 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4252
* automatic configuration of FileLockClusterService #4262 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4265
* Next is 2.15.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4257
* Enhance test covarege of MTOM with PAYLOAD data format by @llowinge in https://github.com/apache/camel-quarkus/pull/4264
* add file cluster service automatic configuration itest #4262 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4267
* controlbus: Added language tests (fixes #4008) by @djcoleman in https://github.com/apache/camel-quarkus/pull/4266
* CxfSoapClientTest.wsdlUpToDate() and CxfSoapWssClientTest.wsdlUpToDate() fail on the platform by @ppalaga in https://github.com/apache/camel-quarkus/pull/4268
* Cover endpoint URI based CXF definitions by @llowinge in https://github.com/apache/camel-quarkus/pull/4270
* Fix #4258 make xml integration tests working in Quarkus Platform by @zhfeng in https://github.com/apache/camel-quarkus/pull/4275
* Remove camel-quarkus-support-xstream from salesforce extension by @svkcemk in https://github.com/apache/camel-quarkus/pull/4276
* Revert "Springless JPA extension (#4049)" by @zhfeng in https://github.com/apache/camel-quarkus/pull/4286
* Fix #3980 to disable JMX in commons-pool2 during native building by @zhfeng in https://github.com/apache/camel-quarkus/pull/4287
* Add a separate version property for FHIR core dependencies by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4292
* Minor tidy up of controlbus extension documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4296
* Extend tests with RAW and CXF_MESSAGE dataFormats by @llowinge in https://github.com/apache/camel-quarkus/pull/4293
* Add CXF WS-SecurityPolicy test by @ppalaga in https://github.com/apache/camel-quarkus/pull/4224
* camel-quarkus-catalog - Make it possible to get the camel version  by @ppalaga in https://github.com/apache/camel-quarkus/pull/4302
* Add quarkus-pooled-jms-deployment to camel-quarkus-bom by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4309
* Test Java first CXF server endpoint with multiple SEI methods  by @ppalaga in https://github.com/apache/camel-quarkus/pull/4307
* Upgrade Quarkus to 2.15.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4308
* Remove superfluous actions heading from controlbus docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4311
* Bump Optaplanner to 8.31.0.Final by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4303
* Avoid port clashes with WireMock dynamically allocated port by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4314
* Manage jakarta.xml.soap-api and sync jakarta.ws properties with CXF by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4312
* Fix Opentelemetry port number in the documentation by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4320
* Fix #4317 to update jms documentation for pooling and XA support by @zhfeng in https://github.com/apache/camel-quarkus/pull/4323
* Upgrade to Quarkus 2.15.0.Final and Quarkiverse CXF 1.7.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4322
* tidy up of jms extension documentation by @zhfeng in https://github.com/apache/camel-quarkus/pull/4327
* Updating librairies by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4329

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.14.0...2.15.0

## 2.14.0

* Next is 2.14.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4131
* Fix malformed id headings for AWS extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4132
* Upgrade Quarkus CXF to 1.5.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4136
* kubernetes: auto-configure Kubernetes Cluster Service #4086 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4093
* Manage `reactor-core` & `google-oauth-client` by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4140
* Update release guide by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4137
* Increase test coverage of ref extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4144
* Add tests for rest-openapi extension (apache#4117) by @djcoleman in https://github.com/apache/camel-quarkus/pull/4125
* Adjust the set of components to search in catalog tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4145
* kubernetes: add documentation related to cluster service #4086 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4146
* doc: switch to === syntax to match doc tooling by @aldettinger in https://github.com/apache/camel-quarkus/pull/4150
* Convert germanbooks-iso-8859-1.json to *nix line endings by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4154
* Set explicit nofile ulimit for activemq-artemis-broker image to make it work also on system with low default nofile ulimit by @ppalaga in https://github.com/apache/camel-quarkus/pull/4152
* Update Debezium to 1.9.6.Final by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4156
* Upgrade to camel-3.19.0 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4151
* Fix Netty integration tests on FIPS system by @osmman in https://github.com/apache/camel-quarkus/pull/4130
* Deprecated parameters in several annotations are ignored - inconsisten by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4099
* Add cloudEvents and knative extensions by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3705
* Align Azure SDK BOM version with Camel  by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4163
* Upgrade Quarkus to 2.13.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4160
* Add sync tag for ahc.version property by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4164
* Fix Salesforce endpoint URIs for CDC eventing by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4166
* rest-openapi: Minor update to usage doc (#4117) by @djcoleman in https://github.com/apache/camel-quarkus/pull/4168
* Fallback to mocked responses if XChange APIs are not available by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4170
* Improve test coverage for scheduler component. by @svkcemk in https://github.com/apache/camel-quarkus/pull/4133
* Upgrade Quarkus Amazon services to 1.3.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4157
* Fix assertion invocation in core extension tests by @osmman in https://github.com/apache/camel-quarkus/pull/4173
* Upgrade Quarkus to 2.13.2.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4174
* Add a PackageScanClassResolver implementation that works in native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4178
* Avoid usage of deprecated ClientProxyUnwrapper by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4179
* Exclude com.github.stephenc.jcip:jcip-annotations from Azure extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4183
* Add extension DSL modeline by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3867
* Convert pull_request_template.md to *nix line endings by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4185
* Upgrade to quarkus-cxf 1.5.4 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4184
* Add usage section to ref documentation for CDI integration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4186
* Upgrade cq-maven-plugin to 3.2.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4189
* Upgrade Optaplanner to 8.29.0.Final by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4190
* Upgrade Quarkus Amazon services to 1.3.1 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4191
* Cxf-soap tests: Refactor and split by @ppalaga in https://github.com/apache/camel-quarkus/pull/4195
* Upgrade Quarkus to 2.13.3.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4199
* Increased workflow memory limit to avoid java heap space errors by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4201
* controlbus: Added action option tests (fixes #4009) by @djcoleman in https://github.com/apache/camel-quarkus/pull/4200
* Use official azure-core-http-vertx client in Azure extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4182
* Sync camel-main / quarkus-main nightly CI workflows with the main workflow by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4205
* Fix #3904 increase xslt extension test coverage by @zhfeng in https://github.com/apache/camel-quarkus/pull/4018
* More CXF tests and cleanup by @ppalaga in https://github.com/apache/camel-quarkus/pull/4207
* Move all Debezium itest deployment dependencies to virtualDependencies profile by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4209
* Split mtom tests by @llowinge in https://github.com/apache/camel-quarkus/pull/4210
* CxfSoapMtomIT fails in native mode  by @ppalaga in https://github.com/apache/camel-quarkus/pull/4214
* Remove unecessary basedir in Kafka Oauth tests by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4213
* Manage dependency com.jayway.jsonpath:json-path by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4212
* Upgrade Quarkus to 2.14.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4215
* perf-regression: remove workaround and final fix Java 17 issue #4031 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4222
* fix line ending by @aldettinger in https://github.com/apache/camel-quarkus/pull/4221
* Test CXF service having an Implementation class in the application by @ppalaga in https://github.com/apache/camel-quarkus/pull/4223
* replace doc occurence of camel-quarkus-jaxp with camel-quarkus-xml-jaxp by @aldettinger in https://github.com/apache/camel-quarkus/pull/4229
* Upgrade to quarkus-cxf 1.5.5 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4232
* Adding a test for custom ConnectionFactory without quarkus.artemis.url by @zhfeng in https://github.com/apache/camel-quarkus/pull/4228
* Upgrade Quarkus to 2.14.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4236
* Use Camel hapi-base-version property for hapi-base by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4239
* Fix #3951 to add a test with quarkus-pooled-jms by @zhfeng in https://github.com/apache/camel-quarkus/pull/4237
* Fix : flakiness of Scheduler integration-tests. by @svkcemk in https://github.com/apache/camel-quarkus/pull/4240
* Upgrade Qpid JMS to 0.39.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4245
* camel-quarkus-xchange: MissingResourceException: Can't find bundle for base name sun.util.resources.CurrencyNames, locale en_US by @ppalaga in https://github.com/apache/camel-quarkus/pull/4244

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.13.0...2.14.0

## 2.13.1

* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4143
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4147
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4167
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4176
* Backports 2.13.x by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4188
* 2.13.x backports + Camel 3.18.3 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4227
* Upgrade to quarkus-cxf 1.5.5 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4234

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.13.0...2.13.1

## 2.13.0

* Fix #4007 increase JPA extension test coverage by @osmman in https://github.com/apache/camel-quarkus/pull/4053
* master: fix itests harness in native mode by @aldettinger in https://github.com/apache/camel-quarkus/pull/4055
* pg-replication-slot: fix usage of deprecated method by @aldettinger in https://github.com/apache/camel-quarkus/pull/4056
* Fix deprecated methods by @aldettinger in https://github.com/apache/camel-quarkus/pull/4057
* Ban camel-directvm  by @ppalaga in https://github.com/apache/camel-quarkus/pull/4060
* Migrate to the new config format of the flattener mojo by @ppalaga in https://github.com/apache/camel-quarkus/pull/4061
* Google-pubsub: Improve google-pubsub test coverage with MESSAGE_ID #4062 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4064
* pg-replication-slot: fix warning by @aldettinger in https://github.com/apache/camel-quarkus/pull/4067
* Disabling ContinuousDevTest by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4070
* Improve google-bigquery test coverage #3949 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4035
* Testing guide confusing about CamelTestSupport by @ppalaga in https://github.com/apache/camel-quarkus/pull/4072
* Use AsciiDoc attributes to conditionally include content by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4073
* CXF test fails in Quarkus Platform because of hard-coded absolute local wsdlLocation by @ppalaga in https://github.com/apache/camel-quarkus/pull/4074
* master: add test with openshift #4077 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4078
* update virtual profiles by @aldettinger in https://github.com/apache/camel-quarkus/pull/4079
* Upgrade Camel to 3.18.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4081
* Fix name of artifact id by @llowinge in https://github.com/apache/camel-quarkus/pull/4082
* JCache native mode by @javaduke in https://github.com/apache/camel-quarkus/pull/4088
* Springless JPA extension by @zhfeng in https://github.com/apache/camel-quarkus/pull/4049
* Upgrade Quarkus to 2.13.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4094
* Fix #4096 to sync snakeyaml with quarkus-bom by @zhfeng in https://github.com/apache/camel-quarkus/pull/4098
* Restore named DataSource autowiring for JDBC and SQL extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4101
* Register LegacyPDFStreamEngine for runtime initialization by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4110
* Fix generated AsciiDoc id headings by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4113
* Upgrade OptaPlanner to 8.27.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4118
* Upgrade Quarkus to 2.13.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4121
* CamelQuarkusTestSupport: Alow to use AdiceWith with another route #4104 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/4105
* Upgrade dependencies versions by @zbendhiba in https://github.com/apache/camel-quarkus/pull/4129

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.12.0...2.13.0

## 2.12.0

## What's Changed
* Fix CamelMainRoutesIncludePatternWithAbsoluteFilePrefixDevModeTest on Windows by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3939
* Delete potentially locked file on VM exit for Windows dev mode test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3940
* Next is 2.12.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3941
* Upgrade Quarkus to 2.11.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3943
* Add missing jboss-jaxrs-api_2.1_spec dependency to ServiceNow extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3948
* Test Azure Storage Blob with credentialType AZURE_IDENTITY by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3950
* Add more details to the Jackson ObjectMapper usage section by @ppalaga in https://github.com/apache/camel-quarkus/pull/3952
* Increase test coverage (file/http) for camel-quarkus-validator extension by @svkcemk in https://github.com/apache/camel-quarkus/pull/3953
* Fix intermittent native mode failures of JacksonJsonTest by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3955
* Close the streams returned by Files.walk properly by @ppalaga in https://github.com/apache/camel-quarkus/pull/3954
* Add docs for Sonarcloud build and analysis results by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3958
* Camel Quarkus CXF Extension by @javaduke in https://github.com/apache/camel-quarkus/pull/3931
* Merge the performance regression detection prototype #3905 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3959
* Improve the CXF extension and its tests: by @ppalaga in https://github.com/apache/camel-quarkus/pull/3968
* Update observability guide to reference OpenTelemetry instead of OpenTracing by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3973
* perf-regression: Align to the root mvnw #3960 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3969
* perf-regression: use pure Camel transformation #3974 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3975
* Add more clarity around the purpose of tracing extensions encoding config property by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3976
* CamelTestSupport style of testing #3511 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3847
* Avoid io.quarkus.platform:quarkus-bom usage in perf-regression module by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3981
* Fix doc reference to JUnit 5 component by @gzurowski in https://github.com/apache/camel-quarkus/pull/3986
* Upgrade Camel to 3.18.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3984
* Minor tidying of test-framework modules and docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3990
* CxfSoapClientIT.wsSecurityClient fails in native mode: wsse:Nonce not present in the request  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3985
* Improve/clean-up camel-quarkus-validator extension tests. by @svkcemk in https://github.com/apache/camel-quarkus/pull/3972
* Remove documentation references to REST DSL inline routes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3997
* Fix route inclusion / exclusion filtering by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3996
* Docs module cleanups by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3999
* perf-regression: add integration tests #3982 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3988
* perf-regression: add unit test #4001 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4002
* perf-regression: avoid using fixed port #4004 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4005
* Upgrade Quarkus to 2.12.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4003
* perf-regression: collect reports during releases #3967 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4013
* Restore ability to run js-dsl native tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4017
* Register HttpOperationFailedException for reflection by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4019
* Fix #3389: geocoder test returned wrong city by @djcoleman in https://github.com/apache/camel-quarkus/pull/4020
* Improve google-pubsub test coverage #3910 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3919
* perf-regression: workaround hyperfoil-maven-plugin issue with JDK 17 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4026
* perf-regression: disable itests when building with -Dquickly by @aldettinger in https://github.com/apache/camel-quarkus/pull/4022
* Pass encrypt property to Debezium MS SQL Server JDBC URL by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4030
* Fix conflict in property name with OS env variable by @llowinge in https://github.com/apache/camel-quarkus/pull/4032
* Enlarge timeout for Keycloak startup by @llowinge in https://github.com/apache/camel-quarkus/pull/4034
* perf-regresssion: fix number format exception when java and mvnw don't have same default locale by @aldettinger in https://github.com/apache/camel-quarkus/pull/4024
* [closes #4028] Fix sqsAutoCreateDelayedQueue test by @llowinge in https://github.com/apache/camel-quarkus/pull/4036
* Upgrade to Quarkiverse CXF 1.5.0.CR1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/4037
* Make nightly branch sync workflows report failures if the build was cancelled by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4038
* file: experiment disabling filter test #3584 by @aldettinger in https://github.com/apache/camel-quarkus/pull/4041
* Fix potential NPE in change feed result checking by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4042
* Upgrade Quarkus to 2.12.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4044
* Filter out non-service endpoints in Salesforce testGetRestResources by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4046
* Log warning instead of throw exception for Debezium MongoDB container script execution failure by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4045
* Adapt Dropbox tests to new authentication mechanism by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/4048
* Upgrade to Quarkiverse CXF 1.5.0, the Santuario related stuff moved to quarkus-cxf-santuario by @ppalaga in https://github.com/apache/camel-quarkus/pull/4052

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.11.0...2.12.0

## 2.11.0

* Next is 2.11.0 by @zhfeng in https://github.com/apache/camel-quarkus/pull/3855
* file: attempt atomic move to fix flakiness #3584 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3854
* Use JUnit fail static method import instead of JSoup by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3859
* paho-mqtt5 test failing with Error: Unable to create websockets listener by @ppalaga in https://github.com/apache/camel-quarkus/pull/3861
* Fix #3863 to use quarkus-extension-maven-plugin by @zhfeng in https://github.com/apache/camel-quarkus/pull/3866
* Fix #3864 to add nofile ulimit in ArangodbTestResource by @zhfeng in https://github.com/apache/camel-quarkus/pull/3865
* Upgrade Optaplanner to 8.23.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3868
* Remove redundant BOM imports from integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3873
* Fix PDF encryption in native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3872
* Ensure new extensions are generated to reference quarkus-extension-maven-plugin by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3874
* Upgrade artemiscloud/activemq-artemis-broker to 1.0.5 by @zhfeng in https://github.com/apache/camel-quarkus/pull/3880
* Parametrize infinispan image by @llowinge in https://github.com/apache/camel-quarkus/pull/3881
* Simplify support for Quartz clustering by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3882
* Remove redundant Spring dependencies from JDBC extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3883
* Fix #3884 upgrade quarkus-artemis to 1.2.0 by @zhfeng in https://github.com/apache/camel-quarkus/pull/3885
* Fix #3858 rename to quarkus.camel.source-location-enabled by @zhfeng in https://github.com/apache/camel-quarkus/pull/3886
* Fix #3809 add reflections for all transports by @zhfeng in https://github.com/apache/camel-quarkus/pull/3892
* Upgrade Quarkus to 2.10.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3894
* Remove inline routes from platform-http REST DSL docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3895
* core: Fix some warnings by @aldettinger in https://github.com/apache/camel-quarkus/pull/3898
* dozer: Deprecate the typeConverterEnabled config #3900 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3901
* Increase JAXB extension test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3902
* Upgrade Camel to 3.18.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3897
* Add support for InfinispanRemoteAggregationRepository by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3906
* Add JQ extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3907
* Add some tests for issues fixed in Camel 3.18.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3908
* Remove last logs to ensure flakiness is now corrected #3584 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3912
* Fix some cq warnings by @aldettinger in https://github.com/apache/camel-quarkus/pull/3913
* Fix #3914 to get TransactionManager and UserTransaction from Arc container by @zhfeng in https://github.com/apache/camel-quarkus/pull/3915
* Upgrade Quarkus to 2.11.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3917
* Remove platform-http-vertx workarounds now that Quarkus & Camel Vert.x is in sync by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3920
* Fix #3924 make sure the http client options working in native mode by @zhfeng in https://github.com/apache/camel-quarkus/pull/3925
* [closes #3927] Improve camel quarkus master integration test by @llowinge in https://github.com/apache/camel-quarkus/pull/3928
* Add DataSet extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3929
* Upgrade Quarkus to 2.11.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3930
* Add native support for Azure Identity service principal authentication by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3932
* Fix Google Big Query grpc-netty-shaded exclusions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3933
* Upgrade third party Quarkus extension dependencies by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3934
* Hashicorp vault by @oscerd in https://github.com/apache/camel-quarkus/pull/3935
* Test Quarkus Jackson `ObjectMapper` with `JacksonDataFormat` + Docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3937
* Upgrade to Debezium 1.9.5.Final by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3938

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.10.0...2.11.0

## 2.10.0

* Fix verification of AWS S3 download links by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3777
* Next is 2.10.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3776
* Camel Datasonnet Extension by @javaduke in https://github.com/apache/camel-quarkus/pull/3769
* Fix #3703 to use valueWithDefault during processing @Producer and @EndpointInject by @zhfeng in https://github.com/apache/camel-quarkus/pull/3780
* Upgrade Google Cloud Native Image Support to 0.14.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3779
* Fix failing Slack integration tests with the real Slack service by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3782
* Remove redundant infinispan-jboss-marshalling from the BOM by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3785
* file: Add some extra logs to attempt catching info about flakiness #3584 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3787
* Increase FHIR client socket timeout by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3789
* Exclude unwanted Apache Commons dependencies from hapi-fhir-structures-dstu2.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3794
* Split compression related tests into a test group #3689 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3791
* Improve camel-quarkus-velocity test coverage #3790 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3793
* Upgrade Google Cloud Native Image Support to 0.14.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3796
* Upgrade Camel to 3.17.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3792
* Add debug JVM only extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3800
* Use WireMock for xchange tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3802
* Add migration guide for the 2.10.0 release by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3801
* Create an Azure Key Vault Extension by @oscerd in https://github.com/apache/camel-quarkus/pull/3806
* Reinitialize ActiveMQ IdGenerator at runtime to ensure generated id uniqueness by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3808
* Remove some superfluous runtime initialized classes from camel-quarkus-support-reactor-netty by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3811
* Add zhfeng key by @zhfeng in https://github.com/apache/camel-quarkus/pull/3814
* net.openhft:affinity included in flattened BOM only on Mac  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3813
* Fix #3774 to add tests for openApi oneOf, allOf and anyOf with annotation by @zhfeng in https://github.com/apache/camel-quarkus/pull/3818
* Increase FHIR container startup timeout to 5 minutes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3824
* Better manage FHIR dependencies by @ppalaga in https://github.com/apache/camel-quarkus/pull/3827
* Fix #3829 disable checksum validation when testing with quarkus-aws2-s3 client by @zhfeng in https://github.com/apache/camel-quarkus/pull/3830
* Fix invalid links in the contributor guide doc by @aldettinger in https://github.com/apache/camel-quarkus/pull/3833
* Fix #3823 make quarkus-agroal an optional dependency in camel-quarkus-quartz by @zhfeng in https://github.com/apache/camel-quarkus/pull/3836
* Registry lookup for overridden DefaultBean types does not work by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3821
* Pin fake-gcs-server container image to 1.37 tag by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3839
* Use timer repeatCount of 1 in foundation timer tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3840
* Auto discover routes created as LambdaEndpointRouteBuilder by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3842
* Modify Cassandra & AWS S3 tests to pick up Camel 3.17.0 fixes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3843
* Ensure ElasticSearch REST delete index route uses the correct URI scheme by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3845
* Upgrade Quarkus to 2.10.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3846
* Upgrade OptaPlanner to 8.22.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3849
* Upgrade Quarkus to 2.10.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3851
* Add KeyStoreParameters for reflection by @ismailbaskin in https://github.com/apache/camel-quarkus/pull/3755
* Fix #3828 to update openapi-java document by @zhfeng in https://github.com/apache/camel-quarkus/pull/3852
* update openapi-java usage.adoc by @zhfeng in https://github.com/apache/camel-quarkus/pull/3853

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.9.0...2.10.0

## 2.9.0

* Test Azure Blob with autowiredEnabled=false by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3699
* camel-quarkus-paho: Add reflection config for RandomAccessFile by @zhfeng in https://github.com/apache/camel-quarkus/pull/3691
* Ban javax.el:el-api in favor of  jakarta.el:jakarta.el-api by @ppalaga in https://github.com/apache/camel-quarkus/pull/3690
* Improve mail test coverage #3674 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3685
* Next is 2.9.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3696
* Fix documentation by @aldettinger in https://github.com/apache/camel-quarkus/pull/3704
* Add missing build dependency link on Mail Integration tests by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3710
* Avoid creating serialization config for non-serializable classes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3714
* Fix `Aws2KinesisTest.kinesis` test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3715
* Fix #3656 Improve camel-quarkus-paho-mqtt5 test coverage by @zhfeng in https://github.com/apache/camel-quarkus/pull/3709
* Reduce the noise in verbose flattened BOMs #3702 by @ppalaga in https://github.com/apache/camel-quarkus/pull/3717
* Azure Storage Queue : increase Producer test coverage by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3719
* fixup 501833 Fix Aws2KinesisTest.kinesis test  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3723
* Import quarkus-bom in Catalog by @ppalaga in https://github.com/apache/camel-quarkus/pull/3725
* Fix #3728 AWS S3 integration test should remove all objects in finally block by @zhfeng in https://github.com/apache/camel-quarkus/pull/3729
* Fix #3730 improve paho-mqtt5 ssl tests by @zhfeng in https://github.com/apache/camel-quarkus/pull/3731
* Azure Storage Queue : add consumer integration tests by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3726
* paho: expand test coverage #3720 by @ppalaga in https://github.com/apache/camel-quarkus/pull/3733
* Ensure the S3 download link really works by @ppalaga in https://github.com/apache/camel-quarkus/pull/3734
* Telegram integration test : test sending message with customized keyboard by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3735
* Fix #3737 to register reflection methods of OpenAPI Schema by @zhfeng in https://github.com/apache/camel-quarkus/pull/3738
* Add missing service include pattern for properties-function by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3740
* Fix compilation warnings in Azure Vert.x HTTP Client by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3744
* Fix MockEndpoint usage in Infinispan tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3747
* Remove c3p0 from Quartz extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3751
* Fix MockEndpoint usage in gRPC tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3752
* Upgrade Quarkus to 2.9.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3753
* Remove infinispan-core dependency for unsupported InfinispanRemoteAggregationRepository by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3766
* Upgrade Quarkus to 2.9.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3767
* Upgrade Optaplanner to 8.20.0.Final by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3760
* Upgrade Amazon Services to 1.1.1 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3761
* Update of jakarta.mail to 1.6.7 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3770
* Upgrade Quarkus Qpid JMS to 0.34.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3772
* paho: add test case for RFC3986 style urls #3758 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3759
* Upgrade Debezium to 1.9.2.Final by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3762

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.8.0...2.9.0

## 2.8.0

* Add Azure Core HTTP Client Vert.x extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3597
* Complete the release guide by @aldettinger in https://github.com/apache/camel-quarkus/pull/3607
* Bump Optaplanner to 8.18.0.Final by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3589
* zipfile: complete test coverage #3610 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3611
* Upgrade Quarkus to 2.7.4.Final by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3613
* Increase FHIR extension test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3614
* Remote println from SQS test by @mmuzikar in https://github.com/apache/camel-quarkus/pull/3616
* Disable flaky GrpcTest.forwardOnError by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3622
* Fix cassandraql itests failing on quarkus-platform #3621 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3623
* Slack test : update Readme with new Oauth Configuration by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3620
* Fix #3606 to add reflection configs for Schema and its subClasses by @zhfeng in https://github.com/apache/camel-quarkus/pull/3624
* Disable FHIR versions not required by default by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3626
* Exclude ipfs from camel-quarkus-test-list #3618 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3629
* Fix the antora-playbook.yml link by @aldettinger in https://github.com/apache/camel-quarkus/pull/3633
* Support additional FHIR 2.x versions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3630
* Upgrade Quarkus to 2.7.5.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3631
* Use com.github.java-json-tools:* instead of com.github.fge:*  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3634
* Fix #3432 add a build step to support source location by @zhfeng in https://github.com/apache/camel-quarkus/pull/3628
* Upgrade cassandra-quarkus-client to 1.1.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3636
* Upgrade and sync xmlgraphics-commons by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3637
* Camel quarkus 3532 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3639
* Fix Gradle native builds for Spring backed extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3640
* Slack : fix native support for Webhook URL + add test coverage by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3643
* Fix registration of consul client API proxy interface by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3644
* file: Rewrite the charset related test #3627 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3645
* Tidy geronimo-jms_2.0_spec exclusions by @ppalaga in https://github.com/apache/camel-quarkus/pull/3646
* file: Ensure FileTest.charset is fixed under Windows #3530 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3647
* Switch from `NativeImageTest` to `QuarkusIntegrationTest` by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3648
* Avoid compiling regular expressions in loops by @ppalaga in https://github.com/apache/camel-quarkus/pull/3649
* Upgrade Quarkus to 2.8.0.CR1 + Upgrade Camel to 3.16.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3653
* Fix typo in docs & Added tests for NotNull validator checks by @mmuzikar in https://github.com/apache/camel-quarkus/pull/3652
* Improve Infinispan extension test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3659
* Fix loading of XML routes with routes-include-pattern wildcard by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3660
* Exclude jboss-marshalling-osgi from infinispan-jboss-marshalling by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3661
* Azure integration tests - upgrade the setup script by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3654
* Stop importing io.quarkus:quarkus-bom into camel-quarkus-bom by @ppalaga in https://github.com/apache/camel-quarkus/pull/3662
* Align `com.github.java-json-tools:json-patch` version with Camel by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3663
* Add more details about `@QuarkusIntegrationTest` to the testing guide by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3664
* Revert "Add Camel 3.16.0 staging repository" by @ppalaga in https://github.com/apache/camel-quarkus/pull/3667
* Create Camel Google Secret Manager Extension by @oscerd in https://github.com/apache/camel-quarkus/pull/3668
* paho: fix NullPointerException when MqqtException occurs during reconnect attempt in native mode by @aldettinger in https://github.com/apache/camel-quarkus/pull/3672
* Better control what we manage in our BOM  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3666
* Ban javax.validation and junit 4 by @ppalaga in https://github.com/apache/camel-quarkus/pull/3673
* Ban javax.activation-api, javax.annotation-api and JBoss spec artifacts by @ppalaga in https://github.com/apache/camel-quarkus/pull/3675
* Update antora.yml to point at 3.16.x branches by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3676
* More bans by @ppalaga in https://github.com/apache/camel-quarkus/pull/3678
* paho: Add test coverage for file persistence #3680 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3681
* Improve MicroProfile Fault Tolerance extension test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3679
* Kafka Oauth test bump Keycloak container version to 16.1.1 & leverage Strimzi dev services container by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3619
* Fix potential Azure Blob test failures when testing against the real service by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3683
* Stop managing Snappy in Spark BOM, as it is now managed in quarkus-bom by @ppalaga in https://github.com/apache/camel-quarkus/pull/3684
* Upgrade Quarkus to 2.8.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3693

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.8.0-M1...2.8.0

## 2.7.1

* update antora.yml and source-map.yml for doc release by @djencks in https://github.com/apache/camel-quarkus/pull/3507
* [2.7.x] backports by @ppalaga in https://github.com/apache/camel-quarkus/pull/3519
* [2.7.x] backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3527
* [2.7.x] Backports by @ppalaga in https://github.com/apache/camel-quarkus/pull/3538
* [2.7.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3573
* [2.7.x] Backports + Camel 3.14.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3600
* [2.7.x] Backports by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3635
* Revert "Remove IPFS and Weka extensions temporarily, workaround #3532" by @aldettinger in https://github.com/apache/camel-quarkus/pull/3641
* [2.7.x] Backports by @ppalaga in https://github.com/apache/camel-quarkus/pull/3687

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.7.0...2.7.1

## 2.8.0-M1

* Next is 2.8.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3502
* Fix #3496 Ban javax.servlet:javax.servlet-api in favor of jakarta.servlet:jakarta.servlet-api by @zhfeng in https://github.com/apache/camel-quarkus/pull/3506
* fix update script by @djencks in https://github.com/apache/camel-quarkus/pull/3508
* Fix deprecation warnings and typos in csimple extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3510
* Upgrade formatter-maven-plugin to 2.17.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3512
* Clean up usage of deprecated Quarkus APIs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3513
* Fix nitrate collection tests on slow machines by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3515
* Prevent various plugins from resolving commons-logging:commons-logging by @ppalaga in https://github.com/apache/camel-quarkus/pull/3518
* Expand Cassandra CQL extension test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3522
* Build with Maven 3.8.4 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3523
* Clean up usage of `quarkus.test.flat-class-path` by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3524
* Add release guide notes for updating k8s version labels in example projects by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3516
* Fix #3251 expose REST DSL services to quarkus openapis by @zhfeng in https://github.com/apache/camel-quarkus/pull/3481
* Upgrade to Camel 3.15.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3525
* Remove IPFS and Weka extensions temporarily by @ppalaga in https://github.com/apache/camel-quarkus/pull/3533
* #3503 Add xslt.features support by @DenisIstomin in https://github.com/apache/camel-quarkus/pull/3526
* Exclude json-simple from camel-slack, workaround for CAMEL-17619 by @ppalaga in https://github.com/apache/camel-quarkus/pull/3536
* Upgrade to Quarkus 2.7.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/3535
* Incorrect version of quarkus-maven-plugin may be resolved #3520 by @ppalaga in https://github.com/apache/camel-quarkus/pull/3528
* Fix #1384 camel-mybatis native support by @zhfeng in https://github.com/apache/camel-quarkus/pull/2525
* Run integration tests on Windows by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3537
* Upgrade Maven Wrapper to 3.1.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3539
* Disable flaky messaging resequence test on GitHub CI (#2957) by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3540
* DebeziumMongodbTest skipped tests #3213 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3266
* Poll for optaplanner results to avoid timeouts on slow machines by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3542
* Remove duplicate software.amazon.awssdk:iam dependency declaration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3543
* Deprecate camel-quarkus-spark  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3544
* Remove freemarker inaccurate documentation by @aldettinger in https://github.com/apache/camel-quarkus/pull/3545
* Fixup e72113e send optaplanner test messages asynchronously by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3546
* Fix link to Quarkus Freemarker docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3547
* Fix #3548 to exclude geronimo-jta_1.1_spec in camel-activemq by @zhfeng in https://github.com/apache/camel-quarkus/pull/3550
* Improve available port discovery in tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3555
* gson: complete doc and test coverage #3556 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3557
* Fix #3551 ban all non-canonical JTA specs artifacts by @zhfeng in https://github.com/apache/camel-quarkus/pull/3559
* Upgrade Azurite container image to 3.15.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3563
* Deprecate AHC and AHC-WS extensions  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3560
* Increase azure-storage-blob extension test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3568
* Upgrade Quarkus to 2.7.2.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3570
* Fix JSON keys are unquoted when using writeAsString in native mode #3571 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3576
* Exclude optional `reactor-netty-http-brave` by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3575
* Ensure correct camel-quarkus-examples branch for maintenance branch PRs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3578
* jsonpath: fix different number of ObjectMapper modules between JVM and native mode by @aldettinger in https://github.com/apache/camel-quarkus/pull/3583
* Improve camel-quarkus-bean-validator test coverage #3567 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3572
* Upgrade Quarkus to 2.7.3.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3586
* Add Extension for Camel Azure Servicebus by @oscerd in https://github.com/apache/camel-quarkus/pull/3587
* Manage mvel via cq:sync-versions to allow additional consistency checks by @ppalaga in https://github.com/apache/camel-quarkus/pull/3588
* Bump Hazelcast client to 3.0.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3590
* Remove redundant workarounds for Kotlin compilation on JDK 17 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3591
* file: fix a race condition where an exchange is missed due to a call to mockEndpoint.reset() by @aldettinger in https://github.com/apache/camel-quarkus/pull/3596
* Merge platform-http-engine tests with platform-http by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3598
* Fix #3553 make beans of InterceptStrategy unremovable by @zhfeng in https://github.com/apache/camel-quarkus/pull/3593
* Upgrade third party Quarkus extensions prior to release by @aldettinger in https://github.com/apache/camel-quarkus/pull/3603

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.7.0...2.8.0-M1

## 2.7.0

* See #3396 Main content-as tables by @djencks in https://github.com/apache/camel-quarkus/pull/3397
* Next is 2.7.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3404
* Ban commons-logging and commons-logging-api by @ppalaga in https://github.com/apache/camel-quarkus/pull/3406
* Use Quarkus Platform Maven Wrapper by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3409
* Force Bouncycastle dependency bcutil-jdk15on version by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3412
* Add released branch to official documentation by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3413
* Manage Eclipse jgit version and jzlib version by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3415
* Added Json-Patch JVM Extension  by @oscerd in https://github.com/apache/camel-quarkus/pull/3419
* main update to docs local build v2 by @djencks in https://github.com/apache/camel-quarkus/pull/3422
* camel-website #701: RI info (main) by @djencks in https://github.com/apache/camel-quarkus/pull/3426
* Explicit that the cron extension should be used in conjunction with another extension offering an implementation by @aldettinger in https://github.com/apache/camel-quarkus/pull/3431
* Bump Optaplanner version to 8.14.0.Final by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3311
* Add test coverage for Quarkus SecurityIdentity & Principal in platform-http routes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3435
* Fix timestamp validation in syslog integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3437
* Exclude commons-logging from htmlunit-driver by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3438
* Setup a reproducer showing that camel.main.durationMaxSeconds is not honoured by @aldettinger in https://github.com/apache/camel-quarkus/pull/3434
* Add test coverage for OpenTelemetry `@WithSpan` annotation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3443
* Avoid usage of deprecated io.quarkus.arc.AlternativePriority by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3445
* Remove redundancy in pom description by @apupier in https://github.com/apache/camel-quarkus/pull/3440
* Ban log4j 1.x  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3452
* Remove vertx-kafka extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3450
* Add 2.7.0 migration guide by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3454
* Make local maven settings applied to surefire plugin so that Quarkus could build the application model accordingly by @ppalaga in https://github.com/apache/camel-quarkus/pull/3455
* aws2-quarkus-client Verify that no client except quarkus one is used  by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3458
* Ban netty-all  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3465
* Aws2  quarkus client ddb refactor by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3462
* Update Salesforce Integration tests to Salesforce API upgrade by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3467
* Improve documentation on setting up the Salesforce developer account by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3469
* Upgrade Quarkus to 2.7.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3473
* Avoid creating `CamelBeanBuildItem` for health checks if they are disabled by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3475
* Add NOTE section for smallrye-reactive-messaging usage docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3476
* camel-website #701 camel-quarkus RI table setup (main) by @djencks in https://github.com/apache/camel-quarkus/pull/3477
* Bump Optaplanner to 8.16.0.Final and enable CI tests by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3484
* Update Salesforce doc, with more details on setting developer account by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3482
* Remove quarkus.camel.main.enabled configuration property by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3485
* Dependency management tweaks by @ppalaga in https://github.com/apache/camel-quarkus/pull/3486
* Upgrade Quarkus to 2.7.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3489
* Manage camel-quarkus-catalog in camel-quarkus-bom to allow Camel K to by @ppalaga in https://github.com/apache/camel-quarkus/pull/3490
* Require Maven 3.8.1+ to build Camel Quarkus by @ppalaga in https://github.com/apache/camel-quarkus/pull/3492
* CVE-2020-8908 guava: local information disclosure via temporary directory created with unsafe permissions by @ppalaga in https://github.com/apache/camel-quarkus/pull/3495
* Removed the deprecated @BuildTimeAvroDataFormat annotation #2791 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3500
* Upgrade Camel to 3.14.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3497
* Use parametrized groupId for quarkus-maven-plugin so that it is possible to run our tests with io.quarkus.platform:quarkus-maven-plugin by @ppalaga in https://github.com/apache/camel-quarkus/pull/3501

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.6.0...2.7.0

## 2.2.1

* update to new indexer,jsonpath syntax, fix some versions by @djencks in https://github.com/apache/camel-quarkus/pull/3215
* 2.2.x Backports by @ppalaga in https://github.com/apache/camel-quarkus/pull/3439
* Upgrade to Camel 3.11.5 by @ppalaga in https://github.com/apache/camel-quarkus/pull/3441
* 2.2.x backports by @ppalaga in https://github.com/apache/camel-quarkus/pull/3449
* Backport ban log4j 1.x by @ppalaga in https://github.com/apache/camel-quarkus/pull/3453
* 2.2.x backports by @ppalaga in https://github.com/apache/camel-quarkus/pull/3456

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.2.0...2.2.1

## 2.6.0

* Make building possible with Java 11 through 17 by @ppalaga in https://github.com/apache/camel-quarkus/pull/3313
* Disable FOP integration test failed in native mode on Mac OS #3280 by @ffang in https://github.com/apache/camel-quarkus/pull/3314
* Graceful shutdown strategy used as default one by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3310
* Next is 2.6.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3317
* Document that vertx-websocket consumers run on the Quarkus Vert.x web server by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3320
* Fix `MicroProfileHealthTest.testFailureThreshold` test failure by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3322
* JFR Native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3319
* core: Add RouteConfigurationsBuilder before regular RoutesBuilder when camel main is disabled by @aldettinger in https://github.com/apache/camel-quarkus/pull/3316
* Verify that `kafka.bootstrap.servers` property is available before attempting to use it for dev services by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3330
* Sql test using derby doesn't start dev service and stored procedure is not working on derby db by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3324
* Add manifest to camel-quarkus-support-spring source JARs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3328
* Remove `camel-quarkus-support-common` by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3334
* Ban com.google.code.findbugs:jsr305 unconditionally  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3337
* Fix misalignment of protobuf dependencies by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3338
* Avoid hard coding Bindy resource path for `NativeImageResourceDirectoryBuildItem` by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3349
* :white_check_mark: Kafka Oauth Integration test with Strimzi and Keycloak by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3336
* Fix bash syntax error in CI integration tests step by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3351
* Fix AWS Lambda failing itest #3356 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3357
* fix  [JDK17]kudu:integration test failed in native mode #3340 by @ffang in https://github.com/apache/camel-quarkus/pull/3341
* Workaround for NoSuchFileException: .../target/classes when executing a by @ppalaga in https://github.com/apache/camel-quarkus/pull/3359
* Ban com.sun.activation:javax.activation  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3362
* Use yq installed on the GitHub actions VM instead of downloading and installing it by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3368
* Upgrade Quarkus to 2.6.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3367
* Test AWS2 SQS in isolation by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3352
* Exclude maven-artifact from camel-quarkus-debezium-mongodb  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3373
* Ensure consistent version of `software.amazon.awssdk` dependencies by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3371
* Remove com.amazonaws:aws-java-sdk-swf-libraries from the BOM  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3376
* Ban log4j-core and log4j-slf4j-impl  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3378
* Fix #3374 to use File.separatorChar when get fqcn from generated xslt class files by @zhfeng in https://github.com/apache/camel-quarkus/pull/3380
* Use the Quarkus Artemis BOM and upgrade to 1.0.2 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3383
* protobuf: Missing method "getName" when using contentTypeFormat=json in native mode by @aldettinger in https://github.com/apache/camel-quarkus/pull/3384
* Sql doc update because of the failure with external oracle database by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3272
* Upgrade Quarkus 2.6.0.Final by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3388
* POC/WIP local build setup by @djencks in https://github.com/apache/camel-quarkus/pull/3385
* Upgrade Quarkiverse jgit and Amazon services by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3393
* Upgrade Tika version by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3395
* Upgrade Camel to 3.14.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3392

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.5.0...2.6.0

## 2.5.0

* update to new indexer,jsonpath syntax by @djencks in https://github.com/apache/camel-quarkus/pull/3214
* Import quarkus-bom before camel-quarkus-bom in tests  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3223
* 2592 aws2 quarkus clients tests by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3224
* Temporary disable Tika native tests #3230 by @ppalaga in https://github.com/apache/camel-quarkus/pull/3231
* Disable MongoDB dev services in slack itest by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3234
* Stub GeoCoder nominatim APIs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3235
* camel quarkus main latest to next by @djencks in https://github.com/apache/camel-quarkus/pull/3232
* Simplify `defining-camel-routes.adoc` and remove camel-k-version property by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3238
* Use log await strategy for RabbitMQ container by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3240
* Fix #3095 improve the aws2-s3 doc to explain chunk_signature in multipart upload by @zhfeng in https://github.com/apache/camel-quarkus/pull/3237
* Remove bulkhead related tests as this is not yet implemented in Camel by @aldettinger in https://github.com/apache/camel-quarkus/pull/3241
* Use log await strategy for Spring RabbitMQ container by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3242
* Align Azurite container version with Camel by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3244
* Added explanation that Quarkus-amazon-lambda can not be used by aws2-lambda by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3245
* Add notes about the salesforce-maven-plugin to the Salesforce extension docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3250
* Assert that Component DSL and Endpoint DSL work for AtlasMap by @ppalaga in https://github.com/apache/camel-quarkus/pull/3246
* Sql - native tests fail on NPE during db initialization #3247 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3248
* Make sure all AWS extensions tested with Quarkus clients have the option documented by @ppalaga in https://github.com/apache/camel-quarkus/pull/3253
* Fix #3254 to re-enable KafkaSaslSslIT and KafkaSslIT native tests by @zhfeng in https://github.com/apache/camel-quarkus/pull/3255
* Expand route configurations test coverage #2978 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3257
* Sql - enable stored procedure test for different db types #3080 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3261
* Cannot run tests against alternative BOMs  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3262
* Fixup 218bef4 Deactivate the virtualDepenencies profile via -DnoVirtualDependencies rather than !virtualDependencies by @ppalaga in https://github.com/apache/camel-quarkus/pull/3263
* Remove not needed anymore BeansWeakCache substitution. Closes #3226 by @vladimirfx in https://github.com/apache/camel-quarkus/pull/3264
* avro: Fix the documentation about avro schema build time parsing #3270 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3271
* Fixup 218bef4 Special fix for Solr - Cannot run tests against by @ppalaga in https://github.com/apache/camel-quarkus/pull/3278
* Fix #3276 to make sure all objects have been deleted after each test in AWS2 S3 by @zhfeng in https://github.com/apache/camel-quarkus/pull/3279
* Remove BOM reference to non existent camel-quarkus-xstream-common-deployment dependency by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3285
* Fixup 218bef4 Special fix for JMS extensions depending on by @ppalaga in https://github.com/apache/camel-quarkus/pull/3286
* Fix #3284 to get kafka.bootstrap.servers from DevServicesLauncherConfigResultBuildItem by @zhfeng in https://github.com/apache/camel-quarkus/pull/3287
* Upgrade Quarkus to 2.5.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3288
* Debezium tests are using JUnit 4 Assertions and Assumptions #3289 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3290
* Added missing stored procedure test for oracle by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3269
* Improve Release guide by @ppalaga in https://github.com/apache/camel-quarkus/pull/3292
* Avoid using Camel Salesforce DTO types as the return type or method parameters in integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3291
* Remove management of netty-tcnative-boringssl-static by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3293
* Revert "Fixup 218bef4 Special fix for JMS extensions depending on" by @ppalaga in https://github.com/apache/camel-quarkus/pull/3300
* Make the dependency management more consistent by @ppalaga in https://github.com/apache/camel-quarkus/pull/3303
* Add route configuration test coverage for endpoint route builder #2078 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3301
* Upgrade Quarkus to 2.5.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3305
* Fix broken links to SmallRye Reactive Messaging documentation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3306
* Upgrade Camel to 3.13.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3299
* Upgrade Quarkus Qpid JMS to 0.30.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3308
* Bump nimbus-jose-jwt version from Quarkus bom by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3309

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.4.0...2.5.0

## 2.4.0

* Upgrade Camel to 3.12.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3144
* Camel 3.12 follow-up fixes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3153
* Revert Aws2TestEnvContext changes in order to extend aws lambda function version operations test coverage by @aldettinger in https://github.com/apache/camel-quarkus/pull/3158
* Register CDI event bridges only when required by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3151
* Document how to pass parts of release announcement to Quarkus team by @ppalaga in https://github.com/apache/camel-quarkus/pull/3162
* Check whether service binding is enabled before creating QuarkusKafkaClientFactory by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3161
* CI build should test example projects with camel-quarkus SNAPSHOT #3160 by @ppalaga in https://github.com/apache/camel-quarkus/pull/3165
* Switch back to using containers for ActiveMQ messaging tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3166
* fix  Aws2S3Test could fail with real AWS S3 service caused by bucket name conflicts by @ffang in https://github.com/apache/camel-quarkus/pull/3164
* AWS S3 tests should delete all buckets they create by @ppalaga in https://github.com/apache/camel-quarkus/pull/3168
* AWS Aws2DdbQuarkusClientTest fails with real AWS #3174 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3175
* Fix path to spring boot docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3177
* Fix #3169 to add S3 KMS encryption test by @zhfeng in https://github.com/apache/camel-quarkus/pull/3176
* Update declaring Bean Capability by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3178
* Upgrade Quarkus to 2.4.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3180
* Simplify native mode locale docs section by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3182
* Fix source code formatting in native mode reflection docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3183
* Improve the User guide by @ppalaga in https://github.com/apache/camel-quarkus/pull/3185
* Fixes #3184: Add requires attribute to playbook by @djencks in https://github.com/apache/camel-quarkus/pull/3187
* Aws2-lambda: Add event source mapping test #2749 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3186
* Remove quarkiverse-google-cloud-services dependencies by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3188
* Use log await strategy for ipfs container by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3202
* Upgrade Quarkiverse Minio to 2.3.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3205
* Fix #3157 to make sure the brokers option is configured in native mode by @zhfeng in https://github.com/apache/camel-quarkus/pull/3198
* Aws sqs test fix by @VratislavHais in https://github.com/apache/camel-quarkus/pull/3170
* Add missing shiro reflective class configuration for commons-beanutils converters by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3209
* Upgrade Quarkus to 2.4.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3210
* doc: rephrase the section about charsets in native mode to remove link to substratevm 19.3.0 source code by @aldettinger in https://github.com/apache/camel-quarkus/pull/3220
* Use JDK 17 in alternative JVM jobs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3212
* Upgrade Quarkus Qpid JMS to 0.29.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3221
* Fix #3206 to produce RunTimeConfigurationDefaultBuildItem with camel.component.kafka.brokers property by @zhfeng in https://github.com/apache/camel-quarkus/pull/3218
* Documentation and metadata improvements by @ppalaga in https://github.com/apache/camel-quarkus/pull/3197

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.3.0...2.4.0

## 2.3.0

* Exclude main-unknown-args-ignore test from the list of tests for the platform again by @ppalaga in https://github.com/apache/camel-quarkus/pull/3055
* Document mvn cq:await-release -Dcq.version=$VERSION by @ppalaga in https://github.com/apache/camel-quarkus/pull/3057
* [#3028] Increase test coverage of a binding mode of camel-rest component by @VratislavHais in https://github.com/apache/camel-quarkus/pull/3052
* Document tagging examples in the Release Guide by @ppalaga in https://github.com/apache/camel-quarkus/pull/3059
* Add additional test coverage for REST DSL methods by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3060
* Use testcontainers gcloud for Google PubSub itest by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3061
* Test OpenAPI with YAML responses by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3062
* [Website] fix attribute syntax by @djencks in https://github.com/apache/camel-quarkus/pull/3064
* Allow extending an existing Catalog by @ppalaga in https://github.com/apache/camel-quarkus/pull/3063
* Fixed reference to Kamelets catalog by @oscerd in https://github.com/apache/camel-quarkus/pull/3070
* Fixed reference to camel-kamelets-catalog by @oscerd in https://github.com/apache/camel-quarkus/pull/3071
* Expand OpenApi Java test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3073
* Sql - enable test with different databases #3053 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/3066
* Remove redundant dependencies from openapi-java extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3081
* Add documentation to OpenApi Java extension about apiContextIdListing limitations by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3082
* Only configure OpenTracingTracer if Quarkus Jaeger tracing is enabled by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3085
* Fix #2745 Expand AWS S3 test coverage by @zhfeng in https://github.com/apache/camel-quarkus/pull/3077
* Remove superfluous xml-jaxb dependency from the REST test by @ppalaga in https://github.com/apache/camel-quarkus/pull/3090
* Fixup 9788fb65 Remove leftover extensions-jvm/digitalocean by @ppalaga in https://github.com/apache/camel-quarkus/pull/3098
* Use test grouping for MongoDB extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3092
* Add notes on how to configure the Platform HTTP server by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3096
* Salesforce : add Platform events test fixes #2938 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3068
* Upgrade to the latest Camel & Quarkus releases by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3099
* WIP: [#2777] Increase test coverage of aws2sqs component by @VratislavHais in https://github.com/apache/camel-quarkus/pull/3074
* Improve handling of quarkus.*.enabled configuration properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3100
* Reinstate binance in xchange tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3106
* Improve native support for `org.apache.http.impl.client.BasicAuthCache` by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3104
* Revert back to taking the azure-sdk-bom version from Camel by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3101
* Fix lambda tests on localstack #2595 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3102
* Upgrade aws localstack version #2749 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3108
* use Antora 3.0.0-alpha.9 and fail build on warn to check xrefs by @djencks in https://github.com/apache/camel-quarkus/pull/3110
* Fixup 68be716 Allow extending an existing Catalog by @ppalaga in https://github.com/apache/camel-quarkus/pull/3113
* Test platform-http with SSL enabled by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3112
* Salesforce: Enable Platform Event custom fields in native mode by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3111
* Update Salesforce testCDCAndStreamingEvents test to wait for consuming the right Document by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3105
* Fixup 0be98ea Use test grouping for MongoDB extensions #3089 by @ppalaga in https://github.com/apache/camel-quarkus/pull/3117
* Upgrade Quarkus to 2.2.3.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3118
* aws2-lambda: Added updateFunction and getFunction tests  #2749 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3120
* Upgrade Quarkus to 2.3.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3122
* Use camel-quarkus-build-parent-it as a direct parent of each test module by @ppalaga in https://github.com/apache/camel-quarkus/pull/3128
* Improve Kafka integration with Quarkus dev services by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3125
* aws2-lambda: Add alias tests  #2749 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3126
* Add notes on Quarkus Dev Services to JDBC & SQL extension docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3127
* Improve Kafka test coverage by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3124
* Test Quarkus DynamoDB extension with Camel Quarkus AWS 2 DDB by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3133
* Fix list continuation in aws2-ddb docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3138
* Add missing grouped test modules to formatting steps by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3136
* aws2-lambda: Add lambda function tag operations test #2749 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3139
* Make integration tests runnable against Quarkus Platform BOM  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3134
* Test Quarkus S3 extension with Camel Quarkus AWS 2 S3 by @zhfeng in https://github.com/apache/camel-quarkus/pull/3135
* Upgrade Quarkus to 2.3.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3140
* Fixes #3143: Add latest non-prerelease eips by @djencks in https://github.com/apache/camel-quarkus/pull/3146
* Issue 3143 fix eip xrefs some more by @djencks in https://github.com/apache/camel-quarkus/pull/3147
* Enable auto replacement of camel-spring-boot docs branch reference by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3148
* Enable Aws2TestEnvContext to handle setting up Quarkus AWS configuration properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3141
* Avoid release:prepare failure "The version could not be updated: by @ppalaga in https://github.com/apache/camel-quarkus/pull/3150

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.2.0...2.3.0

## 2.2.0

* Remove an outdated sentence from the SQL extension docs, which was related to quarkus.camel.sql.script-files that was removed recently by @ppalaga in https://github.com/apache/camel-quarkus/pull/2944
* Test Quarkus and Camel ElasticSearch REST client configuration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2943
* [Camel 3.11] Solr cloud integration tests are failing #2814 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2942
* Add native support for transferException by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2940
* Expand ElasticSearch REST test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2945
* Stress the more preferred way of configuring components via CDI by moving it up by @ppalaga in https://github.com/apache/camel-quarkus/pull/2947
* Next is 2.2.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2949
* Document upgrading Camel Quarkus in Quarkus platform by @ppalaga in https://github.com/apache/camel-quarkus/pull/2950
* AWS2 ddb-streams integration tests failures #2860 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2951
* Polish the wording of the Configuration docs page by @ppalaga in https://github.com/apache/camel-quarkus/pull/2956
* More EIP tests by @ppalaga in https://github.com/apache/camel-quarkus/pull/2953
* Improve the Configuration by convention section of the configuration guide by @ppalaga in https://github.com/apache/camel-quarkus/pull/2958
* Removed static modifier from top level class example by @mmuzikar in https://github.com/apache/camel-quarkus/pull/2959
* Fix foundation core faulttolerance itest package name by @aldettinger in https://github.com/apache/camel-quarkus/pull/2961
* More EIP DSL method tests by @ppalaga in https://github.com/apache/camel-quarkus/pull/2962
* Introduce a build time optimized FastComponentNameResolver by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2963
* Use quarkus-grpc-common instead of quarkus-grpc in the gRPC extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2971
* Test sending messages to an SNS FIFO topic #2625 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2960
* Skip querying Jandex for quarkus.camel.native.reflection.include-patterns by @ppalaga in https://github.com/apache/camel-quarkus/pull/2973
* jolt: remove the DeepCopySubstitution in favor of Quarkus serialization by @aldettinger in https://github.com/apache/camel-quarkus/pull/2972
* leveldb: remove useless substitutions in favor of graalvm built-in MethodHandles support by @aldettinger in https://github.com/apache/camel-quarkus/pull/2984
* Upgrade Quarkus to 2.1.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2986
* Use shaded spring support dependencies in Spring RabbitMQ extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2985
* Run cq:sync-versions in the CI build to verify properties are in sync by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2987
* Expand AWS DDB tests #2776 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2988
* Improve grouped test modules READMEs by @ppalaga in https://github.com/apache/camel-quarkus/pull/2993
* xchange: explicitly register the CurrencyNames bundle at build time by @aldettinger in https://github.com/apache/camel-quarkus/pull/2992
* Upgrade Camel to 3.11.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2994
* Upgrade to cq-maven-plugin 0.38.0, keep spaces in simple XML elements by @ppalaga in https://github.com/apache/camel-quarkus/pull/2995
* Test language() DSL method & Language component native support by @ppalaga in https://github.com/apache/camel-quarkus/pull/2996
* Salesforce : Expand producer test coverage by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2998
* Be more specific about which endpoints are mocked in AdviceWith test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3003
* Avoid deprecated BuildProducer injection by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3004
* Fix read-lock tests by @philschaller in https://github.com/apache/camel-quarkus/pull/2889
* Introduce a common set of extendable messaging tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3000
* fix extension creation on Windows by @dufoli in https://github.com/apache/camel-quarkus/pull/3008
* Upgrade to cassandra-quarkus 1.1.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/3007
* Salesforce: Expand Consumer integration tests : add tests for Streaming by @zbendhiba in https://github.com/apache/camel-quarkus/pull/3005
* Add dev mode support to camel-quarkus-kotlin by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3011
* Reduce console log output from EipTest.throttle by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3012
* Ensure ActiveMQ container is fully started before running tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3014
* Upgrade Quarkus to 2.2.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3015
* CoreTest.testCamelContextAwareRegistryBeansInitialized failing after RouteBuilder started implementing CamelContextAware by @ppalaga in https://github.com/apache/camel-quarkus/pull/3019
* Test camel-xchange with kraken until binance issues are resolved by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3020
* Test setting the kamelet from an external file provided at runtime #3025 by @aldettinger in https://github.com/apache/camel-quarkus/pull/3026
* property attribute with @Consume does not work  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3029
* Fix failure of SalesforceTest.testGetAccountByQueryHelper by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3030
* Add additional test coverage to messaging extesnions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3031
* Enable Kubernetes extension native tests on CI by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3032
* Add ability to test HL7 extension with multiple HAPI implementations by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3034
* Fix #2965 to register SunJaxb21NamespacePrefixMapp for reflection by @zhfeng in https://github.com/apache/camel-quarkus/pull/3033
* Upgrade Quarkus to 2.2.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3035
* Upgrade Qpid JMS & Quarkiverse Google Cloud Services by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3038
* Remove dummy quarkus.google.cloud.project-id after it was made optional by @ppalaga in https://github.com/apache/camel-quarkus/pull/3039
* Exclude grpc-netty-shaded in favour of grpc-netty as it is not supported by quarkus-opentelemetry by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3040
* Fix Splunk container exposed ports configuration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3041
* fix SolrTest failure when using CloundContainer #2967 by @ffang in https://github.com/apache/camel-quarkus/pull/3024
* Fixup 5cf60953 SolrTest failure when using CloundContainer by @ppalaga in https://github.com/apache/camel-quarkus/pull/3046
* Set the version in docs/antora.yml when tagging a release by @ppalaga in https://github.com/apache/camel-quarkus/pull/3045
* Remove reference to camel-quarkus-attachments from platform-http docs as it is a non-optional transitive dependency by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3047
* Have each release on a <major>.<minor>.x branch to make Antora happy by @ppalaga in https://github.com/apache/camel-quarkus/pull/3048
* Fix branch names in CI builds section by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/3049
* Document the usage of partial Quarkus BOMs  by @ppalaga in https://github.com/apache/camel-quarkus/pull/3050
* Upgrade to OptaPlanner Quarkus 8.9.1.Final by @ppalaga in https://github.com/apache/camel-quarkus/pull/3051
* Polish dependency management by @ppalaga in https://github.com/apache/camel-quarkus/pull/2982

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.1.0...2.2.0

## 2.1.0

* Improve extension descriptions by @ppalaga in https://github.com/apache/camel-quarkus/pull/2848
* Remove Camel 3.11 staging repository by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2851
* Add OpenTelemetry extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2854
* Class loader issues in AvroRpcTest with Quarkus 2.0.0.Alpha3 #2651 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2859
* Dependency upgrades by @ppalaga in https://github.com/apache/camel-quarkus/pull/2863
* Fix usage of incorrect @Produces annotation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2865
* Fix #2763 to ensure jvmSince and nativeSince not newer that current version by @zhfeng in https://github.com/apache/camel-quarkus/pull/2866
* Fix #2869 to combine build sanity checks into a single scirpt by @zhfeng in https://github.com/apache/camel-quarkus/pull/2875
* Clean up unnecessary use of @Unremovable by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2871
* Default to JDK 11 source & target compiler options by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2870
* Add test for camel.faulttolerance.* properties #2780 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2877
* kafka : impossible to authenticate with oauth2 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2879
* Upgrade Quarkus to 2.0.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2883
* Avoid producing FeatureBuildItem in support extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2882
* kudu: remove the useless test harness logic dedicated to Java 8 #2885 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2886
* Remove useless docker-java8 profile by @aldettinger in https://github.com/apache/camel-quarkus/pull/2887
* Leverage catalog metadata to discover unremovable types by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2881
* Add a summary page for messaging extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2890
* Remove extra .Final in plugin version by @gastaldi in https://github.com/apache/camel-quarkus/pull/2895
* add info about usage of nimbus-jose-jwt by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2896
* Warn users that not all combinations of artifacts managed by by @ppalaga in https://github.com/apache/camel-quarkus/pull/2899
* Remove redundant jdk-8-classpath profile by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2891
* Add tests for Quarkus traced beans with Camel routes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2898
* Test EIPs DSL methods #2628 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2641
* fix  can't build camel-fhir extension native image #2906 by @ffang in https://github.com/apache/camel-quarkus/pull/2907
* Upgrade Quarkus to 2.1.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2910
* camel-quarkus-minio: quarkus.minio.url is not mandatory by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2911
* Prevent configuration of QuarkusKafkaClientFactory if quarkus-kubernetes-service-binding is not on the classpath by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2913
* Add information about BOMs and precedence of them by @mmuzikar in https://github.com/apache/camel-quarkus/pull/2912
* Use of serialization feature of Quakus (includes Sql and Nitrite) by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2904
* Build with JDK 16 as 15 is EOL by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2700
* Laveraging quarkus-security by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2915
* Remove reflective class registration for ScramSaslClientFactory by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2920
* Remove quarkus.camel.sql.script-files configuration property  by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2916
* Expand Netty test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2918
* Support JMS ObjectMessage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2919
* Document the options for configuring the ElasticSearch REST client by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2923
* add zbendhiba public key by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2917
* fix  js-dsl integration test failure #2908 by @ffang in https://github.com/apache/camel-quarkus/pull/2909
* Make the creation of a GitHub release a part of the release process by @ppalaga in https://github.com/apache/camel-quarkus/pull/2924
* Use static issues to report nightly sync workflow failures by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2928
* Use default timer delay for main-unknown-args itest application by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2931
* Upgrade to Optaplanner 8.8.0 & fix removeProperty tests by @ppalaga in https://github.com/apache/camel-quarkus/pull/2933
* Upgrade to Quarkus 2.1.0.Final by @ppalaga in https://github.com/apache/camel-quarkus/pull/2934
* Salesforce - Mock existing integration tests by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2921
* Upgrade Quarkus Qpid JMS to 0.26.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2936
* Upgrade Quarkus Debezium to 1.6.1.Final and Quarkus Google Cloud to 0.9.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2937

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.0.0...2.1.0

## 2.0.0

* Avoid duplicating code in BaseModel & FastCamelContext by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2765
* Use autoOffsetReset earliest to avoid missing messages sent by the Kafka producer by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2768
* Avoid port clashes where it may already be reserved via the build-helper-maven-plugin by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2769
* Camel quarkus 2652 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2764
* fix Unable to determine the status of the running process in TimerIT without resteasy by @ffang in https://github.com/apache/camel-quarkus/pull/2773
* Kafka - add Kafka Idempotent repository test by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2771
* Retry Solr cloud container 'is started' steps on failure by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2770
* Test setting MLLP default charset from system property for native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2774
* Adapt Observability docs page to Quarkus 2.0.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2775
* Remove health configuration workaround by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2779
* Fix typo in JIRA relationship name by @mmuzikar in https://github.com/apache/camel-quarkus/pull/2784
* Exclude org.bouncycastle:bcprov-debug-jdk15on from camel-as2 because we by @ppalaga in https://github.com/apache/camel-quarkus/pull/2785
* Deprecated @BuildTimeAvroDataFormat in favor of quarkus-avro build time class generation by @aldettinger in https://github.com/apache/camel-quarkus/pull/2787
* Avoid leaking localstack containers when ryuk is disabled by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2792
* Fixup #2658 Intermittent failures in MongoDbTest.testTailingConsumer and MongoDbTest.testPersistentTailingConsumer() by @ppalaga in https://github.com/apache/camel-quarkus/pull/2795
* Test Quarkus and Camel Infinispan client configuration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2798
* Fix JSON Jackson jacksonConversionPojo test-addressed feedback #2726 by @ffang in https://github.com/apache/camel-quarkus/pull/2790
* fix Issue/2733 : Native support for kamelet.yaml discovery by @valdar in https://github.com/apache/camel-quarkus/pull/2799
* Website build fixed after commit of kamelet docs by @oscerd in https://github.com/apache/camel-quarkus/pull/2804
* fix hazelcast integration-test failed in native mode(On Mac) #2719 by @ffang in https://github.com/apache/camel-quarkus/pull/2789
* Expand jsonpath test coverage #2783 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2803
* Dependency upgrades by @ppalaga in https://github.com/apache/camel-quarkus/pull/2788
* Remove redundant apt dependency from camel-quarkus-bom by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2808
* Upgrade to Cassandra Quarkus 1.1.0-rc2, fix #2801 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2807
* Expand HTTP clients test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2806
* Reduce Splunk minimum free disk space requirement by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2809
* Upgrade to azure-bom 1.0.3 and fix #2207 Upgrading to Jackson 2.12.1 via Quarkus BOM 1.12 breaks Azure SDK v12 extensions by @ppalaga in https://github.com/apache/camel-quarkus/pull/2758
* Remove camel-quarkus-main  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2811
* Unmanage unnecessary google dependencies  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2817
* Replace quarkus.camel.native.resources.* with quarkus.native.resources.* by @ppalaga in https://github.com/apache/camel-quarkus/pull/2813
* Tested and documented the specification of custom beans in application.properties by @aldettinger in https://github.com/apache/camel-quarkus/pull/2818
* Upgrade to Quarkiverse Google Cloud Services 0.8.0.CR1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2820
* Manage Groovy dependencies with groovy-bom import by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2821
* Test camel.threadpool.* set of properties #2781 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2825
* Migration guide: mention Quarkus 2.0 migration guide, by @ppalaga in https://github.com/apache/camel-quarkus/pull/2824
* Update the Command mode docs page by @ppalaga in https://github.com/apache/camel-quarkus/pull/2826
* Deprecate quarkus.camel.sql.script-files configuration property by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2827
* Upgrade to Quarkus 2.0.0.Final by @ppalaga in https://github.com/apache/camel-quarkus/pull/2835
* Use a custom NativeImageStartedNotifier for box native tests to work around #2830 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2837
* Upgrade to Camel 3.11 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2839
* Add a TypeConverter for platform-http to convert Buffer to ByteBuffer and work around #2838 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2846
* Add a section about mocking remote endpoints to Testing page of user guide by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2843
* Fix InfinispanTest failed on Mac #2840 by @ffang in https://github.com/apache/camel-quarkus/pull/2841
* Fix #2285 upgrade to use the lasest narayana lra-coordinator by @zhfeng in https://github.com/apache/camel-quarkus/pull/2847
* Add a section about testing to the Contributor guide by @ppalaga in https://github.com/apache/camel-quarkus/pull/2836

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.0.0-M2...2.0.0

## 2.0.0-M2

* jackson: add test coverage #2634 by @ffang in https://github.com/apache/camel-quarkus/pull/2672
* Fix aws2-support dependency on httpclient by @aloubyansky in https://github.com/apache/camel-quarkus/pull/2683
* Add tests and documentation for transaction policies by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2684
* Fix Error when using camel-quarkus-jackson in native mode: java.lang.ClassNotFoundException by @ffang in https://github.com/apache/camel-quarkus/pull/2680
* Fix typo in EndpointInject title by @kdubois in https://github.com/apache/camel-quarkus/pull/2686
* Fix another typo 'EndpointInject' by @kdubois in https://github.com/apache/camel-quarkus/pull/2687
* Upgrade to Quarkus 2.0.0.CR2 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2688
* fix typo by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2697
* Upgrade SmallRye Reactive Messaging Camel to 3.3.2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2698
* Document usage of dashed query params with platform-http by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2699
* fix camel-quarkus-jacksonxml:JsonView annotations take no effect in native mode by @ffang in https://github.com/apache/camel-quarkus/pull/2682
* Expanded Saxon test coverage with XPath saxon tests and fixed related native issues by @aldettinger in https://github.com/apache/camel-quarkus/pull/2703
* Fix vertx-http SSL integration test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2705
* Added move and delete tests for FTP extension #2645 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2710
* Expand Sql test coverage #2623 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2694
* Enable test for REST body validation by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2717
* Add missing use cases into MongoDb test coverage #2715 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2716
* Fixes for Quarkus Platform 2.0.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2720
* Fix #2608 to create destination correctly and re-enable the test case by @zhfeng in https://github.com/apache/camel-quarkus/pull/2725
* camel-jackson-protobuf data format native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2728
* Upgrade to Quarkus 2.0.0.CR3 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2722
* Fix MasterTest health check endpoint path by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2732
* Disable dev services for Kafka itests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2734
* Improve KafkaClientFactory integration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2735
* Fixed the CryptoIT test #2673 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2738
* Unable to determine the status of the running process in LogIT without resteasy by @ppalaga in https://github.com/apache/camel-quarkus/pull/2740
* Add integration test for discovering custom ProtobufMapper beans by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2742
* Intermittent failures in Aws2SqsSnsIT  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2743
* Intermittent failures in MongoDbTest-testTailingConsumer and testPersistentTailingConsumer by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2746
* Sql aggregator does not work in native mode. by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2748
* camel-jackson-avro data format native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2754
* yaml-dsl: enable flow mode deserialization by default by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/2757
* Unregister MongoDb's ChangeStreamDocument from reflection once it is provided by Quarkus by @ppalaga in https://github.com/apache/camel-quarkus/pull/2759
* Fix Qute component metadata in Camel Quarkus catalog by @ppalaga in https://github.com/apache/camel-quarkus/pull/2760

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/2.0.0-M1...2.0.0-M2

## 2.0.0-M1

* DigitalOcean native support #1594 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2387
* openstack: added nova server and swift tests #1943 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2438
* Show deprecation info in Extensions reference  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2444
* Fix xref-validator download failure by @ppalaga in https://github.com/apache/camel-quarkus/pull/2446
* Upgrade CassandraQL quarkus extension to 1.0.1 #2423 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2440
* [fix] Enforce correct maven version by @llowinge in https://github.com/apache/camel-quarkus/pull/2443
* couchbase JVM : add integration tests fixes #2326 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2327
* Add code.quarkus.io badges on extension pages  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2454
* Fix #2441 to remove the unused beanContainer in FhirR5Processor by @zhfeng in https://github.com/apache/camel-quarkus/pull/2455
* camel-quarkus-dozer Error when running native executable #2449 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2453
* Forward HBase testcontainer's log to stdout to see whether #2458 is occurring by @ppalaga in https://github.com/apache/camel-quarkus/pull/2459
* Make Quarkus Micrometer optional for Reactor Netty extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2462
* Added native support for camel-openstack * components #1943 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2463
* Fix intermittent failure in native MicroprofileMetricsIT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2464
* Improve MongoDB extension documentation of named clients by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2467
* Docs improvements by @ppalaga in https://github.com/apache/camel-quarkus/pull/2469
* Upper case Bootstrap, Fix #2164 by @cunningt in https://github.com/apache/camel-quarkus/pull/2472
* Revisit the documentation #2136 #2470 #2374 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2474
* documentation fixes by @aldettinger in https://github.com/apache/camel-quarkus/pull/2475
* Run Azure tests grouped on the CI thus saving some time by @ppalaga in https://github.com/apache/camel-quarkus/pull/2477
* Upgrade to Quarkus 1.13.2.Final by @ppalaga in https://github.com/apache/camel-quarkus/pull/2478
* Test skipped in native mode should be executed on default Java version at least  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2482
* Add Quarkus service binding support to Kafka extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2480
* Reballance CI test categories, do not validate docs links in inital-mvn-install CI phase by @ppalaga in https://github.com/apache/camel-quarkus/pull/2485
* Reballance CI test categories even more by @ppalaga in https://github.com/apache/camel-quarkus/pull/2488
* Fix invalid xref link to configuration.adoc by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2494
* Simplify testcontainers usage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2493
* Fix SlackConfig class for native builds by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2495
* bindy: fixed the locale test so that it detects issues on Java 8 too by @aldettinger in https://github.com/apache/camel-quarkus/pull/2496
* update ci doc by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2498
* Add integration tests #2388 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2497
* Replace references to camel master branch with main in antora playbooks by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2501
* Add test coverage for Kafka with SSL by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2500
* Add integration-tests-support-kafka module by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2504
* Add test coverage for Kafka with SASL_SSL by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2507
* Qute component improperly classified as other in Camel Quarkus Catalog by @ppalaga in https://github.com/apache/camel-quarkus/pull/2510
* Fix admonitions in adocs by @tadayosi in https://github.com/apache/camel-quarkus/pull/2516
* Fix asciidoctor warnings at camel-website build by @tadayosi in https://github.com/apache/camel-quarkus/pull/2517
* #2490 fix camel-quarkus-hbase-integration-test by @ffang in https://github.com/apache/camel-quarkus/pull/2506
* Fixed classpath and no prefix resources that were ignored in dev mode by @aldettinger in https://github.com/apache/camel-quarkus/pull/2515
* Document the possibility to use CDI beans with camel bean component by @ppalaga in https://github.com/apache/camel-quarkus/pull/2531
* fix org.apache.camel.quarkus.component.lra.it.LraTest failed #2523 by @ffang in https://github.com/apache/camel-quarkus/pull/2524
* Test class component, test bean language  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2530
* Expand REST test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2535
* Expand HL7 test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2538
* Added tests and docs for the simple language #2533 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2541
* fix org.apache.camel.quarkus.component.splunk.it.SplunkTest failure if system timezone isn't UTC by @ffang in https://github.com/apache/camel-quarkus/pull/2543
* Test bean binding  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2544
* XChange native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2545
* Test Batch Consumer, charset, filter, sortby, idempotent of the file component by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2548
* Wrap PR template content in comments by default by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2552
* xpath: Fixed native issue and completed tests/documentation #2547 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2551
* Add MLLP test coverage for setting charset by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2555
* #2550 fix several native integration-test failure by @ffang in https://github.com/apache/camel-quarkus/pull/2553
* Explained how to override default build locale in bindy documentation by @aldettinger in https://github.com/apache/camel-quarkus/pull/2557
* Update changelog action to v1.4 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2556
* [fix] #2356 Test file language by @KurtStam in https://github.com/apache/camel-quarkus/pull/2559
* Move RestBindingMode XML tests to rest itest module by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2563
* Google Storage support #2421 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2505
* AtlasMap: Use jandex to discover the types we need to register for reflection by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2509
* Custom TypeConverter is not automatically registered #2260 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2560
* Add additional SOAP extension test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2562
* With fix of #2260 too many @Converters are registered #2570 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2571
* Do not use deprecated BuildStep.applicationArchiveMarkers() by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2583
* Support @EndpointInject and @Produce  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2581
* Add DataSource configuration docs to JDBC & SQL extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2587
* Test TypeConverters #2537 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2584
* Support @Consume  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2588
* Fixed native issue and completed test/documentation #1710 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2585
* REST extension doc link is not processed correctly  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2593
* AWS2 components have to set client.endpointOverride() to work on Localstack by @ppalaga in https://github.com/apache/camel-quarkus/pull/2596
* Revisit core, main and foundation integration tests #2362 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2597
* Leverage quarkus-hazelcast-client-bom, upgrade to quarkus-hazelcast-client 1.2.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2602
* Document locale limitations in native mode  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2601
* Update first-steps.adoc by @yazidaqel in https://github.com/apache/camel-quarkus/pull/2603
* Add tests for MicroProfile metrics configuration options by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2611
* Add test coverage for additional JMS message types by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2609
* Test core languages in isolation by @ppalaga in https://github.com/apache/camel-quarkus/pull/2614
* Add tests for path prefixes and all HTTP methods by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2619
* Use concurrency key instead of cancel-workflow-runs action by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2620
* add quarkus-jackson in the camel-quarkus-kafka extension by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2629
* Use WireMock for GitHub tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2639
* #2633 jacksonxml: add test coverage by @ffang in https://github.com/apache/camel-quarkus/pull/2635
* Expand MongoDb test coverage #2622 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2642
* Upgrade to Camel 3.10.0 and Quarkus 2.0.0.Alpha3 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2643
* Use CamelContextCustomizer from camel-api by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2640
* Ensure camel-quarkus-integration-wiremock-support is test scope by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2644
* Quick fix - Intermittent failures in MongoDbTest.testTailingConsumer by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2665
* quarkus-jackson is included quarkus-kafka-client since quarkus 2.0.0 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2668
* Fix #2604 to add a camel-sql case in jta integration tests by @zhfeng in https://github.com/apache/camel-quarkus/pull/2655
* upgrade json-smart by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2669
* Eagerly initialized Random in various extensions by @ppalaga in https://github.com/apache/camel-quarkus/pull/2663

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.8.1...2.0.0-M1

## 1.8.1

* Quarkiverse dependency upgrades by @ppalaga in https://github.com/apache/camel-quarkus/pull/2390
* Revert Disable doc xref checks as there is no camel-3.9.x branch yet by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2394
* Increase MicroProfile health extension test coverage by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2369
* Remove Camel 3.9.0 staging repository by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2404
* bindy: created a test to show the locale issue in native and documented by @aldettinger in https://github.com/apache/camel-quarkus/pull/2408
* Document the process for maintaining (quarkus | camel)-master branches by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2412
* bindy: do not embed useless resources #2413 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2414
* core: Removed the href in core javadoc as it does not generate a correct documentation by @aldettinger in https://github.com/apache/camel-quarkus/pull/2418
* Fix intermittent failure in camel-quarkus-master-integration-test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2419
* Fix of Spring RabbitMQ integration test failures after spring update by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2415
* docs: add xref support for adoc file sourced from javadoc by @ppalaga in https://github.com/apache/camel-quarkus/pull/2420
* Consume kubernetes-client-bom via Quarkus BoM by @ppalaga in https://github.com/apache/camel-quarkus/pull/2427
* MLLP Native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2428
* Split misc test category into new networking-dataformats category by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2430
* Upgrade quarkus-google-cloud-services to 0.5.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2429
* Fix NPE if Qute template can't be found by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2434
* Make camel-quarkus-xml-io work again after we broke it with camel-quarkus-xml-io-dsl in 1.8.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2432
* Azure Storage Data Lake appends newline to the file content  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2437

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.8.0...1.8.1

## 1.8.0

* Next is 1.8.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2274
* Prefer SyntheticBeanBuildItem to initializing bean producers via volatile fields  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2277
* Improve the release guide by @ppalaga in https://github.com/apache/camel-quarkus/pull/2279
* Freemarker native support  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2184
* Test AWS 2 CloudWatch by @ppalaga in https://github.com/apache/camel-quarkus/pull/2280
* bindy: fixed reflective classes registration, fixes #2268 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2281
* LRA native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2286
* Test AWS 2 Lambda by @ppalaga in https://github.com/apache/camel-quarkus/pull/2283
* Nitrite native support #1298 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2254
* Test AWS 2 SES by @ppalaga in https://github.com/apache/camel-quarkus/pull/2288
* Use azure-sdk-bom instead of individual azure artifact versions by @ppalaga in https://github.com/apache/camel-quarkus/pull/2297
* Azure Storage Data Lake JVM support #2289 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2305
* bindy: added native support for methods registered with @DataField by @aldettinger in https://github.com/apache/camel-quarkus/pull/2298
* Review usage of quarkus.ssl.native in integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2307
* Move HTTP send-dynamic test to HTTP itest module by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2303
* Stitch and Huawei SMN JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/2309
* Paho MQTT 5 support  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2310
* Adjust references to the runner JAR for the fast-jar format by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2311
* Upgrade Quarkus to 1.12.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2313
* Unable to use Salesforce DTOs in native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2315
* Use camel-servicenow-maven-plugin to generate model classes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2321
* Updated link in testing doc by @mmuzikar in https://github.com/apache/camel-quarkus/pull/2324
* HL7 Native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2325
* Upgrade Quarkus to 1.12.2.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2329
* openstack: added cinder volume tests #1943 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2330
* Document that AtlasMap user classes may need to be registered for reflection by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2328
* openstack: added cinder snapshots and glance tests #1943 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2335
* Add configuration option to ignore unknown arguments by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2338
* Disable OptaplannerTest.solveSync on CI by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2337
* Register classes with Solr Field annotations for reflection by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2342
* Upgrade to Quarkus 1.13.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2343
* Workaround #2207 in Azure Storage Data Lake test by @ppalaga in https://github.com/apache/camel-quarkus/pull/2322
* Remove AWS SDK v1 extensions after they have been removed from Camel by @ppalaga in https://github.com/apache/camel-quarkus/pull/2345
* openstack: added keystone tests #1943 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2346
* Upgrade Kotlin to 1.4.31 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2347
* Fix wrong format of RouteBuilder by @llowinge in https://github.com/apache/camel-quarkus/pull/2334
* Add a link to a blog post about Camel Quarkus command line applications by @ppalaga in https://github.com/apache/camel-quarkus/pull/2352
* Fix some deprecation warnings by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2348
* Deprecate camel-quarkus-componentdsl and camel-quarkus-endpointdsl example by @ppalaga in https://github.com/apache/camel-quarkus/pull/2355
* openstack: added neutron network tests #1943 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2359
* Add test coverage for FTPS by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2357
* Use AbstractHealthCheck for custom health checks by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2364
* openstack: added neutron port, subnet and nova flavor tests #1943 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2372
* Upgrade to Quarkus 1.13.0.Final by @ppalaga in https://github.com/apache/camel-quarkus/pull/2370
* Only invoke completed method on unknown arguments if the failure remedy is FAIL by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2373
* Upgrade to Camel 3.9.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2378
* Replace hbase-testing-util with docker container #2295 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2379
* Merge camel-quarkus-main into camel-quarkus-core #2358 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2371


**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.7.0...1.8.0

## 1.7.0

* Improve the release docs by @ppalaga in https://github.com/apache/camel-quarkus/pull/2141
* add Awaitility + suppress warnings #2127 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2146
* Azure Storage Queue Service native support by @ppalaga in https://github.com/apache/camel-quarkus/pull/2143
* Upgrade to cq-maven-plugin 0.27, stop using mvnd.rules altogether by @ppalaga in https://github.com/apache/camel-quarkus/pull/2148
* Improve the release docs by @ppalaga in https://github.com/apache/camel-quarkus/pull/2150
* Avoid automatic minio client autowiring #2134 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2140
* Avoid usage of deprecated capabilities string constants by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2154
* Upgrade Camel to 3.7.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2158
* IPFS native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2159
* Improvement for first-steps docs by @tstuber in https://github.com/apache/camel-quarkus/pull/2162
* Fix #1895 to remove registerNarayanaReflectiveClass by @zhfeng in https://github.com/apache/camel-quarkus/pull/2165
* XML Tokenize language native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2169
* Upgrade Quarkus to 1.11.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2173
* Some JVM-only extensions falsely advertized as supporting native by @ppalaga in https://github.com/apache/camel-quarkus/pull/2179
* StAX native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2177
* Azure Event Hubs support by @ppalaga in https://github.com/apache/camel-quarkus/pull/2178
* Import quarkus-qpid-jms-bom to our BoM by @ppalaga in https://github.com/apache/camel-quarkus/pull/2181
* XML Security native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2186
* Added OAI-PMH support by @aldettinger in https://github.com/apache/camel-quarkus/pull/2190
* AWS2 EventBridge native support by @oscerd in https://github.com/apache/camel-quarkus/pull/2193
* Changelog is back by @oscerd in https://github.com/apache/camel-quarkus/pull/2195
* Syslog native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2197
* Upgrade SmallRye Reactive Messaging Camel to 2.8.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2199
* Test AWS 2 S3 properly by @ppalaga in https://github.com/apache/camel-quarkus/pull/2198
* Replace the deprecated VanillaUuidGenerator with DefaultUuidGenerator by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/2203
* Replaced the JettyTestServer class with MockOaipmhServer based on wirmock by @aldettinger in https://github.com/apache/camel-quarkus/pull/2214
* Test AWS 2 SQS properly by @ppalaga in https://github.com/apache/camel-quarkus/pull/2217
* PubNub native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2218
* Test AWS 2 SNS properly by @ppalaga in https://github.com/apache/camel-quarkus/pull/2222
* Fix camel-master CI branch build by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2224
* Upgrade to Camel 3.8.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2227
* Ensure integration tests have enough free disk space by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2228
* Use Java 15 as 14 is EOL by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2229
* Allow running AWS 2 tests both grouped and isolated by @ppalaga in https://github.com/apache/camel-quarkus/pull/2230
* Upgrade to Quarkus 1.12.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2231
* Azure EventHubs native build fails with Quarkus 1.12  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2208
* Remove redundant Netty reflective class configuration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2232
* camel-spring-rabbitmq - new component #2128 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2233
* Add Kamelet component extension  by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2235
* [update] Upgrade testcontainers version by @llowinge in https://github.com/apache/camel-quarkus/pull/2236
* Test AWS 2 DynamoDB properly by @ppalaga in https://github.com/apache/camel-quarkus/pull/2238
* Docs xref checks failure with Camel 3.8.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2240
* Remove outdated extensions README by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2244
* Removing unnecessary exclusions in test for CassandaQL by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2234
* AtlasMap native support #1989 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2237
* Test AWS 2 DynamoDB Streams by @ppalaga in https://github.com/apache/camel-quarkus/pull/2242
* await for consumer fixes #2205 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2245
* Enable FOP native integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2247
* Fix intermittent failure of SpringRabbitmqTest by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2246
* Remove Camel 3.8.0 staging repository by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2251
* AWS 2 Kinesis native support  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2252
* Added CBOR data format native support #1754 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2257
* Test AWS 2 Firehose by @ppalaga in https://github.com/apache/camel-quarkus/pull/2256
* Upgrade Quarkus to 1.12.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2263
* JFR JVM only support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2259
* Deprecate Webocket JSR 356 #2262 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2265
* Add basic Netty UDP tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2264
* Upgrade Quarkus Qpid JMS to 0.23.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2270
* Make git tests ignore local configuration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2269
* Disable ryuk on CI builds by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2267
* Azure EventHubs test fixup by @ppalaga in https://github.com/apache/camel-quarkus/pull/2271

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.6.0...1.7.0

## 1.6.0

* Require Java 11 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2073
* Documented the Camel Quarkus testing approach #1981 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2075
* Next is 1.6.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2076
* Last release is 1.5.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2086
* CAMEL-15948: examples.json instead of .adoc files by @zregvart in https://github.com/apache/camel-quarkus/pull/2087
* Added JSONata extension by @aldettinger in https://github.com/apache/camel-quarkus/pull/2088
* Upgrade to Quarkus 1.11.0.Beta1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2089
* Removed loadsApplicationClasses = true as it has no more effect by @aldettinger in https://github.com/apache/camel-quarkus/pull/2092
* Fix twitter itest in native mode using ConfigProvider insted of ConfigProperty by @llowinge in https://github.com/apache/camel-quarkus/pull/2096
* Hazelcast native support fixes #1647 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2093
* Temporarily switch to actions/setup-java by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2104
* Upgrade SmallRye Reactive Messaging Camel to 2.7.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2100
* Temporary workaround for #2109 antora/xref-validator after a component was removed from Camel by @ppalaga in https://github.com/apache/camel-quarkus/pull/2110
* Added Redis Aggregation Repository support in JVM mode only #2085 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2106
* antora/xref-validator failure after a component was removed from Camel by @ppalaga in https://github.com/apache/camel-quarkus/pull/2114
* CSimple NPE even if CSimple language is not used  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2108
* Remove docker.io prefix for images by @llowinge in https://github.com/apache/camel-quarkus/pull/2117
* Remove hystrix from test-categories.yaml by @llowinge in https://github.com/apache/camel-quarkus/pull/2105
* Add more integration tests for camel-hazelcast extension #2094 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2115
* Align Kotlin and SmallRye Reactive Messaging Camel with Quarkus by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2118
* Upgrade Quarkus to 1.11.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2111
* Switch back to AdoptOpenJDK/install-jdk action by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2121
* Remove changelog generation workflow by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2124
* Debezium MongoDB Connector native support by @ppalaga in https://github.com/apache/camel-quarkus/pull/2123
* Add vertx-kafka component support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2125
* add missing QuarkusTestResource by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2129
* Add notes on enabling Geolocation APIs for geocoder extension integration tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2130
* Minio native support #2040 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2116
* Upgrade Quarkus to 1.11.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2133
* Allow running the Azure test against the real Azure API in addition to by @ppalaga in https://github.com/apache/camel-quarkus/pull/2132
* Hazelcast integration tests : switch to testcontainers #2127 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2137
* Upgrade Quarkus Qpid JMS to 0.22.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2138
* Azure Storage Blob native support  by @ppalaga in https://github.com/apache/camel-quarkus/pull/2139

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.5.0...1.6.0

## 1.5.0

* Fix debezium itest pom dependencies to deployments by @llowinge in https://github.com/apache/camel-quarkus/pull/2020
* Leverage Quarkus plugin's generate-code mojo instead of protobuf-maven-plugin to generate protobuf stubs by @ppalaga in https://github.com/apache/camel-quarkus/pull/2018
* Fix protobuf itest dependency on deployment by @llowinge in https://github.com/apache/camel-quarkus/pull/2021
* Camel Avro RPC component native support #1941 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/2025
* Upgrade Quarkus to 1.10.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2032
* Upgrade Quarkus Qpid JMS to 0.21.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2030
* Test the Freemarker extension properly, although only in JVM mode for by @ppalaga in https://github.com/apache/camel-quarkus/pull/2028
* Add Nitrite tests by @ppalaga in https://github.com/apache/camel-quarkus/pull/2035
* JSch native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2038
* Add JVM only extensions for minio by @github-actions in https://github.com/apache/camel-quarkus/pull/2039
* OptaPlanner native support fixes #1721 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/1822
* Fixup 94ef785 Next is 1.5.0-SNAPSHOT by @ppalaga in https://github.com/apache/camel-quarkus/pull/2041
* Fix GeocoderNominationTest postalCode field assertion failure by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2043
* Upgrade Quarkus to 1.10.2.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2047
* Micrometer component support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2050
* Use camel-dependencies as parent pom to inherit camel version properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2051
* Updated the release guide in order to publish sources by @aldettinger in https://github.com/apache/camel-quarkus/pull/2053
* Solr native support fixes #1703 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/2026
* Fix persistence of WireMock mappings by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2054
* Polish how we pass -Djavax.net.ssl.trustStore to the Solr test #2029 by @ppalaga in https://github.com/apache/camel-quarkus/pull/2056
* Upgrade to Quarkus 1.10.3.Final by @ppalaga in https://github.com/apache/camel-quarkus/pull/2062

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.4.0...1.5.0

## 1.4.0

* Jenkinsfile build: changed the JDK name following INFRA new approach by @oscerd in https://github.com/apache/camel-quarkus/pull/1929
* FOP tests fail in Quarkus Platform #1930 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1931
* Increase test coverage for Spring dependent extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1932
* Remove Camel 3.6.0 staging repository by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1934
* Set retention-days parameter on upload-artifact action by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1936
* Exclude glassfish dependencies from hbase-testing-util by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1939
* Lumberjack native support fixes #1732 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/1938
* Camel 3.6.0 upgrade leftovers by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1940
* Enable Spring dependent extensions to work with Quarkus Spring by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1937
* Fix usage of deprecated GitHub actions commands by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1946
* ActiveMQ Default XPath evaluator could not be loaded by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1947
* lumberjack : move client payload sending to test fixes #1949 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/1950
* Leverage camel-platform-http-vertx in platform-http extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1954
* Replace `${camel.quarkus.project.root}` set by directory-maven-plugin with `${maven.multiModuleProjectDirectory}` by @ppalaga in https://github.com/apache/camel-quarkus/pull/1953
* Document Spark peculiarities #1928 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1957
* Leverage Quarkus JAXP extension where applicable #1806 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1958
* Provide a quickly profile by @ppalaga in https://github.com/apache/camel-quarkus/pull/1962
* Twilio native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1964
* Fix #819 saga native support by @zhfeng in https://github.com/apache/camel-quarkus/pull/1963
* Zendesk native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1967
* Upgrade Quarkus to 1.9.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1966
* Fixup Provide a quick profile #1607 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1968
* Automate the process of creating jvm only extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1974
* Add JVM only extensions for aws2-eventbridge by @github-actions in https://github.com/apache/camel-quarkus/pull/1975
* Configuration option (not) to start the runtime #1969 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1977
* Reenable Olingo4 integration test by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1980
* PostgresSQL Event : add usage of Quarkus AgroalDatasource fixes #1909 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/1982
* Added nagios native support #1726 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1991
* Migrate ServiceNow, Slack, Geocoder & Telegram tests to WireMock by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1993
* core: allign BaseModel with org.apache.camel.impl.DefaultModel by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1985
* Upgrade Quarkus to 1.9.2.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1994
* Configurable Debezium itest timeout by @llowinge in https://github.com/apache/camel-quarkus/pull/1999
* Added MSV native support by @aldettinger in https://github.com/apache/camel-quarkus/pull/2000
* Upgrade to Quarkus 1.10.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2004
* Kudu: unshade and remove embedded netty and use quarkus-netty instead by @ppalaga in https://github.com/apache/camel-quarkus/pull/2008
* Added JSLT native support #1740 by @aldettinger in https://github.com/apache/camel-quarkus/pull/2010
* Added key by @aldettinger in https://github.com/apache/camel-quarkus/pull/2015
* Fix github itest to use oauth token instead username/password by @llowinge in https://github.com/apache/camel-quarkus/pull/2009
* Upgrade Quarkus to 1.10.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2016
* Upgrade Quarkus Qpid JMS to 0.20.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/2017

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.3.0...1.4.0

## 1.3.0

* Align jackson-dataformat-xml version with Quarkus jackson by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1925
* typo corrected by @talhacevik in https://github.com/apache/camel-quarkus/pull/1926
* Spark JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1916

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.2.0...1.3.0

## 1.2.0

* Upgrade to cq-maven-plugin 0.19.0, Make mvn -N cq:format remove empty application.properties files by @ppalaga in https://github.com/apache/camel-quarkus/pull/1774
* Wrong jira component version definition in camel-quarkus-bom #1775 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1778
* Fix intermittent failures of SmallRyeReactiveMessagingIT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1780
* Added automatic changelog gh action by @oscerd in https://github.com/apache/camel-quarkus/pull/1784
* Caffeine native support by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1785
* Exclude daily branch build bot generated issues from the changelog by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1786
* Switch from Gitter to Zulip by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1787
* Document update by @NiteshKoushik in https://github.com/apache/camel-quarkus/pull/1788
* Update camel-quarkus-last-release property to 1.1.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1789
* Disruptor native support by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1791
* Use asciidoctor-antora-indexer to produce lists of extensions by @ppalaga in https://github.com/apache/camel-quarkus/pull/1777
* Added jing native support fixes #1741 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1792
* Remove UpdateDocExtensionsListMojo followup #1777 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1794
* gRPC native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1797
* FOP native support #1642 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1793
* Browse native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1801
* Upgrade Quarkus to 1.8.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1809
* Fix launch of camel main applications from IntelliJ by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1808
* feat(ssh): promoting native extension by @squakez in https://github.com/apache/camel-quarkus/pull/1802
* Align guava & google-http-client versions with Quarkus by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1811
* Added UniVocity data formats native support #1756 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1813
* Feat(mongodb): add support for named client by @squakez in https://github.com/apache/camel-quarkus/pull/1688
* Camel quarkus disable auto route discovery not working by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1817
* Run verify for the docs module on the CI #1819 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1820
* Configure NativeImageResourceBuildItem for camel route classpath resources by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1821
* Remove redundant skip of maven-enforcer-plugin execution by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1825
* Fix intermittent failure of AHC-WS itest by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1827
* Use partials instead of pages for the individual Camel bits by @ppalaga in https://github.com/apache/camel-quarkus/pull/1814
* Exclude .idea directory from license checks by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1833
* Atom native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1831
* Use distinct descriptions for the for the various JSON data formats by @ppalaga in https://github.com/apache/camel-quarkus/pull/1832
* List of misc. components empty after the recent docs generation changes by @ppalaga in https://github.com/apache/camel-quarkus/pull/1835
* RSS native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1836
* Ensure InputStream is closed after reading rome.properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1842
* AWS XRay, Headersmap, Jasypt, LevelDB, LRA JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1841
* Velocity Support #837 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1804
* Shiro, Ribbon, JCache JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1849
* Duplicate license headers in Velocity test templates #1843 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1847
* Document camel main xml configuration properties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1853
* Upgrade to cq-maven-plugin 0.20.0, set nativeSince property when promoting an extension to native by @ppalaga in https://github.com/apache/camel-quarkus/pull/1854
* String template native support #1694 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1828
* Improve mock backend logging by @llowinge in https://github.com/apache/camel-quarkus/pull/1818
* Document allowContextMapAll native mode limitations by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1855
* Tidy up pom.xml files by @ppalaga in https://github.com/apache/camel-quarkus/pull/1858
* platform-http: handle requests using a thread from the worker pool by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1857
* Turn of Maven connection pooling to avoid connection issues on the CI actions/runner-images#1499 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1859
* Fix platform-http handler exception handling by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1863
* Move examples to a separate git repository by @ppalaga in https://github.com/apache/camel-quarkus/pull/1864
* Geocoder native support fixes #1645 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/1856
* Upgrade SmallRye Reactive Messaging Camel to 2.4.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1866
* Let's see if removing -Dmaven.wagon.http.pool=false solves the Maven connection problems by @ppalaga in https://github.com/apache/camel-quarkus/pull/1867
* Generate the list of examples from the AsciiDoc pages generated in the by @ppalaga in https://github.com/apache/camel-quarkus/pull/1870
* Headersmap native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1872
* Add an Examples step to the release guide by @ppalaga in https://github.com/apache/camel-quarkus/pull/1875
* NoSuchMethodException: org.apache.camel.service.lra.LRASagaRoutes.<init>() by @ppalaga in https://github.com/apache/camel-quarkus/pull/1873
* Crypto (JCE) native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1878
* Crypto extension requires reflective access to DigitalSignatureConstants by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1883
* CAMEL-QUARKUS-1720: Added Postgres replication slot native support by @aldettinger in https://github.com/apache/camel-quarkus/pull/1880
* Upgrade Quarkus to 1.9.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1887
* Quarkus 1.9.0 post upgrade fixes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1890
* Restrict changelog workflow to only run on apache/camel-quarkus repo by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1893
* Fix twitter itest so it initially waits when start polling tweets by @llowinge in https://github.com/apache/camel-quarkus/pull/1877
* File specific issues for TODOs where necessary #1285 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1897
* camel-quarkus-jira: Add resteasy-common dependency by @mmelko in https://github.com/apache/camel-quarkus/pull/1899
* Upgrade to Debezium 1.3.0.Final by @ppalaga in https://github.com/apache/camel-quarkus/pull/1882
* Document all ways to start a new project by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1903
* Prevent CI workflows running on forks by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1904
* HBase JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1906
* PostgresSQL Event native support fixes #1719 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/1905
* Workaround AdviceWithRouteBuilder and MicroprofileMetrics conflict by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1915
* pgevent: correct service name by @zbendhiba in https://github.com/apache/camel-quarkus/pull/1919
* Upgrade Quarkus to 1.9.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1921
* Added nsq native support fixes #1722 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1920
* LevelDB native support #1839 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1902
* Trigger changelog generation on workflow_dispatch by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1923
* Upgrade Quarkus Qpid JMS to 0.19.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1924

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.1.0...1.2.0

## 1.1.0

* Add master extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1511
* Add extension for smallrye-reactive-messaging-camel by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1514
* 1.0.0 post release cleanup by @ppalaga in https://github.com/apache/camel-quarkus/pull/1524
* Fixed the VerifyError in the health example fixes #1517 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1525
* Completed bean itests with an @InjectMock test by @aldettinger in https://github.com/apache/camel-quarkus/pull/1515
* Avoid purging artifacts for builds that may be in progress by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1529
* Upgarde to camel v3.4.3 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1527
* Let the list of extensions page show extensions, move list of supported by @ppalaga in https://github.com/apache/camel-quarkus/pull/1534
* chore(build): configure ci to run on release rbanches by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1536
* Fixup c11d8da9 Let the list of extensions page show extensions by @ppalaga in https://github.com/apache/camel-quarkus/pull/1537
* Switch from restcountries.eu to estcountries.com as the .eu service is not reliable anymore by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1539
* Added support for flatpack dataformat fixes #796 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1542
* update create new extension guide : add update of test-categories.yaml by @zbendhiba in https://github.com/apache/camel-quarkus/pull/1544
* Add missing netty dependencies to olingo4 extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1543
* Use proper extension page URLs in quarkus-extension.yaml by @ppalaga in https://github.com/apache/camel-quarkus/pull/1546
* Publish SNAPSHOT builds by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1547
* Fix #765 Git support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1548
* Document snapshot builds in CI docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1551
* Fix telegram itest component property resolution by @llowinge in https://github.com/apache/camel-quarkus/pull/1554
* Update mvnd rules and split their entries by newlines where merge conflicts may happen by @ppalaga in https://github.com/apache/camel-quarkus/pull/1555
* Compute the component counts using JavaScript to avoid merge conflicts by @ppalaga in https://github.com/apache/camel-quarkus/pull/1552
* APNS, Asterisk and Atom JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1559
* Fix itest when Twitter acount have spaces in name by @llowinge in https://github.com/apache/camel-quarkus/pull/1562
* RabbitMQ native extension by @Jeansen in https://github.com/apache/camel-quarkus/pull/1567
* Added flatpack component support #1541 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1568
* Atomix, AWS 2 Kinesis, AWS 2 Lambda, Azure Storage Blob and Azure Storage Queue JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1569
* Clean google drive in google sheets itest by @llowinge in https://github.com/apache/camel-quarkus/pull/1575
* Add Dropbox, jOOQ & NATS JVM support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1576
* Improve docs by @ppalaga in https://github.com/apache/camel-quarkus/pull/1579
* Bonita, Beanstalk, Caffeine, ChatScript and Chunk JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1585
* CM SMS Gateway, CMIS, CoAP, CometD and Corda JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1590
* Add DNS, etcd & plusar JVM only extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1595
* Fix servicenow itest to clean resources by @llowinge in https://github.com/apache/camel-quarkus/pull/1596
* Crypto (JCE), DigitalOcean, Disruptor, Deep Java Library and Drill JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1600
* Upgrade to cq-maven-plugin 0.17.0, sanitize model.name when using it as by @ppalaga in https://github.com/apache/camel-quarkus/pull/1603
* Send unique messages in Slack itest to avoid false positive tests by @llowinge in https://github.com/apache/camel-quarkus/pull/1604
* Fix #1602 Speed up the CI by @ppalaga in https://github.com/apache/camel-quarkus/pull/1606
* Fix JAXB dependency alignment issues by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1609
* ZooKeeper, ZooKeeper Master, Zendesk, Yammer, XSLT Saxon, XMPP, XML Security and XJ JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1618
* Add IPFS, IRC & JSch JVM only extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1627
* CI definition tweaks by @ppalaga in https://github.com/apache/camel-quarkus/pull/1624
* XChange, Workday, Wordpress, Weka and Web3j JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1629
* Upgrade Quarkus to 1.7.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1628
* Weather, Velocity, Twilio, Thrift and Stub JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1636
* FOP, Flink, Facebook, ElSQL and Ehcache JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1643
* HDFS, hazelcast, Guava EventBus, Geocoder and Ganglia JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1650
* More CI tweaks by @ppalaga in https://github.com/apache/camel-quarkus/pull/1651
* Hipchat, IEC 60870, Ignite, IOTA and JBPM JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1657
* Re-ballance test-categories.yaml once again by @ppalaga in https://github.com/apache/camel-quarkus/pull/1655
* Fix Twitter itest to wait for latest sent message by @llowinge in https://github.com/apache/camel-quarkus/pull/1660
* Register missing Camel quartz job classes for reflection by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1665
* Add some logging to debug #1632 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1668
* Upgrade to Camel 3.5.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1670
* Fix Google gmail itest to wait for deletion of mail by @llowinge in https://github.com/apache/camel-quarkus/pull/1671
* Add vertx-http component extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1673
* Upgrade to Quarkus 1.8.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1678
* Remove UnbannedReflectiveBuildItem by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1677
* Added nats native support fixes #1578 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1674
* Add integration tests for the camel-quarkus-kotlin extension by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1679
* Align dependencies with latest camel & quarkus releases by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1682
* Move FastUuidGenerator to Camel by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1683
* Add vertx-websocket component extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1681
* Remove camel 3.5.0 staging repositories by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1690
* Replace mvnd.builder.rules with virtual dependencies by @ppalaga in https://github.com/apache/camel-quarkus/pull/1689
* Speedup the initial CI mvn install by adding -T1C by @ppalaga in https://github.com/apache/camel-quarkus/pull/1692
* String Template, Stomp, StAX, Splunk HEC and Splunk JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1699
* Upgrade to cq-maven-plugin 0.18.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1700
* Soroush, Solr, SNMP, SMPP, SIP, and Schematron JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1708
* Dropbox native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1693
* XQuery, Saga, RSS, Robot Framework and QuickFix JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1713
* Printer, PostgresSQL Event, PostgresSQL Replication Slot, OptaPlanner and NSQ JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1724
* Fixed bean parameter bindings using language annotations in native mode by @aldettinger in https://github.com/apache/camel-quarkus/pull/1716
* SSH, Nagios, MyBatis, MVEL and MSV JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1729
* Promote AWS 2 Lambda to native by @Jeansen in https://github.com/apache/camel-quarkus/pull/1730
* MLLP, Milo, Lumberjack, Lucene, LDIF, LDAP, Language JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1736
* Weather native support fixes #1631 by @zbendhiba in https://github.com/apache/camel-quarkus/pull/1715
* Enhance Github itest with configurable credentials by @llowinge in https://github.com/apache/camel-quarkus/pull/1738
* JT400, JSLT, Jing, JCR, JClouds JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1743
* Management JVM support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1744
* Aws2 sts by @oscerd in https://github.com/apache/camel-quarkus/pull/1745
* JGroups, JGroups raft, HL7 Terser, Syslog, JSonApi, ASN.1 File, Barcode, BeanIO, CBOR, JSon Fastjson, uniVocity, Freemarker JVM support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1757
* AWS2-Lambda Extension: Adding interceptors and align to the other extension by @oscerd in https://github.com/apache/camel-quarkus/pull/1758
* Tests for #1563 contextPath ignored for platform-http with REST DSL by @ppalaga in https://github.com/apache/camel-quarkus/pull/1565
* Test for #1497 xml-io should pass namespace info to NamespaceAware by @ppalaga in https://github.com/apache/camel-quarkus/pull/1760
* Added ssl authentation to the nats extensions by @aldettinger in https://github.com/apache/camel-quarkus/pull/1764
* Document cq:create -Dcq.nativeSupported=false for creating JVM-only extensions by @ppalaga in https://github.com/apache/camel-quarkus/pull/1767
* Quarkus 1.8.0.final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1766
* camel-quarkus-main - Uses reflection for setting its name by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1768
* Add some pre-release tasks to the release guide by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1771

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.0.1...1.1.0

## 1.0.1

* Upgrade camel to v3.4.3 (1.0.x) by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1535
* Cherry pick fixes to 1.0.x by @ppalaga in https://github.com/apache/camel-quarkus/pull/1637

## 1.0.0

* Fix #1455 Exclude the node directory from src kit by @WillemJiang in https://github.com/apache/camel-quarkus/pull/1458
* Fix #1428 to add the usage.adoc by @zhfeng in https://github.com/apache/camel-quarkus/pull/1457
* Document how to use mock in JVM mode tests #1449 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1452
* org.apache.camel.quarkus.main.CamelMainApplication should be registered for reflection by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1454
* Add example about how to use @Handler with beans registered to the Camel Context by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1461
* Fix issues with building from the source release zip by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1463
* improve bootstrap and configuration documentation by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1464
* Add JPA extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1467
* Fix #1468 Intermittent failure of CamelDevModeTest by @ppalaga in https://github.com/apache/camel-quarkus/pull/1473
* Fix #1448 Add an integration test for the command mode by @ppalaga in https://github.com/apache/camel-quarkus/pull/1477
* Use java.home system property in TrustStoreResource by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1479
* Fixup #1448 Add delay=-1&repeatCount=1 to the command mode test route to finish faster by @ppalaga in https://github.com/apache/camel-quarkus/pull/1480
* Fix #1426 Dependency parity checks are now done by Quarkus extension-descriptor mojo by @ppalaga in https://github.com/apache/camel-quarkus/pull/1481
* Fixup #1468 Intermittent failure of CamelDevModeTest by @ppalaga in https://github.com/apache/camel-quarkus/pull/1483
* Upgrade to Apache Camel 3.4.1 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1484
* Reproducer for #1459 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1465
* Upgrade Quarkus to 1.6.1.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1489
* Upgrade to Camel 3.4.2 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1492
* Make camel-quarkus-rest depend on camel-quarkus-platform-http by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1496
* Correct bind type for OpenTracingTracer bean by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1498
* Remove service exclude for ThreadPoolProfileConfigurationProperties by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1499
* Fixup #1244 Improve the docs about the default REST transport provider by @ppalaga in https://github.com/apache/camel-quarkus/pull/1503
* Added grok support #1466 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1504
* Fix #1286 Pass deprecation info to quarkus-extension.yaml by @ppalaga in https://github.com/apache/camel-quarkus/pull/1485
* Upgrade to Quarkus 1.7.0.CR1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1508
* Upgrade to Quarkus 1.7.0.CR2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1513
* Add missing camel-quarkus-main dependency to braintree & twitter itests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1516
* Simplify component configuration for box and fhir itests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1518
* Upgrade to Quarkus 1.7.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1520
* Disable merge commits on pull requests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1519
* Upgrade Quarkus Qpid JMS to 0.17.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1521

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.0.0-CR3...1.0.0

## 1.0.0-CR3

* chore(deps): update testcontainers to v1.14.3 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1279
* Add rest and restapi to the list of discoverable factories by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1290
* Fix #1288: automatically set content-length or chunked on platform-http by @nicolaferraro in https://github.com/apache/camel-quarkus/pull/1289
* Drop the Jaxb based XMLRoutesDefinitionLoader by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1292
* Remove superflous metrics dependencies from OpenTracing extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1295
* 1.0.0-CR2 post release polishing by @ppalaga in https://github.com/apache/camel-quarkus/pull/1296
* Enable SSL for Azure extension #1269 by @galderz in https://github.com/apache/camel-quarkus/pull/1294
* Debezium SQL Server Connector native support #1193 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1278
* Minor fixes by @ppalaga in https://github.com/apache/camel-quarkus/pull/1302
* Create extension for camel-openapi-java by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1293
* Fixup #1193 Debezium SQL Server Connector native support by @ppalaga in https://github.com/apache/camel-quarkus/pull/1304
* chore(pgp): update pgp key for Nicola Ferraro by @nicolaferraro in https://github.com/apache/camel-quarkus/pull/1307
* Add AWS2-EC2 native extension by @oscerd in https://github.com/apache/camel-quarkus/pull/1306
* Revert #1299 require Java 11 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1305
* Fixed link in Debezium-sqlserver extension by @oscerd in https://github.com/apache/camel-quarkus/pull/1315
* Leverage cq:promote when porting extensions from JVM to native by @ppalaga in https://github.com/apache/camel-quarkus/pull/1316
* Aws2 translate by @oscerd in https://github.com/apache/camel-quarkus/pull/1317
* Fix #1314 Make Kudu native test runnable on Quarkus Platform on Java 8 and 11 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1318
* Added couchdb consumer support in native mode #1022 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1323
* Fixing the contributing guide links in README.adoc by @oscerd in https://github.com/apache/camel-quarkus/pull/1327
* Avoid duplicate feature registration of camel-support-debezium by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1326
* Add servlet-api to classpath to help pointsto analysis #1319 by @galderz in https://github.com/apache/camel-quarkus/pull/1324
* Debezium tests fail on Quarkus Platform in native mode #1311 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1325
* Fixed contributor guide index.adoc links by @oscerd in https://github.com/apache/camel-quarkus/pull/1330
* Link fix by @oscerd in https://github.com/apache/camel-quarkus/pull/1332
* Revert link by @oscerd in https://github.com/apache/camel-quarkus/pull/1333
* Revert "Fixed how to build link in create new extension guide" by @oscerd in https://github.com/apache/camel-quarkus/pull/1334
* Fix #1066 Remove the need for registering RestBindingJaxbDataFormatFactory by @ppalaga in https://github.com/apache/camel-quarkus/pull/1331
* AWS2-ECS native extension by @oscerd in https://github.com/apache/camel-quarkus/pull/1329
* Declare org.graalvm.nativeimage:svm as provided by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1335
* Add vertx component extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1336
* Cleanup by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1337
* The next is 1.0.0-CR3 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1339
* Add notes related to quarkus extension configuration for amqp, kubernetes & mongo-gridfs docs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1341
* Upgrade to Quarkus 1.5.1.Final by @ppalaga in https://github.com/apache/camel-quarkus/pull/1343
* debezium: exclude org.apache.kafka:kafka-log4j-appender from debezium-embedded transitive dependencies by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1347
* integration-tests: make influxdb tests more idiomatic by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1346
* chore(doc): rephrase CamelServiceDestination javadocs. by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1348
* debezium-embedded dependencies #1340 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1351
* Unable to GET an https URL with netty-http client #695 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1352
* Aws2 eks by @oscerd in https://github.com/apache/camel-quarkus/pull/1354
* AWS2-ECS: Fixed the deployment module by @oscerd in https://github.com/apache/camel-quarkus/pull/1362
* AWS2 DDB by @oscerd in https://github.com/apache/camel-quarkus/pull/1361
* Aws2 iam by @oscerd in https://github.com/apache/camel-quarkus/pull/1364
* AWS2-KMS by @oscerd in https://github.com/apache/camel-quarkus/pull/1369
* Tika support by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/998
* Upgrade to Camel 3.4.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1375
* Adding a dependency to make example work  by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1377
* Type safe component injection by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1372
* Fix #1300 Document SSL auto-enabled by extensions by @ppalaga in https://github.com/apache/camel-quarkus/pull/1378
* Aws2 mq by @oscerd in https://github.com/apache/camel-quarkus/pull/1374
* Log message for FileNotFound exception not clear enough #1365 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1379
* Upgrade to Quarkus 1.5.2.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1382
* AWS2-MSK by @oscerd in https://github.com/apache/camel-quarkus/pull/1385
* Post Camel 3.4.0 upgrade cleanups by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1386
* Fix #1350 List Quarkus configuration options on extension pages by @ppalaga in https://github.com/apache/camel-quarkus/pull/1389
* Revisit camel-quarkus bootstrap by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1344
* Replace joschi/setup-jdk with AdoptOpenJDK/install-jdk by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1392
* Fix potential NPE by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1393
* component injection: produce a CamelRuntimeTaskBuildItem for synchronization purpose by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1394
* The quarkus-bootstrap-maven-plugin is listed twice on all the runtime extensions POMs by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1396
* Cleanup by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1397
* Observe Camel's Management events by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1400
* Aws2 ses by @oscerd in https://github.com/apache/camel-quarkus/pull/1401
* Observe Camel's Lifecycle events by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1404
* Observe Camel's events by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1405
* Update dependencies by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1409
* CAMEL-15216 : Omit the warning of level index by @AemieJ in https://github.com/apache/camel-quarkus/pull/1414
* Added an AWS2-Athena native extension by @oscerd in https://github.com/apache/camel-quarkus/pull/1412
* Added Json Validator support #1367 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1408
* Create a camel-componentdsl extension by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1407
* Rename camel-quarkus-package-maven-plugin to camel-quarkus-maven-plugin by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1416
* Ensure CamelContextAware beans have CamelContext set when bound to the registry by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1420
* Make the local Antora build produce .htaccess file with redirects based by @ppalaga in https://github.com/apache/camel-quarkus/pull/1342
* Ensure Quarkus Vertx instance is set on the camel component when not using camel-quarkus-main by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1422
* Upgrade to Quarkus 1.6.0.CR1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1423
* Enable Debezium tests after the upgrade to Quarkus 1.6.0.CR1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1424
* Fix #1313 Upgrade to Kubernetes Client 4.10.2 to align with Quarkus by @ppalaga in https://github.com/apache/camel-quarkus/pull/1425
* Fix #1263 - Add the camel-jta extension by @zhfeng in https://github.com/apache/camel-quarkus/pull/1411
* Added Jolt support #1421 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1432
* Add skip option to UpdateExtensionDocPageMojo to be able to workaround by @ppalaga in https://github.com/apache/camel-quarkus/pull/1434
* Fix #838 TimeZone-less DTSTART and DTEND not changed to GMT by @ppalaga in https://github.com/apache/camel-quarkus/pull/1366
* Fix #1415 The CI should fail if there are uncommitted changes after the build by @ppalaga in https://github.com/apache/camel-quarkus/pull/1435
* Upgrade cq-maven-plugin to 0.11.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1438
* Improve the bootstrap docs by @ppalaga in https://github.com/apache/camel-quarkus/pull/1442
* Re-enable TimerDevModeTest.logMessageEdit() after the upgrade to Quarkus 1.6.0.CR1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1440
* Split main startup logic by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1444
* Make sure that Quarkus orders booting our runtime before starting to serve HTTP endpoints. by @ppalaga in https://github.com/apache/camel-quarkus/pull/1445
* Upgrade to Quarkus 1.6.0.Final by @ppalaga in https://github.com/apache/camel-quarkus/pull/1446
* Feature: Add camel-mock #531 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1447
* Upgrade Quarkus Qpid JMS to 0.16.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1450

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.0.0-CR2...1.0.0-CR3

## 1.0.0-CR2

* Stub Jira endpoints for integration testing by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1270
* extension dependencies issue by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1276

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.0.0-CR1...1.0.0-CR2

## 1.0.0-CR1

* build: create build-parent-it pom to collect common integration tests set-up by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1151
* Improve master -> camel-master sync workflow by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1158
* Automatic sync branch master to camel-master by @github-actions in https://github.com/apache/camel-quarkus/pull/1150
* deps: update kotlin to v1.3.72 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1159
* Release process improvements by @ppalaga in https://github.com/apache/camel-quarkus/pull/1162
* Upgrade Activemq to version 5.11.12 by @oscerd in https://github.com/apache/camel-quarkus/pull/1149
* Upgrade Quarkus Qpid JMS to 0.14.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1165
* Add native support for Elasticsearch REST by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1164
* Fix #1153 Import camel-quarkus-bom-test into camel-quarkus-build-parent by @ppalaga in https://github.com/apache/camel-quarkus/pull/1168
* Bump Camel-Quarkus last release to 1.0.0-M7 by @oscerd in https://github.com/apache/camel-quarkus/pull/1170
* Bump Jandex to version 1.0.8 by @oscerd in https://github.com/apache/camel-quarkus/pull/1172
* Fixed the jvm8 CI build #1157 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1176
* Fix #1177 Add simple timer dev mode test by @ppalaga in https://github.com/apache/camel-quarkus/pull/1178
* Replace hard coded native test categories with dynamic lookup by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1169
* Code cleanup by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1175
* Upgrade Quarkus to 1.4.2.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1181
* Build Quarkus master faster with -Denforcer.skip -Dquarkus.build.skip -DskipDocs by @ppalaga in https://github.com/apache/camel-quarkus/pull/1184
* Fix #1182 ASM Unsupported api 524288 after the upgrade to Gizmo 1.0.3 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1183
* Add branch commit to the intergration build failure report by @ppalaga in https://github.com/apache/camel-quarkus/pull/1187
* AWS2 Extensions cleanup by @oscerd in https://github.com/apache/camel-quarkus/pull/1186
* AWS2-Commons extensions: Added back SSL Native support by @oscerd in https://github.com/apache/camel-quarkus/pull/1189
* Fix ClassNotFoundException when using quartz extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1196
* Ensure consistent itest name tag by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1199
* Use artifact uploads/downloads instead of cache to pass Maven repo to dependent CI jobs by @ppalaga in https://github.com/apache/camel-quarkus/pull/1198
* Import software.amazon.awssdk:bom instead of managing the items individually by @ppalaga in https://github.com/apache/camel-quarkus/pull/1194
* fix caffeine-lrucache modules name by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1202
* ftp: remove redundant build items by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1201
* Do not skip tests in native jobs using -DskipTest it skips also by @ppalaga in https://github.com/apache/camel-quarkus/pull/1203
* Idiomatic Mustache test by @ppalaga in https://github.com/apache/camel-quarkus/pull/1204
* Move integration-tests support modules out of integration-tests folder by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1207
* Add jaxb as dependency and allow skipping formatting by @galderz in https://github.com/apache/camel-quarkus/pull/1185
* Refactor core module layout by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1211
* Do not use version literals in BOMs by @ppalaga in https://github.com/apache/camel-quarkus/pull/1213
* Rename test support extension to a more meaningful name by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1214
* Remove 'core' prefix for main related integration tests modules by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1217
* Introduce RuntimeCamelContextCustomizerBuildItem to allow to customize the camel context before it is started by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1216
* Workaround for quarkusio/quarkus/issues/9273 by @gnodet in https://github.com/apache/camel-quarkus/pull/1221
* Upgrade to Camel 3.3.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1224
* Camel 3.3.0 upgrade follow ups by @ppalaga in https://github.com/apache/camel-quarkus/pull/1226
* Run the CI on Java 14 instead of Java 12 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1227
* Test Azure extension with Azurite by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1228
* Remove Elasticsearch Rest doc title fix by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1229
* Improve Quarkus metadata by @ppalaga in https://github.com/apache/camel-quarkus/pull/1230
* Bump Testcontainers to version 1.14.2 by @oscerd in https://github.com/apache/camel-quarkus/pull/1231
* Follow the existing naming convention in SAP NetWeaver test artifactId by @ppalaga in https://github.com/apache/camel-quarkus/pull/1220
* Add REST OpenApi native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1236
* Remove duplicate kubernetes-client.version property by @ppalaga in https://github.com/apache/camel-quarkus/pull/1238
* Fix #1212 - Add the allowTemplateFromHeader option in the qute component by @zhfeng in https://github.com/apache/camel-quarkus/pull/1241
* Reduce the number of Qute component dependencies by @ppalaga in https://github.com/apache/camel-quarkus/pull/1240
* Added native support for avro dataformat #1180 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1246
* Add MongoDB GridFS native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1242
* Debezium PostgresSQL Connector native support #1191 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1215
* Add support for MicroProfile Fault Tolerance by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1248
* Fix #1232 Per-extension documentation pages by @ppalaga in https://github.com/apache/camel-quarkus/pull/1249
* Enable update-extension-doc-page mojo in newly scaffolded extensions by @ppalaga in https://github.com/apache/camel-quarkus/pull/1250
* Automatically cancel redundant workflow runs by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1252
* Upgrade to Quarkus 1.5.0.CR1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1253
* #1232 Extension pages by @ppalaga in https://github.com/apache/camel-quarkus/pull/1254
* Document how quarkus-extemsion.yaml is generated by @ppalaga in https://github.com/apache/camel-quarkus/pull/1256
* Fix #1255 Document how extension pages are generated by @ppalaga in https://github.com/apache/camel-quarkus/pull/1257
* Split doc pages to user-guide and contributor-guide directories and set redirects by @ppalaga in https://github.com/apache/camel-quarkus/pull/1259
* Status badges on extension pages by @ppalaga in https://github.com/apache/camel-quarkus/pull/1260
* Remove camel-rest workaround as the fic for the issue is included in Apache Camel 3.3.0 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1265
* Fix #1208 Use influxdb 2.18 as in Camel 3.3.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1264
* Upgrade Quarkus to 1.5.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1267
* Debezium MySQL Connector native support #1192 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1258
* Add Peter Palaga's PGP key by @ppalaga in https://github.com/apache/camel-quarkus/pull/1268

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.0.0-M7...1.0.0-CR1

## 1.0.0-M7

* CI build improvements by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1059
* Post Camel 3.2.0 upgrade fixes by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1063
* Use test resources instead of system properties for easy testing within the quarkus platform by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1060
* Move spring's kotlin processors and substitutions to a dedicated file by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1065
* Post 3.2 cleanup by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1067
* Sidebar menu tidy up names-14567 by @rimshach in https://github.com/apache/camel-quarkus/pull/1068
* Add a native extension for AWS2-SQS by @oscerd in https://github.com/apache/camel-quarkus/pull/1070
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/1062
* Bump to Camel Quarkus 1.0.0-M6 by @oscerd in https://github.com/apache/camel-quarkus/pull/1073
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/1080
* Upgrade to Testcontainers 1.14.0 by @oscerd in https://github.com/apache/camel-quarkus/pull/1081
* Automatic sync branch master to camel-master by @github-actions in https://github.com/apache/camel-quarkus/pull/1074
* http-log example by @davsclaus in https://github.com/apache/camel-quarkus/pull/1079
* Upgrade Jackson to version 2.10.3 to align with Camel version used by @oscerd in https://github.com/apache/camel-quarkus/pull/1082
* AWS2 S3 native extension by @oscerd in https://github.com/apache/camel-quarkus/pull/1085
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/1087
* Upgrade Quarkus to 1.4.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1089
* InfluxDB native support #1036 by @JiriOndrusek in https://github.com/apache/camel-quarkus/pull/1050
* Fix AMQP tests hanging in CI environment by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1091
* Introducing an AWS2 support extension by @oscerd in https://github.com/apache/camel-quarkus/pull/1093
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/1094
* Fixed Typo in the antora.yml file by @oscerd in https://github.com/apache/camel-quarkus/pull/1096
* Docs navigation improvements, docs wording by @ppalaga in https://github.com/apache/camel-quarkus/pull/1098
* Added an AWS2 SNS extension by @oscerd in https://github.com/apache/camel-quarkus/pull/1100
* Kubernetes extension native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1095
* Fix #984 introduce the CamelContextCustomizerBuildItem by @zhfeng in https://github.com/apache/camel-quarkus/pull/1076
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/1102
* Simplify the Catalog related code after the upgrade to Camel 3.2.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1103
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/1104
* Automatic sync branch master to camel-master by @github-actions in https://github.com/apache/camel-quarkus/pull/1106
* Remove unneccessary reflective class registration for camel configuration by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1107
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/1111
* Add integration test for #1105 by @philschaller in https://github.com/apache/camel-quarkus/pull/1110
* Paho extension native support for Websocket Connections by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1114
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/1118
* Dynamic endpoint cannot be resolved properly (toD) by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1117
* Fix Kudu native build on JDK 11 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1122
* Trigger project build on push by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1119
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/1124
* Improve master -> quarkus-master branch scheduled synchronization workflow by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1127
* Fixed wrong repository to point for releasing by @oscerd in https://github.com/apache/camel-quarkus/pull/1129
* Add a doc page to describe the CI setup by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1130
* Added release guide to the documentation by @oscerd in https://github.com/apache/camel-quarkus/pull/1131
* Added an AWS2-CW native extension by @oscerd in https://github.com/apache/camel-quarkus/pull/1134
* Fixes CAMEL-14945: move attribute to component descriptor... by @djencks in https://github.com/apache/camel-quarkus/pull/1125
* Update quarkus to v1.4.0.Final by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1135
* Exclude docs module from quarkus-master sync build by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1139
* Bump Github-api to version 1.111 by @oscerd in https://github.com/apache/camel-quarkus/pull/1138
* Upgrade Quarkus Qpid JMS to 0.14.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1143
* Bump Testcontainers to version 1.14.1 by @oscerd in https://github.com/apache/camel-quarkus/pull/1137
* Update quarkus to v1.4.1.Final by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1147

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.0.0-M6...1.0.0-M7

## 1.0.0-M6

* Add soap dataformat quarkus extension by @mmelko in https://github.com/apache/camel-quarkus/pull/883
* camel-quarkus-core: make caffeine cache optional by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/887
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/890
* telegram extension cannot construct IncomingMessageEntity for commands by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/893
* Update to introduce the quarkus qute extension by @zhfeng in https://github.com/apache/camel-quarkus/pull/878
* The first JVM-only extension by @ppalaga in https://github.com/apache/camel-quarkus/pull/895
* Bump to Camel-Quarkus 1.0.0-M5 by @oscerd in https://github.com/apache/camel-quarkus/pull/900
* Fix #901 GraphQLResource should read from the classpath instead from the filesystem by @ppalaga in https://github.com/apache/camel-quarkus/pull/902
* POM enhancements by @ppalaga in https://github.com/apache/camel-quarkus/pull/903
* Add Google Mail extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/905
* Add Google Drive extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/907
* Fix #897 List the JVM-only extensions in the docs by @ppalaga in https://github.com/apache/camel-quarkus/pull/898
* Cassandra, CouchDB and Couchbase by @ppalaga in https://github.com/apache/camel-quarkus/pull/911
* chore(it): small telegram tests cleanup by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/912
* Add Google Calendar extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/914
* Add Google Sheets extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/919
* Debezium connectors by @ppalaga in https://github.com/apache/camel-quarkus/pull/920
* Use Awaitility to poll for Google calendar deletion by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/921
* chore(deprecation): replace usage fo deprecated io.quarkus.vertx.web.deployment.BodyHandlerBuildItem by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/913
* Updated Maven version in Prerequisites section by @djcoleman in https://github.com/apache/camel-quarkus/pull/923
* InfluxDB, Kudu, MongoDB GridFS and Nitrite by @ppalaga in https://github.com/apache/camel-quarkus/pull/929
* OpenStack support (JVM only) by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/930
* PubNub and RabbitMQ by @ppalaga in https://github.com/apache/camel-quarkus/pull/933
* Add quartz & cron extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/935
* SAP NetWeaver, Groovy and OGNL by @ppalaga in https://github.com/apache/camel-quarkus/pull/938
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/940
* Add Google BigQuery extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/954
* AWS 2 by @ppalaga in https://github.com/apache/camel-quarkus/pull/955
* Fix #936 Warn if JVM only extensions are used in native mode by @ppalaga in https://github.com/apache/camel-quarkus/pull/957
* Added options to select resources for inclusion in native executable #868 by @aldettinger in https://github.com/apache/camel-quarkus/pull/960
* AWS SWF and SDB by @ppalaga in https://github.com/apache/camel-quarkus/pull/962
* Add Kubernetes extension (JVM only) by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/966
* Remove redundant native profile from kubernetes extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/967
* Add AMQP extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/968
* Add Avro JVM Extension by @mmelko in https://github.com/apache/camel-quarkus/pull/969
* Documented the use of quarkus.camel.resources.*-patterns #961 by @aldettinger in https://github.com/apache/camel-quarkus/pull/971
* Update to add the camel-servicenow extension by @zhfeng in https://github.com/apache/camel-quarkus/pull/970
* Clean up poms: introduce test BOM, manage only where necessary by @ppalaga in https://github.com/apache/camel-quarkus/pull/973
* Protobuf and gRPC by @ppalaga in https://github.com/apache/camel-quarkus/pull/974
* Fix asciidoc syntax error by @djencks in https://github.com/apache/camel-quarkus/pull/979
* Add Google Pubsub extension (JVM only) by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/978
* Fix #976 REST OpenApi support (JVM only) by @ppalaga in https://github.com/apache/camel-quarkus/pull/977
* Speedup the Validate PR Style GH workflow by invoking only mvn validate by @ppalaga in https://github.com/apache/camel-quarkus/pull/975
* Fix asciidoc syntax errors in native-mode.adoc by @djencks in https://github.com/apache/camel-quarkus/pull/980
* Separate some misc extensions into their own categories by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/981
* Move quarkus.camel.resources.* config options to quarkus.camel.native.resources.* by @ppalaga in https://github.com/apache/camel-quarkus/pull/983
* Update Quarkus extension description for zip-deflater and lzf by @rsvoboda in https://github.com/apache/camel-quarkus/pull/986
* Fix #253 Build time property to register classes for reflection by @ppalaga in https://github.com/apache/camel-quarkus/pull/987
* Add GitHub extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/982
* Make a tabular list of examples for the examples page by @AemieJ in https://github.com/apache/camel-quarkus/pull/991
* [CAMEL-13704] - Added PR template file with instructions on what to include by @Xxyumi-hub in https://github.com/apache/camel-quarkus/pull/990
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/993
* Add jaxb dependency #996 by @galderz in https://github.com/apache/camel-quarkus/pull/997
* Update Quarkus to v1.3.1.Final by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/992
* New source assembly descriptor and root pom rename by @ppalaga in https://github.com/apache/camel-quarkus/pull/1002
* Make consul depend on core-cloud #1003 by @galderz in https://github.com/apache/camel-quarkus/pull/1004
* Fix #963 Build processor class template may create redundant LOG field by @ppalaga in https://github.com/apache/camel-quarkus/pull/1008
* Upgrade Quarkus Qpid JMS to 0.13.1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1007
* Workaround for the platform http not be able to handle matchOnUriPrefix by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1012
* Fix native images issues on Java 11 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1011
* Use testcontainer to test camel infinispan extension to reduce test dependencies on infinispan by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1014
* chore: small cleanup by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1019
* Camel-AWS-SDB: Make the extension Native by @oscerd in https://github.com/apache/camel-quarkus/pull/1020
* Added native support for Couchdb extension in producer mode #989 by @aldettinger in https://github.com/apache/camel-quarkus/pull/1024
* Fix #1017 Do not hardcode the TransformerFactory implementation by @ppalaga in https://github.com/apache/camel-quarkus/pull/1018
* chore(xslt): cleanup and small fix by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1026
* Temporarilly disable JmsTest until #1023 is fixed by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1025
* Revisit #1017 Be even less intrusive when registering our TransformerFactory by @ppalaga in https://github.com/apache/camel-quarkus/pull/1027
* AWS-SWF: Make the extension native by @oscerd in https://github.com/apache/camel-quarkus/pull/1030
* Fix #706 Improve RuntimeCatalogConfig docs by @ppalaga in https://github.com/apache/camel-quarkus/pull/1009
* Fix intermittent failure of messaging tests by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1031
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/1033
* SAP NetWeaver native support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1038
* TrustStoreResource does not work on Java 11 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1042
* BeanInfo::getImplClazz can be null for primitives or arrays by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1043
* Update groovy to v3.0.2 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/1045
* Servlet tests #853 #854 by @ppalaga in https://github.com/apache/camel-quarkus/pull/1046
* Webhook component support by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1048
* CI build improvements by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/1049
* Completed the contributor guide with a section to promote JVM Only extension to native by @aldettinger in https://github.com/apache/camel-quarkus/pull/1051
* Moved the guide to promote a JVM Only extension to native to a dedicated page by @aldettinger in https://github.com/apache/camel-quarkus/pull/1052
* Bump Quarkus to version 1.3.2.Final by @oscerd in https://github.com/apache/camel-quarkus/pull/1056

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.0.0-M5...1.0.0-M6

## 1.0.0-M5

* Fix #784 iCal support by @ppalaga in https://github.com/apache/camel-quarkus/pull/839
* Added Johnzon extension #775 by @aldettinger in https://github.com/apache/camel-quarkus/pull/840
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/829
* Add JMS extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/841
* Fix since versions on various places by @ppalaga in https://github.com/apache/camel-quarkus/pull/845
* Fix #785 JacksonXML support by @ppalaga in https://github.com/apache/camel-quarkus/pull/848
* Support for camel-xml-io by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/849
* Upgrade to Quarkus 1.3.0.CR1 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/851
* add camel-jaxb into extensions by @mmelko in https://github.com/apache/camel-quarkus/pull/850
* Post Quarkus 1.3.0.CR1 upgrade tidy ups by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/855
* Bump to Quarkus 1.0.0-M4 by @oscerd in https://github.com/apache/camel-quarkus/pull/856
* Fix #787 JSon XStream dataformat support by @ppalaga in https://github.com/apache/camel-quarkus/pull/857
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/859
* Add ActiveMQ extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/860
* Optimize PR build GitHub action by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/861
* Upgrade Quarkus to 1.3.0.CR2 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/862
* Fix #795 File Watch support by @ppalaga in https://github.com/apache/camel-quarkus/pull/863
* Add GraphQL extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/864
* Fix #865 Re-org the source tree by @ppalaga in https://github.com/apache/camel-quarkus/pull/869
* chore(build): update maven to v3.6.3 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/871
* chore(doc): fix contributor guide example by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/873
* Fixed kafka itests as @Inject is not supported in native tests by @aldettinger in https://github.com/apache/camel-quarkus/pull/876
* Ref component support by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/874
* Remove default or redundant config of the failsafe plugin by @ppalaga in https://github.com/apache/camel-quarkus/pull/882
* FastCamelContext to implement ModelCamelContext by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/881
* Upgrade Quarkus to 1.3.0.Final by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/884

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.0.0-M4...1.0.0-M5

## 1.0.0-M4

* Add integration test for Jackson unmarshalling with different POJOs by @philschaller in https://github.com/apache/camel-quarkus/pull/663
* Upgrade TestContainers to version 1.12.5 by @oscerd in https://github.com/apache/camel-quarkus/pull/662
* Bump To Quarkus 1.0.0-M3 by @oscerd in https://github.com/apache/camel-quarkus/pull/664
* Fix the <firstVersion> of extensions that were released for the first time by @ppalaga in https://github.com/apache/camel-quarkus/pull/665
* Cleanup by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/667
* fix(rest-json/pom.xml): use quarkus-based dependency by @hanzo2001 in https://github.com/apache/camel-quarkus/pull/674
* Add GitHub action for automatic testing of camel-master branch by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/677
* camel-fhir record FHIR context instead of creating it at runtime. Onl by @johnpoth in https://github.com/apache/camel-quarkus/pull/678
* Create enforce profile that disables dependency checks by @johnpoth in https://github.com/apache/camel-quarkus/pull/679
* Add stream extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/682
* Upgrade to quarkus 1.3.0.Alpha1 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/685
* Snapshots and Actions by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/687
* Automatic sync branch master to camel-master by @github-actions in https://github.com/apache/camel-quarkus/pull/689
* Add olingo4 extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/693
* Add olingo4 itest to project list by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/702
* Remove enableJni from integration tests as JNI si always enabled on GraalVM 19.3.1 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/701
* Use camel-quarkus-support-httpclient in slack extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/703
* Cleanup: Declare quarkus-development-mode-spi as a nonExtensionArtifact by @ppalaga in https://github.com/apache/camel-quarkus/pull/691
* Fix #696 Test HTTPS with the HTTP clients by @ppalaga in https://github.com/apache/camel-quarkus/pull/697
* #670 fix PDF itests in native mode by @ffang in https://github.com/apache/camel-quarkus/pull/707
* Ensure that catalog files are added to the native image by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/704
* Update mvnd.builder.rules by @ppalaga in https://github.com/apache/camel-quarkus/pull/705
* #688 #694 Minor create-extension mojo fixes by @ppalaga in https://github.com/apache/camel-quarkus/pull/709
* create camel-jira extension #710 by @ffang in https://github.com/apache/camel-quarkus/pull/712
* camel-jira extension:more polish up by @ffang in https://github.com/apache/camel-quarkus/pull/714
* Schedule sync of quarkus-master branch by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/718
* Fix #716 Move the Atlassian Maven repository to the Jira runtime module by @ppalaga in https://github.com/apache/camel-quarkus/pull/717
* Fix #713 Revisit the Quarkus native image mojo options generated by create-extension by @ppalaga in https://github.com/apache/camel-quarkus/pull/719
* Fix #598 Use quarkus.package.type=native instead of the native-image mojo by @ppalaga in https://github.com/apache/camel-quarkus/pull/720
* Update quarkus to v1.3.0.Alpha2 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/727
* Improve Jira extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/728
* Resolves #162 adds camel-box support by @johnpoth in https://github.com/apache/camel-quarkus/pull/722
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/724
* Update kotlin to v1.3.61 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/730
* Cleanup tests by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/731
* Removed useless Jenkinsfile(s) by @oscerd in https://github.com/apache/camel-quarkus/pull/732
* Speed up build for misc extensions   by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/733
* CAMEL-QUARKUS-729: Centralized JSON dataformats related itests in dataformats-json by @aldettinger in https://github.com/apache/camel-quarkus/pull/734
* build: Do resource hungry native builds in parallel by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/735
* Fix XSLT extension NoSuchMethodError: TransformerFactory.newInstance(String,ClassLoader) by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/736
* Created a camel gson extension #681 by @aldettinger in https://github.com/apache/camel-quarkus/pull/738
* chore(test): add test for bean(class, method) by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/737
* Add LoginToken to Salesforce reflective class list by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/742
* Automatic sync branch master to camel-master by @github-actions in https://github.com/apache/camel-quarkus/pull/744
* Minor cleanup and a mvnd.builder.rule update by @ppalaga in https://github.com/apache/camel-quarkus/pull/749
* FhirDataformatTest failure -TransformerFactoryImpl not found by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/750
* Remove redundant quarkus.ssl.native=true from box application.properties by @ppalaga in https://github.com/apache/camel-quarkus/pull/751
* Remove redundant mvnd.builder.rules from the top level pom.xml by @ppalaga in https://github.com/apache/camel-quarkus/pull/755
* Fix #960 Do not expose mutable collections from FHIR BuildItems by @ppalaga in https://github.com/apache/camel-quarkus/pull/754
* Jira integration test fails in native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/758
* Upgrade to camel 3.1.0 by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/768
* Add telegram extension to CI build by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/773
* Add Azure extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/774
* Automatic sync branch master to quarkus-master by @github-actions in https://github.com/apache/camel-quarkus/pull/745
* Ensure that the GitHub Actions run each itest by @ppalaga in https://github.com/apache/camel-quarkus/pull/778
* Fix #498 Improve the XSLT test coverage by @ppalaga in https://github.com/apache/camel-quarkus/pull/777
* Add xpath language extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/779
* Add Braintree extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/826
* #791 #783 #788 Compression dataformats by @ppalaga in https://github.com/apache/camel-quarkus/pull/827
* Fix #831 Move Groovy executions under the enforce profile by @ppalaga in https://github.com/apache/camel-quarkus/pull/832
* Add websocket-jsr356 extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/833
* Remove MongoDB dependency overrides by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/834
* Automatic sync branch master to camel-master by @github-actions in https://github.com/apache/camel-quarkus/pull/830

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.0.0-M3...1.0.0-M4

## 1.0.0-M3

* Add camel-endpointdsl extension by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/552
* Modular RoutesCollector by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/554
* A JUnit dependency is required to run our integration tests outside of by @ppalaga in https://github.com/apache/camel-quarkus/pull/557
* Randomize http test port by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/555
* Restored the native profile for camel-quarkus-pdf integration tests by @aldettinger in https://github.com/apache/camel-quarkus/pull/556
* Update docs to version 1.0.0-M2 by @oscerd in https://github.com/apache/camel-quarkus/pull/558
* Fix first version of Camel-SQL extension by @oscerd in https://github.com/apache/camel-quarkus/pull/559
* AWS-EC2 Extension by @oscerd in https://github.com/apache/camel-quarkus/pull/565
* Fix #568 Untrack Artemis test instance data by @ppalaga in https://github.com/apache/camel-quarkus/pull/569
* Remove mp-config workaround by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/571
* Create an AWS commons extension by @oscerd in https://github.com/apache/camel-quarkus/pull/567
* Fix #560 AHC extension by @ppalaga in https://github.com/apache/camel-quarkus/pull/570
* AWS support: Leverage Quarkus-jackson instead of using Jackson stuff in each extension by @oscerd in https://github.com/apache/camel-quarkus/pull/573
* Don't search license in csv file by @oscerd in https://github.com/apache/camel-quarkus/pull/576
* CSV File Splitter To Log by @Namphibian in https://github.com/apache/camel-quarkus/pull/575
* Cleanup by @oscerd in https://github.com/apache/camel-quarkus/pull/577
* Add basic tests for dev mode by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/583
* Throw NoSuchLanguageException if a language cannot be found by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/582
* Dozer extension should use JaxbFileRootBuildItem by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/580
* Add tests for #543 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/581
* Added AWS-Lambda extension by @oscerd in https://github.com/apache/camel-quarkus/pull/584
* chore: include test xml routes in native image by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/585
* Camel-AWS extensions: Cleanup by @oscerd in https://github.com/apache/camel-quarkus/pull/586
* Move MicroProfile metrics camel context configuration to static init by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/587
* Some cleanup and improvements by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/588
* Improve dev mode test and *BeanBuldItem by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/589
* Format method param JavaDoc in columns by @ppalaga in https://github.com/apache/camel-quarkus/pull/590
* Remove @Nullable leftovers by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/591
* build: add Java 12 to PR build action by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/592
* chore: remove jetty-maven-plugin.version as jetty-maven-plugin is not more used by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/594
* build: formatter-maven-plugin should validate code format when check-format profile is active by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/593
* Create a camel-http extension by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/597
* Consolidate http based component integration tests by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/602
* ahc dependencies by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/604
* Bump Quarkus to version 1.1.1.Final by @oscerd in https://github.com/apache/camel-quarkus/pull/607
* Created a camel-jsonpath extension #426 by @aldettinger in https://github.com/apache/camel-quarkus/pull/609
* Add spring common extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/608
* Added an AWS Translate extension by @oscerd in https://github.com/apache/camel-quarkus/pull/614
* Initial camel-consul support by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/615
* Fix #620 Introduce CamelServiceInfo transformers by @ppalaga in https://github.com/apache/camel-quarkus/pull/621
* chore: fix sjm2 extension metadata by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/629
* Fix #622 Encourage users to ask for missing extensions and to help implementing them by @ppalaga in https://github.com/apache/camel-quarkus/pull/633
* Add impsort-maven-plugin by @ppalaga in https://github.com/apache/camel-quarkus/pull/632
* Update to Apache Camel 3.0.1 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/630
* Removed the Apache repositories since now the situation should be ok in central by @oscerd in https://github.com/apache/camel-quarkus/pull/639
* Fix #617 Registerable and discoverable Camel services by @ppalaga in https://github.com/apache/camel-quarkus/pull/618
* Fix #640 Lower the level of "Could not find a non-optional class for key message by @ppalaga in https://github.com/apache/camel-quarkus/pull/641
* Remove PlatformHttpSpanDecorator from OpenTracing extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/643
* WIP - Prepare for Quarkus 1.2 by @gsmet in https://github.com/apache/camel-quarkus/pull/603
* Removed the now useless alias for DefaultAnnotationExpressionFactory by @aldettinger in https://github.com/apache/camel-quarkus/pull/645
* Added an AWS Kinesis extension by @oscerd in https://github.com/apache/camel-quarkus/pull/626
* Run native tests with GitHub Actions by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/634
* chore: move dependencies exlusions to runtime bom by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/646
* Add tests and improvements for FastFactoryFinder by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/648
* fhir: reduce uri options by leveraging component configuration by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/655
* Add ahc-ws extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/625
* Determine the Camel version at build time by @ppalaga in https://github.com/apache/camel-quarkus/pull/636
* Fix #599 Document the need to set quarkus.native.add-all-charsets = true in HTTP extensions by @ppalaga in https://github.com/apache/camel-quarkus/pull/644
* Context and FactoryFinder improvements by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/656
* factory-finder: move reactive-executor filter to core processor as the related service is always programmatically configured by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/657
* Update Quarkus to v1.2.0.Final by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/658
* The MicroProfile test fails if message history is turned off by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/660
* Create a Camel ReactiveStreams extension (initial implementation) by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/659
* Fix #635 Dependency parity check by @ppalaga in https://github.com/apache/camel-quarkus/pull/638
* Enable local native sftp tests by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/547
* Fix #518 Rely on configurers for Configuration classes instead of using by @ppalaga in https://github.com/apache/camel-quarkus/pull/647
* Set firstVersion to 1.0.0 to match the reality by @ppalaga in https://github.com/apache/camel-quarkus/pull/661

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.0.0-M2...1.0.0-M3

## 1.0.0-M2

* Automatically register dozer mapping classes for reflection by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/507
* chore: Fix pom sorting script execution by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/510
* Do not use hard coded ports in integration-tests by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/509
* ftp: implement ftp/sft server test support with QuarkusTestResourceLifecycleManager by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/512
* introduce testcontainers by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/517
* Create AWS-IAM Extension by @oscerd in https://github.com/apache/camel-quarkus/pull/515
* chore: cleanup poms by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/519
* Fix #520 XSLT documentation is misleading by @ppalaga in https://github.com/apache/camel-quarkus/pull/523
* Do not check licenses in integration-tests/sjms/data by @ppalaga in https://github.com/apache/camel-quarkus/pull/522
* Add a workaround for Camel's DI (see CAMEL-14271) by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/525
* 1.0.0-M1 by @oscerd in https://github.com/apache/camel-quarkus/pull/527
* Re-introduce RoutesBuilderBuildItem by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/528
* Replace <firstVersion> 0.5.0 with 1.0.0-M1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/529
* refactor FHIR integration tests by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/524
* Add SQL component extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/533
* chore: Remove redundant banned dependency by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/530
* chore: Clarify supported URI schemes in SQL script-files config property by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/535
* examples: add timer-log kotlin example by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/542
* Fix commons-logging setup by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/545
* Ensure RoutesBuilder instances created by a CDI Producder are  not removed by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/546
* Ensure custom services such as deataformats, languages and component are not removed from container by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/548
* Add initial support for kotlin by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/549
* Quarkus 1.1 upgrade by @gsmet in https://github.com/apache/camel-quarkus/pull/550

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/1.0.0-M1...1.0.0-M2

## 1.0.0-M1

* Fixes #411 bean-validator extension by @davsclaus in https://github.com/apache/camel-quarkus/pull/412
* Initial JMS support by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/430
* Improve bean validator extension by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/431
* Consolidate microprofile integration tests by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/434
* Create a Camel Kafka extension by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/432
* Update latest version released to 0.4.0 by @oscerd in https://github.com/apache/camel-quarkus/pull/436
* xslt extension not working on java 11 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/437
* Fix #428 XSLT extension does not work with file: URIs by @ppalaga in https://github.com/apache/camel-quarkus/pull/440
* Add Dozer component extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/442
* Add Camel-AWS-KMS extension by @oscerd in https://github.com/apache/camel-quarkus/pull/443
* Fixed FhirProcessor imports by @oscerd in https://github.com/apache/camel-quarkus/pull/448
* Add Camel-AWS-ECS extension by @oscerd in https://github.com/apache/camel-quarkus/pull/447
* Remove workaround for https://github.com/quarkusio/quarkus/issues/4564 as it seems to be fixed by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/445
* Add Hystirx component extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/449
* Update to Quarkus 1.0.0-CR2 and regen by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/450
* #416, #415 by @ppalaga in https://github.com/apache/camel-quarkus/pull/452
* Fix #455 Support only classpath: XSLT URIs by @ppalaga in https://github.com/apache/camel-quarkus/pull/456
* Add support extension for commons-logging by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/458
* Add spring-di example by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/462
* Add FTP extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/464
* Startup a FHIR server when running the FHIR integration tests by @johnpoth in https://github.com/apache/camel-quarkus/pull/461
* Upgrade to camel 3.0.0 and quarkus 1.0.0.Final by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/471
* Fix #453 Move the platform-http component to Camel by @ppalaga in https://github.com/apache/camel-quarkus/pull/459
* Support dependency injection of FluentProducerTemplate/FluentConsumerTemplate by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/475
* Fixes native test and add support for FHIR R5 specification by @johnpoth in https://github.com/apache/camel-quarkus/pull/478
* Add MongoDB extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/480
* Simplify the Tooling hierarchy by @ppalaga in https://github.com/apache/camel-quarkus/pull/479
* Fix #476 List itests in an XML file for the Quarkus platform by @ppalaga in https://github.com/apache/camel-quarkus/pull/482
* Add camel-sjms2 extension by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/485
* Update to quarkus-1.0.1.Final by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/487
* Add workaroud for MP Config profile awarness by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/481
* Fix #484 The validator test should use a classpath URI for the XSD by @ppalaga in https://github.com/apache/camel-quarkus/pull/486
* Fix #483 Flatten the integration-tests hierarchy and remove camel-quarkus-test-list.xml from git by @ppalaga in https://github.com/apache/camel-quarkus/pull/489
* Document and test referring to beans by name by @ppalaga in https://github.com/apache/camel-quarkus/pull/491
* Scripts to sort pom.xml files by @ppalaga in https://github.com/apache/camel-quarkus/pull/492
* Fix #494 Group catalog re-generation and formatting mojos under a single profile by @ppalaga in https://github.com/apache/camel-quarkus/pull/495
* mp-config: enable testing profiles in native mode by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/496
* fix dependencies management in package-maven-plugin by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/497
* Issue 490: Add tagsoup support by @jsight in https://github.com/apache/camel-quarkus/pull/493
* feat: seda extension. by @davsclaus in https://github.com/apache/camel-quarkus/pull/502
* Filter out beans from CDI from build time discovery by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/501
* Improve registration of DozerTypeConverter by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/503
* Improve bean discovery filtering by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/504
* feat: base64 extension by @davsclaus in https://github.com/apache/camel-quarkus/pull/506

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/0.4.0...1.0.0-M1

## 0.4.0

* Use uppercase FHIR where appropriate by @ppalaga in https://github.com/apache/camel-quarkus/pull/359
* Fix #354 Update contributor guide: s/json/yaml/, extension adoc page by @ppalaga in https://github.com/apache/camel-quarkus/pull/355
* Camel quarkus netty by @dhartford in https://github.com/apache/camel-quarkus/pull/353
* chore: fix cs by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/362
* Make -Pnative equivalent with -Dnative, prefer -Pnative in the docs by @ppalaga in https://github.com/apache/camel-quarkus/pull/363
* Fix #357 Make netty-http dependent on netty and remove the duplications by @ppalaga in https://github.com/apache/camel-quarkus/pull/365
* Set camel-quarkus-last-release: 0.3.1 in site.yml by @ppalaga in https://github.com/apache/camel-quarkus/pull/366
* Add description to extension matedata by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/367
* Use capabilities instead of feature names by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/369
* Upgrade to quarkus 0.28.0 by @gnodet in https://github.com/apache/camel-quarkus/pull/373
* Reuse camel package maven plugin to avoid duplicating code, fixes #336 by @gnodet in https://github.com/apache/camel-quarkus/pull/375
* chore(deps): remove jetty-client from suport/common by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/374
* Use local legal files, fixes #376 by @gnodet in https://github.com/apache/camel-quarkus/pull/377
* Issue 337 by @gnodet in https://github.com/apache/camel-quarkus/pull/371
* Update to quarkus 0.28.1 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/378
* Improve the Github workflows: use -ntp, check licenses, better step and WF names by @ppalaga in https://github.com/apache/camel-quarkus/pull/370
* chore: remove checkstyle leftovers by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/380
* Update to Quarkus 1.0.0.CR1 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/381
* Recommend mvn license:format -Plicense in the docs by @ppalaga in https://github.com/apache/camel-quarkus/pull/387
* feat: controlbus extension. by @davsclaus in https://github.com/apache/camel-quarkus/pull/388
* Add SnakeYAML extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/390
* Perform TypeConverterLoader discovery using jandex instead of camel's class path scanner by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/392
* Fix license headers by @ppalaga in https://github.com/apache/camel-quarkus/pull/394
* Cleanup Netty extension by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/395
* Cleanup and CamelConfig improvements by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/397
* Add file component extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/399
* chore: Fix deprecation warning on netty tests and simplify it a bit by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/401
* Publish discovered RoutesBuilders via CamelBeanBuildItem by @ppalaga in https://github.com/apache/camel-quarkus/pull/358
* added Camel validation extension by @ploef in https://github.com/apache/camel-quarkus/pull/393
* Fix #136 and #396 by @ppalaga in https://github.com/apache/camel-quarkus/pull/402
* Scheduler by @davsclaus in https://github.com/apache/camel-quarkus/pull/404
* Xslt by @davsclaus in https://github.com/apache/camel-quarkus/pull/407
* Dataformat by @davsclaus in https://github.com/apache/camel-quarkus/pull/409
* core: avoid referencing application classes in bean build items and loading them in core build processors by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/405
* chore: add timer-log-cdi example by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/414
* Fix #382 Support path parameters in platform-http by @ppalaga in https://github.com/apache/camel-quarkus/pull/418
* chore: Remove unused itest dependencies by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/421
* Add exec component extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/420
* build(actions): enable build in jvm mode for all the supported java versions by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/423
* xslt extension not building in native mode by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/422
* Fix #356 Issues in the List of extensions by @ppalaga in https://github.com/apache/camel-quarkus/pull/425

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/0.3.1...0.4.0

## 0.3.1

* Fix #326 platform-http should return 415 for an unaccepted content type by @ppalaga in https://github.com/apache/camel-quarkus/pull/335
* Improve camel service discovery and filtering by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/340
* main: impove events and build phase by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/339
* Improve the contributor guide, esp. the create-extension examples by @ppalaga in https://github.com/apache/camel-quarkus/pull/343
* Set camel-quarkus-last-release: 0.3.0 in site.yml by @ppalaga in https://github.com/apache/camel-quarkus/pull/345
* Created a camel-pdf component extension fixes #341 by @aldettinger in https://github.com/apache/camel-quarkus/pull/342
* Improve service filter and related methods by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/351
* Fix #270 TarfileTest can fail on exotic platforms by @ppalaga in https://github.com/apache/camel-quarkus/pull/349
* Fix #220 platform-http component should return 204 for success and no body by @ppalaga in https://github.com/apache/camel-quarkus/pull/348
* Upgrade to quarkus 0.27.0 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/350

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/0.3.0...0.3.1

## 0.3.0

* Auto configuration of metrics management strategies by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/205
* Upgrade the docs to camel-quarkus-last-release: 0.2.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/210
* Upgarde to quarkus v0.23.1 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/211
* cleanup aws and paho extensions by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/212
* Submodule for support extensions by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/214
* chore(build): move depdendencies enforcer script to build/scripts by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/215
* Remove scaffold-integration-test.groovy by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/216
* chore: fix mail cs by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/219
* chore(deps): upgrade quarkus to v0.23.2 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/221
* Fix #184 Leverage platform http service by @ppalaga in https://github.com/apache/camel-quarkus/pull/201
* chore: rename CamelRegistryBuildItem to a more meaningful CamelBeanBuildItem by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/222
* Rename substitutions class names by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/225
* Use MainSupport as base for running Camel by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/226
* Cleanup by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/228
* Fix #133 Test netty4-http as a producer by @ppalaga in https://github.com/apache/camel-quarkus/pull/134
* Do not use deprecated methods by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/229
* Ensure the PlatformHttpComponent is registered before the routes are started by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/230
* Add support for quarkus provided event loop by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/231
* Improve the beans and registry BuildItems' JavaDoc by @ppalaga in https://github.com/apache/camel-quarkus/pull/237
* #234 Added log when starting or stoping ActiveMQ Broker failed by @WillemJiang in https://github.com/apache/camel-quarkus/pull/235
* Add workarounds for https://github.com/quarkusio/quarkus/issues/4407 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/236
* port rest-json quickstart to camel-quarkus by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/238
* Added License header on the html file by @WillemJiang in https://github.com/apache/camel-quarkus/pull/239
* Auto configure MicroProfile metrics Camel context event notifier by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/241
* quarkus-camel-catalog by @davsclaus in https://github.com/apache/camel-quarkus/pull/242
* Generate extension list readme file via tooling like we do at Apache Camel by @davsclaus in https://github.com/apache/camel-quarkus/pull/243
* chore: remove workaround for runtime registry as issue has been fixed upstream by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/244
* chore: add some doc about camel-main build steps by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/246
* #245 update the extensions list in the website docs also by @davsclaus in https://github.com/apache/camel-quarkus/pull/250
* camel application property routesUri is not overridden at runtime by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/252
* Remove the script for generating synthetic test-jars by @ppalaga in https://github.com/apache/camel-quarkus/pull/255
* Fix #259 The User guide should refer to the rest-json example by @ppalaga in https://github.com/apache/camel-quarkus/pull/261
* Fix #251 Unrecognized configuration key by @ppalaga in https://github.com/apache/camel-quarkus/pull/254
* Fix #217 Support rest dsl in platform-http component by @ppalaga in https://github.com/apache/camel-quarkus/pull/256
* Fixes #265 - Add tarfile extension by @davsclaus in https://github.com/apache/camel-quarkus/pull/266
* Polish2 by @davsclaus in https://github.com/apache/camel-quarkus/pull/269
* Add MicroProfile Health extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/271
* fix: CamelContext.getVersion() always returns an empty string by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/272
* fix: Paho integration tests fail in native mode by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/274
* fix(build): Handle loading routes from XML with latest camel SNAPSHOT by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/277
* Make xml and jaxb disabled by default and opt-in when depending on camel #188 by @gnodet in https://github.com/apache/camel-quarkus/pull/249
* fix: Add OpenTracing extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/279
* Update to quarkus 0.25.0 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/280
* Add minimal extension metadata by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/282
* Document health, metrics & OpenTracing extensions by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/283
* master failing by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/291
* Activate bomEntryVersion now that we are on Quarkus 0.25 by @ppalaga in https://github.com/apache/camel-quarkus/pull/287
* Fix version in integration-test-pom.xml by @johnpoth in https://github.com/apache/camel-quarkus/pull/296
* Followup #278 Assert that quarkus-extension.json exists for each extension by @ppalaga in https://github.com/apache/camel-quarkus/pull/284
* Enable PlatformHttpTest.invalidMethod() by @ppalaga in https://github.com/apache/camel-quarkus/pull/295
* Improve XML support by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/298
* Fix #285 Un-negate the config options names by @ppalaga in https://github.com/apache/camel-quarkus/pull/300
* chore: add tests for quarkusio/quarkus#4408 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/301
* chore: rename SubstrateProcessor by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/307
* Timer example by @davsclaus in https://github.com/apache/camel-quarkus/pull/312
* Fixup #262 Use bomEntryVersion of CreateExtensionMojo by @ppalaga in https://github.com/apache/camel-quarkus/pull/313
* Add observability example by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/311
* Upgrade Camel & Quarkus to latest releases by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/317
* Fix #308 Support multipart/form-data in platform-http extension by @ppalaga in https://github.com/apache/camel-quarkus/pull/321
* Upgarde quarkus to v0.26.1 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/325
* chore(build): add doclint-java8-disable profile borrowed from Apache Camel by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/329
* Improve dev mode by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/327
* chore(build): set groovy-maven-plugin version by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/328
* Fix #322 Make camel-attachments an optional dependency of platform-http by @ppalaga in https://github.com/apache/camel-quarkus/pull/323
* Add FHIR extension by @johnpoth in https://github.com/apache/camel-quarkus/pull/286
* Add Slack extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/331
* Add jackson and vm extensions, fixes #306 and #318 by @gnodet in https://github.com/apache/camel-quarkus/pull/320
* Do not use docker to run native tests, fixes #332 by @gnodet in https://github.com/apache/camel-quarkus/pull/333
* Polish examples and use -P native for all of them for native build. by @davsclaus in https://github.com/apache/camel-quarkus/pull/334

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/0.2.0...0.3.0

## 0.2.0

* rename xml extension to xml-common by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/117
* Override DefaultStreamCachingStrategy::resolveSpoolDirectory to avoid NPE by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/120
* chore: replace custom properties binding implementation with PropertyBindingSupport from camel support by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/119
* Fix #112 Make camel-quarkus-bom usable as a parent for user applications by @ppalaga in https://github.com/apache/camel-quarkus/pull/115
* create extension for camel's core cloud impl by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/121
* User guide by @ppalaga in https://github.com/apache/camel-quarkus/pull/122
* Fix #113 Document the release process by @ppalaga in https://github.com/apache/camel-quarkus/pull/125
* Fix #114 Move create-extension-templates to i.e. tooling by @ppalaga in https://github.com/apache/camel-quarkus/pull/126
* Order the dependencies managed in the BOM by @ppalaga in https://github.com/apache/camel-quarkus/pull/127
* Fix #129 Properies evaluation broken after the introduction of PropertyBindingSupport by @ppalaga in https://github.com/apache/camel-quarkus/pull/130
* Upgrade to Quarkus 0.21.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/135
* Upgrade to Quarkus 0.21.1 by @ppalaga in https://github.com/apache/camel-quarkus/pull/137
* chore(extension): add extension for camel-http-common by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/144
* chore(test): add test to validate camel registry hooks in ArC by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/146
* Basic impl for #147 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/148
* Add tests to the twitter itest project by @ppalaga in https://github.com/apache/camel-quarkus/pull/138
* Fix #124 Deployment BOM by @ppalaga in https://github.com/apache/camel-quarkus/pull/145
* Fix #41 Re-introduce test scope in integration test projects by @ppalaga in https://github.com/apache/camel-quarkus/pull/150
* Modularize recorder and processors by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/152
* Set disableReports=true in quarkus-maven-plugin config to speed up the itests execution by @ppalaga in https://github.com/apache/camel-quarkus/pull/151
* Adapt the Contributor guide to the new layout of POMs and new native build arguments by @ppalaga in https://github.com/apache/camel-quarkus/pull/153
* Update the link to the new site of Camel by @oscerd in https://github.com/apache/camel-quarkus/pull/155
* Remove unecessary conditional discovery by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/154
* Fix #58 Enforce dependency rules by @ppalaga in https://github.com/apache/camel-quarkus/pull/156
* Fixup #124 Deployments POM by @ppalaga in https://github.com/apache/camel-quarkus/pull/157
* Fix #70 Bean extension does not fail in native mode due to dynamic proxies anymore by @ppalaga in https://github.com/apache/camel-quarkus/pull/158
* Fix README link by @fviolette in https://github.com/apache/camel-quarkus/pull/161
* Groovy script to scaffold an integration test when creating a new by @ppalaga in https://github.com/apache/camel-quarkus/pull/160
* camel-zipfile quarkus extension by @davsclaus in https://github.com/apache/camel-quarkus/pull/165
* Fixes #123 adding a deploy profile by @oscerd in https://github.com/apache/camel-quarkus/pull/166
* CSV Extension by @ppalaga in https://github.com/apache/camel-quarkus/pull/167
* Add website build trigger by @zregvart in https://github.com/apache/camel-quarkus/pull/169
* Upgrade to Apache Camel RC1 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/170
* Expose metrics in quarkus by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/174
* Fix checkstyle violations by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/175
* Upgrade to Maven 3.6.2 by @ppalaga in https://github.com/apache/camel-quarkus/pull/176
* Make code generators checkstyle compliant by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/177
* Issue 69 by @gnodet in https://github.com/apache/camel-quarkus/pull/187
* Fix #179 Generate a jar with a non-test pom for each integration test by @ppalaga in https://github.com/apache/camel-quarkus/pull/181
* chore(reflection): add CamelContext and StreamCachingStrategy to the list of reflective classes by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/193
* don't start camel context before the container is fully started by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/192
* chore(it): add noDeps to quarkus-maven-plugin to make hot-reload working in multi-module projects by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/194
* Support for camel-mail by @gnodet in https://github.com/apache/camel-quarkus/pull/164
* chore(deps): update quarkus to v0.21.2 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/196
* Add the camel-paho extension to support the MQTT by @zhfeng in https://github.com/apache/camel-quarkus/pull/197
* improve the paho integration test by @zhfeng in https://github.com/apache/camel-quarkus/pull/200
* Upgrade to Quarkus 0.22.0 by @ppalaga in https://github.com/apache/camel-quarkus/pull/202
* Set the missing deploy plugin properties for the reusable-test-jar by @ppalaga in https://github.com/apache/camel-quarkus/pull/204
* Improve the maven deployment of the synthetic test jars by @ppalaga in https://github.com/apache/camel-quarkus/pull/206
* Disable deployment of synthetic test poms for now see #207 by @ppalaga in https://github.com/apache/camel-quarkus/pull/208

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/0.1.0...0.2.0

## 0.1.0

* Fix typo s/AWs_REGION/AWS_REGION/ by @ppalaga in https://github.com/apache/camel-quarkus/pull/94
* Move test packages to org.apache.camel by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/77
* Omit the artifactId in release tags by @ppalaga in https://github.com/apache/camel-quarkus/pull/95
* Add twitter extension by @jamesnetherton in https://github.com/apache/camel-quarkus/pull/99
* Upgrade to Camel 3.0.0-M4 by @ppalaga in https://github.com/apache/camel-quarkus/pull/72
* chore(cleanup): remove ide-config by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/101
* Upgrade quarkus to v0.20.0  plsu some cleanup by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/100
* Add configuration for quarkus:create-extension mojo by @ppalaga in https://github.com/apache/camel-quarkus/pull/102
* Rename the rest of java packages in itests by @ppalaga in https://github.com/apache/camel-quarkus/pull/103
* Remove the TravisCI badge from the README by @ppalaga in https://github.com/apache/camel-quarkus/pull/104
* Fix #37 Setup the documentation by @ppalaga in https://github.com/apache/camel-quarkus/pull/105
* Use source block instead of code by @zregvart in https://github.com/apache/camel-quarkus/pull/106
* Figure out whether the Reifier substitutions can be added conditionally by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/110

**Full Changelog**: https://github.com/apache/camel-quarkus/compare/0.0.2...0.1.0

## 0.0.2

* Add Parent POM by @ppalaga in https://github.com/apache/camel-quarkus/pull/1
* Add Maven wrapper and .travis.yml by @ppalaga in https://github.com/apache/camel-quarkus/pull/2
* Migrate Camel extensions from Quarkus by @ppalaga in https://github.com/apache/camel-quarkus/pull/3
* Add keys by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/13
* Use vanilla Maven Compiler Plugin version (from maven central) by @davsclaus in https://github.com/apache/camel-quarkus/pull/21
* chore(build): use project.version by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/15
* Initial import cleanup by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/22
* Fix #11 Test the native mode on TravisCI by @ppalaga in https://github.com/apache/camel-quarkus/pull/12
* Add badges and How to build section to the README by @ppalaga in https://github.com/apache/camel-quarkus/pull/27
* Fix #25 Use the same naming scheme for artifactIds and module names by @ppalaga in https://github.com/apache/camel-quarkus/pull/26
* Avoid 3rd party maven repositories if possible by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/28
* Use docker-build in travis by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/30
* Fix #6 Rename packages from io.quarkus.camel to org.apache.camel.quarkus by @ppalaga in https://github.com/apache/camel-quarkus/pull/23
* Manage camel-servlet and camel-aws-sns in the BOM by @ppalaga in https://github.com/apache/camel-quarkus/pull/40
* Fix #10 Produce test-jars of the integration tests by @ppalaga in https://github.com/apache/camel-quarkus/pull/35
* chore: ignore ObjectStore folder by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/43
* Caffeine by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/46
* chore(build): add Jenkinsfile by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/49
* chore(arc): add ConsumerTemplate and ProducerTemplate producers by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/48
* Do not use io.quarkus...FeatureBuildItem feature constants by @ppalaga in https://github.com/apache/camel-quarkus/pull/50
* Removed Jenkinsfile not used by @oscerd in https://github.com/apache/camel-quarkus/pull/52
* chore(build): remove travis build by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/53
* Use RestAssured in JDBC test by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/47
* Fix #31 Move infinispan integration-tests out of core by @ppalaga in https://github.com/apache/camel-quarkus/pull/55
* Review extensions' dependencies chains by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/59
* chore(build): cleanup build set-up by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/61
* Upgrade quarkus to v0.19.0 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/64
* Upgrade quarkus to v0.19.1 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/66
* Prepare for Camel 3.0.0-M3/4 by @ppalaga in https://github.com/apache/camel-quarkus/pull/67
* Dedicated extension for jetty and xstream by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/78
* Move netty integration-tests out of core by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/79
* Fixes #18 Add a Camel AWS-EKS Extension by @oscerd in https://github.com/apache/camel-quarkus/pull/83
* chore(it): cleanup application.properties and poms by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/84
* Add a single integration test module for the AWS extensions by @oscerd in https://github.com/apache/camel-quarkus/pull/85
* Fix typo s/nett4/netty4/ by @ppalaga in https://github.com/apache/camel-quarkus/pull/86
* chore(test): add camel core cdi test by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/87
* Changed the integration test module name and folder name for AWS by @oscerd in https://github.com/apache/camel-quarkus/pull/88
* AWS Extension configuration classes need to be registered for reflection by @oscerd in https://github.com/apache/camel-quarkus/pull/89
* Remove unused imports from CamelRoute in netty4-http integration test by @ppalaga in https://github.com/apache/camel-quarkus/pull/91
* chore(it): cleanup it poms by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/90
* chore(build): move release profile to camel-quarkus-parent by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/92
* Added failIfNoTest option in surefire configuration by @oscerd in https://github.com/apache/camel-quarkus/pull/93

**Full Changelog**: https://github.com/apache/camel-quarkus/commits/0.0.2

## 0.0.1

* Add Parent POM by @ppalaga in https://github.com/apache/camel-quarkus/pull/1
* Add Maven wrapper and .travis.yml by @ppalaga in https://github.com/apache/camel-quarkus/pull/2
* Migrate Camel extensions from Quarkus by @ppalaga in https://github.com/apache/camel-quarkus/pull/3
* Add keys by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/13
* Use vanilla Maven Compiler Plugin version (from maven central) by @davsclaus in https://github.com/apache/camel-quarkus/pull/21
* chore(build): use project.version by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/15
* Initial import cleanup by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/22
* Fix #11 Test the native mode on TravisCI by @ppalaga in https://github.com/apache/camel-quarkus/pull/12
* Add badges and How to build section to the README by @ppalaga in https://github.com/apache/camel-quarkus/pull/27
* Fix #25 Use the same naming scheme for artifactIds and module names by @ppalaga in https://github.com/apache/camel-quarkus/pull/26
* Avoid 3rd party maven repositories if possible by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/28
* Use docker-build in travis by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/30
* Fix #6 Rename packages from io.quarkus.camel to org.apache.camel.quarkus by @ppalaga in https://github.com/apache/camel-quarkus/pull/23
* Manage camel-servlet and camel-aws-sns in the BOM by @ppalaga in https://github.com/apache/camel-quarkus/pull/40
* Fix #10 Produce test-jars of the integration tests by @ppalaga in https://github.com/apache/camel-quarkus/pull/35
* chore: ignore ObjectStore folder by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/43
* Caffeine by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/46
* chore(build): add Jenkinsfile by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/49
* chore(arc): add ConsumerTemplate and ProducerTemplate producers by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/48
* Do not use io.quarkus...FeatureBuildItem feature constants by @ppalaga in https://github.com/apache/camel-quarkus/pull/50
* Removed Jenkinsfile not used by @oscerd in https://github.com/apache/camel-quarkus/pull/52
* chore(build): remove travis build by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/53
* Use RestAssured in JDBC test by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/47
* Fix #31 Move infinispan integration-tests out of core by @ppalaga in https://github.com/apache/camel-quarkus/pull/55
* Review extensions' dependencies chains by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/59
* chore(build): cleanup build set-up by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/61
* Upgrade quarkus to v0.19.0 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/64
* Upgrade quarkus to v0.19.1 by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/66
* Prepare for Camel 3.0.0-M3/4 by @ppalaga in https://github.com/apache/camel-quarkus/pull/67
* Dedicated extension for jetty and xstream by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/78
* Move netty integration-tests out of core by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/79
* Fixes #18 Add a Camel AWS-EKS Extension by @oscerd in https://github.com/apache/camel-quarkus/pull/83
* chore(it): cleanup application.properties and poms by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/84
* Add a single integration test module for the AWS extensions by @oscerd in https://github.com/apache/camel-quarkus/pull/85
* Fix typo s/nett4/netty4/ by @ppalaga in https://github.com/apache/camel-quarkus/pull/86
* chore(test): add camel core cdi test by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/87
* Changed the integration test module name and folder name for AWS by @oscerd in https://github.com/apache/camel-quarkus/pull/88
* AWS Extension configuration classes need to be registered for reflection by @oscerd in https://github.com/apache/camel-quarkus/pull/89
* Remove unused imports from CamelRoute in netty4-http integration test by @ppalaga in https://github.com/apache/camel-quarkus/pull/91
* chore(it): cleanup it poms by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/90
* chore(build): move release profile to camel-quarkus-parent by @lburgazzoli in https://github.com/apache/camel-quarkus/pull/92
* Added failIfNoTest option in surefire configuration by @oscerd in https://github.com/apache/camel-quarkus/pull/93

**Full Changelog**: https://github.com/apache/camel-quarkus/commits/camel-quarkus-parent-0.0.1
