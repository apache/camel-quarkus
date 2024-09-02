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
package org.apache.camel.quarkus.component.hashicorp.vault.it;

import java.util.Objects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(fields = false)
public class SecretPojo {
    private String secretA;
    private String secretB;
    private String secretC;

    public String getSecretA() {
        return secretA;
    }

    public void setSecretA(String secretA) {
        this.secretA = secretA;
    }

    public String getSecretB() {
        return secretB;
    }

    public void setSecretB(String secretB) {
        this.secretB = secretB;
    }

    public String getSecretC() {
        return secretC;
    }

    public void setSecretC(String secretC) {
        this.secretC = secretC;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SecretPojo that = (SecretPojo) o;
        return Objects.equals(secretA, that.secretA) && Objects.equals(secretB, that.secretB)
                && Objects.equals(secretC, that.secretC);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secretA, secretB, secretC);
    }
}
