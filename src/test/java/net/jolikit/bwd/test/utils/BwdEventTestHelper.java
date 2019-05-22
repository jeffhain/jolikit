/*
 * Copyright 2019 Jeff Hain
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jolikit.bwd.test.utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdEvent;
import net.jolikit.bwd.impl.utils.HostStateUtils;

/**
 * Helper class to manage a list of info about BWD events,
 * for tests.
 * Also allows to add custom event info (Object) in the list.
 */
public class BwdEventTestHelper {

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Immutable.
     * 
     * NB: Could be made public, but for now we don't need,
     * we just use its toString().
     */
    private static class MyEventInfo {
        /**
         * To represent multiple events of same type following each other
         * by a single "multiple" event.
         * Ex.:
         * <time_of_1st_event> MOVED (multiplicity = 0)
         * <time_of_nth_event> MOVED [N] (multiplicity = N (>= 2))
         * with N = total number (first one included) of consecutive events of same type.
         */
        final int multiplicity;
        final long timeNs;
        final BwdEvent event;
        final boolean readStateShowing;
        final boolean readStateFocused;
        final boolean readStateIconified;
        final boolean readStateMaximized;
        final boolean readStateClosed;
        public MyEventInfo(
                int multiplicity,
                long timeNs,
                BwdEvent event) {
            final InterfaceBwdHost host = (InterfaceBwdHost) event.getSource();
            this.timeNs = timeNs;
            this.multiplicity = multiplicity;
            this.event = event;
            this.readStateShowing = host.isShowing();
            this.readStateFocused = host.isFocused();
            this.readStateIconified = host.isIconified();
            this.readStateMaximized = host.isMaximized();
            this.readStateClosed = host.isClosed();
        }
        @Override
        public String toString() {
            final boolean readStateOk = HostStateUtils.isReadStateCompatibleWithEventType(
                    this.event.getEventType(),
                    //
                    this.readStateShowing,
                    this.readStateFocused,
                    this.readStateIconified,
                    this.readStateMaximized,
                    this.readStateClosed);
            
            final String hostState = HostStateUtils.toStringHostState(
                    this.readStateShowing,
                    this.readStateFocused,
                    this.readStateIconified,
                    this.readStateMaximized,
                    this.readStateClosed);
            
            final String timeStr = BwdTestUtils.timeNsToStringS(this.timeNs);
            
            final InterfaceBwdHost host = (InterfaceBwdHost) this.event.getSource();
            final Object hostId = hid(host);
            
            final String stateStr =
                    (readStateOk ? "ok" : " KO-")
                    + "-" + hostState;
            
            // Variable size info last (time size rarely changes).
            final String info = timeStr + " " + hostId + " " + stateStr + "-" + event.getEventType()
                    + ((this.multiplicity != 0) ? "[" + this.multiplicity + "]" : "");
            
            return info;
        }
        public MyEventInfo withMultiplicity(int multiplicity) {
            if (multiplicity < 2) {
                throw new IllegalArgumentException("" + multiplicity);
            }
            return new MyEventInfo(
                    multiplicity,
                    this.timeNs,
                    this.event,
                    this.readStateShowing,
                    this.readStateFocused,
                    this.readStateIconified,
                    this.readStateMaximized,
                    this.readStateClosed);
        }
        public boolean sameSourceAndTypeAndState(MyEventInfo other) {
            return (this.event.getSource() == other.event.getSource())
                    && (this.event.getEventType() == other.event.getEventType())
                    && (this.readStateShowing == other.readStateShowing)
                    && (this.readStateFocused == other.readStateFocused)
                    && (this.readStateIconified == other.readStateIconified)
                    && (this.readStateMaximized == other.readStateMaximized)
                    && (this.readStateClosed == other.readStateClosed);
        }
        private MyEventInfo(
                int multiplicity,
                long timeNs,
                BwdEvent event,
                boolean readStateShowing,
                boolean readStateFocused,
                boolean readStateIconified,
                boolean readStateMaximized,
                boolean readStateClosed) {
            this.timeNs = timeNs;
            this.multiplicity = multiplicity;
            this.event = event;
            this.readStateShowing = readStateShowing;
            this.readStateFocused = readStateFocused;
            this.readStateIconified = readStateIconified;
            this.readStateMaximized = readStateMaximized;
            this.readStateClosed = readStateClosed;
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final int maxEventInfoListSize;
    
    /**
     * Info about last events (MyEventInfo), plus custom events (Object).
     */
    private final LinkedList<Object> lastEventInfoList = new LinkedList<Object>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param maxEventInfoListSize Must be >= 0.
     * @throws IllegalArgumentException if maxEventInfoListSize is < 0.
     */
    public BwdEventTestHelper(int maxEventInfoListSize) {
        if (maxEventInfoListSize < 0) {
            throw new IllegalArgumentException("" + maxEventInfoListSize);
        }
        this.maxEventInfoListSize = maxEventInfoListSize;
    }

    /**
     * @return A new mutable list containing event info from oldest to newest.
     */
    public List<Object> getEventInfoList() {
        return new ArrayList<Object>(this.lastEventInfoList);
    }

    public void addCustomEventInfo(Object eventInfo) {
        this.addEventInfoAndEnsureNotTooLarge(eventInfo);
    }
    
    /**
     * Call it on each new event to consider,
     * possibly from different hosts.
     */
    public void addBwdEventInfo(
            BwdEvent event,
            long eventTimeNs) {
        
        final MyEventInfo prevEventInfo;
        if (this.lastEventInfoList.size() == 0) {
            prevEventInfo = null;
        } else {
            final Object prevEvent = this.lastEventInfoList.getLast();
            if (prevEvent instanceof MyEventInfo) {
                prevEventInfo = (MyEventInfo) prevEvent;
            } else {
                prevEventInfo = null;
            }
        }
        
        MyEventInfo eventInfo = new MyEventInfo(
                0,
                eventTimeNs,
                event);
        boolean mustReplaceLast = false;
        if ((prevEventInfo != null)
                && (eventInfo.sameSourceAndTypeAndState(prevEventInfo))) {
            final int newMultiplicity;
            if (prevEventInfo.multiplicity == 0) {
                newMultiplicity = 2;
            } else {
                newMultiplicity = prevEventInfo.multiplicity + 1;
                mustReplaceLast = true;
            }
            eventInfo = eventInfo.withMultiplicity(newMultiplicity);
        }
        
        if (mustReplaceLast) {
            this.lastEventInfoList.removeLast();
        }
        
        this.addEventInfoAndEnsureNotTooLarge(eventInfo);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void addEventInfoAndEnsureNotTooLarge(Object eventInfo) {
        this.lastEventInfoList.add(eventInfo);
        
        while (this.lastEventInfoList.size() > this.maxEventInfoListSize) {
            this.lastEventInfoList.removeFirst();
        }
    }
    
    /**
     * @return Something to identify the host in logs.
     */
    private static Object hid(InterfaceBwdHost host) {
        return host.getTitle();
    }
}
