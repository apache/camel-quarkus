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
package org.apache.camel.quarkus.component.jpa.it.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import org.apache.camel.Exchange;
import org.apache.camel.component.jpa.Consumed;
import org.apache.camel.component.jpa.PreConsumed;

@Entity
@NamedQuery(name = "findByName", query = "SELECT f FROM Fruit f WHERE f.name = :fruitName")
@NamedQuery(name = "unprocessed", query = "SELECT f FROM Fruit f WHERE f.processed = false")
public class Fruit {

    @Id
    @GeneratedValue
    private Integer id;

    @Column(length = 50, unique = true)
    private String name;

    private Boolean processed = false;

    public Fruit() {
    }

    public Fruit(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getProcessed() {
        return processed;
    }

    public void setProcessed(Boolean processed) {
        this.processed = processed;
    }

    @PreConsumed
    public void preConsumed(Exchange exchange) {
        exchange.getMessage().setHeader("preConsumed", true);
    }

    @Consumed
    public void consumed(Exchange exchange) {
        if (processed) {
            throw new AssertionError("The entity has already been processed!");
        }
        setProcessed(true);
    }
}
