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
package org.apache.camel.quarkus.support.xstream.deployment;

import java.util.Arrays;
import java.util.List;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.mapper.CGLIBMapper;
import com.thoughtworks.xstream.mapper.Mapper;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

public class XStreamSupportProcessor {

    private static final List<String> INTERFACES_TO_REGISTER = Arrays.asList(
            Converter.class.getName(),
            Mapper.class.getName());

    private static final List<String> EXCLUDED_CLASSES = Arrays.asList(
            CGLIBMapper.class.getName());

    @BuildStep
    AdditionalApplicationArchiveMarkerBuildItem additionalApplicationArchives() {
        return new AdditionalApplicationArchiveMarkerBuildItem("com/thoughtworks/xstream/XStream.class");
    }

    @BuildStep
    void process(
            CombinedIndexBuildItem indexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {

        for (String className : INTERFACES_TO_REGISTER) {
            for (ClassInfo classInfo : indexBuildItem.getIndex().getAllKnownImplementors(DotName.createSimple(className))) {
                String name = classInfo.name().toString();
                if (!EXCLUDED_CLASSES.contains(name)) {
                    reflectiveClassBuildItemBuildProducer.produce(new ReflectiveClassBuildItem(false, false, name));
                }
            }
        }

        reflectiveClassBuildItemBuildProducer.produce(new ReflectiveClassBuildItem(false, false, false,
                Class.class,
                ClassLoader.class));

        reflectiveClassBuildItemBuildProducer.produce(new ReflectiveClassBuildItem(false, false,
                "[Ljava.lang.String;",
                "com.thoughtworks.xstream.converters.ConverterLookup",
                "com.thoughtworks.xstream.converters.ConverterRegistry",
                "com.thoughtworks.xstream.converters.basic.StringBuilderConverter",
                "com.thoughtworks.xstream.converters.basic.UUIDConverter",
                "com.thoughtworks.xstream.converters.extended.CharsetConverter",
                "com.thoughtworks.xstream.converters.extended.CurrencyConverter",
                "com.thoughtworks.xstream.converters.extended.DurationConverter",
                "com.thoughtworks.xstream.converters.extended.PathConverter",
                "com.thoughtworks.xstream.converters.extended.StackTraceElementConverter",
                "com.thoughtworks.xstream.converters.extended.StackTraceElementFactory15",
                "com.thoughtworks.xstream.converters.reflection.FieldUtil15",
                "com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider",
                "com.thoughtworks.xstream.converters.reflection.ReflectionProvider",
                "com.thoughtworks.xstream.converters.reflection.SunUnsafeReflectionProvider",
                "com.thoughtworks.xstream.converters.time.ChronologyConverter",
                "com.thoughtworks.xstream.converters.time.DurationConverter",
                "com.thoughtworks.xstream.converters.time.HijrahDateConverter",
                "com.thoughtworks.xstream.converters.time.InstantConverter",
                "com.thoughtworks.xstream.converters.time.JapaneseDateConverter",
                "com.thoughtworks.xstream.converters.time.JapaneseEraConverter",
                "com.thoughtworks.xstream.converters.time.LocalDateConverter",
                "com.thoughtworks.xstream.converters.time.LocalDateTimeConverter",
                "com.thoughtworks.xstream.converters.time.LocalTimeConverter",
                "com.thoughtworks.xstream.converters.time.MinguoDateConverter",
                "com.thoughtworks.xstream.converters.time.MonthDayConverter",
                "com.thoughtworks.xstream.converters.time.OffsetDateTimeConverter",
                "com.thoughtworks.xstream.converters.time.OffsetTimeConverter",
                "com.thoughtworks.xstream.converters.time.PeriodConverter",
                "com.thoughtworks.xstream.converters.time.ThaiBuddhistDateConverter",
                "com.thoughtworks.xstream.converters.time.YearConverter",
                "com.thoughtworks.xstream.converters.time.YearMonthConverter",
                "com.thoughtworks.xstream.converters.time.ZoneIdConverter",
                "com.thoughtworks.xstream.converters.time.ZonedDateTimeConverter",
                "com.thoughtworks.xstream.core.ClassLoaderReference",
                "com.thoughtworks.xstream.core.JVM",
                "com.thoughtworks.xstream.core.JVM$Test",
                "com.thoughtworks.xstream.core.util.Base64JavaUtilCodec",
                "com.thoughtworks.xstream.core.util.CustomObjectOutputStream",
                "com.thoughtworks.xstream.mapper.AnnotationConfiguration",
                "com.thoughtworks.xstream.mapper.Mapper",
                "com.thoughtworks.xstream.mapper.Mapper$Null",
                "com.thoughtworks.xstream.security.AnyTypePermission",
                "com.thoughtworks.xstream.security.ArrayTypePermission",
                "com.thoughtworks.xstream.security.ExplicitTypePermission",
                "com.thoughtworks.xstream.security.ForbiddenClassException",
                "com.thoughtworks.xstream.security.InterfaceTypePermission",
                "com.thoughtworks.xstream.security.NoPermission",
                "com.thoughtworks.xstream.security.NoTypePermission",
                "com.thoughtworks.xstream.security.NullPermission",
                "com.thoughtworks.xstream.security.PrimitiveTypePermission",
                "com.thoughtworks.xstream.security.ProxyTypePermission",
                "com.thoughtworks.xstream.security.RegExpTypePermission",
                "com.thoughtworks.xstream.security.TypeHierarchyPermission",
                "com.thoughtworks.xstream.security.TypePermission",
                "com.thoughtworks.xstream.security.WildcardTypePermission",
                "java.awt.Color",
                "java.awt.Font",
                "java.awt.font.TextAttribute",
                "java.io.File",
                "java.io.InputStream",
                "java.lang.Boolean",
                "java.lang.Byte",
                "java.lang.Character",
                "java.lang.Double",
                "java.lang.Exception",
                "java.lang.Float",
                "java.lang.Integer",
                "java.lang.Long",
                "java.lang.Number",
                "java.lang.Object",
                "java.lang.Short",
                "java.lang.StackTraceElement",
                "java.lang.String",
                "java.lang.StringBuffer",
                "java.lang.StringBuilder",
                "java.lang.Throwable",
                "java.lang.Void",
                "java.lang.invoke.SerializedLambda",
                "java.lang.reflect.Constructor",
                "java.lang.reflect.Field",
                "java.lang.reflect.InvocationHandler",
                "java.lang.reflect.Member",
                "java.lang.reflect.Method",
                "java.lang.reflect.Proxy",
                "java.math.BigDecimal",
                "java.math.BigInteger",
                "java.net.URI",
                "java.net.URL",
                "java.nio.charset.Charset",
                "java.nio.file.Path",
                "java.nio.file.Paths",
                "java.sql.Date",
                "java.sql.Time",
                "java.sql.Timestamp",
                "java.text.AttributedCharacterIterator$Attribute",
                "java.text.DecimalFormatSymbols",
                "java.time.Clock$FixedClock",
                "java.time.Clock$OffsetClock",
                "java.time.Clock$SystemClock",
                "java.time.Clock$TickClock",
                "java.time.DayOfWeek",
                "java.time.Duration",
                "java.time.Instant",
                "java.time.LocalDate",
                "java.time.LocalDateTime",
                "java.time.LocalTime",
                "java.time.Month",
                "java.time.MonthDay",
                "java.time.OffsetDateTime",
                "java.time.OffsetTime",
                "java.time.Period",
                "java.time.Year",
                "java.time.YearMonth",
                "java.time.ZoneId",
                "java.time.ZoneOffset",
                "java.time.ZoneRegion",
                "java.time.ZonedDateTime",
                "java.time.chrono.Chronology",
                "java.time.chrono.HijrahChronology",
                "java.time.chrono.HijrahDate",
                "java.time.chrono.HijrahEra",
                "java.time.chrono.IsoChronology",
                "java.time.chrono.JapaneseChronology",
                "java.time.chrono.JapaneseDate",
                "java.time.chrono.JapaneseEra",
                "java.time.chrono.MinguoChronology",
                "java.time.chrono.MinguoDate",
                "java.time.chrono.MinguoEra",
                "java.time.chrono.ThaiBuddhistChronology",
                "java.time.chrono.ThaiBuddhistDate",
                "java.time.chrono.ThaiBuddhistEra",
                "java.time.temporal.ChronoField",
                "java.time.temporal.ChronoUnit",
                "java.time.temporal.IsoFields$Field",
                "java.time.temporal.IsoFields$Unit",
                "java.time.temporal.JulianFields$Field",
                "java.time.temporal.ValueRange",
                "java.time.temporal.WeekFields",
                "java.util.ArrayList",
                "java.util.BitSet",
                "java.util.Calendar",
                "java.util.Collection",
                "java.util.Comparator",
                "java.util.Currency",
                "java.util.Date",
                "java.util.EnumMap",
                "java.util.EnumSet",
                "java.util.GregorianCalendar",
                "java.util.HashMap",
                "java.util.HashSet",
                "java.util.Hashtable",
                "java.util.LinkedHashMap",
                "java.util.LinkedHashSet",
                "java.util.LinkedList",
                "java.util.List",
                "java.util.Locale",
                "java.util.Map",
                "java.util.Map$Entry",
                "java.util.Properties",
                "java.util.Set",
                "java.util.SortedMap",
                "java.util.SortedSet",
                "java.util.TimeZone",
                "java.util.TreeMap",
                "java.util.TreeSet",
                "java.util.UUID",
                "java.util.Vector",
                "java.util.concurrent.ConcurrentHashMap",
                "java.util.regex.Pattern",
                "javax.activation.ActivationDataFlavor",
                "javax.security.auth.Subject",
                "javax.swing.LookAndFeel",
                "javax.xml.datatype.Duration"));

        reflectiveClassBuildItemBuildProducer.produce(new ReflectiveClassBuildItem(true, false,
                "com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder"));

        runtimeInitializedClass.produce(new RuntimeInitializedClassBuildItem(
                "com.thoughtworks.xstream.converters.extended.DynamicProxyConverter$Reflections"));
    }
}
