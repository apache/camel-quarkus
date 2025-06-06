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

import java.util.HashMap;
import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Header;

@ApplicationScoped
@RegisterForReflection
public class LraCreditService {

    private int totalCredit;

    private Map<String, Integer> reservations = new HashMap<>();

    public LraCreditService() {
        this.totalCredit = 100;
    }

    public synchronized void reserveCredit(@Header("Long-Running-Action") String id, @Header("amount") int amount) {
        int credit = getCredit();
        if (amount > credit) {
            throw new IllegalStateException("Insufficient credit");
        }
        if (reservations.containsKey(id)) {
            reservations.put(id, reservations.get(id) + amount);
        } else {
            reservations.put(id, amount);
        }
    }

    public synchronized void refundCredit(@Header("Long-Running-Action") String id) {
        reservations.remove(id);
    }

    public synchronized int getCredit() {
        return totalCredit - reservations.values().stream().reduce(0, (a, b) -> a + b);
    }

    public void setTotalCredit(int totalCredit) {
        this.totalCredit = totalCredit;
    }
}
