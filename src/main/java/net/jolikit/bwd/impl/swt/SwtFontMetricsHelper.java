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
package net.jolikit.bwd.impl.swt;

import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.lang.LangUtils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Helper class to compute text width.
 * 
 * Should use a single instance per binding instance,
 * because it uses some relatively heavy resources,
 * that would be overkill to create for each font.
 * 
 * TODO swt Extremely slow, but can't seem to do better.
 */
public class SwtFontMetricsHelper {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final GC gForMetrics;
    
    private final Shell shellForMetrics;
    private final Text textForMetrics;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public SwtFontMetricsHelper(Display display) {
        LangUtils.requireNonNull(display);
        
        this.gForMetrics = new GC(display);
        
        this.shellForMetrics = new Shell(display);
        
        // Other values: SWT.MULTI, SWT.BORDER, SWT.WRAP.
        this.textForMetrics = new Text(this.shellForMetrics, SWT.SINGLE);
    }
    
    public GC getGForMetrics() {
        return this.gForMetrics;
    }
    
    public FontMetrics newBackingMetrics(Font backingFont) {
        final FontMetrics backingFontMetrics;
        synchronized (gForMetrics) {
            gForMetrics.setFont(backingFont);
            backingFontMetrics = gForMetrics.getFontMetrics();
            gForMetrics.setFont(null);
        }
        return backingFontMetrics;
    }
    
    public int computeTextWidth(Font font, String text) {
        final int width;
        synchronized (this.textForMetrics) {
            this.gForMetrics.setFont(font);
            final Point extent2D = this.gForMetrics.textExtent(text, SWT.NONE);
            width = extent2D.x;

            /*
             * TODO swt Sticking to textExtent(...),
             * because both getCharWidth(...) and getAdvanceWidth(...)
             * are inconsistent with it.
             */
            
            if (false
                    && (text.length() != 0)) {
                int sum = 0;
                int ci = 0;
                final StringBuilder sb = new StringBuilder();
                while (ci < text.length()) {
                    final int cp = text.codePointAt(ci);
                    
                    sb.append(" ");
                    sb.append(BwdUnicode.toDisplayString(cp));
                    
                    final int cpWidth;
                    if (cp <= BwdUnicode.MAX_FFFF) {
                        if (true) {
                            /*
                             * Returns the width of the specified character
                             * in the font selected into the receiver.
                             */
                            cpWidth = this.gForMetrics.getCharWidth((char) cp);
                        } else {
                            /*
                             * Returns the advance width of the specified character
                             * in the font which is currently selected into the receiver.
                             */
                            cpWidth = this.gForMetrics.getAdvanceWidth((char) cp);
                        }
                    } else {
                        cpWidth = 0;
                    }
                    sum += cpWidth;
                    ci += Character.charCount(cp);
                }
                if (sum != width) {
                    throw new AssertionError(sum + " != " + width + ", code points = " + sb.toString());
                }
            }

            // Removing references.
            this.gForMetrics.setFont(null);
            this.textForMetrics.setFont(null);
            this.textForMetrics.setText("");
        }
        return width;
    }
    
    public void dispose() {
        this.gForMetrics.dispose();
        
        this.textForMetrics.dispose();
        this.shellForMetrics.dispose();
    }
}
