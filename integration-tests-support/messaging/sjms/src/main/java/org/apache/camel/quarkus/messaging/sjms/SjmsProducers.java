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
package org.apache.camel.quarkus.messaging.sjms;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Named;
import org.apache.camel.Exchange;
import org.apache.camel.component.sjms.SjmsConstants;

public class SjmsProducers {

    @Named
    public DestinationHeaderSetter destinationHeaderSetter() {
        return new DestinationHeaderSetter();
    }

    @RegisterForReflection(fields = false)
    static final class DestinationHeaderSetter {

        public void setJmsDestinationHeader(Exchange exchange) {
            org.apache.camel.Message message = exchange.getMessage();
            String destinationName = message.getHeader("DestinationName", String.class);
            message.setHeader(SjmsConstants.JMS_DESTINATION_NAME, destinationName);
        }
    }
}
