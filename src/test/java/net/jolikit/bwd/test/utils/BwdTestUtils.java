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
package net.jolikit.bwd.test.utils;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontMetrics;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.awt.AwtBwdBinding;
import net.jolikit.bwd.impl.awt.AwtBwdBindingConfig;
import net.jolikit.bwd.impl.utils.DefaultDefaultFontInfoComputer;
import net.jolikit.bwd.impl.utils.basics.OsUtils;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.lang.DefaultExceptionHandler;
import net.jolikit.lang.DefaultThreadFactory;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NumbersUtils;
import net.jolikit.test.JlkTestConfig;
import net.jolikit.time.clocks.hard.InterfaceHardClock;
import net.jolikit.time.clocks.hard.NanoTimeClock;
import net.jolikit.time.sched.hard.HardScheduler;

/**
 * Utilities for BWD tests.
 */
public class BwdTestUtils {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final GPoint MY_DEVICE_RESOLUTION = GPoint.valueOf(
            JlkTestConfig.getDeviceResolutionWidth(),
            JlkTestConfig.getDeviceResolutionHeight());
    
    /*
     * 
     */
    
    /**
     * Usual border rect (left,top,right,bottom) on Microsoft Windows.
     */
    private static final GRect MY_WINDOWS_BORDER_RECT = GRect.valueOf(4, 4+19, 4, 4);
    
    /**
     * TODO algr Allegro5 is special.
     * The "display" surface that we call client area or just client for simplicity,
     * is not at (0,0) in actual (visible) client area, but at (1,1), but that
     * might just be a visual illusion due to borders not being paint properly.
     */
    private static final GRect MY_WINDOWS_BORDER_RECT_ALGR5 = GRect.valueOf(4+2, 4+19+2, 4+2, 4+2);
    
    /**
     * TODO On mac, visible border is infinitely thin,
     * but draggable border is not, and overlaps the client area
     * on the sides, which is terrible since it doesn't allow
     * to handle mouse events properly on these sides.
     * Same kind of bad design as to have controls annoyingly
     * overlapping videos and subtitle on most video players.
     */
    private static final GRect MY_MAC_BORDER_RECT = GRect.valueOf(0, 4+19, 0, 0);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    public static final List<String> BONUS_SYSTEM_FONT_FILE_PATH_LIST;
    static {
        final List<String> list = new ArrayList<String>();
        list.add(BwdTestResources.TEST_FONT_LUCIDA_CONSOLE_TTF);
        BONUS_SYSTEM_FONT_FILE_PATH_LIST = Collections.unmodifiableList(list);
    }
    
    public static final DefaultDefaultFontInfoComputer DEFAULT_FONT_INFO_COMPUTER =
            new DefaultDefaultFontInfoComputer();

    /**
     * Default binding lazily created, so that tests can use this class
     * without systematically creating it, which could cause "interferences"
     * (such as between QtJambi and AWT, for some reason).
     */
    private static class MyDefaultBindingHolder {
        static final AwtBwdBinding DEFAULT_BINDING =
                new AwtBwdBinding(
                        new AwtBwdBindingConfig());
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static InterfaceBwdBinding getDefaultBinding() {
        return MyDefaultBindingHolder.DEFAULT_BINDING;
    }
    
    /**
     * @param fullElseAvailable Must not be CONFIGURED.
     */
    public static GRect getScreenBoundsFromDefaultBinding(ScreenBoundsType screenBoundsType) {
        LangUtils.requireNonNull(screenBoundsType);
        if (screenBoundsType == ScreenBoundsType.CONFIGURED) {
            throw new IllegalArgumentException("" + screenBoundsType);
        }
        
        final AwtBwdBinding binding = (AwtBwdBinding) getDefaultBinding();
        final AwtBwdBindingConfig config = binding.getBindingConfig();
        
        final ScreenBoundsType oldSbt = config.getScreenBoundsType();
        config.setScreenBoundsType(screenBoundsType);
        
        final GRect bounds = binding.getScreenBounds();
        
        config.setScreenBoundsType(oldSbt);
        
        return bounds;
    }
    
