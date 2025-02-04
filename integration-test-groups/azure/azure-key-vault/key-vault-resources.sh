#!/bin/bash
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

#script to create/delete all resources required for key-vault refresh test.
#In comparison with ../azure-resources/sh, the script is not creating any permissions, resource groups, ...
#Following properties has to be se upon running the script
#export RESOURCE_GROUP=<existing-resource-group>
#export ZONE=<your-zone>
#export EH_NAMESPACE=<existing event hub namespace>
#export AZURE_STORAGE_ACCOUNT_NAME=<existing event hub storage account name>

if ! which az > /dev/null 2>&1; then
  echo "$(basename $0) requires the Azure CLI."
  echo
  echo "https://docs.microsoft.com/en-us/cli/azure/"
  echo
  exit 1
fi

suffix="$(az ad signed-in-user show --query displayName -o tsv | tr '[:upper:]' '[:lower:]' | tr -cd '[:alnum:]' | cut -c-12)"
suffix="${suffix}4"

export AZURE_VAULT_REFRESH_EH_NAME=camel-quarkus-secret-refresh-hub-${suffix}
export AZURE_BLOB_CONTAINER_NAME=cq-container-${suffix}

function createResources() {
    set -e
    set -x
    AZURE_EVENT_HUBS_CONNECTION_STRING=$(az eventhubs namespace authorization-rule keys list --resource-group ${RESOURCE_GROUP} --namespace-name ${EH_NAMESPACE} --name RootManageSharedAccessKey  --query primaryConnectionString -o tsv)

    az storage container create --account-name ${AZURE_STORAGE_ACCOUNT_NAME} --name ${AZURE_BLOB_CONTAINER_NAME} --auth-mode login

    AZURE_STORAGE_ACCOUNT_KEY=$(az storage account keys list --account-name ${AZURE_STORAGE_ACCOUNT_NAME} --query '[0].value' -o tsv)

    az eventhubs eventhub create --name ${AZURE_VAULT_REFRESH_EH_NAME} --resource-group ${RESOURCE_GROUP} --namespace-name ${EH_NAMESPACE}  --cleanup-policy Delete --partition-count 1  --retention-time 1

    set +x
    echo "Add the following to your environment:"
    echo 'export AZURE_VAULT_EVENT_HUBS_BLOB_CONTAINER_NAME="'${AZURE_BLOB_CONTAINER_NAME}'"'
    echo 'export AZURE_VAULT_EVENT_HUBS_CONNECTION_STRING="'$AZURE_EVENT_HUBS_CONNECTION_STRING';EntityPath='${AZURE_VAULT_REFRESH_EH_NAME}'"'
    echo 'export AZURE_STORAGE_ACCOUNT_KEY="'${AZURE_STORAGE_ACCOUNT_KEY}'"'
}


function deleteResources() {
    set -x
    set +e

    az storage container delete --account-name ${AZURE_STORAGE_ACCOUNT_NAME} --name ${AZURE_BLOB_CONTAINER_NAME} --auth-mode login

    az eventhubs eventhub delete --name ${AZURE_VAULT_REFRESH_EH_NAME} --resource-group ${RESOURCE_GROUP} --namespace-name ${EH_NAMESPACE}
}

case "$1" in
create)  echo "Creating Azure resources"
    createResources
    ;;
delete)  echo "Deleting Azure resources"
    deleteResources
    ;;
*) echo "usage: $0 [create|delete]"
   ;;
esac
