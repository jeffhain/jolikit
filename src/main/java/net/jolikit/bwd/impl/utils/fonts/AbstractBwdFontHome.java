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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import net.jolikit.bwd.api.fonts.BwdFontId;
import net.jolikit.bwd.api.fonts.BwdFontKind;
import net.jolikit.bwd.api.fonts.InterfaceBwdFont;
import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.impl.utils.basics.BindingCoordsUtils;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.InterfaceDefaultFontInfoComputer;
import net.jolikit.bwd.impl.utils.basics.OneCallChecker;
import net.jolikit.bwd.impl.utils.fonts.FontOfHeightHelper.InterfaceFontCreator;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;

/**
 * Optional class to make it easier to implement InterfaceBwdFontHome.
 * 
 * @param BF The class for backing fonts.
 * @param BFG The class for objects allowing to generate backing fonts of a given kind.
 *        If such a class is useless, you can use Void.
 */
public abstract class AbstractBwdFontHome<BF,BFG> implements InterfaceBwdFontHome {

    /*
     * As a general rule, using a sorted sets/maps for determinism and ordering,
     * for debug or if used to return result collections through the API.
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;

    /**
     * It's common for fonts APIs to use the absolute unit of a "point" for
     * the size, which is theoretically 1/72th of an inch, but most often,
     * if not always, it actually corresponds to 1.3333 (OS-resolution) pixel
     * (or rather 1/0.75, which is 1.333333...), whatever the actual physical
     * size of these pixels.
     * 
     * This constant defines this ratio for us.
     * 
     * This fixed ratio is also the reason why fonts properly resize when
     * OS resolution changes, which would not be the case if they would
     * preserve their theoretical absolute size.
     */
    private static final double POINTS_PER_OS_PIXEL = 0.75;

    //--------------------------------------------------------------------------
    // PROTECTED CLASSES
    //--------------------------------------------------------------------------

    protected class MyLoadedFontData {
        private final InterfaceCanFontDisplayComputer cfdc;
        private final BFG bfg;
        public MyLoadedFontData(
                InterfaceCanFontDisplayComputer cfdc,
                BFG bfg) {
            this.cfdc = LangUtils.requireNonNull(cfdc);
            this.bfg = bfg;
        }
        public InterfaceCanFontDisplayComputer getCanFontDisplayComputer() {
            return this.cfdc;
        }
        public BFG getBackingFontGenerator() {
            return this.bfg;
        }
    }

