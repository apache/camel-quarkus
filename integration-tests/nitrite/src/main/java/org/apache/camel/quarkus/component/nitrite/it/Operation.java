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
package org.apache.camel.quarkus.component.nitrite.it;

import org.apache.camel.component.nitrite.operation.CollectionOperation;
import org.apache.camel.component.nitrite.operation.RepositoryOperation;
import org.apache.camel.component.nitrite.operation.collection.FindCollectionOperation;
import org.apache.camel.component.nitrite.operation.collection.RemoveCollectionOperation;
import org.apache.camel.component.nitrite.operation.collection.UpdateCollectionOperation;
import org.apache.camel.component.nitrite.operation.common.InsertOperation;
import org.apache.camel.component.nitrite.operation.repository.FindRepositoryOperation;
import org.apache.camel.component.nitrite.operation.repository.RemoveRepositoryOperation;
import org.apache.camel.component.nitrite.operation.repository.UpdateRepositoryOperation;
import org.dizitart.no2.Document;
import org.dizitart.no2.filters.Filters;
import org.dizitart.no2.objects.filters.ObjectFilters;

public class Operation {

    enum Type {
        update, find, delete, findGt, insert
    };

    private Type type;

    private String field;

    private Object value;

    private EmployeeSerializable employeeSerializable;
    private EmployeeMappable employeeMappable;
    private Document document;

    public Operation() {
    }

    private Operation(Type type, String field, Object value) {
        this.type = type;
        this.field = field;
        this.value = value;
    }

    public Operation(Type type, String field, Object value, EmployeeSerializable employeeSerializable,
            EmployeeMappable employeeMappable) {
        this(type, field, value);
        this.employeeSerializable = employeeSerializable;
        this.employeeMappable = employeeMappable;
    }

    public Operation(Type type, String field, Object value, Document document) {
        this(type, field, value);
        this.document = document;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Employee getEmployeeSerializable() {
        return employeeSerializable;
    }

    public void setEmployeeSerializable(EmployeeSerializable employeeSerializable) {
        this.employeeSerializable = employeeSerializable;
    }

    public EmployeeMappable getEmployeeMappable() {
        return employeeMappable;
    }

    public void setEmployeeMappable(EmployeeMappable employeeMappable) {
        this.employeeMappable = employeeMappable;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public RepositoryOperation toRepositoryOperation() {

        switch (type) {
        case update:
            return new UpdateRepositoryOperation(ObjectFilters.eq(field, value));
        case find:
            return new FindRepositoryOperation(ObjectFilters.eq(field, value));
        case findGt:
            return new FindRepositoryOperation(ObjectFilters.gt(field, value));
        case delete:
            return new RemoveRepositoryOperation(ObjectFilters.eq(field, value));
        default:
            throw new UnsupportedOperationException();
        }
    }

    public CollectionOperation toCollectionOperation() {
        switch (type) {
        case update:
            return new UpdateCollectionOperation(Filters.eq(field, value));
        case find:
            return new FindCollectionOperation(Filters.eq(field, value));
        case delete:
            return new RemoveCollectionOperation(Filters.eq(field, value));
        case insert:
            return new InsertOperation();
        default:
            throw new UnsupportedOperationException();
        }
    }
}
