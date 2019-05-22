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
package net.jolikit.bwd.impl.sdl2;

import java.util.List;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.basics.OsUtils;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;

public class SdlBwdBindingConfig extends BaseBwdBindingConfig {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private GRect decorationInsets;
    
    /**
     * TODO sdl Value below which client area width ("window width" for SDL)
     * can go, but not the actual client area of visible window, causing
     * the client area to occupy only a part of the visible client area.
     * 
     * Looks like it must be 100 + decoration margin span.
     */
    private int minClientWidthIfDecorated = 104;

    /**
     * Poll period for system SDL events.
     */
    private double eventPollPeriodS = 0.01;
    
    /**
     * TODO sdl On Mac, iconification (by using SDL_MinimizeWindow(...),
     * native decoration or else), causes backing hiding state and event
     * to occur, while the window visibly remains actually just iconified,
     * and they do before expected backing iconified state and event.
     * As a best effort attempt to make up for this undesired false-hiding,
     * we do the following hacks:
     * - If SDL_WINDOWEVENT_MINIMIZED occurs while the backing library pretends
     *   that the window is hidden (or between SDL_WINDOWEVENT_HIDDEN and
     *   eventual subsequent SDL_WINDOWEVENT_SHOWN, which should also work),
     *   we set a boolean to true to indicate that we must consider
     *   that the backing window is actually not hidden.
     * - On programmatic hiding, and on SDL_WINDOWEVENT_HIDDEN event,
     *   and also on SDL_WINDOWEVENT_SHOWN event for safety,
     *   we reset the boolean to false.
     * This does not prevent from temporary HIDDEN/SHOWN events to be fired
     * to client at iconification time, but at least the durable state is
     * correct.
     */
    private boolean mustGuardAgainstHidingDueToIconification = OsUtils.isMac();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Uses default values for parameters not in arguments.
     * 
     * @param bonusSystemFontFilePathList Can be null or empty here,
     *        but then must be set with at least one font before loading fonts.
     * @param decorationInsets Rectangle containing (left,top,right,bottom) spans
     *        of border when window is decorated.
     */
    public SdlBwdBindingConfig(
            List<String> bonusSystemFontFilePathList,
            GRect decorationInsets) {
        
        this.setBonusSystemFontFilePathList_final(bonusSystemFontFilePathList);
        
        /*
         * TODO sdl Issues for huge sizes, but it seems only above Short.MAX_VALUE (default).
         * Ex.: family = FreeMono, size = 983040: backing ascent = -175308, backing descent = 58437.0.
         *      (looks like an overflow since signs are inverted)
         */
        if (false) {
            this.setMaxRawFontSize_final(Short.MAX_VALUE);
        }

        this.decorationInsets = decorationInsets;
        
        this.setScreenBoundsType_final(ScreenBoundsType.PRIMARY_SCREEN_AVAILABLE);
        
        /*
         * TODO sdl On Mac, on iconification, backing state becomes hidden
         * (and SDL_WINDOWEVENT_HIDDEN gets fired),
         * some time before backing state becomes iconified
         * (and SDL_WINDOWEVENT_MINIMIZED gets fired),
         * so we need to wait before eventually taking
         * backing hidden state into account.
         */
        this.setBackingWindowHiddenStabilityDelayS_final(OsUtils.isMac() ? 0.3 : 0.0);
        /*
         * TODO sdl On Mac, setting backing bounds while iconified causes deiconification,
         * so we don't want to do that.
         */
        this.setMustSetBackingDemaxBoundsWhileHiddenOrIconified_final(!OsUtils.isMac());
    }

    /**
     * @return Rectangle containing (left,top,right,bottom) spans
     *         of border when window is decorated.
     */
    public GRect getDecorationInsets() {
        return this.decorationInsets;
    }

    /**
     * @param decorationInsets Rectangle containing (left,top,right,bottom) spans
     *        of border when window is decorated.
     */
    public void setDecorationInsets(GRect decorationInsets) {
        this.decorationInsets = decorationInsets;
    }

    public int getMinClientWidthIfDecorated() {
        return this.minClientWidthIfDecorated;
    }

    public void setMinClientWidthIfDecorated(int minClientWidthIfDecorated) {
        this.minClientWidthIfDecorated = minClientWidthIfDecorated;
    }

    public double getEventPollPeriodS() {
        return this.eventPollPeriodS;
    }

    public void setEventPollPeriodS(double eventPollPeriodS) {
        this.eventPollPeriodS = eventPollPeriodS;
    }

    public boolean getMustGuardAgainstHidingDueToIconification() {
        return this.mustGuardAgainstHidingDueToIconification;
    }

    public void setMustGuardAgainstHidingDueToIconification(
            boolean mustGuardAgainstHidingDueToIconification) {
        this.mustGuardAgainstHidingDueToIconification = mustGuardAgainstHidingDueToIconification;
    }
}
