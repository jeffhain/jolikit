/*
 * Copyright 2019-2020 Jeff Hain
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
package net.jolikit.bwd.impl.algr5;

import java.util.List;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.lang.OsUtils;

public class AlgrBwdBindingConfig extends BaseBwdBindingConfig {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * TODO algr We don't just configure device resolution, and then compute
     * X and Y OS over device pixel ratio from it and current OS resolution,
     * because we don't have a way to retrieve OS resolution,
     * and because supporting dynamic resolution changes could get messy,
     * in particular since we mandatorily don't for fonts "point size".
     */
    private double pixelRatioOsOverDeviceX;
    private double pixelRatioOsOverDeviceY;

    private GRect decorationInsets;

    /**
     * Poll period for system SDL events.
     */
    private double eventPollPeriodS = 0.01;

    private double clientResizeDelayS = 0.25;

    private double exposePostponeDelayS = 0.1;
    
    /**
     * TODO algr Because backing MOUSE_AXES events can be old,
     * and with very wrong bounds due to being generated
     * whenever the mouse moves not relatively to the screen,
     * but relatively to the window, i.e. also when just
     * the window moves.
     */
    private boolean mustSetFreshPosInMouseAxesEvent = true;

    /**
     * TODO algr On Windows, DISPLAY_EXPOSE event is generated
     * instead of DISPLAY_RESIZE event.
     * 
     * TODO algr On Mac, can't seem to get DISPLAY_EXPOSE event to occur,
     * even with ALLEGRO_GENERATE_EXPOSE_EVENTS flag, but
     * DISPLAY_RESIZE event is properly generated.
     */
    private boolean mustUseDisplayExposeAsDisplayResize = OsUtils.isWindows();
    
    /**
     * TODO algr On Mac, if not delaying display resize,
     * after a decoration border drag, we pass here frantically,
     * and the window frantically and cyclically passes through
     * multiple sizes.
     */
    private boolean mustDelayDisplayResizeOnBackingResizeEvent = OsUtils.isMac();

    /**
     * TODO algr On Mac, we need to do that.
     */
    private boolean mustResizeDisplayExplicitlyOnBoundsSetting = OsUtils.isMac();
    
    /**
     * TODO algr On Mac, Need to restore previous top-left corner
     * position, else it's moved by the resize,
     * which uses bottom-right corner as fix point.
     */
    private boolean mustRestoreWindowPositionAfterDisplayResize = OsUtils.isMac();

    /**
     * TODO algr Allegro iconified flag doesn't seem to work.
     * To make up for it, we just use our synthetic hiding logic,
     * with a synthetic iconified flag.
     */
    private boolean mustRelyOnBackingIconification = false;
    
    /**
     * TODO algr On Mac, doesn't work well, since the window
     * is constrained by the OS to overlap the screen.
     * 
     * Using large positive values, for top-left buttons
     * of eventual decoration to still be usable in one click
     * if the OS ensures some overlapping with screen.
     */
    private int hiddenHackClientX = 32000;
    private int hiddenHackClientY = 32000;

    /**
     * TODO algr On Mac, system cursors sometimes fallback to arrow cursor.
     */
    private boolean mustUseSystemCursorsWhenAvailable = (!OsUtils.isMac());
    
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
    public AlgrBwdBindingConfig(
            List<String> bonusSystemFontFilePathList,
            double pixelRatioOsOverDeviceX,
            double pixelRatioOsOverDeviceY,
            GRect screenBounds,
            GRect decorationInsets) {
        
        /*
         * TODO algr ALLEGRO_EVENT_MOUSE_AXES event is generated
         * even if mouse is still, as long as it moves relatively
         * to the window.
         * 
         * False by default because "mustSetFreshPosInMouseAxesEvent"
         * is true by default and solves the issue already.
         */
        this.setMustFixBoundsDuringDrag_final(false);

        /*
         * TODO algr On Mac, can't preserve pixels from previous paintings,
         * so we need to do that (else it does some kind of flip/flop).
         */
        this.setMustMakeAllDirtyAtEachPainting_final(OsUtils.isMac());

        this.setBonusSystemFontFilePathList_final(bonusSystemFontFilePathList);
        
        /*
         * TODO algr Can get a "java.lang.Error: Invalid memory access"
         * in al_get_glyph_width(...), for large font sizes, so should
         * not go above Short.MAX_VALUE / 10.
         * Ex: family = FreeMono, size = 7680.
         * Also, Allegro al_get_glyph_width(...) and al_get_text_width(...),
         * get slower as font grows, up to very slow, so we lower the bound
         * even more for user and tests not to experience annoying slowness.
         */
        this.setMaxRawFontSize_final(Short.MAX_VALUE / (10 * 10));
        
        this.pixelRatioOsOverDeviceX = pixelRatioOsOverDeviceX;
        this.pixelRatioOsOverDeviceY = pixelRatioOsOverDeviceY;
        this.decorationInsets = decorationInsets;
        
        this.setScreenBoundsType_final(ScreenBoundsType.CONFIGURED);
        this.setScreenBounds_final(screenBounds);
        
        /*
         * TODO algr On Mac, maximization/demaximization work according to
         * backing flags, but bounds don't change, so we need to update bounds
         * "manually".
         */
        this.setMustRestoreBoundsOnShowDeicoDemax_final(OsUtils.isMac());
        this.setMustEnforceBoundsOnShowDeicoMax_final(OsUtils.isMac());
        
        /*
         * TODO algr On Mac, can causes random crashes, so not doing it.
         */
        this.setMustSetBackingDemaxBoundsWhileHiddenOrIconified_final(!OsUtils.isMac());
    }

    public double getPixelRatioOsOverDeviceX() {
        return this.pixelRatioOsOverDeviceX;
    }

    public void setPixelRatioOsOverDeviceX(double pixelRatioOsOverDeviceX) {
        this.pixelRatioOsOverDeviceX = pixelRatioOsOverDeviceX;
    }

    public double getPixelRatioOsOverDeviceY() {
        return this.pixelRatioOsOverDeviceY;
    }

    public void setPixelRatioOsOverDeviceY(double pixelRatioOsOverDeviceY) {
        this.pixelRatioOsOverDeviceY = pixelRatioOsOverDeviceY;
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

    public double getEventPollPeriodS() {
        return this.eventPollPeriodS;
    }

    public void setEventPollPeriodS(double eventPollPeriodS) {
        this.eventPollPeriodS = eventPollPeriodS;
    }
    
    public double getClientResizeDelayS() {
        return this.clientResizeDelayS;
    }

    public void setClientResizeDelayS(double clientResizeDelayS) {
        this.clientResizeDelayS = clientResizeDelayS;
    }

    public double getExposePostponeDelayS() {
        return this.exposePostponeDelayS;
    }

    public void setExposePostponeDelayS(double exposePostponeDelayS) {
        this.exposePostponeDelayS = exposePostponeDelayS;
    }
    
    public boolean getMustSetFreshPosInMouseAxesEvent() {
        return this.mustSetFreshPosInMouseAxesEvent;
    }

    public void setMustSetFreshPosInMouseAxesEvent(boolean mustSetFreshPosInMouseAxesEvent) {
        this.mustSetFreshPosInMouseAxesEvent = mustSetFreshPosInMouseAxesEvent;
    }
    
    public boolean getMustUseDisplayExposeAsDisplayResize() {
        return this.mustUseDisplayExposeAsDisplayResize;
    }

    public void setMustUseDisplayExposeAsDisplayResize(boolean mustUseDisplayExposeAsDisplayResize) {
        this.mustUseDisplayExposeAsDisplayResize = mustUseDisplayExposeAsDisplayResize;
    }

    public boolean getMustDelayDisplayResizeOnBackingResizeEvent() {
        return this.mustDelayDisplayResizeOnBackingResizeEvent;
    }

    public void setMustDelayDisplayResizeOnBackingResizeEvent(boolean mustDelayDisplayResizeOnBackingResizeEvent) {
        this.mustDelayDisplayResizeOnBackingResizeEvent = mustDelayDisplayResizeOnBackingResizeEvent;
    }
    
    public boolean getMustResizeDisplayExplicitlyOnBoundsSetting() {
        return this.mustResizeDisplayExplicitlyOnBoundsSetting;
    }

    public void setMustResizeDisplayExplicitlyOnBoundsSetting(boolean mustResizeDisplayExplicitlyOnBoundsSetting) {
        this.mustResizeDisplayExplicitlyOnBoundsSetting = mustResizeDisplayExplicitlyOnBoundsSetting;
    }
    
    public boolean getMustRestoreWindowPositionAfterDisplayResize() {
        return this.mustRestoreWindowPositionAfterDisplayResize;
    }

    public void setMustRestoreWindowPositionAfterDisplayResize(boolean mustRestoreWindowPositionAfterDisplayResize) {
        this.mustRestoreWindowPositionAfterDisplayResize = mustRestoreWindowPositionAfterDisplayResize;
    }

    public boolean getMustRelyOnBackingIconification() {
        return this.mustRelyOnBackingIconification;
    }

    public void setMustRelyOnBackingIconification(boolean mustRelyOnBackingIconification) {
        this.mustRelyOnBackingIconification = mustRelyOnBackingIconification;
    }

    public int getHiddenHackClientX() {
        return this.hiddenHackClientX;
    }

    public void setHiddenHackClientX(int hiddenHackClientX) {
        this.hiddenHackClientX = hiddenHackClientX;
    }

    public int getHiddenHackClientY() {
        return this.hiddenHackClientY;
    }

    public void setHiddenHackClientY(int hiddenHackClientY) {
        this.hiddenHackClientY = hiddenHackClientY;
    }

    public boolean getMustUseSystemCursorsWhenAvailable() {
        return this.mustUseSystemCursorsWhenAvailable;
    }

    public void setMustUseSystemCursorsWhenAvailable(boolean mustUseSystemCursorsWhenAvailable) {
        this.mustUseSystemCursorsWhenAvailable = mustUseSystemCursorsWhenAvailable;
    }
}
