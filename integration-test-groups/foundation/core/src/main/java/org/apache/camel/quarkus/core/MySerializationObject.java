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
package org.apache.camel.quarkus.core;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(serialization = true)
public class MySerializationObject implements Serializable {

    private Long _long;
    private Integer _integer;
    private Date _date;
    private String _string;
    private Float _float;
    private Double _double;
    private BigInteger _bigInteger;
    private HashMap _hashMap;
    private LinkedHashMap _linkedHashMap;
    private Character _char;
    private Boolean _boolean;
    private Byte _byte;

    public void initValues() {
        _long = 1l;
        _integer = 1;
        _date = Date.from(Instant.now());
        _string = "A";
        _float = Float.valueOf(1);
        _double = Double.valueOf(1);
        _bigInteger = BigInteger.valueOf(1);
        _hashMap = new HashMap();
        _hashMap.put(1, "one");
        _linkedHashMap = new LinkedHashMap();
        _linkedHashMap.put(1, "one");
        _char = Character.valueOf('a');
        _boolean = Boolean.FALSE;
        _byte = Byte.valueOf("1");
    }

    public boolean isNutNull() {
        return _long != null &&
                _integer != null &&
                _date != null &&
                _string != null &&
                _float != null &&
                _double != null &&
                _bigInteger != null &&
                _hashMap != null &&
                _linkedHashMap != null &&
                _char != null &&
                _boolean != null &&
                _byte != null;
    }

}
