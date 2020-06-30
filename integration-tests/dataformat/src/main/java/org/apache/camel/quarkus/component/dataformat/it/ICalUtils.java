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
package org.apache.camel.quarkus.component.dataformat.it;

import java.net.URI;
import java.time.ZonedDateTime;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.TzId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

public class ICalUtils {

    protected static Calendar createTestCalendar(ZonedDateTime start, ZonedDateTime end, String summary, String attendee) {
        // Create a TimeZone
        TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
        String tzId = start.getZone().getId();

        // Create the event
        PropertyList propertyList = new PropertyList();
        DateTime ts = new DateTime(true);
        ts.setTime(0);
        propertyList.add(new DtStamp(ts));
        propertyList.add(new DtStart(toDateTime(start, registry)));
        propertyList.add(new DtEnd(toDateTime(end, registry)));
        propertyList.add(new Summary(summary));
        VEvent meeting = new VEvent(propertyList);

        // add timezone info..
        meeting.getProperties().add(new TzId(tzId));

        // generate unique identifier..
        meeting.getProperties().add(new Uid("00000000"));

        // add attendees..
        Attendee dev1 = new Attendee(URI.create("mailto:" + attendee));
        dev1.getParameters().add(Role.REQ_PARTICIPANT);
        dev1.getParameters().add(new Cn(attendee));
        meeting.getProperties().add(dev1);

        // Create a calendar
        net.fortuna.ical4j.model.Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
        icsCalendar.getProperties().add(Version.VERSION_2_0);
        icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
        icsCalendar.getProperties().add(CalScale.GREGORIAN);

        // Add the event and print
        icsCalendar.getComponents().add(meeting);
        return icsCalendar;
    }

    static DateTime toDateTime(ZonedDateTime zonedDateTime, TimeZoneRegistry registry) {
        return new DateTime(zonedDateTime.toInstant().toEpochMilli());
    }

}
