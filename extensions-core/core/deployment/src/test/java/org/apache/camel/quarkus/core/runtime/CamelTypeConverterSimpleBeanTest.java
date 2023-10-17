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
package org.apache.camel.quarkus.core.runtime;

import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.Converter;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CamelTypeConverterSimpleBeanTest {

    @RegisterExtension
    static final QuarkusUnitTest CONFIG = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Car.class, Bike.class, CarToBikeMapper.class));

    @Inject
    CamelContext context;

    @Test
    public void testTypeConverterAsSimpleBean() {
        String model = "Test";

        Car car = new Car();
        car.setModel(model);

        Bike bike = context.getTypeConverter().tryConvertTo(Bike.class, car);
        assertNotNull(bike);
        assertEquals(model, bike.getMake());
    }

    @Converter
    public static final class CarToBikeConverter {
        CarToBikeMapper mapper = new CarToBikeMapper();

        @Converter
        public Bike carToBike(Car car) {
            return mapper.mapCarToBike(car);
        }
    }

    public static final class CarToBikeMapper {
        public Bike mapCarToBike(Car car) {
            Bike bike = new Bike();
            bike.setMake(car.getModel());
            return bike;
        }
    }

    public static final class Car {
        private String model;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    static final class Bike {
        private String make;

        public String getMake() {
            return make;
        }

        public void setMake(String make) {
            this.make = make;
        }
    }
}