    /*
     * 
     */

    public static boolean isAwtBinding(InterfaceBwdBinding binding) {
        return isXxxBinding(binding, "Awt");
    }
    
    public static boolean isSwingBinding(InterfaceBwdBinding binding) {
        return isXxxBinding(binding, "Swing");
    }
    
    public static boolean isJfxBinding(InterfaceBwdBinding binding) {
        return isXxxBinding(binding, "Jfx");
    }
    
    public static boolean isSwtBinding(InterfaceBwdBinding binding) {
        return isXxxBinding(binding, "Swt");
    }
    
    public static boolean isLwjglBinding(InterfaceBwdBinding binding) {
        return isXxxBinding(binding, "Lwjgl");
    }
    
    public static boolean isJoglBinding(InterfaceBwdBinding binding) {
        return isXxxBinding(binding, "Jogl");
    }
    
    public static boolean isQtjBinding(InterfaceBwdBinding binding) {
        return isXxxBinding(binding, "Qtj");
    }
    
    public static boolean isAlgrBinding(InterfaceBwdBinding binding) {
        return isXxxBinding(binding, "Algr");
    }
    
    public static boolean isSdlBinding(InterfaceBwdBinding binding) {
        return isXxxBinding(binding, "Sdl");
    }
    
    /*
     * 
     */
    
    /**
     * @param nbrOfThreads Must be >= 1.
     */
    public static HardScheduler newHardScheduler(int nbrOfThreads) {
        final String testContext = null;
        return newHardScheduler(testContext, nbrOfThreads);
    }
    
    /**
     * @param testContext Can be null.
     * @param nbrOfThreads Must be >= 1.
     */
    public static HardScheduler newHardScheduler(
            String testContext,
            int nbrOfThreads) {
        
        // If swallowing, might not see the issue.
        final boolean mustSwallowElseRethrow = true;
        
        final UncaughtExceptionHandler exceptionHandler =
                new DefaultExceptionHandler(mustSwallowElseRethrow);
        
        final ThreadFactory backingThreadFactory = null;
        final ThreadFactory threadFactory = new DefaultThreadFactory(
                backingThreadFactory,
                exceptionHandler);
        
        final InterfaceHardClock clock = NanoTimeClock.getDefaultInstance();
        final String threadNamePrefix =
                "BwdTest-BG" + ((testContext != null) ? "-" + testContext : "");
        final boolean daemon = true;
        
        return HardScheduler.newInstance(
                clock,
                threadNamePrefix,
                daemon,
                nbrOfThreads,
                threadFactory);
    }
    
    /*
     * 
     */
    
    /**
     * @return {xRatio, yRatio}
     */
    public static double[] getPixelRatioOsOverDeviceXyArr() {
        final GPoint devRes = getDeviceResolution();
        final GPoint osRes = getOsResolution();
        
        final double xRatio = osRes.x() / (double) devRes.x();
        final double yRatio = osRes.y() / (double) devRes.y();
        return new double[]{xRatio, yRatio};
    }
    
    public static GRect getBorderRect() {
        final GRect borderRect;
        if (OsUtils.isWindows()) {
            borderRect = MY_WINDOWS_BORDER_RECT;
        } else if (OsUtils.isMac()) {
            borderRect = MY_MAC_BORDER_RECT;
        } else {
            throw new UnsupportedOperationException("OS not supported: " + OsUtils.getOsName());
        }
        return borderRect;
    }

    public static GRect getBorderRectAlgr5() {
        final GRect borderRect;
        if (OsUtils.isWindows()) {
            borderRect = MY_WINDOWS_BORDER_RECT_ALGR5;
        } else if (OsUtils.isMac()) {
            borderRect = MY_MAC_BORDER_RECT;
        } else {
            throw new UnsupportedOperationException("OS not supported: " + OsUtils.getOsName());
        }
        return borderRect;
    }
    
    /*
     * 
     */
    
