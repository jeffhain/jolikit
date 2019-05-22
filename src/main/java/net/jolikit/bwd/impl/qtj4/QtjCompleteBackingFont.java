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
package net.jolikit.bwd.impl.qtj4;

import net.jolikit.lang.LangUtils;

import com.trolltech.qt.gui.QFont;
import com.trolltech.qt.gui.QFontMetrics;

/**
 * Class to hold both QFont and/or QFontMetrics,
 * so that both can be disposed on font disposal.
 */
public final class QtjCompleteBackingFont {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final QFont backingFont;
    
    private final QFontMetrics backingMetrics;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param backingFont Must not be null.
     * @param backingMetrics Can be null (typically for temporary instances).
     */
    public QtjCompleteBackingFont(
            QFont backingFont,
            QFontMetrics backingMetrics) {
        this.backingFont = LangUtils.requireNonNull(backingFont);
        this.backingMetrics = backingMetrics;
    }
    
    @Override
    public String toString() {
        return "[" + this.backingFont + "," + this.backingMetrics + "]";
    }
    
    public QFont backingFont() {
        return this.backingFont;
    }
    
    /**
     * @return Can be null (typically for temporary instances).
     */
    public QFontMetrics backingMetrics() {
        return this.backingMetrics;
    }
    
    public void dispose() {
        if (this.backingMetrics != null) {
            this.backingMetrics.dispose();
        }
        
        this.backingFont.dispose();
    }
}
