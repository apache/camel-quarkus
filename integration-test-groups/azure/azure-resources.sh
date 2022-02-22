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

if ! which az > /dev/null 2>&1; then
  echo "$(basename $0) requires the Azure CLI."
  echo
  echo "https://docs.microsoft.com/en-us/cli/azure/"
  echo
  exit 1
fi

suffix="$(az ad signed-in-user show --query displayName -o tsv | tr '[:upper:]' '[:lower:]' | tr -cd '[:alnum:]' | cut -c-12)"
suffix="${suffix}4"
export AZURE_STORAGE_ACCOUNT_NAME=cqacc${suffix}
export AZURE_BLOB_CONTAINER_NAME=cq-container-${suffix}

export RESOURCE_GROUP=cq-res-group-${suffix}
export ZONE=westeurope
export EH_NAMESPACE=cq-eh-namenspace-${suffix}
export EH_NAME=cq-event-hub-${suffix}

function createResources() {
    set -e
    set -x
    az group create --name ${RESOURCE_GROUP} --location ${ZONE}

    az storage account create --name ${AZURE_STORAGE_ACCOUNT_NAME} --resource-group ${RESOURCE_GROUP} --location ${ZONE} --sku Standard_LRS --kind StorageV2
    az storage account blob-service-properties update --enable-change-feed true --delete-retention-days 1 -n ${AZURE_STORAGE_ACCOUNT_NAME} -g ${RESOURCE_GROUP}

    SUBSCRIPTION_ID="$(az account list --query '[0].id' -o tsv)"
    USER_ID="$(az ad signed-in-user show --query objectId -o tsv)"
    az role assignment create --role "Storage Blob Data Contributor"  --assignee ${USER_ID} --scope "/subscriptions/${SUBSCRIPTION_ID}/resourceGroups/${RESOURCE_GROUP}/providers/Microsoft.Storage/storageAccounts/${AZURE_STORAGE_ACCOUNT_NAME}"

    sleep 30

    az storage container create --account-name ${AZURE_STORAGE_ACCOUNT_NAME} --name ${AZURE_BLOB_CONTAINER_NAME} --auth-mode login

    az eventhubs namespace create --name ${EH_NAMESPACE} --resource-group ${RESOURCE_GROUP} --location ${ZONE}
    az eventhubs eventhub create --name ${EH_NAME} --resource-group ${RESOURCE_GROUP} --namespace-name ${EH_NAMESPACE} --partition-count 1

    AZURE_EVENT_HUBS_CONNECTION_STRING=$(az eventhubs namespace authorization-rule keys list --resource-group ${RESOURCE_GROUP} --namespace-name ${EH_NAMESPACE} --name RootManageSharedAccessKey  --query primaryConnectionString -o tsv)

    AZURE_STORAGE_ACCOUNT_KEY=$(az storage account keys list --account-name ${AZURE_STORAGE_ACCOUNT_NAME} --query '[0].value' -o tsv)

    set +x
    echo "Add the following to your environment:"

    echo 'export AZURE_STORAGE_ACCOUNT_NAME="'${AZURE_STORAGE_ACCOUNT_NAME}'"'
    echo 'export AZURE_STORAGE_ACCOUNT_KEY="'${AZURE_STORAGE_ACCOUNT_KEY}'"'
    echo 'export AZURE_EVENT_HUBS_BLOB_CONTAINER_NAME="'${AZURE_BLOB_CONTAINER_NAME}'"'
    echo 'export AZURE_EVENT_HUBS_CONNECTION_STRING="'$AZURE_EVENT_HUBS_CONNECTION_STRING';EntityPath='${EH_NAME}'"'
}


function deleteResources() {
    set -x
    set +e
    az eventhubs eventhub delete --name ${EH_NAME} --resource-group ${RESOURCE_GROUP} --namespace-name ${EH_NAMESPACE}
    az eventhubs namespace delete --name ${EH_NAMESPACE} --resource-group ${RESOURCE_GROUP}
    az storage container delete --account-name ${AZURE_STORAGE_ACCOUNT_NAME} --name ${AZURE_BLOB_CONTAINER_NAME}
    az storage account delete --name ${AZURE_STORAGE_ACCOUNT_NAME} --resource-group ${RESOURCE_GROUP} --yes
    az group delete --name ${RESOURCE_GROUP} --yes
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
