package net.jolikit.bwd.impl.utils;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.impl.utils.basics.InterfaceDefaultFontInfoComputer;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFont;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFontHome;
import net.jolikit.bwd.impl.utils.fonts.InterfaceCanFontDisplayComputer;
import net.jolikit.bwd.impl.utils.fonts.InterfaceFontDisposeCallListener;

public class FontHomeForApiTests extends AbstractBwdFontHome<String,Integer> {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final DefaultDefaultFontInfoComputer defaultFontInfoComputer =
            new DefaultDefaultFontInfoComputer();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public FontHomeForApiTests() {
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected Map<BwdFontKind,MyLoadedFontData> loadSystemFonts() {
        return new TreeMap<BwdFontKind,MyLoadedFontData>();
    }

    @Override
    protected Map<BwdFontKind,MyLoadedFontData> loadFontsAtPath(
            String fontFilePath) {
        return new TreeMap<BwdFontKind,MyLoadedFontData>();
    }

    @Override
    protected List<String> getBonusSystemFontFilePathList() {
        return null;
    }

    @Override
    protected InterfaceDefaultFontInfoComputer getDefaultFontInfoComputer() {
        return this.defaultFontInfoComputer;
    }

    @Override
    protected AbstractBwdFont<String> createBackingFontAndFont(
            BwdFontId fontId,
            MyDisposableFontDisposeCallListener disposeCallListener) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected AbstractBwdFont<String> createFontReusingBackingFont(
            int homeId,
            BwdFontId fontId,
            InterfaceCanFontDisplayComputer canFontDisplayComputer,
            InterfaceFontDisposeCallListener disposeCallListener,
            String backingFont) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void disposeBackingFont(String backingFont) {
    }

    @Override
    protected void disposeBfg(Integer bfg) {
    }
}