    /**
     * @return Initial client bounds to use, when they are not defined as main argument.
     */
    public static GRect computeDefaultInitialClientBounds(
            InterfaceBwdBinding binding,
            InterfaceBwdTestCaseHome home) {
        final GPoint spans = home.getInitialClientSpans();
        final int width = spans.x();
        final int height = spans.y();
        
        /*
         * Centering, unless host is larger than screen.
         */
        
        final GRect screenBounds = binding.getScreenBounds();
        
        final int x;
        if (width <= screenBounds.xSpan()) {
            x = (screenBounds.xSpan() - width) / 2;
        } else {
            x = 0;
        }
        
        final int y;
        if (height <= screenBounds.ySpan()) {
            y = (screenBounds.ySpan() - height) / 2;
        } else {
            y = 0;
        }
        
        return GRect.valueOf(x, y, width, height);
    }

    /*
     * 
     */
    
    /**
     * @return String representation of the time,
     *         in seconds, with millisecond precision.
     */
    public static String timeNsToStringS(long nowNs) {
        // ms precision.
        final long nowMs = nowNs / (1000 * 1000);
        final double nowS = nowMs / 1000.0;
        String nowStr = NumbersUtils.toStringNoCSN(nowS);
        
        final int dotIndex = nowStr.indexOf('.');
        final int pastCommaCount = nowStr.length() - (dotIndex + 1);
        final int paddingSize = Math.max(0, 3 - pastCommaCount);
        for (int i = 0; i < paddingSize; i++) {
            nowStr += "0";
        }
        
        return nowStr;
    }
    
    /*
     * 
     */
    
    /**
     * Easier to identify when on client area border.
     */
    public static void drawRectStipple(
            InterfaceBwdGraphics g,
            GRect rect) {
        
        final int factor = 1;
        final short pattern = (short) 0x00FF;
        int pixelNum = 0;
        
        pixelNum = g.drawLineStipple(
                rect.x(), rect.y(), rect.xMax(), rect.y(),
                factor, pattern, pixelNum);
        if (rect.ySpan() >= 2) {
            if (rect.ySpan() >= 3) {
                pixelNum = g.drawLineStipple(
                        rect.xMax(), rect.y() + 1, rect.xMax(), rect.yMax() - 1,
                        factor, pattern, pixelNum);
            }
            pixelNum = g.drawLineStipple(
                    rect.xMax(), rect.yMax(), rect.x(), rect.yMax(),
                    factor, pattern, pixelNum);
            if ((rect.ySpan() >= 3)
                    && (rect.xSpan() >= 2)) {
                pixelNum = g.drawLineStipple(
                        rect.x(), rect.yMax() - 1, rect.x(), rect.y() + 1,
                        factor, pattern, pixelNum);
            }
        }
    }
    
    /*
     * 
     */
    
    /**
     * Computes points for drawing a spiral polygon,
     * centered on (0,0).
     * 
     * @param pointCount Must be even.
     * @param xArr (out) Length must be >= pointCount.
     * @param yArr (out) Length must be >= pointCount.
     */
    public static void computeSpiralPolygonPoints(
            int xMaxRadius,
            int yMaxRadius,
            int pointCount,
            double roundCount,
            //
            int[] xArr,
            int[] yArr) {
        
        if (!NumbersUtils.isEven(pointCount)) {
            throw new IllegalArgumentException();
        }
        final int halfPointCount = pointCount / 2;
        
        {
            final double xRadiusStep = xMaxRadius / (double) (halfPointCount - 1);
            final double yRadiusStep = yMaxRadius / (double) (halfPointCount - 1);
            final double angStepRad = roundCount * (2*Math.PI / (halfPointCount - 1));
            int k = 0;
            double xRadius = 0.0;
            double yRadius = 0.0;
            double angRad = Math.PI;
            for (; k < halfPointCount; k++) {
                xArr[k] = (int) (xRadius * Math.sin(angRad));
                yArr[k] = (int) (yRadius * Math.cos(angRad));
                final int kk = (pointCount - 1) - k;
                final double internalAngRad;
                if (kk == halfPointCount) {
                    internalAngRad = angRad;
                } else {
                    internalAngRad = angRad + angStepRad * 0.5;
                }
                xArr[kk] = (int) ((xRadius * 0.8) * Math.sin(internalAngRad));
                yArr[kk] = (int) ((yRadius * 0.8) * Math.cos(internalAngRad));
                xRadius += xRadiusStep;
                yRadius += yRadiusStep;
                angRad += angStepRad;
            }
        }
    }
    
