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


# alternative way of running the tests.
# script starts the conjur quickstart via docker compose and do all required configuration
# after running it, please export  all 6 variables and then you can start the test
# When asked `Select the environment you want to use:`
# please choose `Conjur Open Source`

echo "Temporary folder 'tmp' is used"
mkdir tmp
cd tmp

echo "************************************************"
echo "****** SETUP A CONJUR OSS ENVIRONMENT  *********"
echo "************************************************"

echo "Cloning conjur quickstart"
git clone https://github.com/cyberark/conjur-quickstart.git

cd conjur-quickstart
echo "Removing previous instances"
docker-compose rm -fsv

echo "Step 1: Pull the Docker image"
docker-compose pull

echo "Step 2: Generate the master key"
docker-compose run --no-deps --rm conjur data-key generate > data_key

echo "Step 3: Load master key as an environment variable"
export CONJUR_DATA_KEY="$(< data_key)"

echo "Step 4: Start the Conjur OSS environment"
docker-compose up -d

echo "Step 5: Create admin account"
docker-compose exec conjur conjurctl account create myConjurAccount > admin_data

echo "Step 6: Connect the Conjur client to the Conjur server"
docker-compose exec client conjur init -u https://proxy -a myConjurAccount --self-signed

echo "************************************************"
echo "****************** 2. DEFINE POLICY ************"
echo "************************************************"

echo "Step 1: Login in to Conjur as admin"
docker-compose exec client conjur login -i admin -p $(awk 'END {print $NF}' admin_data)

echo "Step 2: Load the sample policy"
docker-compose exec client conjur policy load -b root -f policy/BotApp.yml > my_app_data

echo "Step 3: Logout of Conjur"
docker-compose exec client conjur logout

echo
echo ------------ please export following properties ------------------------
echo export CQ_CONJUR_URL=http://localhost:8080/
echo export CQ_CONJUR_ACCOUNT=myConjurAccount
echo export CQ_CONJUR_READ_USER=host/BotApp/myDemoApp
echo export CQ_CONJUR_READ_USER_API_KEY=$(jq -r '.created_roles."myConjurAccount:host:BotApp/myDemoApp".api_key' my_app_data)
echo export CQ_CONJUR_READ_WRITE_USER=user/Dave@BotApp
echo export CQ_CONJUR_READ_WRITE_USER_API_KEY=$(jq -r '.created_roles."myConjurAccount:user:Dave@BotApp".api_key' my_app_data)
echo "# to avoid port conflict with quarkus (against opensource conjur)"
echo export QUARKUS_HTTP_PORT=0
echo export QUARKUS_HTTPS_PORT=0
