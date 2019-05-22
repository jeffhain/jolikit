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
package net.jolikit.bwd.impl.utils.gprim;

import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.GTransform;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.threading.prl.InterfaceParallelizer;

/**
 * Graphics which all methods throw UnsupportedOperationException.
 * Handy to implement partial graphics for tests.
 */
public class ThrowingBwdGraphics implements InterfaceBwdGraphics {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public ThrowingBwdGraphics() {
    }
    
    @Override
    public InterfaceParallelizer getPaintingParallelizer() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public InterfaceBwdGraphics newChildGraphics(GRect childBox) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void init() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void finish() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public GRect getBoxInClient() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public GRect getBaseClipInClient() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GRect getBaseClipInUser() {
        throw new UnsupportedOperationException();
    }

    @Override
    public GRect getClipInClient() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public GRect getClipInUser() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void addClipInClient(GRect clip) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addClipInUser(GRect clip) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void removeLastAddedClip() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void removeAllAddedClips() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public GTransform getTransform() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setTransform(GTransform transform) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void addTransform(GTransform transform) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void removeLastAddedTransform() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void removeAllAddedTransforms() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BwdColor getColor() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setColor(BwdColor color) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getArgb64() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setArgb64(long argb64) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getArgb32() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setArgb32(int argb32) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InterfaceBwdFont getFont() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setFont(InterfaceBwdFont font) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void clearRectOpaque(int x, int y, int xSpan, int ySpan) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clearRectOpaque(GRect rect) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void drawPoint(int x, int y) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public int drawLineStipple(
            int x1, int y1, int x2, int y2,
            int factor, short pattern, int pixelNum) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void drawRect(int x, int y, int xSpan, int ySpan) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void drawRect(GRect rect) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void fillRect(int x, int y, int xSpan, int ySpan) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void fillRect(GRect rect) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void drawOval(int x, int y, int xSpan, int ySpan) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void drawOval(GRect rect) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void fillOval(int x, int y, int xSpan, int ySpan) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void fillOval(GRect rect) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void drawArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void drawArc(
            GRect rect,
            double startDeg, double spanDeg) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void fillArc(
            int x, int y, int xSpan, int ySpan,
            double startDeg, double spanDeg) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void fillArc(
            GRect rect,
            double startDeg, double spanDeg) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void drawText(int x, int y, String text) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void drawImage(
            int x, int y,
            InterfaceBwdImage image) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void drawImage(
            int x, int y, int xSpan, int ySpan,
            InterfaceBwdImage image) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void drawImage(
            GRect rect,
            InterfaceBwdImage image) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void drawImage(
            int x, int y, int xSpan, int ySpan,
            InterfaceBwdImage image,
            int sx, int sy, int sxSpan, int sySpan) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void drawImage(
            GRect rect,
            InterfaceBwdImage image,
            GRect sRect) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void flipColors(int x, int y, int xSpan, int ySpan) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flipColors(GRect rect) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public long getArgb64At(int x, int y) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public int getArgb32At(int x, int y) {
        throw new UnsupportedOperationException();
    }
}
