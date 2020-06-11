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
package net.jolikit.bwd.impl.utils.graphics;

import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.Argb64;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.impl.utils.basics.BindingBasicsUtils;

/**
 * Utilities for blending:
 * - "source over" only (cf. InterfaceBwdGraphics javadoc).
 * - assuming destination color is fully opaque (background color
 *   is always opaque, even if window itself has an alpha,
 *   cf. InterfaceBwdGraphics javadoc).
 * 
 * Convention for color values naming:
 * - {_,_,_,_} : abcd
 * - {alpha,_,_,_} : axyz
 * - {_,_,_,alpha} : xyza
 */
public class BindingColorUtils {
    
    /*
     * Literature:
     * https://www.cs.princeton.edu/courses/archive/fall00/cs426/papers/smith95a.pdf
     * https://www.slideshare.net/Mark_Kilgard/blend-modes-for-opengl
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    /**
     * TODO optim Set it to false for a small performance improvement,
     * if wanting to trust binding code.
     */
    private static final boolean AZZERTIONS = true;
    
    /**
     * True because our backgrounds are always fully opaque
     * (except due to window transparency, but it applies after client painting).
     */
    static final boolean CAN_ASSUME_THAT_DST_ALPHA_8_IS_255 = true;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static boolean isOpaque(int argb32, BwdColor colorElseNull) {
        final boolean ret;
        if (colorElseNull != null) {
            final long argb64 = colorElseNull.toArgb64();
            ret = Argb64.isOpaque(argb64);
        } else {
            ret = Argb32.isOpaque(argb32);
        }
        return ret;
    }
    
    /**
     * @param a8 Must be in [0,255].
     * @param b8 Must be in [0,255].
     * @param c8 Must be in [0,255].
     * @param d8 Must be in [0,255].
     * @return The corresponding ABCD32 color.
     */
    public static int toAbcd32_noCheck(int a8, int b8, int c8, int d8) {
        int i = a8;
        i <<= 8;
        i |= b8;
        i <<= 8;
        i |= c8;
        i <<= 8;
        i |= d8;
        return i;
    }

    public static int toArgb32FromPixelWithMasksAndShifts(
            int pixel,
            int aMask, int rMask, int gMask, int bMask,
            int aShift, int rShift, int gShift, int bShift) {
        final int alpha8;
        if (aMask == 0) {
            // "no alpha" in formats means always opaque
            // (always transparent would be pointless).
            alpha8 = 0xFF;
        } else {
            alpha8 = ((pixel & aMask) >>> aShift);
        }
        final int red8 = ((pixel & rMask) >>> rShift);
        final int green8 = ((pixel & gMask) >>> gShift);
        final int blue8 = ((pixel & bMask) >>> bShift);
        
        return toAbcd32_noCheck(alpha8, red8, green8, blue8);
    }

    /**
     * @param cptFp Must be in [0,1].
     * @return The corresponding integer value in [0,255].
     */
    public static int toInt8FromFp_noCheck(double cptFp) {
        return toInt8FromFp255_noCheck(cptFp * 255);
    }

    /**
     * @param cpt8 Must be in [0,255].
     * @return The corresponding floating-point value in [0,1].
     */
    public static double toFpFromInt8_noCheck(int cpt8) {
        return cpt8 * (1.0/255);
    }
    
    /**
     * @param axyz32 An AXYZ32 color, i.e. with alpha in MSByte.
     * @param alpha8 Must be in [0,255].
     * @return The color with the XYZ components of the specified color
     *         and the specified alpha.
     */
    public static int withAlpha8_noCheck(int axyz32, int alpha8) {
        final int xyz24 = (axyz32 & 0xFFFFFF);
        return (alpha8 << 24) | xyz24;
    }

    /**
     * @param cpt8 Must be in [0,255].
     * @param alphaFp Must be in [0,1].
     * @return The corresponding premultiplied component, in [0,255].
     */
    public static int toPremul8_noCheck(int cpt8, double alphaFp) {
        return toInt8FromFp255_noCheck(alphaFp * cpt8);
    }
    
    /**
     * @param premulCpt8 Must be in [0,255].
     * @param oneOverAlphaFp Must be in [0,255/premulCpt8].
     * @return The corresponding non-premultiplied component.
     */
    public static int toNonPremul8_noCheck(int premulCpt8, double oneOverAlphaFp) {
        return toInt8FromFp255_noCheck(premulCpt8 * oneOverAlphaFp);
    }
    
