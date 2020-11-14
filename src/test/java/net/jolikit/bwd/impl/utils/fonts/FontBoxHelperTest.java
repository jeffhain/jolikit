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
package net.jolikit.bwd.impl.utils.fonts;

import java.util.Arrays;
import java.util.TreeSet;

import junit.framework.TestCase;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.bwd.impl.utils.basics.BindingStringUtils;
import net.jolikit.bwd.test.utils.BwdTestResources;
import net.jolikit.lang.LangUtils;

public class FontBoxHelperTest extends TestCase {
    
    /*
     * We tests some formats that we don't support, to check that
     * the helper doesn't throw and just returns default values.
     * 
     * Font formats by decreasing popularity, according to
     * https://fileinfo.com/filetypes/font
     * (+ for those we test here):
     *+.TTF    TrueType Font   
     * .MF METAFONT File   
     *+.WOFF   Web Open Font Format File   
     * .FNT    Windows Font File   
     *+.OTF    OpenType Font   
     * .ABF    Adobe Binary Screen Font File   
     * .PCF    PaintCAD Font   
     * .FOT    Font Resource File  
     * .ETX    TeX Font Encoding File  
     * .ODTTF  Obfuscated OpenType Font    
     * .PFA    Printer Font ASCII File 
     *+.PFB    Printer Font Binary File    
     * .VNF    Vision Numeric Font 
     * .COMPOSITEFONT  Windows Composite Font File 
     * .PFM    Printer Font Metrics File   
     * .SFD    Spline Font Database File   
     * .FON    Generic Font File   
     * .GDR    Symbian OS Font File    
     * .VLW    Processing Font File    
     * .SFP    Soft Font Printer File  
     *+.TTC    TrueType Font Collection    
     * .ACFM   Adobe Composite Font Metrics File   
     * .AMFM   Adobe Multiple Font Metrics File    
     *+.BDF    Glyph Bitmap Distribution Format    
     * .DFONT  Mac OS X Data Fork Font 
     * .PFR    Portable Font Resource File 
     * .PMT    PageMaker Template File 
     * .T65    PageMaker Template File 
     * .TFM    TeX Font Metric File    
     * .VFB    FontLab Studio Font File    
     * .WOFF2  Web Open Font Format 2.0 File   
     * .XFN    Ventura Printer Font    
     * .GF METAFONT Bitmap File    
     * .CHR    Borland Character Set File  
     * .MXF    Maxis Font File 
     * .EOT    Embedded OpenType Font  
     *+.AFM    Adobe Font Metrics File 
     * .YTF    Google Picasa Font Cache    
     * .NFTR   Nintendo DS Font Type File  
     * .PK Packed METAFONT File    
     * .TTE    Private Character Editor File   
     * .CHA    Character Layout File   
     * .FFIL   Mac Font Suitcase   
     * .GXF    General CADD Pro Font File  
     * .LWFN   Adobe Type 1 Mac Font File  
     * .SUIT   Macintosh Font Suitcase 
     * .F3F    Crazy Machines Font File    
     * .TXF    Celestia Font Texture File  
     * .EUF    Private Character Editor File   
     * .MCF    Watchtower Library Font File    
     * .XFT    ChiWriter Printer Font  
     */

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public FontBoxHelperTest() {
    }
    
    public void test_otf() {
        final boolean canComputeCanDisplay = true;
        final boolean canComputeFontKind = true;
        test_xxx(
                BwdTestResources.TEST_FONT_FREE_MONO_OTF,
                canComputeCanDisplay,
                canComputeFontKind,
                new String[]{"free","mono"});
    }

    public void test_ttf() {
        final boolean canComputeCanDisplay = true;
        final boolean canComputeFontKind = true;
        test_xxx(
                BwdTestResources.TEST_FONT_FREE_MONO_TTF,
                canComputeCanDisplay,
                canComputeFontKind,
                new String[]{"free","mono"});
    }

    public void test_afm() {
        final boolean canComputeCanDisplay = true;
        final boolean canComputeFontKind = false;
        test_xxx(
                BwdTestResources.TEST_FONT_A010013L_AFM,
                canComputeCanDisplay,
                canComputeFontKind,
                new String[]{"a"});
    }

    public void test_ttc() {
        final boolean canComputeCanDisplay = true;
        final boolean canComputeFontKind = true;
        test_xxx(
                BwdTestResources.TEST_FONT_WQY_MICROHEI_TTC,
                canComputeCanDisplay,
                canComputeFontKind,
                new String[]{"micro","hei"});
    }
    
