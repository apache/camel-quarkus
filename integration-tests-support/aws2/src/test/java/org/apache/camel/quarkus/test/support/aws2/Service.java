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
package org.apache.camel.quarkus.test.support.aws2;

public enum Service {
    API_GATEWAY("apigateway"),
    EC2("ec2"),
    KINESIS("kinesis"),
    DYNAMODB("dynamodb"),
    DYNAMODB_STREAMS("dynamodbstreams"),
    S3("s3"),
    FIREHOSE("firehose"),
    LAMBDA("lambda"),
    SNS("sns"),
    SQS("sqs"),
    REDSHIFT("redshift"),
    SES("ses"),
    ROUTE53("route53"),
    CLOUDFORMATION("cloudformation"),
    CLOUDWATCH("cloudwatch"),
    SSM("ssm"),
    SECRETSMANAGER("secretsmanager"),
    STEPFUNCTIONS("stepfunctions"),
    CLOUDWATCHLOGS("logs"),
    STS("sts"),
    IAM("iam"),
    KMS("kms");

    private final String serviceName;

    Service(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getName() {
        return serviceName;
    }
}
