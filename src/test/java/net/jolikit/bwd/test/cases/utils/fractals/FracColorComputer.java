/*
 * Copyright 2020 Jeff Hain
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
package net.jolikit.bwd.test.cases.utils.fractals;

import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.impl.utils.graphics.BindingColorUtils;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;

public class FracColorComputer {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final int ALPHA_8 = 0xFF;

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * To avoid overflows in computations,
     * and should usually be much lower.
     */
    public static final int ITER_BITS_SIZE = 24;
    public static final int ITER_BITS_MASK = (-1 >>> (32 - ITER_BITS_SIZE));
    
    private int argb32ForIterMax;
    
    private FracColoring coloring;
    private int colorIterPeriod;
    private double colorIterPeriodInv;
    
    private int maxIter;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public FracColorComputer() {
        this.setArgb32ForIterMax(BwdColor.BLACK.toArgb32());
        this.setColoring(FracColoring.CYCLIC_RGB);
        this.setColorIterPeriod(0x80);
        this.setMaxIter(100);
    }

    public final void setArgb32ForIterMax(int argb32ForIterMax) {
        this.argb32ForIterMax = argb32ForIterMax;
    }

    public final void setColoring(FracColoring coloring) {
        this.coloring = LangUtils.requireNonNull(coloring);
    }
    
    public FracColoring getColoring() {
        return this.coloring;
    }

    /**
     * @param colorIterPeriod Must be in [1,256].
     */
    public final void setColorIterPeriod(int colorIterPeriod) {
        if ((colorIterPeriod < 1)
                || (colorIterPeriod > 256)) {
            throw new IllegalArgumentException("" + colorIterPeriod);
        }
        this.colorIterPeriod = colorIterPeriod;
        this.colorIterPeriodInv = 1.0/colorIterPeriod;
    }
    
    public int getColorPeriod() {
        return this.colorIterPeriod;
    }

    /**
     * @param maxIter Must be in [1,0xFFFFFF].
     *        Considered in fractal when iter >= maxIter.
     */
    public final void setMaxIter(int maxIter) {
        NbrsUtils.requireInRange(1, ITER_BITS_MASK, maxIter, "maxIter");
        this.maxIter = maxIter;
    }
    
    public int getMaxIter() {
        return this.maxIter;
    }

    public int iterToArgb32(int iter) {
        final boolean consideredIn = (iter >= this.maxIter);
        
        final int argb32;
        if (consideredIn) {
            argb32 = this.argb32ForIterMax;
        } else if (iter == 0) {
            // Fully transparent.
            argb32 = 0;
        } else {
            final FracColoring coloring = this.coloring;
            switch (coloring) {
                case CYCLIC_GREY: {
                    iter %= this.colorIterPeriod;
                    
                    final double maxIterDiv2 = (this.colorIterPeriod * 0.5);
                    final double maxIterDiv2Inv = (2.0 * this.colorIterPeriodInv);
                    
                    argb32 = iterToArgb32_GREY_2Cases(
                            ALPHA_8,
                            iter,
                            maxIterDiv2,
                            maxIterDiv2Inv);
                } break;
                case CYCLIC_RGB: {
                    iter %= this.colorIterPeriod;
                    
                    final double maxIterDiv3 = (this.colorIterPeriod * (1.0/3));
                    final double maxIterDiv3Inv = (3.0 * this.colorIterPeriodInv);
                    
                    argb32 = iterToArgb32_RGB_3Cases(
                            ALPHA_8,
                            iter,
                            maxIterDiv3,
                            maxIterDiv3Inv);
                } break;
                default:
                    throw new IllegalArgumentException("" + coloring);
            }
        }
        
        return argb32;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int iterToArgb32_GREY_2Cases(
            int alpha8,
            int iter,
            double maxIterDiv2,
            double maxIterDiv2Inv) {
        
        final double cptFp;
        if (iter < maxIterDiv2) {
            final double ratioFp = (iter * maxIterDiv2Inv);
            cptFp = 1.0 - ratioFp;
        } else {
            final double ratioFp = ((iter - maxIterDiv2) * maxIterDiv2Inv);
            cptFp = ratioFp;
        }
        
        final int cpt8 = BindingColorUtils.toInt8FromFp_noCheck(cptFp);
        return BindingColorUtils.toAbcd32_noCheck(alpha8, cpt8, cpt8, cpt8);
    }
    
    private static int iterToArgb32_RGB_3Cases(
            int alpha8,
            int iter,
            double maxIterDiv3,
            double maxIterDiv3Inv) {
        
        final double redFp;
        final double greenFp;
        final double blueFp;
        if (iter < maxIterDiv3) {
            final double ratioFp = (iter * maxIterDiv3Inv);
            redFp = 1.0 - ratioFp;
            greenFp = ratioFp;
            blueFp = 0.0;
        } else {
            final double twoMaxIterDiv3 = (maxIterDiv3 + maxIterDiv3);
            if (iter < twoMaxIterDiv3) {
                final double ratioFp = ((iter - maxIterDiv3) * maxIterDiv3Inv);
                redFp = 0.0;
                greenFp = 1.0 - ratioFp;
                blueFp = ratioFp;
            } else {
                final double ratioFp = ((iter - twoMaxIterDiv3) * maxIterDiv3Inv);
                redFp = ratioFp;
                greenFp = 0.0;
                blueFp = 1.0 - ratioFp;
            }
        }
        
        final int red8 = BindingColorUtils.toInt8FromFp_noCheck(redFp);
        final int green8 = BindingColorUtils.toInt8FromFp_noCheck(greenFp);
        final int blue8 = BindingColorUtils.toInt8FromFp_noCheck(blueFp);
        return BindingColorUtils.toAbcd32_noCheck(alpha8, red8, green8, blue8);
    }
}
