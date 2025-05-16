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
package org.apache.camel.quarkus.component.saga.it.lra;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterForReflection
public class LraTicketService {

    private LraTicketServiceStatus train = LraTicketServiceStatus.nothing;

    private LraTicketServiceStatus flight = LraTicketServiceStatus.nothing;

    public LraTicketServiceStatus getTrain() {
        return train;
    }

    public void setTrainError() {
        this.train = LraTicketServiceStatus.error;
    }

    public void setFlightError() {
        this.flight = LraTicketServiceStatus.error;
    }

    public LraTicketServiceStatus getFlight() {
        return flight;
    }

    public void setTicketsRefunded() {
        //all tickets without error are refunded now
        if (this.flight == LraTicketServiceStatus.nothing) {
            this.flight = LraTicketServiceStatus.refunded;
        }
        if (this.train == LraTicketServiceStatus.nothing) {
            this.train = LraTicketServiceStatus.refunded;
        }
    }

    public void setTicketsReserved() {
        //all tickets without error are refunded now
        if (this.flight == LraTicketServiceStatus.nothing) {
            this.flight = LraTicketServiceStatus.reserved;
        }
        if (this.train == LraTicketServiceStatus.nothing) {
            this.train = LraTicketServiceStatus.reserved;
        }
    }

    public void reset() {
        this.train = LraTicketServiceStatus.nothing;
        this.flight = LraTicketServiceStatus.nothing;
    }
}
