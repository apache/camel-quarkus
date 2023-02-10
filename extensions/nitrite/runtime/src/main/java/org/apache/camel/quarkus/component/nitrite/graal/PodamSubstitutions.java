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
package org.apache.camel.quarkus.component.nitrite.graal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import uk.co.jemos.podam.api.AttributeMetadata;
import uk.co.jemos.podam.api.ClassAttribute;
import uk.co.jemos.podam.api.DataProviderStrategy;
import uk.co.jemos.podam.api.MapArguments;
import uk.co.jemos.podam.api.PodamFactoryImpl;
import uk.co.jemos.podam.common.ManufacturingContext;
import uk.co.jemos.podam.typeManufacturers.ArrayTypeManufacturerImpl;

final public class PodamSubstitutions {

}

@TargetClass(PodamFactoryImpl.class)
final class PodamFactoryImplSubstitutions {

    @Substitute
    private Object[] getParameterValuesForConstructor(
            Constructor<?> constructor, Class<?> pojoClass,
            ManufacturingContext manufacturingCtx,
            Map map,
            Type... genericTypeArgs)
            throws InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException {
        return new Object[0];
    }

    @Substitute
    private Object[] getParameterValuesForMethod(
            Method method, Class<?> pojoClass,
            ManufacturingContext manufacturingCtx, Map<String, Type> typeArgsMap,
            Type... genericTypeArgs)
            throws InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException {
        return new Object[0];
    }

    @Substitute
    private void fillMap(MapArguments mapArguments, ManufacturingContext manufacturingCtx)
            throws InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException {
    }

    @Substitute
    private void fillCollection(ManufacturingContext manufacturingCtx,
            List<Annotation> annotations, String attributeName,
            Collection<? super Object> collection,
            Class<?> collectionElementType, Type... genericTypeArgs)
            throws InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException {
    }

    @Substitute
    private void fillArray(Object array, String attributeName, Class<?> elementType,
            Type genericElementType, List<Annotation> annotations,
            ManufacturingContext manufacturingCtx,
            Map<String, Type> typeArgsMap)
            throws InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException {
    }

    @Substitute
    private <T> boolean populateReadWriteField(T pojo, ClassAttribute attribute,
            Map<String, Type> typeArgsMap, ManufacturingContext manufacturingCtx)
            throws InstantiationException, IllegalAccessException,
            InvocationTargetException, ClassNotFoundException {
        return false;
    }
}

@TargetClass(ArrayTypeManufacturerImpl.class)
final class ArrayTypeManufacturerImplSubstitutions {
    @Substitute
    public Cloneable getType(DataProviderStrategy strategy,
            AttributeMetadata attributeMetadata,
            Map<String, Type> genericTypesArgumentsMap) {
        return null;
    }
}
