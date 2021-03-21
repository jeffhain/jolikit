/*
 * Copyright 2019-2021 Jeff Hain
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
package net.jolikit.bwd.impl.jogl;

import java.util.Locale;

import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.lang.OsUtils;

public class JoglBwdBindingConfig extends BaseBwdBindingConfig {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private Locale locale = Locale.ENGLISH;
    
    /*
     * 
     */
    
    /**
     * Poll period for NEWT's EDT.
     * 10ms is NEWT's default.
     */
    private double newtEdtPollPeriodS = 0.01;

    /*
     * 
     */
    
    /**
     * TODO jogl We don't just configure device resolution, and then compute
     * X and Y OS over device pixel ratio from it and current OS resolution,
     * because we don't have a way to retrieve OS resolution,
     * and because supporting dynamic resolution changes could get messy,
     * in particular since we mandatorily don't for fonts "point size".
     */
    private double pixelRatioOsOverDeviceX;
    private double pixelRatioOsOverDeviceY;

    /**
     * GL double buffering causes flip-flop in case of dirty painting,
     * which only paints a part of the buffer, so when this value is true
     * dirty painting is deactivated (dirty clip forced to whole client area
     * on rendering).
     * You might want to set it to true only if you don't need fast dirty
     * paintings, and if it could reduce flickering in your use cases.
     */
    private boolean glDoubleBuffered = false;
    
    /**
     * TODO jogl OpenGL goes black when the "Windows exclusive operation screen mode"
     * gets active, even if from the code everything looks fine, cf.
     * https://stackoverflow.com/questions/32961688/win32-ctrl-alt-del-task-manager-kills-opengl-somehow
     * Best effort workaround: on focus gained, which with JOGL occurs when
     * the user, intrigued by the black issue, clicks on the window, we grow
     * and then resize back the window a bit, for things to get back to normal.
     */
    private boolean mustShakeClientBoundsOnFocusGained = true;

    /**
     * TODO jogl NEWT generates MOUSE_DRAGGED or MOUSE_MOVED events
     * when the window/client area moves relatively to the mouse,
     * even if the mouse is standing still.
     * This can happen in particular during or soon after backing bounds
     * setting, which can cause an erroneous drag start detection
     * (if drag button was down), causing getXxxBounds() to return
     * old and obsolete bounds instead of new actual bounds
     * (cf. getMustFixBoundsDuringDrag()), and client area painting
     * to be messed up accordingly.
     * Cf. HostBoundsSetGetBwdTestCase test.
     * To make up for it, we block drag move detections occurring
     * less than this delay after last backing bounds setting.
     * We don't go as far as blocking MOUSE_DRAGGED or MOUSE_MOVED events,
     * as this could cause new issues.
     */
    private double postBoundsSettingDragMoveBlockingDelayS = 0.1;

    /**
     * TODO jogl A single actual mouse press can cause two consecutive
     * and identical backing mouse pressed events to be generated.
     * (cf. HostBoundsSetGetBwdTestCase, in which pressing mouse button
     * on "click here" window causes two steps per actual press
     * when mustShakeClientBoundsOnFocusGained is true).
     * It seems to occur when a first pressed event is fired synchronously
     * during bounds setting, followed by released, clicked, and then
     * the duplicate pressed event.
     * To make up for it, we ignore backing mouse pressed events
     * if the preceding one was fired during bounds setting and
     * was an identical one, and is not too far from it in time.
     */
    private double duplicateMousePressedBlockingDelayS = 0.2;
    
    /**
     * TODO jogl No API for it, so we can only do our best.
     * On Mac, doesn't work well, since the window is constrained
     * by the OS to overlap the screen.
     * 
     * TODO jogl If doing it on Mac, one second about after deiconification
     * and showing/deiconified bounds restoration, for some reason the window
     * gets moved to the position it was while iconified. As a result, on Mac
     * we don't to it.
     */
    private boolean mustDoBestEffortIconification = (!OsUtils.isMac());
    
    /**
     * TODO jogl On Mac, doesn't work well, since the window
     * is constrained by the OS to overlap the screen.
     * 
     * Using large positive values, for top-left buttons
     * of eventual decoration to still be usable in one click
     * if the OS ensures some overlapping with screen.
     */
    private int hiddenHackClientXInOs = 32000;
    private int hiddenHackClientYInOs = 32000;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Uses default values for parameters not in arguments.
     */
    public JoglBwdBindingConfig(
            double pixelRatioOsOverDeviceX,
            double pixelRatioOsOverDeviceY) {
        this.pixelRatioOsOverDeviceX = pixelRatioOsOverDeviceX;
        this.pixelRatioOsOverDeviceY = pixelRatioOsOverDeviceY;
        
        this.setScreenBoundsType_final(ScreenBoundsType.PRIMARY_SCREEN_FULL);
        
        /*
         * TODO jogl Mouse moved event is generated even if mouse is still,
         * as long as it moves relatively to the window.
         * As a result, we need to fix bounds during drag,
         * else the window can warp around.
         */
        this.setMustFixBoundsDuringDrag_final(true);
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }
    
    public double getNewtEdtPollPeriodS() {
        return this.newtEdtPollPeriodS;
    }

    public void setNewtEdtPollPeriodS(double newtEdtPollPeriodS) {
        this.newtEdtPollPeriodS = newtEdtPollPeriodS;
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
    
    public boolean getGlDoubleBuffered() {
        return this.glDoubleBuffered;
    }

    public void setGlDoubleBuffered(boolean glDoubleBuffered) {
        this.glDoubleBuffered = glDoubleBuffered;
    }

    public boolean getMustShakeClientBoundsOnFocusGained() {
        return this.mustShakeClientBoundsOnFocusGained;
    }

    public void setMustShakeClientBoundsOnFocusGained(boolean mustShakeClientBoundsOnFocusGained) {
        this.mustShakeClientBoundsOnFocusGained = mustShakeClientBoundsOnFocusGained;
    }

    public double getPostBoundsSettingDragMoveBlockingDelayS() {
        return this.postBoundsSettingDragMoveBlockingDelayS;
    }

    public void setPostBoundsSettingDragMoveBlockingDelayS(double postBoundsSettingDragMoveBlockingDelayS) {
        this.postBoundsSettingDragMoveBlockingDelayS = postBoundsSettingDragMoveBlockingDelayS;
    }

    public double getDuplicateMousePressedBlockingDelayS() {
        return this.duplicateMousePressedBlockingDelayS;
    }

    public void setDuplicateMousePressedBlockingDelayS(double duplicateMousePressedBlockingDelayS) {
        this.duplicateMousePressedBlockingDelayS = duplicateMousePressedBlockingDelayS;
    }

    public boolean getMustDoBestEffortIconification() {
        return this.mustDoBestEffortIconification;
    }

    public void setMustDoBestEffortIconification(boolean mustDoBestEffortIconification) {
        this.mustDoBestEffortIconification = mustDoBestEffortIconification;
    }

    public int getHiddenHackClientXInOs() {
        return this.hiddenHackClientXInOs;
    }

    public void setHiddenHackClientXInOs(int hiddenHackClientXInOs) {
        this.hiddenHackClientXInOs = hiddenHackClientXInOs;
    }

    public int getHiddenHackClientYInOs() {
        return this.hiddenHackClientYInOs;
    }

    public void setHiddenHackClientYInOs(int hiddenHackClientYInOs) {
        this.hiddenHackClientYInOs = hiddenHackClientYInOs;
    }
}
