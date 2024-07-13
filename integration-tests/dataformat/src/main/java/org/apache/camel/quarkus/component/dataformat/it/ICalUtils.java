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
import java.time.Instant;
import java.time.ZonedDateTime;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.TzId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale;
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion;

public class ICalUtils {

    protected static Calendar createTestCalendar(ZonedDateTime start, ZonedDateTime end, String summary, String attendee) {
        // Create a TimeZone
        TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
        String tzId = start.getZone().getId();

        // Create the event
        VEvent meeting = new VEvent();
        meeting.replace(new DtStamp(Instant.ofEpochMilli(0)));
        meeting.add(new DtStart(start));
        meeting.add(new DtEnd(end));
        meeting.add(new Summary(summary));

        // add timezone info..
        meeting.add(new TzId(tzId));

        // generate unique identifier..
        meeting.add(new Uid("00000000"));

        // add attendees..
        Attendee dev1 = new Attendee(URI.create("mailto:" + attendee));
        dev1.add(Role.REQ_PARTICIPANT);
        dev1.add(new Cn(attendee));
        meeting.add(dev1);

        // Create a calendar
        net.fortuna.ical4j.model.Calendar icsCalendar = new net.fortuna.ical4j.model.Calendar();
        icsCalendar.add(ImmutableVersion.VERSION_2_0);
        icsCalendar.add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
        icsCalendar.add(ImmutableCalScale.GREGORIAN);

        // Add the event and print
        icsCalendar.add(meeting);
        return icsCalendar;
    }

}
