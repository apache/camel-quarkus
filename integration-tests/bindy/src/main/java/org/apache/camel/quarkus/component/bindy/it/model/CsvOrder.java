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
package org.apache.camel.quarkus.component.bindy.it.model;

import org.apache.camel.dataformat.bindy.annotation.BindyConverter;
import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;
import org.apache.camel.dataformat.bindy.annotation.FormatFactories;

@CsvRecord(separator = ",", endWithLineBreak = false)
@FormatFactories({ NameWithLengthSuffixFormatFactory.class })
public class CsvOrder {

    @DataField(pos = 1)
    private NameWithLengthSuffix nameWithLengthSuffix;

    @DataField(pos = 2)
    @BindyConverter(TestConverter.class)
    private String country;

    public NameWithLengthSuffix getNameWithLengthSuffix() {
        return nameWithLengthSuffix;
    }

    public void setNameWithLengthSuffix(NameWithLengthSuffix nameWithLengthSuffix) {
        this.nameWithLengthSuffix = nameWithLengthSuffix;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