    /**
     * For fonts not owned (solely) by this home.
     */
    protected class MyDisposableFontDisposeCallListener implements InterfaceFontDisposeCallListener {
        /**
         * Guarded by homeMutex.
         * 
         * For consistency check.
         */
        private boolean fontDisposed = false;
        /**
         * Guarded by homeMutex.
         * 
         * Reference counter, or usage counter, for the font
         * (or backing font, which is equivalent since
         * we create a font instance per backing font instance).
         * 
         * We can store it here, since we use one dispose call listener per font.
         */
        private int refCount;
        public MyDisposableFontDisposeCallListener() {
        }
        /**
         * To be called at font creation.
         * 
         * Throws if doesn't start at zero.
         */
        public void incrementRefCount_atCreation() {
            synchronized (homeMutex) {
                if (this.refCount != 0) {
                    throw new AssertionError();
                }
                this.incrementRefCount();
            }
        }
        @Override
        public void onFontDisposeCalled(InterfaceBwdFont font) {
            // Early for type check.
            @SuppressWarnings("unchecked")
            final AbstractBwdFont<BF> aFont = (AbstractBwdFont<BF>) font;
            
            boolean mustDispose = false;
            synchronized (homeMutex) {
                if (this.refCount == 0) {
                    // Must not be called before initial increment.
                    throw new AssertionError();
                }

                if (--this.refCount == 0) {
                    this.fontDisposed = true;
                    
                    mustDispose = true;
                }
            }
            
            if (mustDispose) {
                disposeDisposableFont(aFont);
            }
        }
        private void incrementRefCount() {
            synchronized (homeMutex) {
                if (this.fontDisposed) {
                    throw new AssertionError();
                }
                if (this.refCount == Integer.MAX_VALUE) {
                    throw new IllegalStateException("too many references already: " + this.refCount);
                }
                this.refCount++;
            }
        }
        /**
         * Sets refCount to zero, and indicates font as disposed.
         * 
         * Throws if font already disposed.
         */
        private void setFontDisposed() {
            synchronized (homeMutex) {
                if (this.fontDisposed) {
                    throw new AssertionError();
                }
                this.refCount = 0;
                this.fontDisposed = true;
            }
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyFontCreator_forDisposableFont implements InterfaceFontCreator {
        @Override
        public InterfaceBwdFont newFontWithSize(
                BwdFontKind fontKind,
                int fontSize) {
            return AbstractBwdFontHome.this.newFontWithSize(fontKind, fontSize);
        }
    }

    private class MyFontCreator_forDefaultFont implements InterfaceFontCreator {
        @Override
        public InterfaceBwdFont newFontWithSize(
                BwdFontKind fontKind,
                int fontSize) {
            final BwdFontId fontId = new BwdFontId(fontKind, fontSize);
            final MyDisposableFontDisposeCallListener disposeCallListener = null;
            return createBackingFontAndFont(
                    fontId,
                    disposeCallListener);
        }
    }

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final SortedSet<BwdFontKind> EMPTY_FONT_KIND_SET =
            Collections.unmodifiableSortedSet(new TreeSet<BwdFontKind>());

    private static final AtomicInteger HOME_ID_GENERATOR = new AtomicInteger();

    /**
     * For fonts from different homes to never be equal to each other.
     */
    private final int homeId = HOME_ID_GENERATOR.incrementAndGet();

    /**
     * Taking care not to run alien code into this mutex,
     * except maybe some hashCode()/equals(...).
     * 
     * To guard all mutable things owned by this home.
     */
    private final Object homeMutex = new Object();

    private final OneCallChecker singleFontLoadChecker = new OneCallChecker();

    /**
     * Guarded by homeMutex.
     */
    private final SortedMap<BwdFontKind,MyLoadedFontData> lfdByLoadedFontKind =
            new TreeMap<BwdFontKind,MyLoadedFontData>();

    /**
     * Volatile so that can be retrieved outside homeMutex.
     */
    private volatile SortedSet<BwdFontKind> loadedFontKindSet = EMPTY_FONT_KIND_SET;
    private volatile SortedSet<BwdFontKind> loadedSystemFontKindSet = EMPTY_FONT_KIND_SET;
    private volatile SortedSet<BwdFontKind> loadedUserFontKindSet = EMPTY_FONT_KIND_SET;

    /**
     * Volatile so that can be retrieved outside homeMutex.
     */
    private volatile AbstractBwdFont<BF> defaultFont;

    /**
     * Guarded by homeMutex.
     * 
     * Contains fonts created via call to newFontXxx(...) methods.
     * Only storing the first one of each font id, for we reuse it
     * for supplementary instances, and only actually dispose it
     * (actually its backing font) when there usage counter gets down
     * to zero.
     * 
     * Does NOT contain default font.
     */
    private final SortedMap<BwdFontId,AbstractBwdFont<BF>> disposableFontById =
            new TreeMap<BwdFontId,AbstractBwdFont<BF>>();
    
    private final FontOfHeightHelper fontOfHeightHelper = new FontOfHeightHelper();
    
    private final MyFontCreator_forDisposableFont fontCreator_forDisposableFont =
            new MyFontCreator_forDisposableFont();
    
    private final MyFontCreator_forDefaultFont fontCreator_forDefaultFont =
            new MyFontCreator_forDefaultFont();

    /*
     * 
     */
    
    /**
     * To be set during construction.
     * 
     * Stored here to make sure it doesn't change over time,
     * which would cause havoc.
     */
    private double fontSizeFactor;
    
    /**
     * To be set during construction.
     * 
     * Min allowed user-specified font size.
     */
    private int minFontSize;
    
    /**
     * To be set during construction.
     * 
     * Max allowed user-specified font size.
     */
    private int maxFontSize;
    
    /*
     * 
     */

    /**
     * Guarded by homeMutex.
     */
    private boolean homeDisposed = false;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public AbstractBwdFontHome() {
    }
    
    /*
     * 
     */

    @Override
    public int getMinFontSize() {
        return this.minFontSize;
    }

    @Override
    public int getMaxFontSize() {
        return this.maxFontSize;
    }

    /*
     * 
     */

    @Override
    public SortedSet<String> loadSystemAndUserFonts(List<String> userFontFilePathList) {

        if (DEBUG) {
            Dbg.log("loadSystemAndUserFonts(" + userFontFilePathList + ")");
        }

        this.singleFontLoadChecker.throwIfCalledMoreThanOnce();

        synchronized (this.homeMutex) {
            if (this.homeDisposed) {
                throw new IllegalStateException("this home is disposed");
            }
        }
        
        /*
         * System fonts.
         */
        
        final SortedMap<BwdFontKind,MyLoadedFontData> lfdByLoadedSystemFontKind;
        {
            final Map<BwdFontKind,MyLoadedFontData> map =
                    this.loadSystemFonts();
            lfdByLoadedSystemFontKind = new TreeMap<BwdFontKind,MyLoadedFontData>(map);
        }

        if (DEBUG) {
            Dbg.log("loaded system font kind (1):");
            Dbg.logColl(lfdByLoadedSystemFontKind.keySet());
        }

        final List<String> bonusSystemFontFilePathList =
                this.getBonusSystemFontFilePathList();

        if (DEBUG) {
            Dbg.log("bonusSystemFontFilePathList:");
            Dbg.logColl(bonusSystemFontFilePathList);
        }
        if (bonusSystemFontFilePathList != null) {
            for (String bonusSystemFontFilePath : bonusSystemFontFilePathList) {
                final SortedMap<BwdFontKind,MyLoadedFontData> lfdByLoadedBonusSystemFontKind =
                        new TreeMap<BwdFontKind,MyLoadedFontData>();
                final boolean didLoad = this.loadFontsAt(
                        bonusSystemFontFilePath,
                        lfdByLoadedSystemFontKind, // in,out
                        lfdByLoadedBonusSystemFontKind); // in,out
                if (DEBUG) {
                    Dbg.log("loadFontsAt(" + bonusSystemFontFilePath + ") = " + didLoad);
                }
                if (didLoad) {
                    lfdByLoadedSystemFontKind.putAll(lfdByLoadedBonusSystemFontKind);
                } else {
                    /*
                     * Some libraries (such as SWT) don't allow to know which
                     * font kind has been loaded in case of re-load, resulting in
                     * apparent load fail, so we don't want to throw here.
                     */
                }
            }
        }

        final SortedSet<BwdFontKind> loadedSystemFontKindSet =
                toImmutableSortedSet(lfdByLoadedSystemFontKind.keySet());
        
        /*
         * User fonts.
         */

        final SortedMap<BwdFontKind,MyLoadedFontData> lfdByLoadedUserFontKind =
                new TreeMap<BwdFontKind,MyLoadedFontData>();

        final SortedSet<String> loadedUserFontFilePathSet =
                this.loadUserFonts(
                        userFontFilePathList,
                        lfdByLoadedSystemFontKind, // in,out
                        lfdByLoadedUserFontKind); // out

        if (DEBUG) {
            Dbg.log("loaded system font kind (2):");
            Dbg.logColl(lfdByLoadedSystemFontKind.keySet());
            Dbg.log("loaded user font kind:");
            Dbg.logColl(lfdByLoadedUserFontKind.keySet());
            Dbg.log("loadedUserFontFilePathSet:");
            Dbg.logColl(loadedUserFontFilePathSet);
        }
        
        final SortedSet<BwdFontKind> loadedUserFontKindSet =
                toImmutableSortedSet(lfdByLoadedUserFontKind.keySet());
        
        final SortedSet<BwdFontKind> loadedFontKindSet;
        synchronized (this.homeMutex) {
            this.lfdByLoadedFontKind.putAll(lfdByLoadedSystemFontKind);
            this.lfdByLoadedFontKind.putAll(lfdByLoadedUserFontKind);

            loadedFontKindSet = toImmutableSortedSet(this.lfdByLoadedFontKind.keySet());
        }

        /*
         * Creating default font.
         */

        final InterfaceDefaultFontInfoComputer defaultFontInfoComputer =
                this.getDefaultFontInfoComputer();
        final BwdFontKind defaultFontKind = defaultFontInfoComputer.computeDefaultFontKind(
                loadedSystemFontKindSet,
                loadedUserFontKindSet);

        if (DEBUG) {
            Dbg.log("defaultFontKind = " + defaultFontKind);
        }

        if (!loadedFontKindSet.contains(defaultFontKind)) {
            final StringBuilder sb = new StringBuilder();
            for (BwdFontKind fontKind : loadedFontKindSet) {
                sb.append("\n");
                sb.append(fontKind);
            }
            final String setStr = sb.toString();
            throw new BindingError(
                    "default font kind "
                            + defaultFontKind
                            + " not found among loaded font kinds: "
                            + setStr);

        }

        final AbstractBwdFont<BF> defaultFont;
        if (defaultFontInfoComputer.getMustUseFontSizeElseTargetFontHeight()) {
            final int defaultFontSize = defaultFontInfoComputer.getDefaultFontSize();
            this.checkFontSize(defaultFontSize);

            final BwdFontId defaultFontId = new BwdFontId(defaultFontKind, defaultFontSize);
            final MyDisposableFontDisposeCallListener disposeCallListener = null;
            defaultFont = this.createBackingFontAndFont(
                    defaultFontId,
                    disposeCallListener);
        } else {
            final int minFontSize = this.getMinFontSize();
            final int maxFontSize = this.getMaxFontSize();
            final int targetFontHeight = defaultFontInfoComputer.getTargetDefaultFontHeight();
            final InterfaceBwdFont defaultFont_ = this.fontOfHeightHelper.newFontWithClosestHeight(
                    this.fontCreator_forDefaultFont,
                    minFontSize,
                    maxFontSize,
                    defaultFontKind,
                    targetFontHeight);

            @SuppressWarnings("unchecked")
            final AbstractBwdFont<BF> defaultFont__ = (AbstractBwdFont<BF>) defaultFont_;
            
            defaultFont = defaultFont__;
        }

        // All volatile, but we sync anyway,
        // as we say volatile is just done for reads.
        synchronized (this.homeMutex) {
            this.loadedSystemFontKindSet = loadedSystemFontKindSet;
            this.loadedUserFontKindSet = loadedUserFontKindSet;
            this.loadedFontKindSet = loadedFontKindSet;
            
            this.defaultFont = defaultFont;
        }

        return loadedUserFontFilePathSet;
    }

    @Override
    public SortedSet<BwdFontKind> getLoadedFontKindSet() {
        return this.loadedFontKindSet;
    }

    @Override
    public SortedSet<BwdFontKind> getLoadedSystemFontKindSet() {
        return this.loadedSystemFontKindSet;
    }

    @Override
    public SortedSet<BwdFontKind> getLoadedUserFontKindSet() {
        return this.loadedUserFontKindSet;
    }

    @Override
    public InterfaceBwdFont getDefaultFont() {
        final InterfaceBwdFont defaultFont = this.defaultFont;
        if (defaultFont == null) {
            throw new IllegalStateException("default font not yet created");
        }
        return defaultFont;
    }

    /*
     * 
     */

    @Override
    public InterfaceBwdFont newFontWithSize(BwdFontKind fontKind, int fontSize) {

        if (DEBUG) {
            Dbg.log();
            Dbg.log("newFontWithSize(" + fontKind + ", " + fontSize + ")");
        }

        synchronized (this.homeMutex) {
            if (this.homeDisposed) {
                throw new IllegalStateException("this home is disposed");
            }

            if (!this.loadedFontKindSet.contains(fontKind)) {
                throw new IllegalArgumentException("font kind not loaded: " + fontKind);
            }
        }

        this.checkFontSize(fontSize);

        final BwdFontId fontId = new BwdFontId(fontKind, fontSize);

        AbstractBwdFont<BF> alreadyCreatedFont;
        synchronized (this.homeMutex) {
            alreadyCreatedFont = this.disposableFontById.get(fontId);
        }
        
        AbstractBwdFont<BF> newFont = null;

        if (alreadyCreatedFont == null) {
            final MyDisposableFontDisposeCallListener disposeCallListener = new MyDisposableFontDisposeCallListener();
            newFont = this.createBackingFontAndFont(fontId, disposeCallListener);

            synchronized (this.homeMutex) {
                /*
                 * Taking care to reuse eventually already created backing font,
                 * which could occur in case of concurrent calls.
                 */
                alreadyCreatedFont = this.disposableFontById.get(fontId);
                if (alreadyCreatedFont == null) {
                    this.disposableFontById.put(fontId, newFont);
                }
            }

            if (alreadyCreatedFont != null) {
                /*
                 * Another thread beat us to this:
                 * we will reuse its backing font.
                 */
                newFont.dispose();
                newFont = null;
            }
        }

        if (newFont == null) {
            // Such a font is still up: incrementing the ref counter
            // in the dispose call listener, and returning a shallow copy of the font.
            @SuppressWarnings("unchecked")
            final MyDisposableFontDisposeCallListener disposeCallListener =
            (MyDisposableFontDisposeCallListener) alreadyCreatedFont.getDisposeCallListener();

            disposeCallListener.incrementRefCount();
            newFont = this.createFontReusingBackingFont(
                    this.homeId,
                    // Reusing existing fontId instance,
                    // for eventual hazardous "==" tests to work.
                    alreadyCreatedFont.id(),
                    alreadyCreatedFont.getCanFontDisplayComputer(),
                    disposeCallListener,
                    alreadyCreatedFont.getBackingFont());
        }
        
        return newFont;
    }

    @Override
    public InterfaceBwdFont newFontWithClosestHeight(
            BwdFontKind fontKind,
            int targetFontHeight) {
        final int minFontSize = this.getMinFontSize();
        final int maxFontSize = this.getMaxFontSize();
        return this.fontOfHeightHelper.newFontWithClosestHeight(
                this.fontCreator_forDisposableFont,
                minFontSize,
                maxFontSize,
                fontKind,
                targetFontHeight);
    }

    @Override
    public InterfaceBwdFont newFontWithFloorElseClosestHeight(
            BwdFontKind fontKind,
            int targetFontHeight) {
        final int minFontSize = this.getMinFontSize();
        final int maxFontSize = this.getMaxFontSize();
        return this.fontOfHeightHelper.newFontWithFloorElseClosestHeight(
                this.fontCreator_forDisposableFont,
                minFontSize,
                maxFontSize,
                fontKind,
                targetFontHeight);
    }

    @Override
    public InterfaceBwdFont newFontWithCeilingElseClosestHeight(
            BwdFontKind fontKind,
            int targetFontHeight) {
        final int minFontSize = this.getMinFontSize();
        final int maxFontSize = this.getMaxFontSize();
        return this.fontOfHeightHelper.newFontWithCeilingElseClosestHeight(
                this.fontCreator_forDisposableFont,
                minFontSize,
                maxFontSize,
                fontKind,
                targetFontHeight);
    }
    
    /*
     * 
     */

    /**
     * Disposes all fonts, both the eventual internal ones resulting from
     * font loading, and those created with newFontXxx(...) methods.
     */
    public void dispose() {
        final Object[] lfdArr;
        synchronized (this.homeMutex) {
            if (this.homeDisposed) {
                return;
            }
            this.homeDisposed = true;
            
            /*
             * 
             */
            
            final Collection<MyLoadedFontData> lfdColl = this.lfdByLoadedFontKind.values();
            lfdArr = lfdColl.toArray();
            this.lfdByLoadedFontKind.clear();

            // Letting font kind sets available, as spec says.
            if (false) {
                this.loadedFontKindSet = EMPTY_FONT_KIND_SET;
                this.loadedSystemFontKindSet = EMPTY_FONT_KIND_SET;
                this.loadedUserFontKindSet = EMPTY_FONT_KIND_SET;
            }
        }
        
        for (Object lfd_ : lfdArr) {
            @SuppressWarnings("unchecked")
            final MyLoadedFontData lfd = (MyLoadedFontData) lfd_;
            // Possibly null.
            final BFG bfg = lfd.bfg;
            this.disposeBfg(bfg);
        }
        
        BF defaultBackingFontToDipose = null;
        synchronized (this.homeMutex) {
            /*
             * Not nullifying default font, so that it can still be retrieved,
             * even if it can't be used due to being disposed.
             */
            final AbstractBwdFont<BF> defaultFont = this.defaultFont;
            if (defaultFont != null) {
                if (!defaultFont.isDisposed()) {
                    defaultFont.setDisposed();
                    defaultBackingFontToDipose = defaultFont.getBackingFont();
                }
            }
        }
        if (defaultBackingFontToDipose != null) {
            this.disposeBackingFont(defaultBackingFontToDipose);
        }

        this.disposeDisposableFonts();
    }

    @Override
    public void disposeDisposableFonts() {
        /*
         * OK to be called concurrently,
         * since disposeCreatedFont(...) does nothing
         * if the font has already been removed from the map
         * after we retrieved it and before we try to dispose it.
         */
        final ArrayList<AbstractBwdFont<BF>> fontToDisposeList =
                new ArrayList<AbstractBwdFont<BF>>();
        synchronized (this.homeMutex) {
            final Object[] objArr = this.disposableFontById.values().toArray();
            for (Object obj : objArr) {
                @SuppressWarnings("unchecked")
                final AbstractBwdFont<BF> font = (AbstractBwdFont<BF>) obj;
                
                @SuppressWarnings("unchecked")
                final MyDisposableFontDisposeCallListener disposeCallListener =
                        (MyDisposableFontDisposeCallListener) font.getDisposeCallListener();
                
                disposeCallListener.setFontDisposed();
                fontToDisposeList.add(font);
            }
        }
        
        for (AbstractBwdFont<BF> font : fontToDisposeList) {
            disposeDisposableFont(font);
        }
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    protected int homeId() {
        return this.homeId;
    }
    
    /**
     * Must be called in constructor.
     * 
     * Requiring "raw" min/max bounds allows to change
     * factor independently, without having to update them.
     * 
     * @param minRawFontSize Min user-specified font size,
     *        when not multiplied by font size factor.
     *        Must be >= 1.
     * @param maxRawFontSize Max user-specified font size,
     *        when not multiplied by font size factor.
     *        Must be >= minFontSize.
     * @param fontSizeFactor Must be > 0.
     */
    protected final void initialize_final(
            int minRawFontSize,
            int maxRawFontSize,
            double fontSizeFactor) {
        if (minRawFontSize < 1) {
            throw new IllegalArgumentException("minRawFontSize [" + minRawFontSize + "] must be >= 1");
        }
        if (maxRawFontSize < minRawFontSize) {
            throw new IllegalArgumentException("maxRawFontSize [" + maxRawFontSize + "] must be >= minRawFontSize [" + minRawFontSize + "]");
        }
        if (!(fontSizeFactor > 0.0)) {
            throw new IllegalArgumentException("fontSizeFactor [" + fontSizeFactor + "] must be > 0");
        }
        // Ceil, and max/floor, to make sure factor
        // doesn't get us into invalid territory.
        this.minFontSize = BindingCoordsUtils.ceilToInt(minRawFontSize / fontSizeFactor);
        this.maxFontSize = Math.max(this.minFontSize, BindingCoordsUtils.floorToInt(maxRawFontSize / fontSizeFactor));
        this.fontSizeFactor = fontSizeFactor;
    }
    
    protected double getFontSizeFactor() {
        return this.fontSizeFactor;
    }
    
    protected double computeBackingFontSizeInPixelsFp(int fontSize) {
        return fontSize * this.fontSizeFactor;
    }
    
    protected double computeBackingFontSizeInPointsFp(int fontSize) {
        return this.computeBackingFontSizeInPixelsFp(fontSize) * POINTS_PER_OS_PIXEL;
    }

    protected MyLoadedFontData getLfdForLoadedFontKind(BwdFontKind fontKind) {
        synchronized (this.homeMutex) {
            return this.lfdByLoadedFontKind.get(fontKind);
        }
    }

    /*
     * 
     */

    /**
     * If multiple fonts have the same kind,
     * should use the first encountered one.
     * 
     * @return The map containing loaded font data, if any.
     */
    protected abstract Map<BwdFontKind,MyLoadedFontData> loadSystemFonts();

    /**
     * Should throw in case of IO error, or possibly corrupted file,
     * but should not throw in case of unsupported font format.
     * For this reason, this method should first attempt to actually
     * load the font with the backing library, even if it's not required
     * to compute font kind etc.
     * 
     * @param fontFilePath Not null.
     * @return A map, containing loaded font data if any.
     */
    protected abstract Map<BwdFontKind,MyLoadedFontData> loadFontsAtPath(String fontFilePath);

    /**
     * Used a font loading time.
     * 
     * Useful to have system fonts loaded by the binding
     * when the backing library can't load any on its own.
     * 
     * Can be null or empty, if default font must be found
     * among backing system fonts or specified user fonts.
     * 
     * @return List of file paths corresponding to font to load
     *         as additional system fonts.
     */
    protected abstract List<String> getBonusSystemFontFilePathList();

    /**
     * Used a font loading time, to create default font from loaded fonts.
     */
    protected abstract InterfaceDefaultFontInfoComputer getDefaultFontInfoComputer();

    /**
     * NB: Could call it for each loaded font kind, at load time,
     * to check that it can actually be loaded, but that should
     * most likely only add useless overhead since in practice
     * all loaded font kinds should correspond to loadable fonts.
     * 
     * @param fontId Has a font size in valid range (no need to check it).
     * @throws IllegalArgumentException if no font of corresponding kind is loaded.
     */
    protected abstract AbstractBwdFont<BF> createBackingFontAndFont(
            BwdFontId fontId,
            MyDisposableFontDisposeCallListener disposeCallListener);

    protected abstract AbstractBwdFont<BF> createFontReusingBackingFont(
            int homeId,
            BwdFontId fontId,
            InterfaceCanFontDisplayComputer canFontDisplayComputer,
            InterfaceFontDisposeCallListener disposeCallListener,
            BF backingFont);

    protected abstract void disposeBackingFont(BF backingFont);

    /**
     * Must not dispose, as a side effect, instances corresponding
     * to other loaded fonts of same font kind,
     * so that we can dispose it safely when rolling back a font load
     * due to another font of same kind already being loaded,
     * or due to wanting to replace an already loaded font (and its BFG)
     * by a newly loaded one of same font kind.
     * If there is such a risk, it's better not to dispose anything,
     * which shouldn't cause much memory leak since fonts load
     * is only meant to happen once per binding instance.
     */
    protected abstract void disposeBfg(BFG bfg);

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private static <T extends Comparable<?>> SortedSet<T> toImmutableSortedSet(Set<T> set) {
        return Collections.unmodifiableSortedSet(new TreeSet<T>(set));
    }
    
    /**
     * @throws IllegalArgumentException if the specified font size is out of range.
     */
    private void checkFontSize(int fontSize) {
        final int min = this.getMinFontSize();
        final int max = this.getMaxFontSize();
        if ((fontSize < min)
                || (fontSize > max)) {
            throw new IllegalArgumentException("font size [" + fontSize + "] must be in [" + min + "," + max + "]");
        }
    }
    
    /*
     * 
     */

    /**
     * @param userFontFilePathList (in) Can be null.
     * @param lfdByLoadedSystemFontKind (in,out)
     * @param lfdByLoadedUserFontKind (out)
     * @return A set of user font files paths that could be loaded.
     */
    private SortedSet<String> loadUserFonts(
            List<String> userFontFilePathList,
            Map<BwdFontKind,MyLoadedFontData> lfdByLoadedSystemFontKind,
            Map<BwdFontKind,MyLoadedFontData> lfdByLoadedUserFontKind) {

        lfdByLoadedUserFontKind.clear();

        final SortedSet<String> loadedUserFontFilePathSet = new TreeSet<String>();

        if (userFontFilePathList != null) {
            for (String userFontFilePath : userFontFilePathList) {
                final boolean didLoad = this.loadFontsAt(
                        userFontFilePath,
                        lfdByLoadedSystemFontKind, // in,out
                        lfdByLoadedUserFontKind); // in,out
                if (didLoad) {
                    loadedUserFontFilePathSet.add(userFontFilePath);
                }
            }
        }

        return loadedUserFontFilePathSet;
    }

    /**
     * @param fontFilePath (in)
     * @param toReplaceLfdByLoadedFontKind (in,out)  Map where loaded font kinds
     *        are removed from, if already in.
     * @param lfdByLoadedFontKind (in,out) Map where loaded font kinds are added.
     * @return True if could load the specified font, false otherwise.
     */
    private boolean loadFontsAt(
            String fontFilePath,
            Map<BwdFontKind,MyLoadedFontData> toReplaceLfdByLoadedFontKind,
            Map<BwdFontKind,MyLoadedFontData> lfdByLoadedFontKind) {

        LangUtils.requireNonNull(fontFilePath);

        if (DEBUG) {
            Dbg.log("loadFontsAt(" + fontFilePath + ")");
        }

        final SortedMap<BwdFontKind,MyLoadedFontData> fileLfdByLoadedFontKind;
        {
            final Map<BwdFontKind,MyLoadedFontData> map =
                    this.loadFontsAtPath(fontFilePath);
            fileLfdByLoadedFontKind = new TreeMap<BwdFontKind,MyLoadedFontData>(map);
        }

        if (DEBUG) {
            Dbg.log("loadFontsAt : loaded font kind set = " + fileLfdByLoadedFontKind.keySet());
        }

        final boolean didLoad = (fileLfdByLoadedFontKind.size() != 0);
        if (didLoad) {
            for (Map.Entry<BwdFontKind,MyLoadedFontData> entry : fileLfdByLoadedFontKind.entrySet()) {
                final BwdFontKind fontKind = entry.getKey();
                final MyLoadedFontData lfd = entry.getValue();

                if (lfdByLoadedFontKind.containsKey(fontKind)) {
                    // Already loaded: keeping first one.
                    this.disposeBfg(lfd.bfg);
                } else {
                    // Replacing to-replace font if any.
                    final MyLoadedFontData toReplaceLfd = toReplaceLfdByLoadedFontKind.remove(fontKind);
                    if (toReplaceLfd != null) {
                        this.disposeBfg(toReplaceLfd.bfg);
                    }

                    lfdByLoadedFontKind.put(fontKind, lfd);
                }
            }
        }

        return didLoad;
    }

    /**
     * Does nothing if it has already been removed from the map
     * (due to a concurrent disposal).
     */
    private void disposeDisposableFont(AbstractBwdFont<BF> font) {
        final BwdFontId fontId = font.id();

        final AbstractBwdFont<BF> removed;
        synchronized (this.homeMutex) {
            removed = this.disposableFontById.remove(fontId);
        }
        if (removed == null) {
            return;
        }

        final BwdFontId removedFontId = removed.id();
        // Could just use "!=", since we reuse fontId instances,
        // but that's just an optimization and guard against
        // bad "=="/"!=" tests done by user.
        if (!removedFontId.equals(fontId)) {
            throw new AssertionError(removedFontId + " !equals " + fontId);
        }

        final BF backingFont = font.getBackingFont();
        this.disposeBackingFont(backingFont);
    }
}
