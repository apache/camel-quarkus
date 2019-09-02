/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

def AGENT_LABEL = env.AGENT_LABEL ?: 'ubuntu'
def JDK_NAME = env.JDK_NAME ?: 'JDK 1.8 (latest)'

def MAVEN_PARAMS = "-U -B -e -fae -V"

pipeline {

    agent {
        label AGENT_LABEL
    }

    tools {
        jdk JDK_NAME
    }

    environment {
        MAVEN_SKIP_RC = true
    }

    options {
        buildDiscarder(
            logRotator(artifactNumToKeepStr: '5', numToKeepStr: '10')
        )
        disableConcurrentBuilds()
    }

    stages {

        stage('Website update') {
            when {
                branch 'master'
                changeset 'docs/**/*'
            }

            steps {
                build job: 'Camel.website', wait: false
            }
        }

        stage('Build & Deploy') {
            when {
                branch 'master'
            }
            steps {
                sh "./mvnw $MAVEN_PARAMS clean install deploy -Pdeploy"
            }
        }

        stage('Test') {
            steps {
                sh "./mvnw $MAVEN_PARAMS -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn clean verify -Dnative -Dnative-image.docker-build=true -f pom.xml"
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                    junit allowEmptyResults: true, testResults: '**/target/failsafe-reports/*.xml'
                }
            }
        }

    }

    post {
        always {
            emailext(
                subject: '${DEFAULT_SUBJECT}',
                body: '${DEFAULT_CONTENT}',
                recipientProviders: [[$class: 'CulpritsRecipientProvider']]
            )
        }
    }
}

