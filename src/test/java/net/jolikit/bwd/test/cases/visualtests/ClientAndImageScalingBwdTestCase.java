/*
 * Copyright 2024 Jeff Hain
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
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;

/**
 * To test client and image scaling algorithms,
 * which are in ScaledRectDrawer.
 * 
 * Can reset client bounds with left-click.
 * Can switch between scaling types with right-click.
 */
public class ClientAndImageScalingBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int TARGET_FONT_HEIGHT = 13;
    
    private static final String IMG_FILE_PATH =
        true ? BwdTestResources.TEST_IMG_FILE_PATH_LOREM_PNG
            : BwdTestResources.TEST_IMG_FILE_PATH_STRUCT_COLOR_PNG;
    
    /**
     * Each logical pixel drawn over SCALE physical pixels,
     * to make issues more visible.
     */
    private static final int SCALE = 2;
    
    // Resized on first draw anyway.
    private static final int INITIAL_WIDTH = 100;
    private static final int INITIAL_HEIGHT = 100;
    private static final GPoint INITIAL_CLIENT_SPANS =
        GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);
    
    private static final double FIRST_DRAW_DELAY_S = 0.1;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceBwdFont font;
    
    private InterfaceBwdImage image;
    
    private boolean mustUseAccurateClientScaling = false;
    
    private boolean mustUseAccurateImageScaling = false;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ClientAndImageScalingBwdTestCase() {
    }

    public ClientAndImageScalingBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ClientAndImageScalingBwdTestCase(binding);
    }
    
    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }
    
    @Override
    public Integer getScaleElseNull() {
        return SCALE;
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
                resetClientSize();
            }
        }, FIRST_DRAW_DELAY_S);
    }
    
    /*
     * Mouse events.
     */

    @Override
    public void onMousePressed(BwdMouseEvent event) {
        super.onMousePressed(event);
        
        if (event.getButton() == BwdMouseButtons.PRIMARY) {
            this.resetClientSize();
        }
        
        if (event.getButton() == BwdMouseButtons.SECONDARY) {
            this.tryEnsureNextDrawingType();
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
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
        
        g.drawImage(box, image);
        
        /*
         * 
         */
        
        {
            InterfaceBwdFont font = this.font;
            if (font == null) {
                font = binding.getFontHome().newFontWithClosestHeight(TARGET_FONT_HEIGHT);
                this.font = font;
            }
            final int lineHeight = font.metrics().height() + 1;
            
            g.setColor(BwdColor.BLUE.withAlphaFp(0.25));
            g.setFont(font);
            
            int tmpX = box.x();
            int tmpY = box.y();

            g.drawText(tmpX, tmpY, "(left-click to reset client size)");
            tmpY += lineHeight;
            
            g.drawText(tmpX, tmpY, "(right-click to change scaling types)");
            tmpY += lineHeight;
            
            g.drawText(tmpX, tmpY,
                "accurate client scaling = "
                    + this.getAccurateClientScaling());
            tmpY += lineHeight;
            
            g.drawText(tmpX, tmpY,
                "accurate image scaling = "
                    + this.getAccurateImageScaling());
            tmpY += lineHeight;

            final GRect imgRect = image.getRect();

            g.drawText(tmpX, tmpY, "client scale = " + SCALE);
            tmpY += lineHeight;
            g.drawText(tmpX, tmpY, "xRatio = " + (box.xSpan() / (float) imgRect.xSpan()));
            tmpY += lineHeight;
            g.drawText(tmpX, tmpY, "yRatio = " + (box.ySpan() / (float) imgRect.ySpan()));
            tmpY += lineHeight;
        }
        
        /*
         * 
         */
        
        return null;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return Current value to use in this test.
     */
    private boolean getAccurateClientScaling() {
        return this.mustUseAccurateClientScaling;
    }
    
    /**
     * @return Current value to use in this test.
     */
    private boolean getAccurateImageScaling() {
        return this.mustUseAccurateImageScaling;
    }
    
    private InterfaceBwdImage getOrCreateImage() {
        InterfaceBwdImage image = this.image;
        if (image == null) {
            image = this.getBinding().newImage(IMG_FILE_PATH);
            this.image = image;
        }
        return image;
    }
    
    private void resetClientSize() {
        final InterfaceBwdHost host = this.getHost();
        GRect clientBounds = host.getClientBounds();
        if (clientBounds.isEmpty()) {
            // Can't.
            return;
        }
        
        final GRect imgRect = this.getOrCreateImage().getRect();
        
        // Be scale-aligned to avoid artifacts.
        clientBounds = clientBounds.withPos(
            (clientBounds.x() * SCALE) / SCALE,
            (clientBounds.y() * SCALE) / SCALE);
        
        // Image span.
        clientBounds = clientBounds.withSpans(
            imgRect.xSpan(),
            imgRect.ySpan());
        
        host.setClientBoundsSmart(clientBounds);
        
        host.ensurePendingClientPainting();
    }
    
    private void tryEnsureNextDrawingType() {
        final InterfaceBwdHost host = this.getHost();
        final GRect clientBounds = host.getClientBounds();
        if (clientBounds.isEmpty()) {
            // Can't.
            return;
        }
        
        if (this.mustUseAccurateClientScaling) {
            if (this.mustUseAccurateImageScaling) {
                // 3: (true,true) -> (false,true)
                this.mustUseAccurateClientScaling = false;
            } else {
                // 2: (true,false) -> (true,true)
                this.mustUseAccurateImageScaling = true;
            }
        } else {
            if (this.mustUseAccurateImageScaling) {
                // 4: (false,true) -> (false,false)
                this.mustUseAccurateImageScaling = false;
            } else {
                // 1: (false,false) -> (true,false)
                this.mustUseAccurateClientScaling = true;
            }
        }
        
        // Might need to set it before actual drawing.
        host.setAccurateClientScaling(this.getAccurateClientScaling());
        
        host.ensurePendingClientPainting();
    }
}
