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
import org.apache.camel.component.direct.DirectComponent

camel {
    components {
        direct {
            // set value as method
            timeout 1234
        }
        myDirect(DirectComponent) {
            // set value as method
            timeout = 4321
        }
    }
}



from('direct:routes-with-components-configuration')
    .id('routes-with-components-configuration')
    .setBody(simple('${camelContext.getComponent("myDirect")} != null', Boolean.class))
