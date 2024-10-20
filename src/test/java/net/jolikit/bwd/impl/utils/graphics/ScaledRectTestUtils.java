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

import junit.framework.TestCase;
import net.jolikit.bwd.api.graphics.Argb32;
import net.jolikit.bwd.api.graphics.GRect;

/**
 * Utils to test rectangles scaling.
 */
class ScaledRectTestUtils {
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static InterfaceColorTypeHelper getColorTypeHelper(boolean premul) {
        final InterfaceColorTypeHelper ret;
        if (premul) {
            ret = PremulArgbHelper.getInstance();
        } else {
            ret = NonPremulArgbHelper.getInstance();
        }
        return ret;
    }
    
    public static String toStringPixels(int[][] pixelArrArr) {
        final int height = pixelArrArr.length;
        if (height == 0) {
            return "[]";
        }
        final StringBuilder sb = new StringBuilder();
        final int width = pixelArrArr[0].length;
        for (int j = 0; j < height; j++) {
            sb.append("\n");
            final int[] pixelArr = pixelArrArr[j];
            for (int i = 0; i < width; i++) {
                if (i == 0) {
                    sb.append("[");
                } else {
                    sb.append(",");
                }
                sb.append(Argb32.toString(pixelArr[i]));
                if (i == width - 1) {
                    sb.append("]");
                }
            }
        }
        return sb.toString();
    }
    
    public static int computeMeanColor32(
        InterfaceColorTypeHelper colorTypeHelper,
        InterfaceSrcPixels pixels,
        GRect rect) {
        if (rect.isEmpty()) {
            throw new IllegalArgumentException("rect must not be empty");
        }
        /*
         * fp values are biased
         * (twice less fp span for 0 and 255),
         * so we do mean on int values.
         */
        long a8Sum = 0;
        long b8Sum = 0;
        long c8Sum = 0;
        long d8Sum = 0;
        for (int y = rect.y(); y <= rect.yMax(); y++) {
            for (int x = rect.x(); x <= rect.xMax(); x++) {
                final int color32 = pixels.getColor32At(x, y);
                /*
                 * Our interpolations are done in premul,
                 * so checking mean premul color.
                 */
                final int premulColor32 =
                    colorTypeHelper.asPremul32FromType(color32);
                a8Sum += Argb32.getAlpha8(premulColor32);
                b8Sum += Argb32.getRed8(premulColor32);
                c8Sum += Argb32.getGreen8(premulColor32);
                d8Sum += Argb32.getBlue8(premulColor32);
            }
        }
        final double divisor = 255.0 * rect.areaLong();
        final int premulColor32 = Argb32.toArgb32FromFp(
            a8Sum / divisor,
            b8Sum / divisor,
            c8Sum / divisor,
            d8Sum / divisor);
        return colorTypeHelper.asTypeFromPremul32(premulColor32);
    }
    
    public static void checkCloseArgb32Over(
        InterfaceSrcPixels expected,
        InterfaceSrcPixels actual,
        GRect checkArea,
        int cptDeltaTol) {
        
        final GRect rect = expected.getRect();
        TestCase.assertEquals(rect, actual.getRect());
        
        for (int y = checkArea.y(); y <= checkArea.yMax(); y++) {
            for (int x = checkArea.x(); x <= checkArea.xMax(); x++) {
                final int expectedColor32 = expected.getColor32At(x, y);
                final int actualColor32 = actual.getColor32At(x, y);
                checkCloseColor32(
                    expectedColor32,
                    actualColor32,
                    cptDeltaTol);
            }
        }
    }
    
    public static void checkCloseColor32(
        int expectedColor32,
        int actualColor32,
        int cptDeltaTol) {
        {
            final int ve = Argb32.getAlpha8(expectedColor32);
            final int va = Argb32.getAlpha8(actualColor32);
            TestCase.assertEquals(ve, va, cptDeltaTol);
        }
        {
            final int ve = Argb32.getRed8(expectedColor32);
            final int va = Argb32.getRed8(actualColor32);
            TestCase.assertEquals(ve, va, cptDeltaTol);
        }
        {
            final int ve = Argb32.getGreen8(expectedColor32);
            final int va = Argb32.getGreen8(actualColor32);
            TestCase.assertEquals(ve, va, cptDeltaTol);
        }
        {
            final int ve = Argb32.getBlue8(expectedColor32);
            final int va = Argb32.getBlue8(actualColor32);
            TestCase.assertEquals(ve, va, cptDeltaTol);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private ScaledRectTestUtils() {
    }
}
