/*
 * Copyright 2021 Jeff Hain
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
package net.jolikit.bwd.test.cases.visualtests;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWindowEvent;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.test.utils.TestUtils;

/**
 * To test both accurate image scaling
 * and accurate client scaling flags effects.
 */
public abstract class AbstractImageScalingBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int TARGET_FONT_HEIGHT = 13;
    
    private static final double FIRST_DRAW_DELAY_S = 0.1;
    
    private static final int INITIAL_WIDTH = 960;
    private static final int INITIAL_HEIGHT = 540;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private enum MyDrawType {
        BASE_SCALING_IMG_F_CLI_F(0, false, false),
        BASE_SCALING_IMG_F_CLI_A(0, false, true),
        BASE_SCALING_IMG_A_CLI_F(0, true, false),
        BASE_SCALING_IMG_A_CLI_A(0, true, true),
        BASE_SCALING_P1_IMG_F_CLI_F(1, false, false),
        BASE_SCALING_P1_IMG_F_CLI_A(1, false, true),
        BASE_SCALING_P1_IMG_A_CLI_F(1, true, false),
        BASE_SCALING_P1_IMG_A_CLI_A(1, true, true),
        BASE_SCALING_M1_IMG_F_CLI_F(-1, false, false),
        BASE_SCALING_M1_IMG_F_CLI_A(-1, false, true),
        BASE_SCALING_M1_IMG_A_CLI_F(-1, true, false),
        BASE_SCALING_M1_IMG_A_CLI_A(-1, true, true);
        final int dSpan;
        final boolean isImageAccurate;
        final boolean isClientAccurate;
        private MyDrawType(
            int dSpan,
            boolean isImageAccurate,
            boolean isClientAccurate) {
            this.dSpan = dSpan;
            this.isImageAccurate = isImageAccurate;
            this.isClientAccurate = isClientAccurate;
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdFont font;
    
    private InterfaceBwdImage image;
    
    private MyDrawType drawType = null;
    
    private boolean accurateImageScaling;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractImageScalingBwdTestCase() {
    }

    public AbstractImageScalingBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    /*
     * Window events.
     */
    
    @Override
    public void onWindowShown(BwdWindowEvent event) {
        super.onWindowShown(event);
        
        this.getBinding().getUiThreadScheduler().executeAfterS(new Runnable() {
            @Override
            public void run() {
                tryEnsureNextDrawingType();
            }
        }, FIRST_DRAW_DELAY_S);
    }
    
    /*
     * Mouse events.
     */

    @Override
    public void onMousePressed(BwdMouseEvent event) {
        super.onMousePressed(event);
        
        if (event.getButton() == BwdMouseButtons.SECONDARY) {
            this.tryEnsureNextDrawingType();
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    protected abstract InterfaceBwdImage newImageToDraw();

    /**
     * This class uses variations on this base.
     * 
     * @return Base dstSpan / srcSpan.
     */
    protected abstract double getBaseImageScaling();

    /**
     * @return Current value to use in this test.
     */
    protected boolean getAccurateImageScaling() {
        return this.accurateImageScaling;
    }
    
    @Override
    protected List<GRect> paintClientImpl(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {

        final GRect box = g.getBox();

        final InterfaceBwdBinding binding = this.getBinding();
        
        /*
         * 
         */
        
        final InterfaceBwdImage image = this.getOrCreateImage();
        
        g.setAccurateImageScaling(this.getAccurateImageScaling());
        
        final long t1 = System.nanoTime();
        g.drawImage(box, image);
        final long t2 = System.nanoTime();
        
        /*
         * 
         */
        
        InterfaceBwdFont font = this.font;
        if (font == null) {
            font = binding.getFontHome().newFontWithClosestHeight(TARGET_FONT_HEIGHT);
            this.font = font;
        }
        final int lineHeight = font.metrics().height() + 1;
        
        g.setColor(BwdColor.WHITE);
        g.setFont(font);
        
        int tmpX = box.x();
        int tmpY = box.y();

        g.drawText(tmpX, tmpY, "drawType = " + this.drawType + " (right-click to change)");
        tmpY += lineHeight;

        final GRect imgRect = image.getRect();
        g.drawText(tmpX, tmpY, "image: (" + imgRect.xSpan() + "," + imgRect.ySpan() + ")");
        tmpY += lineHeight;
        
        g.drawText(tmpX, tmpY, "client: (" + box.xSpan() + "," + box.ySpan() + ")");
        tmpY += lineHeight;

        g.drawText(tmpX, tmpY, "xRatio = " + (box.xSpan() / (float) imgRect.xSpan()));
        tmpY += lineHeight;
        g.drawText(tmpX, tmpY, "yRatio = " + (box.ySpan() / (float) imgRect.ySpan()));
        tmpY += lineHeight;

        g.drawText(tmpX, tmpY, "took " + TestUtils.nsToSRounded(t2-t1) + " s");
        tmpY += lineHeight;
        
        /*
         * 
         */
        
        return null;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdImage getOrCreateImage() {
        InterfaceBwdImage image = this.image;
        if (image == null) {
            image = this.newImageToDraw();
            this.image = image;
        }
        return image;
    }
    
    private void tryEnsureNextDrawingType() {
        final InterfaceBwdHost host = this.getHost();
        final GRect clientBounds = host.getClientBounds();
        if (clientBounds.isEmpty()) {
            // Can't.
            return;
        }
        
        final MyDrawType prevType = this.drawType;
        
        final MyDrawType newType;
        if (prevType == null) {
            newType = MyDrawType.BASE_SCALING_IMG_F_CLI_F;
        } else {
            final MyDrawType[] typeArr = MyDrawType.values();
            newType = typeArr[(prevType.ordinal() + 1) % typeArr.length];
        }
        this.drawType = newType;
        
        final double baseScaling = this.getBaseImageScaling();
        final InterfaceBwdImage image = this.getOrCreateImage();
        final GRect imgRect = image.getRect();
        GRect newRect = clientBounds.withSpans(
            NbrsUtils.roundToInt(imgRect.xSpan() * baseScaling),
            NbrsUtils.roundToInt(imgRect.ySpan() * baseScaling));
        newRect = newRect.withSpansDeltas(newType.dSpan, newType.dSpan);
        
        host.setClientBounds(newRect);
        
        this.accurateImageScaling = newType.isImageAccurate;
        host.setAccurateClientScaling(newType.isClientAccurate);
        
        host.ensurePendingClientPainting();
    }
}
