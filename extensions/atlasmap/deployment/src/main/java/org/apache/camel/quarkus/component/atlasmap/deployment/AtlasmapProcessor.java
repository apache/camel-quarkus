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
package org.apache.camel.quarkus.component.atlasmap.deployment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import io.atlasmap.actions.CollectionActions;
import io.atlasmap.actions.DateFieldActions;
import io.atlasmap.actions.ExpressionFieldAction;
import io.atlasmap.actions.NumberFieldActions;
import io.atlasmap.actions.ObjectFieldActions;
import io.atlasmap.actions.StringComplexFieldActions;
import io.atlasmap.actions.StringSimpleFieldActions;
import io.atlasmap.converters.BigDecimalConverter;
import io.atlasmap.converters.BigIntegerConverter;
import io.atlasmap.converters.BooleanConverter;
import io.atlasmap.converters.ByteConverter;
import io.atlasmap.converters.CalendarConverter;
import io.atlasmap.converters.CharBufferConverter;
import io.atlasmap.converters.CharSequenceConverter;
import io.atlasmap.converters.CharacterConverter;
import io.atlasmap.converters.DateConverter;
import io.atlasmap.converters.DoubleConverter;
import io.atlasmap.converters.FloatConverter;
import io.atlasmap.converters.GregorianCalendarConverter;
import io.atlasmap.converters.IntegerConverter;
import io.atlasmap.converters.LocalDateConverter;
import io.atlasmap.converters.LocalDateTimeConverter;
import io.atlasmap.converters.LocalTimeConverter;
import io.atlasmap.converters.LongConverter;
import io.atlasmap.converters.NumberConverter;
import io.atlasmap.converters.ShortConverter;
import io.atlasmap.converters.SqlDateConverter;
import io.atlasmap.converters.SqlTimeConverter;
import io.atlasmap.converters.SqlTimestampConverter;
import io.atlasmap.converters.StringBufferConverter;
import io.atlasmap.converters.StringBuilderConverter;
import io.atlasmap.converters.StringConverter;
import io.atlasmap.converters.ZonedDateTimeConverter;
import io.atlasmap.core.DefaultAtlasContextFactory;
import io.atlasmap.core.DefaultAtlasModuleInfo;
import io.atlasmap.csv.module.CsvModule;
import io.atlasmap.csv.v2.CsvDataSource;
import io.atlasmap.csv.v2.CsvField;
import io.atlasmap.dfdl.module.DfdlModule;
import io.atlasmap.java.module.JavaModule;
import io.atlasmap.java.v2.JavaField;
import io.atlasmap.java.v2.Modifier;
import io.atlasmap.java.v2.ModifierList;
import io.atlasmap.json.module.JsonModule;
import io.atlasmap.json.v2.JsonComplexType;
import io.atlasmap.json.v2.JsonDataSource;
import io.atlasmap.json.v2.JsonEnumField;
import io.atlasmap.json.v2.JsonEnumFields;
import io.atlasmap.json.v2.JsonField;
import io.atlasmap.json.v2.JsonFields;
import io.atlasmap.mxbean.AtlasContextFactoryMXBean;
import io.atlasmap.mxbean.AtlasModuleInfoMXBean;
import io.atlasmap.spi.AtlasConverter;
import io.atlasmap.spi.AtlasFieldAction;
import io.atlasmap.v2.ADMDigest;
import io.atlasmap.v2.AbsoluteValue;
import io.atlasmap.v2.Action;
import io.atlasmap.v2.Add;
import io.atlasmap.v2.AddDays;
import io.atlasmap.v2.AddSeconds;
import io.atlasmap.v2.Append;
import io.atlasmap.v2.AreaUnitType;
import io.atlasmap.v2.AtlasMapping;
import io.atlasmap.v2.Average;
import io.atlasmap.v2.BaseMapping;
import io.atlasmap.v2.Camelize;
import io.atlasmap.v2.Capitalize;
import io.atlasmap.v2.Ceiling;
import io.atlasmap.v2.CollectionType;
import io.atlasmap.v2.Concatenate;
import io.atlasmap.v2.Constant;
import io.atlasmap.v2.Constants;
import io.atlasmap.v2.Contains;
import io.atlasmap.v2.ConvertAreaUnit;
import io.atlasmap.v2.ConvertDistanceUnit;
import io.atlasmap.v2.ConvertMassUnit;
import io.atlasmap.v2.ConvertVolumeUnit;
import io.atlasmap.v2.CopyTo;
import io.atlasmap.v2.Count;
import io.atlasmap.v2.CurrentDate;
import io.atlasmap.v2.CurrentDateTime;
import io.atlasmap.v2.CurrentTime;
import io.atlasmap.v2.DataSource;
import io.atlasmap.v2.DataSourceMetadata;
import io.atlasmap.v2.DataSourceType;
import io.atlasmap.v2.DayOfMonth;
import io.atlasmap.v2.DayOfWeek;
import io.atlasmap.v2.DayOfYear;
import io.atlasmap.v2.DistanceUnitType;
import io.atlasmap.v2.Divide;
import io.atlasmap.v2.EndsWith;
import io.atlasmap.v2.Equals;
import io.atlasmap.v2.Expression;
import io.atlasmap.v2.Field;
import io.atlasmap.v2.FieldAction;
import io.atlasmap.v2.FieldGroup;
import io.atlasmap.v2.FieldStatus;
import io.atlasmap.v2.FieldType;
import io.atlasmap.v2.FileExtension;
import io.atlasmap.v2.Floor;
import io.atlasmap.v2.Format;
import io.atlasmap.v2.GenerateUUID;
import io.atlasmap.v2.IndexOf;
import io.atlasmap.v2.InspectionType;
import io.atlasmap.v2.IsNull;
import io.atlasmap.v2.ItemAt;
import io.atlasmap.v2.LastIndexOf;
import io.atlasmap.v2.Length;
import io.atlasmap.v2.LookupEntry;
import io.atlasmap.v2.LookupTable;
import io.atlasmap.v2.LookupTables;
import io.atlasmap.v2.Lowercase;
import io.atlasmap.v2.LowercaseChar;
import io.atlasmap.v2.Mapping;
import io.atlasmap.v2.MappingType;
import io.atlasmap.v2.Mappings;
import io.atlasmap.v2.MassUnitType;
import io.atlasmap.v2.Maximum;
import io.atlasmap.v2.Minimum;
import io.atlasmap.v2.Multiply;
import io.atlasmap.v2.Normalize;
import io.atlasmap.v2.PadStringLeft;
import io.atlasmap.v2.PadStringRight;
import io.atlasmap.v2.Prepend;
import io.atlasmap.v2.Properties;
import io.atlasmap.v2.Property;
import io.atlasmap.v2.RemoveFileExtension;
import io.atlasmap.v2.Repeat;
import io.atlasmap.v2.ReplaceAll;
import io.atlasmap.v2.ReplaceFirst;
import io.atlasmap.v2.Round;
import io.atlasmap.v2.SeparateByDash;
import io.atlasmap.v2.SeparateByUnderscore;
import io.atlasmap.v2.Split;
import io.atlasmap.v2.StartsWith;
import io.atlasmap.v2.StringList;
import io.atlasmap.v2.SubString;
import io.atlasmap.v2.SubStringAfter;
import io.atlasmap.v2.SubStringBefore;
import io.atlasmap.v2.Subtract;
import io.atlasmap.v2.Trim;
import io.atlasmap.v2.TrimLeft;
import io.atlasmap.v2.TrimRight;
import io.atlasmap.v2.Uppercase;
import io.atlasmap.v2.UppercaseChar;
import io.atlasmap.v2.ValueContainer;
import io.atlasmap.v2.VolumeUnitType;
import io.atlasmap.xml.module.XmlModule;
import io.atlasmap.xml.v2.NodeType;
import io.atlasmap.xml.v2.Restriction;
import io.atlasmap.xml.v2.RestrictionType;
import io.atlasmap.xml.v2.Restrictions;
import io.atlasmap.xml.v2.XmlDataSource;
import io.atlasmap.xml.v2.XmlField;
import io.atlasmap.xml.v2.XmlNamespace;
import io.atlasmap.xml.v2.XmlNamespaces;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.runtime.RuntimeValue;
import org.apache.camel.component.atlasmap.AtlasMapComponent;
import org.apache.camel.quarkus.component.atlasmap.AtlasmapRecorder;
import org.apache.camel.quarkus.core.deployment.spi.CamelBeanBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CamelContextBuildItem;
import org.apache.camel.quarkus.core.deployment.spi.CompiledCSimpleExpressionBuildItem;

