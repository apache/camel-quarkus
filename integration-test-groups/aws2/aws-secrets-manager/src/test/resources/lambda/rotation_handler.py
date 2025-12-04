#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import json
import boto3
import logging
import os
from botocore.exceptions import ClientError

# --- Configuration ---
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Determine the LocalStack endpoint dynamically
ENDPOINT_URL = None

if os.environ.get('LOCALSTACK_HOSTNAME'):
    # Default LocalStack port is 4566
    ENDPOINT_URL = f"http://{os.environ.get('LOCALSTACK_HOSTNAME')}:{os.environ.get('EDGE_PORT', 4566)}"

# Initialize Secrets Manager client
secrets_manager = boto3.client(
    'secretsmanager',
    region_name=os.environ.get('AWS_REGION', 'us-east-1'), # Default LocalStack region is 'us-east-1'
    endpoint_url=ENDPOINT_URL
)

def lambda_handler(event, context):
    """
    Handles the four steps of the AWS Secrets Manager rotation cycle.
    """
    logger.info(f"Received rotation event: {event}")

    arn = event['SecretId']
    token = event['ClientRequestToken']
    step = event['Step']

    # 1. Get Secret Metadata (Used to verify current and pending versions)
    metadata = secrets_manager.describe_secret(SecretId=arn)
    
    # Check if the secret is currently being rotated with the given token
    if not token in metadata['VersionIdsToStages']:
        raise ValueError(f"Secret version {token} has no stage for rotation.")

    # Determine the current version ID
    current_version_id = None
    for version_id, stages in metadata['VersionIdsToStages'].items():
        if 'AWSCURRENT' in stages:
            if version_id == token:
                # The secret is already CURRENT, rotation completed previously
                logger.info(f"Secret {arn} is already CURRENT for token {token}. Skipping steps.")
                return {}
            current_version_id = version_id
            break

    # --- Execute Rotation Steps ---
    if step == 'createSecret':
        return create_secret(arn, token, current_version_id)
    
    elif step == 'setSecret':
        return set_secret(arn, token)
        
    elif step == 'testSecret':
        return test_secret(arn, token)

    elif step == 'finishSecret':
        return finish_secret(arn, token, current_version_id)

    else:
        raise ValueError(f"Invalid step parameter: {step}")

# --- ROTATION LOGIC IMPLEMENTATION ---

def create_secret(arn, token, current_version_id):
    """
    (Step 1) Creates a new version of the secret with the PENDING stage label.
    """
    logger.info("Executing createSecret step.")
    
    # 1. Get the current secret value
    current_value = secrets_manager.get_secret_value(SecretId=arn, VersionId=current_version_id)['SecretString']
    
    # 2. Rotation Logic: Simple numeric increment
    try:
        new_value = str(int(current_value) + 1)
    except ValueError:
        new_value = current_value + "_Rotated"

    # 3. Store the new secret value tagged with the PENDING token
    try:
        secrets_manager.put_secret_value(
            SecretId=arn,
            ClientRequestToken=token,
            SecretString=new_value,
            VersionStages=['PENDING']
        )
        logger.info(f"New PENDING secret created with value: {new_value}")
    except ClientError as e:
        if e.response['Error']['Code'] == 'ResourceExistsException':
             # PENDING version already exists, which is acceptable if re-running
             logger.warning("PENDING secret already exists, continuing.")
             pass
        else:
            raise e

    return {}

def set_secret(arn, token):
    """(Step 2) Updates the underlying service/database. (Skipped for simulation)"""
    logger.info("Executing setSecret step. (Skipped for simple LocalStack test)")
    return {}

def test_secret(arn, token):
    """(Step 3) Validates the new secret. (Skipped for simulation)"""
    logger.info("Executing testSecret step. (Skipped for simple LocalStack test)")
    return {}

def finish_secret(arn, token, current_version_id):
    """
    (Step 4) Moves the AWSCURRENT label from the old version to the PENDING version.
    Requires explicit removal of AWSCURRENT from the old ID for LocalStack/Moto.
    """
    logger.info("Executing finishSecret step.")

    # Critical check based on the previous error logs
    if not current_version_id:
        logger.error("Current version ID is required but missing. Cannot finish rotation.")
        # Raise exception to fail the rotation attempt
        raise ValueError("Missing current_version_id for finishSecret.")

    # Shift the AWSCURRENT label to the version identified by the token
    secrets_manager.update_secret_version_stage(
        SecretId=arn,
        VersionStage='AWSCURRENT',
        MoveToVersionId=token,

        # KEY FIX: Explicitly remove AWSCURRENT from the old version ID
        RemoveFromVersionId=current_version_id
    )
    logger.info(f"Rotation finished. AWSCURRENT moved from {current_version_id} to {token}.")
    return {}