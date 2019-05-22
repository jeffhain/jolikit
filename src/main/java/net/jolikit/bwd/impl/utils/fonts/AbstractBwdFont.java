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
package net.jolikit.bwd.impl.utils.fonts;

import java.util.concurrent.atomic.AtomicBoolean;

import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.utils.BwdUnicode;
import net.jolikit.lang.LangUtils;

/**
 * Optional class to make it easier to implement InterfaceBwdFont
 * and use it in binding implementations.
 * 
 * @param BF The class for backing fonts.
 */
public abstract class AbstractBwdFont<BF> implements InterfaceBwdFont {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * For fonts from different homes to never be equal to each other.
     */
    private final int homeId;
    
    private final BwdFontId fontId;
    
    private final InterfaceCanFontDisplayComputer canFontDisplayComputer;
    
    /**
     * Can be null.
     */
    private final InterfaceFontDisposeCallListener disposeCallListener;
    
    private final AtomicBoolean disposedRef = new AtomicBoolean();
    
    private final BF backingFont;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param homeId Id to identify the home this font comes from.
     * @param fontId Must not be null.
     * @param canFontDisplayComputer Must not be null.
     * @param disposeCallListener Can be null. Must be null for home-owned fonts.
     * @param backingFont Must not be null.
     */
    public AbstractBwdFont(
            int homeId,
            BwdFontId fontId,
            InterfaceCanFontDisplayComputer canFontDisplayComputer,
            InterfaceFontDisposeCallListener disposeCallListener,
            BF backingFont) {
        this.homeId = homeId;
        this.fontId = LangUtils.requireNonNull(fontId);
        this.canFontDisplayComputer = LangUtils.requireNonNull(canFontDisplayComputer);
        this.disposeCallListener = disposeCallListener;
        this.backingFont = LangUtils.requireNonNull(backingFont);
    }
    
    /*
     * 
     */
    
    @Override
    public String toString() {
        return "["
                + this.fontId
                + ", "
                + this.fontMetrics()
                + "]";
    }
    
    @Override
    public int hashCode() {
        // Not bothering to take care of homeId,
        // since in practice fonts used together
        // should be from a same home.
        return this.fontId.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if ((obj == null)
                || (obj.getClass() != this.getClass())) {
            return false;
        }
        final AbstractBwdFont<?> other = (AbstractBwdFont<?>) obj;
        return (this.homeId == other.homeId) 
                && this.fontId.equals(other.fontId);
    }
    
    /*
     * 
     */

    @Override
    public BwdFontId fontId() {
        return this.fontId;
    }

    @Override
    public BwdFontKind fontKind() {
        return this.fontId.fontKind();
    }
    
    @Override
    public String fontFamily() {
        return this.fontId.fontKind().fontFamily();
    }

    /*
     * Style.
     */
    
    @Override
    public int fontStyle() {
        return this.fontId.fontKind().fontStyle();
    }

    @Override
    public boolean isBold() {
        return this.fontId.fontKind().isBold();
    }

    @Override
    public boolean isItalic() {
        return this.fontId.fontKind().isItalic();
    }
    
    /*
     * Size.
     */
    
    @Override
    public int fontSize() {
        return this.fontId.fontSize();
    }
    
    /*
     * Glyphs.
     */
    
    @Override
    public final boolean canDisplay(int codePoint) {
        BwdUnicode.checkCodePoint(codePoint);
        return this.canFontDisplayComputer.canFontDisplay(this, codePoint);
    }
    
    public InterfaceCanFontDisplayComputer getCanFontDisplayComputer() {
        return this.canFontDisplayComputer;
    }
    
    /*
     * Disposal.
     */
    
    /**
     * @return Can be null.
     */
    public InterfaceFontDisposeCallListener getDisposeCallListener() {
        return this.disposeCallListener;
    }
    
    public BF getBackingFont() {
        return this.backingFont;
    }
    
    @Override
    public boolean isDisposed() {
        return this.disposedRef.get();
    }

    @Override
    public void dispose() {
        final InterfaceFontDisposeCallListener disposeCallListener = this.disposeCallListener;
        if (disposeCallListener != null) {
            // Idempotent.
            if (this.disposedRef.compareAndSet(false, true)) {
                disposeCallListener.onFontDisposeCalled(this);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    protected int compareToImpl(AbstractBwdFont<BF> other) {
        {
            // Ids should not wrap in practice, so remain >= 0.
            final int cmp = this.homeId - other.homeId;
            if (cmp != 0) {
                return cmp;
            }
        }
        return this.fontId.compareTo(other.fontId);
    }
    
    //--------------------------------------------------------------------------
    // PACKAGE-PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * To mark home-owned fonts as disposed.
     */
    void setDisposed() {
        /*
         * No need to bother with CAS,
         * since home-owned fonts have no dispose call listener.
         */
        this.disposedRef.set(true);
    }
}