    /*
     * Conversions between ARGB, and ABGR or RGBA.
     * These two last formats are useful to deal with
     * OpenGL libraries in big or little endian.
     */
    
    public static int toArgb32FromAbgr32(int abgr32) {
        // A_G_ | _R__ | ___B
        return (abgr32 & 0xFF00FF00) | ((abgr32 & 0x00000FF) << 16) | ((abgr32 & 0x00FF0000) >>> 16);
    }
    
    public static int toAbgr32FromArgb32(int argb32) {
        // A_G_ | _B__ | ___R
        return (argb32 & 0xFF00FF00) | ((argb32 & 0x00000FF) << 16) | ((argb32 & 0x00FF0000) >>> 16);
    }
    
    public static int toArgb32FromRgba32(int rgba32) {
        // A___ | _RGB
        return (rgba32 << 24) | (rgba32 >>> 8);
    }
    
    public static int toRgba32FromArgb32(int argb32) {
        // ___A | RGB_
        return (argb32 >>> 24) | (argb32 << 8);
    }
    
    /*
     * More utilities for OpenGL libraries, or other libraries
     * using native RGBA format.
     */
    
    public static int toPremulNativeRgba32FromArgb32(int argb32) {
        if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
            // ARGB, in big (Java), to ABGR,
            // which will give RGBA in little (native).
            final int color32 = toAbgr32FromArgb32(argb32);
            final int premulColor32 = toPremulAxyz32(color32);
            return premulColor32;
        } else {
            // ARGB, in big (Java), to RGBA, in big (native).
            final int color32 = toRgba32FromArgb32(argb32);
            final int premulColor32 = toPremulXyza32(color32);
            return premulColor32;
        }
    }
    
    public static int toArgb32FromPremulNativeRgba32(int premulNativeRgba32) {
        if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
            final int color32 = toNonPremulAxyz32(premulNativeRgba32);
            // ABGR, which gives RGBA in native, to ARGB.
            return toArgb32FromAbgr32(color32);
        } else {
            final int color32 = toNonPremulXyza32(premulNativeRgba32);
            // RGBA to ARGB.
            return toArgb32FromRgba32(color32);
        }
    }

    public static int toNativeRgba32FromArgb32(int argb32) {
        if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
            // ARGB, in big (Java), to ABGR,
            // which will give RGBA in little (native).
            return toAbgr32FromArgb32(argb32);
        } else {
            // ARGB, in big (Java), to RGBA, in big (native).
            return toRgba32FromArgb32(argb32);
        }
    }
    
    public static int toArgb32FromNativeRgba32(int nativeRgba32) {
        if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
            // ABGR, which gives RGBA in native, to ARGB.
            return toArgb32FromAbgr32(nativeRgba32);
        } else {
            // RGBA to ARGB.
            return toArgb32FromRgba32(nativeRgba32);
        }
    }

    public static int toPremulNativeRgba32(int nativeRgba32) {
        if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
            return toPremulAxyz32(nativeRgba32);
        } else {
            return toPremulXyza32(nativeRgba32);
        }
    }

    public static int toInvertedPremulNativeRgba32(int premulNativeRgba32) {
        if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
            return toInvertedPremulAxyz32_noCheck(premulNativeRgba32);
        } else {
            return toInvertedPremulXyza32_noCheck(premulNativeRgba32);
        }
    }
    
    /**
     * @param nativeRgba32 Can be premul or not.
     */
    public static int getNativeRgba32Alpha8(int nativeRgba32) {
        if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
            // Argb
            return Argb32.getAlpha8(nativeRgba32);
        } else {
            // rgbA
            return Argb32.getBlue8(nativeRgba32);
        }
    }
    
    public static int blendPremulNativeRgba32(
            int srcPremulNativeRgba32,
            int dstPremulNativeRgba32) {
        if (BindingBasicsUtils.NATIVE_IS_LITTLE) {
            return blendPremulAxyz32_srcOver(
                    srcPremulNativeRgba32,
                    dstPremulNativeRgba32);
        } else {
            return blendPremulXyza32_srcOver(
                    srcPremulNativeRgba32,
                    dstPremulNativeRgba32);
        }
    }

    /*
     * Non premul color to premul color.
     */

    /**
     * @param axyz32 An AXYZ32 color, not alpha-premultiplied.
     * @return The corresponding AXYZ32 color, alpha-premultiplied.
     */
    public static int toPremulAxyz32(int axyz32) {
        final int alpha8 = Argb32.getAlpha8(axyz32);
        if (alpha8 == 0xFF) {
            return axyz32;
        }
        
        int x8 = Argb32.getRed8(axyz32);
        int y8 = Argb32.getGreen8(axyz32);
        int z8 = Argb32.getBlue8(axyz32);
        
        final double alphaFp = toFpFromInt8_noCheck(alpha8);
        
        x8 = toPremul8_noCheck(x8, alphaFp);
        y8 = toPremul8_noCheck(y8, alphaFp);
        z8 = toPremul8_noCheck(z8, alphaFp);
        
        if (AZZERTIONS) {
            if ((x8 > alpha8)
                    || (y8 > alpha8)
                    || (z8 > alpha8)) {
                throw new AssertionError(
                        Argb32.toString(axyz32)
                        + " -> premul axyz = "
                        + alpha8 + ", " + x8 + ", " + y8 + ", " + z8);
            }
        }
        
        final int premulAxyz32 = toAbcd32_noCheck(alpha8, x8, y8, z8);
        
        return premulAxyz32;
    }

    /**
     * @param xyza32 An XYZA32 color, not alpha-premultiplied.
     * @return The corresponding XYZA32 color, alpha-premultiplied.
     */
    public static int toPremulXyza32(int xyza32) {
        final int alpha8 = Argb32.getBlue8(xyza32);
        if (alpha8 == 0xFF) {
            return xyza32;
        }
        
        int x8 = Argb32.getAlpha8(xyza32);
        int y8 = Argb32.getRed8(xyza32);
        int z8 = Argb32.getGreen8(xyza32);
        
        final double alphaFp = toFpFromInt8_noCheck(alpha8);
        
        x8 = toPremul8_noCheck(x8, alphaFp);
        y8 = toPremul8_noCheck(y8, alphaFp);
        z8 = toPremul8_noCheck(z8, alphaFp);

        if (AZZERTIONS) {
            if ((x8 > alpha8)
                    || (y8 > alpha8)
                    || (z8 > alpha8)) {
                throw new AssertionError(
                        Argb32.toString(xyza32)
                        + " -> premul xyza = "
                        + x8 + ", " + y8 + ", " + z8 + ", " + alpha8);
            }
        }

        return toAbcd32_noCheck(x8, y8, z8, alpha8);
    }
    
    /*
     * Premul color to non premul color.
     */

    /**
     * @param axyz32 An AXYZ32 color, alpha-premultiplied.
     * @return The corresponding AXYZ32 color, not alpha-premultiplied.
     */
    public static int toNonPremulAxyz32(int premulAxyz32) {
        final int alpha8 = Argb32.getAlpha8(premulAxyz32);
        if (alpha8 == 0xFF) {
            return premulAxyz32;
        }
        
        int x8 = Argb32.getRed8(premulAxyz32);
        int y8 = Argb32.getGreen8(premulAxyz32);
        int z8 = Argb32.getBlue8(premulAxyz32);
        
        if (AZZERTIONS) {
            if ((x8 > alpha8)
                    || (y8 > alpha8)
                    || (z8 > alpha8)) {
                throw new AssertionError(
                        "premul axyz = "
                                + alpha8 + ", " + x8 + ", " + y8 + ", " + z8);
            }
        }

        // Avoiding division by 0.
        if (alpha8 == 0) {
            // Must be (0,0,0,0) in premul.
            return 0;
        }
        
        final double alphaFp = toFpFromInt8_noCheck(alpha8);
        final double oneOverAlphaFp = 1.0/alphaFp;
        
        x8 = toNonPremul8_noCheck(x8, oneOverAlphaFp);
        y8 = toNonPremul8_noCheck(y8, oneOverAlphaFp);
        z8 = toNonPremul8_noCheck(z8, oneOverAlphaFp);
        
        return toAbcd32_noCheck(alpha8, x8, y8, z8);
    }
    
    /**
     * @param xyza32 An XYZA32 color, alpha-premultiplied.
     * @return The corresponding XYZA32 color, not alpha-premultiplied.
     */
    public static int toNonPremulXyza32(int premulXyza32) {
        final int alpha8 = Argb32.getBlue8(premulXyza32);
        if (alpha8 == 0xFF) {
            return premulXyza32;
        }
        
        int x8 = Argb32.getAlpha8(premulXyza32);
        int y8 = Argb32.getRed8(premulXyza32);
        int z8 = Argb32.getGreen8(premulXyza32);
        
        if (AZZERTIONS) {
            if ((x8 > alpha8)
                    || (y8 > alpha8)
                    || (z8 > alpha8)) {
                throw new AssertionError(
                        "premul xyza = " + x8
                        + ", " + y8
                        + ", " + z8
                        + ", " + alpha8);
            }
        }

        // Avoiding division by 0.
        if (alpha8 == 0) {
            // Must be (0,0,0,0) in premul.
            return 0;
        }
        
        final double alphaFp = toFpFromInt8_noCheck(alpha8);
        final double oneOverAlphaFp = 1.0/alphaFp;
        
        x8 = toNonPremul8_noCheck(x8, oneOverAlphaFp);
        y8 = toNonPremul8_noCheck(y8, oneOverAlphaFp);
        z8 = toNonPremul8_noCheck(z8, oneOverAlphaFp);
        
        return toAbcd32_noCheck(x8, y8, z8, alpha8);
    }
    
    /*
     * Alpha-premultiplied color inverting.
     */

    /**
     * @param axyz32 An AXYZ32 color, alpha-premultiplied.
     * @return The alpha-premultiplied color with each non-alpha component
     *         replaced with its symmetrical from [0,alpha8] center
     *         (for example 0 becomes alpha8, 1 becomes alpha8-1, etc.).
     */
    public static int toInvertedPremulAxyz32_noCheck(int premulAxyz32) {
        final int alpha8 = Argb32.getAlpha8(premulAxyz32);
        
        final int resPremulX8 = alpha8 - Argb32.getRed8(premulAxyz32);
        final int resPremulY8 = alpha8 - Argb32.getGreen8(premulAxyz32);
        final int resPremulZ8 = alpha8 - Argb32.getBlue8(premulAxyz32);
        
        if (AZZERTIONS) {
            if ((resPremulX8|resPremulY8|resPremulZ8) < 0) {
                throw new IllegalArgumentException("not premultiplied axyz : " + Argb32.toString(premulAxyz32));
            }
        }
        
        final int resPremulAxyz32 = toAbcd32_noCheck(alpha8, resPremulX8, resPremulY8, resPremulZ8);
        return resPremulAxyz32;
    }

    /**
     * @param xyza32 An XYZA32 color, alpha-premultiplied.
     * @return The alpha-premultiplied color with each non-alpha component
     *         replaced with its symmetrical from [0,alpha8] center
     *         (for example 0 becomes alpha8, 1 becomes alpha8-1, etc.).
     */
    public static int toInvertedPremulXyza32_noCheck(int premulXyza32) {
        final int alpha8 = Argb32.getBlue8(premulXyza32);
        
        final int resPremulX8 = alpha8 - Argb32.getAlpha8(premulXyza32);
        final int resPremulY8 = alpha8 - Argb32.getRed8(premulXyza32);
        final int resPremulZ8 = alpha8 - Argb32.getGreen8(premulXyza32);

        if (AZZERTIONS) {
            if ((resPremulX8|resPremulY8|resPremulZ8) < 0) {
                throw new IllegalArgumentException("not premultiplied xyza : " + Argb32.toString(premulXyza32));
            }
        }

        final int resPremulXyza32 = toAbcd32_noCheck(resPremulX8, resPremulY8, resPremulZ8, alpha8);
        return resPremulXyza32;
    }
    
    /*
     * Blending.
     * 
     * NB: If using these treatments in a loop, with always the same
     * source color, you can optimize for the case where the destination color
     * doesn't change often, by only computing blended color
     * when destination color changes.
     */

    /**
     * @param srcPremulAxyz32 The color added over the previous color, alpha pre-multiplied.
     * @param dstPremulAxyz32 The initial color, alpha pre-multiplied.
     * @return The resulting color, alpha pre-multiplied.
     */
    public static int blendPremulAxyz32_srcOver(int srcPremulAxyz32, int dstPremulAxyz32) {
        final int srcAlpha8 = Argb32.getAlpha8(srcPremulAxyz32);
        if (srcAlpha8 == 0xFF) {
            // dst fully erased.
            return srcPremulAxyz32;
        }

        if (srcAlpha8 == 0) {
            /*
             * dst not modified.
             * Could end up here even with smart users, for example if
             * painting an image with transparent pixels, and optimizing
             * this case away doesn't cost much, so it's worth it.
             */
            return dstPremulAxyz32;
        }

        return blendPremulAxyz32_srcOver_heavy(srcPremulAxyz32, srcAlpha8, dstPremulAxyz32);
    }

    /**
     * @param srcPremulXyza32 The color added over the previous color, alpha pre-multiplied.
     * @param dstPremulXyza32 The initial color, alpha pre-multiplied.
     * @return The resulting color, alpha pre-multiplied.
     */
    public static int blendPremulXyza32_srcOver(int srcPremulXyza32, int dstPremulXyza32) {
        
        /*
         * Same logic as for Axyz case, but with Xyza components order.
         */
        
        final int srcAlpha8 = Argb32.getBlue8(srcPremulXyza32);
        if (srcAlpha8 == 0xFF) {
            // dst fully erased.
            return srcPremulXyza32;
        }

        if (srcAlpha8 == 0) {
            // dst not modified.
            return dstPremulXyza32;
        }

        return blendPremulXyza32_srcOver_heavy(srcPremulXyza32, srcAlpha8, dstPremulXyza32);
    }

    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------

    /**
     * @param v1 A value in [0,255].
     * @param v2 A value in [0,255].
     * @return The integer value closest to mathematical (v1 * v2) / 255,
     *         which is also in [0,255].
     */
    static int INT_MULT_0_255(int a, int b) {
        /*
         * We want to compute:
         * result = round((a * b) / 255)
         *        = round(prod / 255)
         * 
         * For |r| < 1, most accurate for r small:
         * a/(1-r) = a + a*r + a*r^2 + ...
         * so, with r = 1/256:
         * a/(1-1/256) = a + a/256 + a/256^2 + ...
         *             ~= a + a/256 + a/256^2
         * so
         * 256*a/(256-1) ~= a + a/256 + a/256^2
         * 256*a/255 ~= a + a/256 + a/256^2
         * a/255 ~= a/256 + a/256^2 + a/256^3
         *       ~= a>>8 + a>>16 + a>>24
         *       ~= (((((a >> 8) + a) >> 8) + a) >> 8)
         * 
         * We also add 128 (= 0x80) before each division,
         * to use rounding instead of floor,
         * and improve things up to the point that
         * we can stick to two divisions.
         */
        final int t = a * b + 0x80;
        return (((t >> 8) + t) >> 8);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private BindingColorUtils() {
    }
    
    /*
     * 
     */
    
    /**
     * @param valueFp255 Must be in [0,255].
     * @return The corresponding integer value in [0,255].
     */
    private static int toInt8FromFp255_noCheck(double valueFp255) {
        return (int) (valueFp255 + 0.5);
    }
    
    /*
     * Isolating heavy computations in specific methods,
     * not to incur their memory overhead when only doing opaque painting.
     */
    
    private static int blendPremulAxyz32_srcOver_heavy(
            int srcPremulAxyz32,
            int srcAlpha8,
            int dstPremulAxyz32) {
        
        final int dstAlpha8;
        if (CAN_ASSUME_THAT_DST_ALPHA_8_IS_255) {
            dstAlpha8 = 0xFF;
        } else {
            dstAlpha8 = Argb32.getAlpha8(dstPremulAxyz32);
            if (dstAlpha8 == 0) {
                // dst fully erased.
                return srcPremulAxyz32;
            }
        }

        /*
         * SRC_OVER blending formula:
         * c1 = destination color (drawing over it)
         * c2 = source color (drawing with it)
         * c3 = resulting color, or composite color
         * ===> alpha3 * c3 = alpha2 * c2 + (1 - alpha2) * alpha1 * c1
         *      and, with acx = alphax * cx:
         *      ac3 = ac2 + (1 - alpha2) * ac1
         * ===> As for resulting alpha, alpha3:
         *      alpha3 = alpha2 + (1 - alpha2) * alpha1
         * 
         * resAlpha * resC = srcAlpha * srcC + (1 - srcAlpha) * dstAlpha * dstC
         * resAlpha = srcAlpha + (1 - srcAlpha) * dstAlpha
         */

        final int srcPremulX8 = Argb32.getRed8(srcPremulAxyz32);
        final int srcPremulY8 = Argb32.getGreen8(srcPremulAxyz32);
        final int srcPremulZ8 = Argb32.getBlue8(srcPremulAxyz32);

        final int dstPremulX8 = Argb32.getRed8(dstPremulAxyz32);
        final int dstPremulY8 = Argb32.getGreen8(dstPremulAxyz32);
        final int dstPremulZ8 = Argb32.getBlue8(dstPremulAxyz32);

        /*
         * resAlpha_0_1 * resC = srcAlpha_0_1 * srcC + (1 - srcAlpha_0_1) * dstAlpha_0_1 * dstC
         * resPremulC = srcPremulC + (1 - srcAlpha_0_1) * dstPremulC
         * 
         * resAlpha_0_255/255 * resC = srcAlpha_0_255/255 * srcC + (1 - srcAlpha_0_255/255) * dstAlpha_0_255/255 * dstC
         * resAlpha_0_255 * resC = srcAlpha_0_255 * srcC + (1 - srcAlpha_0_255/255) * dstAlpha_0_255 * dstC
         */

        final int srcAlpha8Cmpl = 255 - srcAlpha8;
        final int resPremulX8 = srcPremulX8 + INT_MULT_0_255(srcAlpha8Cmpl, dstPremulX8);
        final int resPremulY8 = srcPremulY8 + INT_MULT_0_255(srcAlpha8Cmpl, dstPremulY8);
        final int resPremulZ8 = srcPremulZ8 + INT_MULT_0_255(srcAlpha8Cmpl, dstPremulZ8);

        /*
         * 
         */

        final int resAlpha8;
        if (CAN_ASSUME_THAT_DST_ALPHA_8_IS_255) {
            resAlpha8 = 0xFF;
        } else {
            resAlpha8 = srcAlpha8 + INT_MULT_0_255(srcAlpha8Cmpl, dstAlpha8);
            if (AZZERTIONS) {
                if ((resPremulX8 > resAlpha8)
                        || (resPremulY8 > resAlpha8)
                        || (resPremulZ8 > resAlpha8)) {
                    throw new AssertionError(
                            "premul axyz = " + resAlpha8
                            + ", " + resPremulX8
                            + ", " + resPremulY8
                            + ", " + resPremulZ8);
                }
            }
        }

        final int resPremulAxyz32 = toAbcd32_noCheck(resAlpha8, resPremulX8, resPremulY8, resPremulZ8);
        return resPremulAxyz32;
    }

    private static int blendPremulXyza32_srcOver_heavy(
            int srcPremulXyza32,
            int srcAlpha8,
            int dstPremulXyza32) {
        
        final int dstAlpha8;
        if (CAN_ASSUME_THAT_DST_ALPHA_8_IS_255) {
            dstAlpha8 = 0xFF;
        } else {
            dstAlpha8 = Argb32.getBlue8(dstPremulXyza32);
            if (dstAlpha8 == 0) {
                // dst fully erased.
                return srcPremulXyza32;
            }
        }

        final int srcPremulX8 = Argb32.getAlpha8(srcPremulXyza32);
        final int srcPremulY8 = Argb32.getRed8(srcPremulXyza32);
        final int srcPremulZ8 = Argb32.getGreen8(srcPremulXyza32);

        final int dstPremulX8 = Argb32.getAlpha8(dstPremulXyza32);
        final int dstPremulY8 = Argb32.getRed8(dstPremulXyza32);
        final int dstPremulZ8 = Argb32.getGreen8(dstPremulXyza32);

        final int srcAlpha8Cmpl = 255 - srcAlpha8;
        final int resPremulX8 = srcPremulX8 + INT_MULT_0_255(srcAlpha8Cmpl, dstPremulX8);
        final int resPremulY8 = srcPremulY8 + INT_MULT_0_255(srcAlpha8Cmpl, dstPremulY8);
        final int resPremulZ8 = srcPremulZ8 + INT_MULT_0_255(srcAlpha8Cmpl, dstPremulZ8);

        final int resAlpha8;
        if (CAN_ASSUME_THAT_DST_ALPHA_8_IS_255) {
            resAlpha8 = 0xFF;
        } else {
            resAlpha8 = srcAlpha8 + INT_MULT_0_255(srcAlpha8Cmpl, dstAlpha8);
            if (AZZERTIONS) {
                if ((resPremulX8 > resAlpha8)
                        || (resPremulY8 > resAlpha8)
                        || (resPremulZ8 > resAlpha8)) {
                    throw new AssertionError(
                            "premul xyza = " + resPremulX8
                            + ", " + resPremulY8
                            + ", " + resPremulZ8
                            + ", " + resAlpha8);
                }
            }
        }

        final int resPremulXyza32 = toAbcd32_noCheck(resPremulX8, resPremulY8, resPremulZ8, resAlpha8);
        return resPremulXyza32;
    }
}