    /*
     * 
     */
    
    public static void drawTextCentered(
            InterfaceBwdGraphics g,
            int centerX, int centerY,
            String text) {
        final InterfaceBwdFont font = g.getFont();
        final InterfaceBwdFontMetrics metrics = font.fontMetrics();
        final int textWidth = metrics.computeTextWidth(text);
        final int textHeight = metrics.fontHeight();
        final int textX = centerX - (textWidth / 2);
        final int textY = centerY - (textHeight / 2);
        g.drawText(textX, textY, text);
    }

    public static void drawTextCentered(
            InterfaceBwdGraphics g,
            int x, int y, int xSpan, int ySpan,
            String text) {
        final int centerX = x + xSpan / 2;
        final int centerY = y + ySpan / 2;
        drawTextCentered(g, centerX, centerY, text);
    }

    /**
     * Restores graphics color before returning.
     */
    public static void drawTextAndBgXCentered(
            InterfaceBwdGraphics g,
            BwdColor bgColor,
            BwdColor fgColor,
            int centerX,
            int y,
            String text) {
        final InterfaceBwdFont font = g.getFont();
        final InterfaceBwdFontMetrics metrics = font.fontMetrics();
        final int bgXSpan = metrics.computeTextWidth(text);
        final int bgYSpan = metrics.fontHeight();
        final int x = centerX - bgXSpan / 2;
        drawTextAndSpannedBg(g, bgColor, fgColor, x, y, text, bgXSpan, bgYSpan);
    }

    /**
     * Restores graphics color before returning.
     */
    public static void drawTextAndBg(
            InterfaceBwdGraphics g,
            BwdColor bgColor,
            BwdColor fgColor,
            int x,
            int y,
            String text) {
        final InterfaceBwdFontMetrics fontMetrics = g.getFont().fontMetrics();
        final int bgXSpan = fontMetrics.computeTextWidth(text);
        final int bgYSpan = fontMetrics.fontHeight();
        drawTextAndSpannedBg(g, bgColor, fgColor, x, y, text, bgXSpan, bgYSpan);
    }

    /**
     * Restores graphics color before returning.
     */
    public static void drawTextAndSpannedBg(
            InterfaceBwdGraphics g,
            BwdColor bgColor,
            BwdColor fgColor,
            int x,
            int y,
            String text,
            int bgXSpan,
            int bgYSpan) {
        final long oldArgb64 = g.getArgb64();
        
        g.setColor(bgColor);
        g.fillRect(x, y, bgXSpan, bgYSpan);
        
        g.setColor(fgColor);
        g.drawText(x, y, text);
        
        g.setArgb64(oldArgb64);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BwdTestUtils() {
    }
    
    private static boolean isXxxBinding(InterfaceBwdBinding binding, String prefix) {
        final String bindingCsn = binding.getClass().getSimpleName();
        return bindingCsn.startsWith(prefix);
    }
    
    /**
     * @return Physical screen resolution.
     */
    private static GPoint getDeviceResolution() {
        return MY_DEVICE_RESOLUTION;
    }

    /**
     * @return OS screen resolution.
     */
    private static GPoint getOsResolution() {
        /*
         * Using AWT to get full primary screen OS resolution.
         */
        
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice gd = ge.getDefaultScreenDevice();
        final GraphicsConfiguration gc = gd.getDefaultConfiguration();
        final Rectangle psb = gc.getBounds();
        final int wOsRes = psb.width;
        final int hOsRes = psb.height;
        
        return GPoint.valueOf(wOsRes, hOsRes);
    }
}
