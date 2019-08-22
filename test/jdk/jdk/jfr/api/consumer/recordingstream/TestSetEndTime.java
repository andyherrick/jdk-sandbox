/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.jfr.api.consumer.recordingstream;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import jdk.jfr.Event;
import jdk.jfr.Name;
import jdk.jfr.Recording;
import jdk.jfr.StackTrace;
import jdk.jfr.consumer.EventStream;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

/**
 * @test
 * @summary Tests EventStream::setEndTime
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.recordingstream.TestSetEndTime
 */
public final class TestSetEndTime {

    @Name("Mark")
    @StackTrace(false)
    public final static class Mark extends Event {
        public boolean before;
    }

    public static void main(String... args) throws Exception {
        try (Recording r = new Recording()) {
            r.setFlushInterval(Duration.ofSeconds(1));
            r.start();
            Mark event1 = new Mark();
            event1.before = true;
            advanceClock();
            event1.commit(); // start time

            Mark event2 = new Mark();
            event2.begin();  // end time
            advanceClock();
          //  event2.end();
            Thread.sleep(100);
            event2.before = false;
            event2.commit();

            Path p = Paths.get("recording.jfr");
            r.dump(p);
            Instant start = null;
            Instant end = null;
            for (RecordedEvent e : RecordingFile.readAllEvents(p)) {
                if (e.getBoolean("before")) {
                    start = e.getStartTime();
                    System.out.println("Start: " + start);
                }
                if (!e.getBoolean("before")) {
                    end = e.getStartTime();
                    System.out.println("End  : " + end);
                }
            }

            AtomicBoolean error = new AtomicBoolean(true);
            try (EventStream d = EventStream.openRepository()) {
                d.setStartTime(start);
                d.setEndTime(end);
                d.onEvent(e -> {
                    System.out.println(e);
                    System.out.println("Event:");
                    System.out.println(e.getStartTime());
                    System.out.println(e.getEndTime());
                    System.out.println(e.getBoolean("before"));
                    System.out.println();
                    boolean before = e.getBoolean("before");
                    if (before) {
                        error.set(false);
                    } else {
                        error.set(true);
                    }
                });
                d.start();
                if (error.get()) {
                    throw new Exception("Found unexpected event!");
                }
            }
        }
    }

    private static void advanceClock() {
        // Wait for some clock movement with
        // java.time clock resolution.
        Instant now = Instant.now();
        while (Instant.now().equals(now)) {
        }
    }
}