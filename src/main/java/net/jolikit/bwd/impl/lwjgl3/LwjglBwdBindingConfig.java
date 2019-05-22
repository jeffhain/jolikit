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
package net.jolikit.bwd.impl.lwjgl3;

import java.util.Locale;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.impl.utils.basics.OsUtils;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;

/**
 * Configuration for LWJGL binding.
 * 
 * NB: When double buffered, dirty paintings paint the whole client area,
 * and buffers swap is quite slow, so if double buffered it's better for
 * client paint delay not to be zero.
 */
public class LwjglBwdBindingConfig extends BaseBwdBindingConfig {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private Locale locale = Locale.ENGLISH;
    
    /**
     * TODO lwjgl We don't just configure device resolution, and then compute
     * X and Y OS over device pixel ratio from it and current OS resolution,
     * because we don't have a way to retrieve OS resolution,
     * and because supporting dynamic resolution changes could get messy,
     * in particular since we mandatorily don't for fonts "point size".
     */
    private double pixelRatioOsOverDeviceX;
    private double pixelRatioOsOverDeviceY;
    
    private GRect decorationInsets;
    
    /**
     * GL double buffering causes flip-flop in case of dirty painting,
     * which only paints a part of the buffer, so when this value is true
     * dirty painting is deactivated (clip forced to whole client area on
     * rendering), thus as slow as full painting.
     * You might want to set it to true only if you don't need fast dirty
     * paintings, and if it could reduce flickering in your use cases.
     */
    private boolean glDoubleBuffered = false;

    /**
     * OpenGL goes black when the "Windows exclusive operation screen mode"
     * gets active, even if from the code everything looks fine, cf.
     * https://stackoverflow.com/questions/32961688/win32-ctrl-alt-del-task-manager-kills-opengl-somehow
     * Best effort workaround: on focus gained, which with lwjgl occurs
     * when the normal screen mode gets back if our window had focus,
     * or when the user, intrigued by the black issue, clicks on the window,
     * we grow and then resize back the window a bit, for things to get back
     * to normal.
     */
    private boolean mustShakeClientBoundsOnFocusGained = true;
    
    /**
     * TODO lwjgl On Mac, iconification causes hiding
     * (but the window is actually not hidden, since it shows up as icon),
     * similarly to what happens with SDL, but unlike SDL, LWJGL doesn't
     * have hidden events, so we can't reset the boolean we use
     * for the hacky workaround when hiding is not programmatic.
     * As a result, we only support boolean reset on programmatic hiding,
     * by doing it in call to hide(). Non-programmatic hiding of a window
     * while it is iconified should hopefully not be a common use case,
     * and if it ever happens, our host would still pretend to be showing
     * instead of being hidden.
     * What we do:
     * - On programmatic backing iconification, which is called by iconify()
     *   when not hidden, we set a boolean to true to indicate that
     *   we must consider that the backing window is not hidden
     *   (even if its state turns to hidden right after).
     * - On programmatic hiding, we reset the boolean to false.
     * This does not prevent from temporary HIDDEN/SHOWN events to be fired
     * to client at iconification time, but at least the durable state is
     * correct.
     */
    private boolean mustGuardAgainstHidingDueToIconification = OsUtils.isMac();
    
    /**
     * TODO lwjgl On Mac, onCursorPosEvent(...) (from GLFWCursorPosCallback)
     * is only called while dragging, not when just moving the mouse,
     * so we need to generate synthetic mouse moved events.
     */
    private boolean mustGenerateSyntheticMouseMovedEvents = OsUtils.isMac();
    
    /**
     * Only used if mustGenerateSyntheticMouseMoves is true.
     */
    private double syntheticMouseMovesPeriodS = 0.01;

    private boolean mustUseSystemCursorsWhenAvailable = true;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * Uses default values for parameters not in arguments.
     * 
     * @param decorationInsets Rectangle containing (left,top,right,bottom) spans
     *        of border when window is decorated.
     */
    public LwjglBwdBindingConfig(
            double pixelRatioOsOverDeviceX,
            double pixelRatioOsOverDeviceY,
            GRect decorationInsets) {
        this.pixelRatioOsOverDeviceX = pixelRatioOsOverDeviceX;
        this.pixelRatioOsOverDeviceY = pixelRatioOsOverDeviceY;
        this.decorationInsets = decorationInsets;
        this.setScreenBoundsType_final(ScreenBoundsType.PRIMARY_SCREEN_FULL);
    }

    public Locale getLocale() {
        return this.locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
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

    public boolean getGlDoubleBuffered() {
        return this.glDoubleBuffered;
    }

    public void setGlDoubleBuffered(boolean glDoubleBuffered) {
        this.glDoubleBuffered = glDoubleBuffered;
    }

    public boolean getMustShakeClientBoundsOnFocusGained() {
        return this.mustShakeClientBoundsOnFocusGained;
    }

    public void setMustShakeClientBoundsOnFocusGained(
            boolean mustShakeClientBoundsOnFocusGained) {
        this.mustShakeClientBoundsOnFocusGained = mustShakeClientBoundsOnFocusGained;
    }

    public boolean getMustGuardAgainstHidingDueToIconification() {
        return this.mustGuardAgainstHidingDueToIconification;
    }

    public void setMustGuardAgainstHidingDueToIconification(
            boolean mustGuardAgainstHidingDueToIconification) {
        this.mustGuardAgainstHidingDueToIconification = mustGuardAgainstHidingDueToIconification;
    }

    public boolean getMustGenerateSyntheticMouseMovedEvents() {
        return this.mustGenerateSyntheticMouseMovedEvents;
    }

    public void setMustGenerateSyntheticMouseMovedEvents(
            boolean mustGenerateSyntheticMouseMovedEvents) {
        this.mustGenerateSyntheticMouseMovedEvents = mustGenerateSyntheticMouseMovedEvents;
    }

    public double getSyntheticMouseMovesPeriodS() {
        return this.syntheticMouseMovesPeriodS;
    }

    public void setSyntheticMouseMovesPeriodS(double syntheticMouseMovesPeriodS) {
        this.syntheticMouseMovesPeriodS = syntheticMouseMovesPeriodS;
    }

    public boolean getMustUseSystemCursorsWhenAvailable() {
        return this.mustUseSystemCursorsWhenAvailable;
    }

    public void setMustUseSystemCursorsWhenAvailable(boolean mustUseSystemCursorsWhenAvailable) {
        this.mustUseSystemCursorsWhenAvailable = mustUseSystemCursorsWhenAvailable;
    }
}
