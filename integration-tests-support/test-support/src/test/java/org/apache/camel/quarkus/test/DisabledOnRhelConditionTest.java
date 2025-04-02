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
package org.apache.camel.quarkus.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DisabledOnRhelConditionTest {

    @Test
    public void testEvaluate() {
        DisabledOnRhelCondition condition = new DisabledOnRhelCondition();

        //rhel 8
        Assertions.assertEquals(true, condition.evaluate("linux", "4.18.0-553.el8_10", 0).isDisabled());
        Assertions.assertEquals(true, condition.evaluate("linux", "4.18.0-553.el8_10", 8).isDisabled());
        Assertions.assertEquals(true, condition.evaluate("linux", "4.18.0-553.el8_10", 9).isDisabled());
        Assertions.assertEquals(true, condition.evaluate("linux", "4.18.0-553.el8_10", 10).isDisabled());

        //rhel 9
        Assertions.assertEquals(true, condition.evaluate("linux", "5.14.0-503.11.1.el9_5", 0).isDisabled());
        Assertions.assertEquals(false, condition.evaluate("linux", "5.14.0-503.11.1.el9_5", 8).isDisabled());
        Assertions.assertEquals(true, condition.evaluate("linux", "5.14.0-503.11.1.el9_5", 9).isDisabled());
        Assertions.assertEquals(true, condition.evaluate("linux", "5.14.0-503.11.1.el9_5", 10).isDisabled());

        //rhel 10
        Assertions.assertEquals(true, condition.evaluate("linux", "5.14.0-503.11.1.el10_5", 0).isDisabled());
        Assertions.assertEquals(false, condition.evaluate("linux", "5.14.0-503.11.1.el10_5", 8).isDisabled());
        Assertions.assertEquals(false, condition.evaluate("linux", "5.14.0-503.11.1.el10_5", 9).isDisabled());
        Assertions.assertEquals(true, condition.evaluate("linux", "5.14.0-503.11.1.el9_5", 10).isDisabled());
    }
}
