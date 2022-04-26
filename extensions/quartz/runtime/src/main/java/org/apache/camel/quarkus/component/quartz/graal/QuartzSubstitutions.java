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
package org.apache.camel.quarkus.component.quartz.graal;

import java.io.ByteArrayOutputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.quartz.impl.jdbcjobstore.CUBRIDDelegate;

public final class QuartzSubstitutions {
}

// Cuts out unwanted references to C3P0ProxyConnection if c3p0 not on the classpath
@TargetClass(value = CUBRIDDelegate.class, onlyWith = C3p0IsAbsent.class)
final class SubstituteCUBRIDDelegate {
    @Substitute
    protected void setBytes(PreparedStatement ps, int index, ByteArrayOutputStream baos)
            throws SQLException {

        byte[] byteArray;
        if (baos == null) {
            byteArray = new byte[0];
        } else {
            byteArray = baos.toByteArray();
        }

        Blob blob = ps.getConnection().createBlob();
        blob.setBytes(1, byteArray);
        ps.setBlob(index, blob);
    }
}

final class C3p0IsAbsent implements BooleanSupplier {
    @Override
    public boolean getAsBoolean() {
        try {
            Thread.currentThread().getContextClassLoader().loadClass("com.mchange.v2.c3p0.C3P0ProxyConnection");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
