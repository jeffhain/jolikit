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
package net.jolikit.bwd.impl.utils.graphics;

import net.jolikit.bwd.api.graphics.Argb32;

/**
 * Package-private.
 */
class PpColorSum {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private InterfaceColorTypeHelper colorTypeHelper;
    
    /*
     * Interpolating in premul, else RGB from low alpha pixels
     * would have same weight as RGB from high alpha pixels.
     */
    
    private double newestPremulA8;
    private double newestPremulB8;
    private double newestPremulC8;
    private double newestPremulD8;
    
    /**
     * In color type (not necessarily premul).
     */
    private int newestColor32 = 0;
    
    private double premulContribSumA8;
    private double premulContribSumB8;
    private double premulContribSumC8;
    private double premulContribSumD8;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Must be configured before use.
     */
    public PpColorSum() {
    }
    
    /**
     * To be called before use, with proper color type helper.
     */
    public void configure(InterfaceColorTypeHelper colorTypeHelper) {
        this.colorTypeHelper = colorTypeHelper;
        this.newestColor32 = 0;
        this.newestPremulA8 = 0;
        this.newestPremulB8 = 0;
        this.newestPremulC8 = 0;
        this.newestPremulD8 = 0;
        this.clearSum();
    }
    
    /**
     * Never clearing cached color: can be used
     * across multiple destination pixels.
     */
    public void clearSum() {
        this.premulContribSumA8 = 0.0;
        this.premulContribSumB8 = 0.0;
        this.premulContribSumC8 = 0.0;
        this.premulContribSumD8 = 0.0;
    }
    
    public void addFullPixelContrib(int toAddColor32) {
        if (toAddColor32 != this.newestColor32) {
            this.updateNewestColorData(toAddColor32);
        }
        this.premulContribSumA8 += this.newestPremulA8;
        this.premulContribSumB8 += this.newestPremulB8;
        this.premulContribSumC8 += this.newestPremulC8;
        this.premulContribSumD8 += this.newestPremulD8;
    }
    
    /**
     * @param ratio Covering ratio (in [0,1]).
     */
    public void addPixelContrib(int toAddColor32, double ratio) {
        if (toAddColor32 != this.newestColor32) {
            this.updateNewestColorData(toAddColor32);
        }
        this.premulContribSumA8 += ratio * this.newestPremulA8;
        this.premulContribSumB8 += ratio * this.newestPremulB8;
        this.premulContribSumC8 += ratio * this.newestPremulC8;
        this.premulContribSumD8 += ratio * this.newestPremulD8;
    }
    
    /**
     * Accumulated values must not cause resulting components
     * to be out of range (which might be the case with bicubic
     * for example, due to negative weights),
     * else must use toValidPremulColor32() instead.
     */
    public int toPremulColor32(double dstPixelSurfInSrcInv) {
        /*
         * Getting values back into [0.0,255.0]
         * by dividing by total dst pixel surf in src.
         */
        final double H = 0.5;
        final int a8 = (int) (this.premulContribSumA8 * dstPixelSurfInSrcInv + H);
        final int b8 = (int) (this.premulContribSumB8 * dstPixelSurfInSrcInv + H);
        final int c8 = (int) (this.premulContribSumC8 * dstPixelSurfInSrcInv + H);
        final int d8 = (int) (this.premulContribSumD8 * dstPixelSurfInSrcInv + H);
        return BindingColorUtils.toAbcd32_noCheck(a8, b8, c8, d8);
    }
    
    /**
     * Uses InterfaceColorTypeHelper.toValidPremul32()
     * to compute the resulting premul color
     * from rounded accumulated values.
     * Suited for algorithms using negative pixel weights,
     * such as bicubic.
     */
    public int toValidPremulColor32(
        InterfaceColorTypeHelper colorTypeHelper,
        double dstPixelSurfInSrcInv) {
        /*
         * Getting values _mostly_ back into [0.0,255.0]
         * by dividing by total dst pixel surf in src.
         * toValidPremul32() will take care
         * of eventual additional bounding.
         */
        final double H = 0.5;
        final int a8 = (int) (this.premulContribSumA8 * dstPixelSurfInSrcInv + H);
        final int b8 = (int) (this.premulContribSumB8 * dstPixelSurfInSrcInv + H);
        final int c8 = (int) (this.premulContribSumC8 * dstPixelSurfInSrcInv + H);
        final int d8 = (int) (this.premulContribSumD8 * dstPixelSurfInSrcInv + H);
        return colorTypeHelper.toValidPremul32(a8, b8, c8, d8);
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void updateNewestColorData(int toAddColor32) {
        final int toAddPremulColor32 =
            this.colorTypeHelper.asPremul32FromType(toAddColor32);
        this.newestPremulA8 = Argb32.getAlpha8(toAddPremulColor32);
        this.newestPremulB8 = Argb32.getRed8(toAddPremulColor32);
        this.newestPremulC8 = Argb32.getGreen8(toAddPremulColor32);
        this.newestPremulD8 = Argb32.getBlue8(toAddPremulColor32);
        this.newestColor32 = toAddColor32;
    }
}