class AtlasmapProcessor {

    private static final String FEATURE = "camel-atlasmap";
    private static final String ATLASMAP_SERVICE_BASE = "META-INF/services/";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    List<ReflectiveClassBuildItem> registerReflectiveClasses() {
        List<ReflectiveClassBuildItem> items = new ArrayList<ReflectiveClassBuildItem>();
        items.add(new ReflectiveClassBuildItem(true, false, CollectionActions.class));
        items.add(new ReflectiveClassBuildItem(true, false, ExpressionFieldAction.class));
        items.add(new ReflectiveClassBuildItem(true, false, NumberFieldActions.class));
        items.add(new ReflectiveClassBuildItem(true, false, ObjectFieldActions.class));
        items.add(new ReflectiveClassBuildItem(true, false, StringComplexFieldActions.class));
        items.add(new ReflectiveClassBuildItem(true, false, StringSimpleFieldActions.class));
        items.add(new ReflectiveClassBuildItem(true, false, BigDecimalConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, BigIntegerConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, BooleanConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, ByteConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, CalendarConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, CharBufferConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, CharSequenceConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, CharacterConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, DateConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, DoubleConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, FloatConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, GregorianCalendarConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, IntegerConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, LocalDateConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, LocalDateTimeConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, LocalTimeConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, LongConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, NumberConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, ShortConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, SqlDateConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, SqlTimeConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, SqlTimestampConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, StringBufferConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, StringBuilderConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, StringConverter.class));
        items.add(new ReflectiveClassBuildItem(true, false, ZonedDateTimeConverter.class));
        items.add(new ReflectiveClassBuildItem(false, false, DefaultAtlasContextFactory.class));
        items.add(new ReflectiveClassBuildItem(false, false, DefaultAtlasModuleInfo.class));
        items.add(new ReflectiveClassBuildItem(true, false, CsvModule.class));
        items.add(new ReflectiveClassBuildItem(true, false, DfdlModule.class));
        items.add(new ReflectiveClassBuildItem(true, false, JavaModule.class));
        items.add(new ReflectiveClassBuildItem(true, true, JavaField.class));
        items.add(new ReflectiveClassBuildItem(true, true, Modifier.class));
        items.add(new ReflectiveClassBuildItem(true, true, ModifierList.class));
        items.add(new ReflectiveClassBuildItem(true, false, JsonModule.class));
        items.add(new ReflectiveClassBuildItem(true, true, JsonComplexType.class));
        items.add(new ReflectiveClassBuildItem(true, true, JsonDataSource.class));
        items.add(new ReflectiveClassBuildItem(true, true, JsonEnumField.class));
        items.add(new ReflectiveClassBuildItem(true, true, JsonEnumFields.class));
        items.add(new ReflectiveClassBuildItem(true, true, JsonField.class));
        items.add(new ReflectiveClassBuildItem(true, true, JsonFields.class));
        items.add(new ReflectiveClassBuildItem(false, true, false, AtlasContextFactoryMXBean.class));
        items.add(new ReflectiveClassBuildItem(false, true, false, AtlasModuleInfoMXBean.class));
        items.add(new ReflectiveClassBuildItem(true, true, AbsoluteValue.class));
        items.add(new ReflectiveClassBuildItem(true, true, Action.class));
        items.add(new ReflectiveClassBuildItem(true, true, Add.class));
        items.add(new ReflectiveClassBuildItem(true, true, AddDays.class));
        items.add(new ReflectiveClassBuildItem(true, true, AddSeconds.class));
        items.add(new ReflectiveClassBuildItem(true, true, Append.class));
        items.add(new ReflectiveClassBuildItem(true, true, AreaUnitType.class));
        items.add(new ReflectiveClassBuildItem(true, true, AtlasMapping.class));
        items.add(new ReflectiveClassBuildItem(true, true, Average.class));
        items.add(new ReflectiveClassBuildItem(true, true, BaseMapping.class));
        items.add(new ReflectiveClassBuildItem(true, true, Camelize.class));
        items.add(new ReflectiveClassBuildItem(true, true, Capitalize.class));
        items.add(new ReflectiveClassBuildItem(true, true, Ceiling.class));
        items.add(new ReflectiveClassBuildItem(false, true, true, CollectionType.class));
        items.add(new ReflectiveClassBuildItem(true, true, Concatenate.class));
        items.add(new ReflectiveClassBuildItem(true, true, Constant.class));
        items.add(new ReflectiveClassBuildItem(true, true, Constants.class));
        items.add(new ReflectiveClassBuildItem(true, true, Contains.class));
        items.add(new ReflectiveClassBuildItem(true, true, ConvertAreaUnit.class));
        items.add(new ReflectiveClassBuildItem(true, true, ConvertDistanceUnit.class));
        items.add(new ReflectiveClassBuildItem(true, true, ConvertMassUnit.class));
        items.add(new ReflectiveClassBuildItem(true, true, ConvertVolumeUnit.class));
        items.add(new ReflectiveClassBuildItem(true, true, CopyTo.class));
        items.add(new ReflectiveClassBuildItem(true, true, Count.class));
        items.add(new ReflectiveClassBuildItem(true, true, CurrentDate.class));
        items.add(new ReflectiveClassBuildItem(true, true, CurrentDateTime.class));
        items.add(new ReflectiveClassBuildItem(true, true, CurrentTime.class));
        items.add(new ReflectiveClassBuildItem(true, true, DataSource.class));
        items.add(new ReflectiveClassBuildItem(false, true, true, DataSourceType.class));
        items.add(new ReflectiveClassBuildItem(true, true, DayOfMonth.class));
        items.add(new ReflectiveClassBuildItem(true, true, DayOfWeek.class));
        items.add(new ReflectiveClassBuildItem(true, true, DayOfYear.class));
        items.add(new ReflectiveClassBuildItem(false, true, true, DistanceUnitType.class));
        items.add(new ReflectiveClassBuildItem(true, true, Divide.class));
        items.add(new ReflectiveClassBuildItem(true, true, EndsWith.class));
        items.add(new ReflectiveClassBuildItem(true, true, Equals.class));
        items.add(new ReflectiveClassBuildItem(true, true, Expression.class));
        items.add(new ReflectiveClassBuildItem(true, true, Field.class));
        items.add(new ReflectiveClassBuildItem(false, true, false, FieldAction.class));
        items.add(new ReflectiveClassBuildItem(true, true, FieldGroup.class));
        items.add(new ReflectiveClassBuildItem(false, true, true, FieldStatus.class));
        items.add(new ReflectiveClassBuildItem(false, true, true, FieldType.class));
        items.add(new ReflectiveClassBuildItem(true, true, FileExtension.class));
        items.add(new ReflectiveClassBuildItem(true, true, Floor.class));
        items.add(new ReflectiveClassBuildItem(true, true, Format.class));
        items.add(new ReflectiveClassBuildItem(true, true, GenerateUUID.class));
        items.add(new ReflectiveClassBuildItem(true, true, IndexOf.class));
        items.add(new ReflectiveClassBuildItem(true, true, IsNull.class));
        items.add(new ReflectiveClassBuildItem(true, true, ItemAt.class));
        items.add(new ReflectiveClassBuildItem(true, true, LastIndexOf.class));
        items.add(new ReflectiveClassBuildItem(true, true, Length.class));
        items.add(new ReflectiveClassBuildItem(true, true, LookupEntry.class));
        items.add(new ReflectiveClassBuildItem(true, true, LookupTable.class));
        items.add(new ReflectiveClassBuildItem(true, true, LookupTables.class));
        items.add(new ReflectiveClassBuildItem(true, true, Lowercase.class));
        items.add(new ReflectiveClassBuildItem(true, true, LowercaseChar.class));
        items.add(new ReflectiveClassBuildItem(true, true, Mapping.class));
        items.add(new ReflectiveClassBuildItem(false, true, true, MappingType.class));
        items.add(new ReflectiveClassBuildItem(true, true, Mappings.class));
        items.add(new ReflectiveClassBuildItem(false, true, true, MassUnitType.class));
        items.add(new ReflectiveClassBuildItem(true, true, Maximum.class));
        items.add(new ReflectiveClassBuildItem(true, true, Minimum.class));
        items.add(new ReflectiveClassBuildItem(true, true, Multiply.class));
        items.add(new ReflectiveClassBuildItem(true, true, Normalize.class));
        items.add(new ReflectiveClassBuildItem(true, true, PadStringLeft.class));
        items.add(new ReflectiveClassBuildItem(true, true, PadStringRight.class));
        items.add(new ReflectiveClassBuildItem(true, true, Prepend.class));
        items.add(new ReflectiveClassBuildItem(true, true, Properties.class));
        items.add(new ReflectiveClassBuildItem(true, true, Property.class));
        items.add(new ReflectiveClassBuildItem(true, true, RemoveFileExtension.class));
        items.add(new ReflectiveClassBuildItem(true, true, Repeat.class));
        items.add(new ReflectiveClassBuildItem(true, true, ReplaceAll.class));
        items.add(new ReflectiveClassBuildItem(true, true, ReplaceFirst.class));
        items.add(new ReflectiveClassBuildItem(true, true, Round.class));
        items.add(new ReflectiveClassBuildItem(true, true, SeparateByDash.class));
        items.add(new ReflectiveClassBuildItem(true, true, SeparateByUnderscore.class));
        items.add(new ReflectiveClassBuildItem(true, true, Split.class));
        items.add(new ReflectiveClassBuildItem(true, true, StartsWith.class));
        items.add(new ReflectiveClassBuildItem(true, true, StringList.class));
        items.add(new ReflectiveClassBuildItem(true, true, SubString.class));
        items.add(new ReflectiveClassBuildItem(true, true, SubStringAfter.class));
        items.add(new ReflectiveClassBuildItem(true, true, SubStringBefore.class));
        items.add(new ReflectiveClassBuildItem(true, true, Subtract.class));
        items.add(new ReflectiveClassBuildItem(true, true, Trim.class));
        items.add(new ReflectiveClassBuildItem(true, true, TrimLeft.class));
        items.add(new ReflectiveClassBuildItem(true, true, TrimRight.class));
        items.add(new ReflectiveClassBuildItem(true, true, Uppercase.class));
        items.add(new ReflectiveClassBuildItem(true, true, UppercaseChar.class));
        items.add(new ReflectiveClassBuildItem(false, true, true, VolumeUnitType.class));
        items.add(new ReflectiveClassBuildItem(true, false, XmlModule.class));
        items.add(new ReflectiveClassBuildItem(false, true, false, DateFieldActions.class));
        items.add(new ReflectiveClassBuildItem(false, true, true, NodeType.class));
        items.add(new ReflectiveClassBuildItem(true, true, Restriction.class));
        items.add(new ReflectiveClassBuildItem(false, true, true, RestrictionType.class));
        items.add(new ReflectiveClassBuildItem(true, true, Restrictions.class));
        items.add(new ReflectiveClassBuildItem(true, true, XmlDataSource.class));
        items.add(new ReflectiveClassBuildItem(true, true, XmlField.class));
        items.add(new ReflectiveClassBuildItem(true, true, XmlNamespace.class));
        items.add(new ReflectiveClassBuildItem(true, true, XmlNamespaces.class));
        items.add(new ReflectiveClassBuildItem(true, true, CsvDataSource.class));
        items.add(new ReflectiveClassBuildItem(true, true, CsvField.class));
        items.add(new ReflectiveClassBuildItem(true, true, ADMDigest.class));
        items.add(new ReflectiveClassBuildItem(true, true, DataSourceMetadata.class));
        items.add(new ReflectiveClassBuildItem(false, true, true, InspectionType.class));
        items.add(new ReflectiveClassBuildItem(true, true, ValueContainer.class));
        return items;
    }

    @BuildStep
    NativeImageResourceBuildItem resource() {
        return new NativeImageResourceBuildItem("META-INF/services/atlas/module/atlas.module");
    }

    @BuildStep
    void registerNativeImageResources(BuildProducer<ServiceProviderBuildItem> services) {
        Stream.of(
                AtlasConverter.class.getName(),
                AtlasFieldAction.class.getName(),
                Action.class.getName())
                .forEach(service -> {
                    try {
                        Set<String> implementations = ServiceUtil.classNamesNamedIn(
                                Thread.currentThread().getContextClassLoader(),
                                ATLASMAP_SERVICE_BASE + service);
                        services.produce(
                                new ServiceProviderBuildItem(service,
                                        implementations.toArray(new String[0])));

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    CamelBeanBuildItem configureComponent(
            RecorderContext recorderContext,
            AtlasmapRecorder recorder,
            CamelContextBuildItem camelContext,
            List<CompiledCSimpleExpressionBuildItem> compiledCSimpleExpressions) {

        final RuntimeValue<?> atlasmapComponent = recorder.createAtlasmapComponent();
        return new CamelBeanBuildItem("atlasmap", AtlasMapComponent.class.getName(), atlasmapComponent);
    }
}