    /**
     * Both booleans can be false.
     */
    public static void test_xxx(
            String fontFilePath,
            boolean canComputeCanDisplay,
            boolean canComputeFontKind,
            String[] familyPartArr) {
        final FontBoxHelper helper = new FontBoxHelper(fontFilePath);
        try {
            if (DEBUG) {
                System.out.println();
                System.out.println(fontFilePath + ":");
            }
            
            final int fontCount = helper.getFontCount();
            if (DEBUG) {
                System.out.println("fontCount = " + fontCount);
            }
            if (canComputeCanDisplay
                    || canComputeFontKind) {
                assertTrue(fontCount > 0);
            } else {
                assertEquals(0, fontCount);
            }
            
            // Doing at least one round,
            // must not throw if could load,
            // and if could not must not throw either.
            final int bound = Math.max(1, fontCount);
            for (int fontIndex = 0; fontIndex < bound; fontIndex++) {
                final BwdFontKind fontKind = helper.computeFontKindElseNull(fontIndex);
                if (DEBUG) {
                    System.out.println(fontIndex + " : fontKind = " + fontKind);
                }
                if (canComputeFontKind) {
                    LangUtils.requireNonNull(fontKind);
                    for (String familyPart : familyPartArr) {
                        assertTrue(BindingStringUtils.containsIgnoreCase(fontKind.family(), familyPart));
                    }
                } else {
                    assertNull(fontKind);
                }
                
                final CodePointSet cps = helper.computeCodePointSetElseNull(fontIndex);
                if (DEBUG) {
                    System.out.println(fontIndex + " : cps = " + cps);
                }
                
                if (canComputeCanDisplay) {
                    // Should always be displayable with our fonts.
                    assertTrue(cps.contains((int) '1'));
                    assertFalse(cps.contains(BwdUnicode.MAX_10FFFF));
                } else {
                    assertNull(cps);
                }
            }
        } finally {
            helper.close();
        }
    }
    
    /*
     * 
     */
    
    public void test_toIntArr_Set() {
        final TreeSet<Integer> sortedSet = new TreeSet<Integer>();
        
        final int[] refArr = new int[]{1,2,3,5,7,11};
        for (int val : refArr) {
            sortedSet.add(val);
        }
        
        final int[] resArr = FontBoxHelper.toIntArr(sortedSet);
        assertEquals(Arrays.toString(refArr), Arrays.toString(resArr));
    }

    public void test_computeRangeCount_intArr() {
        {
            final int[] refArr = new int[]{};
            final int res = FontBoxHelper.computeRangeCount(refArr);
            assertEquals(0, res);
        }
        {
            final int[] refArr = new int[]{1};
            final int res = FontBoxHelper.computeRangeCount(refArr);
            assertEquals(1, res);
        }
        {
            final int[] refArr = new int[]{1,2,3};
            final int res = FontBoxHelper.computeRangeCount(refArr);
            assertEquals(1, res);
        }
        {
            final int[] refArr = new int[]{1,2,3, 5, 7, 11, 13,14};
            final int res = FontBoxHelper.computeRangeCount(refArr);
            assertEquals(5, res);
        }
    }
    
    public void test_toMinMaxCpArr_intArr() {
        {
            final int[] inputArr = new int[]{};
            final int[] refArr = new int[]{};
            final int[] resArr = FontBoxHelper.toMinMaxCpArr(inputArr);
            assertEquals(Arrays.toString(refArr), Arrays.toString(resArr));
        }
        {
            final int[] inputArr = new int[]{1};
            final int[] refArr = new int[]{1,1};
            final int[] resArr = FontBoxHelper.toMinMaxCpArr(inputArr);
            assertEquals(Arrays.toString(refArr), Arrays.toString(resArr));
        }
        {
            final int[] inputArr = new int[]{1,2,3};
            final int[] refArr = new int[]{1,3};
            final int[] resArr = FontBoxHelper.toMinMaxCpArr(inputArr);
            assertEquals(Arrays.toString(refArr), Arrays.toString(resArr));
        }
        {
            final int[] inputArr = new int[]{1,2,3, 5};
            final int[] refArr = new int[]{1,3, 5,5};
            final int[] resArr = FontBoxHelper.toMinMaxCpArr(inputArr);
            assertEquals(Arrays.toString(refArr), Arrays.toString(resArr));
        }
        {
            final int[] inputArr = new int[]{1,2,3, 5, 7, 11, 13,14};
            final int[] refArr = new int[]{1,3, 5,5, 7,7, 11,11, 13,14};
            final int[] resArr = FontBoxHelper.toMinMaxCpArr(inputArr);
            assertEquals(Arrays.toString(refArr), Arrays.toString(resArr));
        }
    }
}
