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
def AGENT_LABEL = env.AGENT_LABEL ?: 'ubuntu'
def JDK_NAME = env.JDK_NAME ?: 'jdk_17_latest'
def MAVEN_PARAMS = '-B -e -ntp'
def VERSION_SUFFIX = "-${env.BRANCH_NAME.toUpperCase().replace('_','-')}-SNAPSHOT"

if (env.BRANCH_NAME == 'camel-main') {
    MAVEN_PARAMS += ' -Papache-snapshots'
}

if (env.BRANCH_NAME == 'quarkus-main') {
    MAVEN_PARAMS += ' -Poss-snapshots -Dquarkus.version=999-SNAPSHOT'
}

pipeline {

    agent {
        label AGENT_LABEL
    }

    tools {
        jdk JDK_NAME
    }

    options {
        buildDiscarder(
            logRotator(artifactNumToKeepStr: '5', numToKeepStr: '10')
        )
        disableConcurrentBuilds()
    }

    stages {
        stage('Set version') {
            when {
                expression { env.BRANCH_NAME ==~ /(.*-main)/ }
            }

            steps {
                script {
                    if (env.BRANCH_NAME == "quarkus-main") {
                        sh 'rm -rf /tmp/quarkus'
                        sh "git clone --depth 1 --branch main https://github.com/quarkusio/quarkus.git /tmp/quarkus"
                        sh "./mvnw ${MAVEN_PARAMS} -Dquickly clean install -f /tmp/quarkus/pom.xml"
                    }

                    def VERSION = sh script: "./mvnw ${MAVEN_PARAMS} help:evaluate -Dexpression=project.version -q -DforceStdout", returnStdout: true
                    def NEW_SNAPSHOT_VERSION = VERSION.replace('-SNAPSHOT', '') + VERSION_SUFFIX
                    sh "sed -i \"s/${VERSION}/${NEW_SNAPSHOT_VERSION}/g\" \$(find . -name pom.xml)"
                }
            }
        }

        stage('Deploy') {
            steps {
                sh "./mvnw ${MAVEN_PARAMS} -Dquickly clean deploy"
            }
        }
    }
}
