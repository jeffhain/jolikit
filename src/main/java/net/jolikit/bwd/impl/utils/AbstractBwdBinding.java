/*
 * Copyright 2019-2021 Jeff Hain
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
package net.jolikit.bwd.impl.utils;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.bwd.impl.utils.basics.BindingError;
import net.jolikit.bwd.impl.utils.basics.ScaleHelper;
import net.jolikit.bwd.impl.utils.fonts.AbstractBwdFontHome;
import net.jolikit.bwd.impl.utils.images.InterfaceBwdImageDisposalListener;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.NbrsUtils;
import net.jolikit.threading.prl.ExecutorParallelizer;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.time.sched.AbstractProcess;
import net.jolikit.time.sched.InterfaceScheduler;
import net.jolikit.time.sched.hard.HardScheduler;

/**
 * Optional abstract class for bindings implementations.
 */
public abstract class AbstractBwdBinding implements InterfaceBwdBindingImpl {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyHostLifecycleListener implements InterfaceHostLifecycleListener<AbstractBwdHost> {
        public MyHostLifecycleListener() {
        }
        @Override
        public void onHostCreated(AbstractBwdHost host) {
            onHostCreatedImpl(host);
        }
        @Override
        public void onHostClosing(AbstractBwdHost host) {
            onHostClosingImpl(host);
        }
        @Override
        public void onHostClosedEventFiring(AbstractBwdHost host) {
            onHostClosedEventFiringImpl(host);
        }
    }
    
    private class MyHostEventLoopRunnable implements Runnable {
        private final AbstractBwdHost host;
        public MyHostEventLoopRunnable(AbstractBwdHost host) {
            this.host = host;
        }
        @Override
        public void run() {
            final AbstractBwdHost host = this.host;
            final InterfaceScheduler scheduler = host.getBinding().getUiThreadScheduler();
            final double nowS = scheduler.getClock().getTimeS();
            
            boolean completedNormally = false;
            try {
                host.runWindowEventLogicLoopOnPeriod(nowS);
                completedNormally = true;
            } finally {
                if (!completedNormally) {
                    if (DEBUG) {
                        Dbg.log("exception during event logic run for " + AbstractBwdHost.hid(host));
                    }
                    final PrintStream stream = getBindingConfig().getIssueStream();
                    // Can help figure out which host threw.
                    stream.println("exception during event logic run for host " + AbstractBwdHost.hid(host));
                }
            }
        }
    }
    
    /**
     * Process to periodically run window event logic.
     * 
     * If a user listener throws, the corresponding host is moved to the end
     * of the list used for iteration, so that messy hosts don't block
     * event processing for other hosts.
     * 
     * We iterate on all hosts until the corresponding CLOSED event has been
     * fired, not only on the showing ones, so that for example if a HIDDEN
     * event gets enforced from within close() method before CLOSED event
     * (because the window was showing), and the user listener throws,
     * the CLOSED event will still be ensured later by this polling
     * (except if an abrupt shutdown has been triggered).
     */
    private class MyEventLogicProcess extends AbstractProcess {
        public MyEventLogicProcess(InterfaceScheduler scheduler) {
            super(scheduler);
        }
        @Override
        protected long process(long theoreticalTimeNs, long actualTimeNs) {
            /*
             * The list might change arbitrarily every time we call user code,
             * i.e. every time we fire an event.
             * We just take a snapshot of it, and loop on it from first to last,
             * or until one throws if not executing each asynchronously.
             */
            final AbstractBwdHost[] hostArr = eventLogicHostDeque.toArray(new AbstractBwdHost[eventLogicHostDeque.size()]);
            for (AbstractBwdHost host : hostArr) {
                final MyHostEventLoopRunnable hostRunnable =
                        new MyHostEventLoopRunnable(host);
                getScheduler().execute(hostRunnable);
            }
            
            final long periodNs = sToNsNoUnderflow(eventLogicPeriodS);
            return plusBounded(actualTimeNs, periodNs);
        }
    }
    
    private class MyImageDisposalListener implements InterfaceBwdImageDisposalListener {
        @Override
        public void onImageDisposed(InterfaceBwdImage image) {
            removeImage(image);
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    /**
     * For threads of each binding to have a unique name.
     */
    private static final AtomicLong INSTANCE_ID_PRODUCER = new AtomicLong();
    
    /**
     * Reference object to use to make sure that at most
     * one not yet shut down instance of this binding exists at any time.
     * Useful for backing libraries that have !!!static mutable state!!!
     * (most of them! Proof that Satan is really the god of this world...).
     * 
     * Must be null to allow for multiple running instances at once.
     */
    private final AtomicReference<AbstractBwdBinding> singleInstanceRef;
    
    /*
     * 
     */
    
    private final String threadsBaseName =
        this.getClass().getSimpleName()
        + "-" + INSTANCE_ID_PRODUCER.incrementAndGet();
    
    private final AtomicBoolean shutdownDownAlreadyCalled = new AtomicBoolean(false);

    /*
     * 
     */

    private final BaseBwdBindingConfig bindingConfig;
    
    /*
     * 
     */
    
    private final InterfaceParallelizer parallelizer;
    
    private final InterfaceParallelizer internalParallelizer;
    
    /*
     * 
     */
    
    private final MyHostLifecycleListener hostLifecycleListener = new MyHostLifecycleListener();
    
    private final Map<Object,InterfaceBwdHost> hostByWindow =
            new HashMap<Object,InterfaceBwdHost>();

    private final Map<Object,InterfaceBwdHost> hostByWindow_unmodView =
            Collections.unmodifiableMap(this.hostByWindow);
    
    /**
     * To have hosts ordered in some way, for determinism when returning lists of hosts.
     */
    private final List<InterfaceBwdHost> hostList = new ArrayList<InterfaceBwdHost>();
    
    /*
     * 
     */
    
    private final InterfaceBwdImageDisposalListener imageDisposalListener = new MyImageDisposalListener();
    
    private final Object imageRepoMutex = new Object();
    
    /**
     * Guarded by imageRepoMutex.
     * 
     * Keeping track of images indexes, for O(1) removal,
     * because there might be a lot of images.
     * 
     * Not using a set, for determinism.
     */
    private final List<InterfaceBwdImage> imageList = new ArrayList<InterfaceBwdImage>();
    
    /**
     * Guarded by imageRepoMutex.
     */
    private final Map<InterfaceBwdImage,Integer> indexByImage = new HashMap<InterfaceBwdImage,Integer>();
    
    /*
     * 
     */
    
    private final HostOfFocusedClientHolder hostOfFocusedClientHolder = new HostOfFocusedClientHolder();
    
    /*
     * 
     */
    
    /**
     * Volatile so that can be used concurrently
     * (not specified, but doesn't hurt).
     */
    private volatile boolean isShutdown = false;
    
    private final double eventLogicPeriodS;
    
    private MyEventLogicProcess eventLogicProcess;

    private final Deque<AbstractBwdHost> eventLogicHostDeque = new LinkedList<AbstractBwdHost>();
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Must call terminateConstruction() at the end of your constructor.
     * 
     * Depending on the backing library, it might not be possible to create
     * another binding once this one has been shut down, requiring to start
     * a new JVM for that.
     * 
     * @param singleInstanceRef Reference object to use to make sure that at most
     *        one not yet shut down instance of this binding exists at any time.
     *        Must be null to allow for multiple running instances at once.
     */
    public AbstractBwdBinding(
            BaseBwdBindingConfig bindingConfig,
            AtomicReference<AbstractBwdBinding> singleInstanceRef) {
        this.bindingConfig = LangUtils.requireNonNull(bindingConfig);
        
        this.singleInstanceRef = singleInstanceRef;
        
        if (singleInstanceRef != null) {
            if (!singleInstanceRef.compareAndSet(null, this)) {
                throw new IllegalStateException("a binding is already and still up");
            }
        }

        {
            final int parallelism = bindingConfig.getParallelizerParallelism();
            NbrsUtils.requireSup(0, parallelism, "parallelism");
            
            final String threadNamePrefix =
                getThreadsBaseName() + "-UI_PRLZR";

            this.parallelizer = BindingPrlUtils.newParallelizer(
                this.bindingConfig,
                parallelism,
                threadNamePrefix);
        }
        
        {
            final int parallelism = bindingConfig.getInternalParallelism();
            NbrsUtils.requireSup(0, parallelism, "parallelism");
            
            final String threadNamePrefix =
                getThreadsBaseName() + "-INT_PRLZR";
            
            this.internalParallelizer = BindingPrlUtils.newParallelizer(
                this.bindingConfig,
                parallelism,
                threadNamePrefix);
        }
        
        this.eventLogicPeriodS = bindingConfig.getWindowEventLogicPeriodS();
    }
    
    /*
     * 
     */
    
    @Override
    public BaseBwdBindingConfig getBindingConfig() {
        return this.bindingConfig;
    }
    
    public ScaleHelper getScaleHelper() {
        return this.bindingConfig.getScaleHelper();
    }
    
    /*
     * 
     */
    
    @Override
    public InterfaceParallelizer getParallelizer() {
        return this.parallelizer;
    }
    
    @Override
    public InterfaceParallelizer getInternalParallelizer() {
        return this.internalParallelizer;
    }
    
    /*
     * Hosts.
     */
    
    @Override
    public List<InterfaceBwdHost> getAllHostList() {
        return new ArrayList<InterfaceBwdHost>(this.hostList);
    }

    @Override
    public List<InterfaceBwdHost> getRootHostList() {
        final ArrayList<InterfaceBwdHost> result = new ArrayList<InterfaceBwdHost>();
        for (InterfaceBwdHost host : this.hostList) {
            if (host.getOwner() == null) {
                result.add(host);
            }
        }
        return result;
    }
    
    @Override
    public int getHostCount() {
        return this.hostByWindow.size();
    }

    public HostOfFocusedClientHolder getHostOfFocusedClientHolder() {
        return this.hostOfFocusedClientHolder;
    }
    
    /*
     * Screen info.
     */
    
    @Override
    public GRect getScreenBounds() {
        return this.getScaleHelper().rectOsToBdContained(
            this.getScreenBoundsInOs());
    }

    @Override
    public GRect getScreenBoundsInOs() {
        return this.getScreenBounds_rawInOs();
    }

    /*
     * Mouse info.
     */
    
    @Override
    public GPoint getMousePosInScreen() {
        return this.getScaleHelper().pointOsToBd(
            this.getMousePosInScreenInOs());
    }

    @Override
    public GPoint getMousePosInScreenInOs() {
        return this.getMousePosInScreen_rawInOs();
    }

    /*
     * Images.
     */

    @Override
    public InterfaceBwdImage newImage(String filePath) {
        final InterfaceBwdImage image = this.newImageImpl(
                filePath,
                this.imageDisposalListener);
        this.addImage(image);
        return image;
    }

    @Override
    public InterfaceBwdWritableImage newWritableImage(int width, int height) {
        NbrsUtils.requireSup(0, width, "width");
        NbrsUtils.requireSup(0, height, "height");
        
        final InterfaceBwdWritableImage image = this.newWritableImageImpl(
                width,
                height,
                this.imageDisposalListener);
        this.addImage(image);
        return image;
    }

    /*
     * Shutdown.
     */
    
    @Override
    public boolean isShutdown() {
        return this.isShutdown;
    }
    
    @Override
    public final void shutdownAbruptly() {
        
        this.getUiThreadScheduler().checkIsWorkerThread();
        
        if (!this.shutdownDownAlreadyCalled.compareAndSet(false, true)) {
            return;
        }
        
        try {
            this.shutdownAbruptly_generic();

            this.shutdownAbruptly_bindingSpecific();
        } finally {
            if (this.singleInstanceRef != null) {
                this.singleInstanceRef.set(null);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    /**
     * Must be called at end of constructors of concrete implementations,
     * to create generic treatments of this class that require binding features
     * not created by this generic class, such as the UI thread scheduler
     * or font homes.
     */
    protected final void terminateConstruction() {
        this.eventLogicProcess = new MyEventLogicProcess(
                this.getUiThreadScheduler());
    }

    /**
     * Final so that can be used in extending classes constructors.
     * 
     * @return Base name for threads of this binding, containing class
     *         simple name and a unique long tied to this instance.
     */
    protected final String getThreadsBaseName() {
        return this.threadsBaseName;
    }
    
    /**
     * @return An unmodifiable view of the internal map.
     */
    protected Map<Object,InterfaceBwdHost> hostByWindow_unmodView() {
        return this.hostByWindow_unmodView;
    }

    /*
     * Hosts.
     */
    
    /**
     * Don't override that, override corresponding xxxImpl methods instead.
     */
    protected final InterfaceHostLifecycleListener<AbstractBwdHost> getHostLifecycleListener() {
        return this.hostLifecycleListener;
    }

    /**
     * Overriding implementations must call super.
     */
    protected void onHostCreatedImpl(AbstractBwdHost host) {
        final Object forCheck = this.hostByWindow.put(host.getBackingWindow(), host);
        if (forCheck != null) {
            throw new BindingError("expected null, got " + forCheck);
        }
        
        this.hostList.add(host);
        
        this.eventLogicHostDeque.addLast(host);
        
        // Start, or restart for ASAP execution.
        this.eventLogicProcess.start();
        
        final GRect defaultBoundsInOs =
            this.bindingConfig.getDefaultClientOrWindowBoundsInOs();
        final ScaleHelper scaleHelper = this.bindingConfig.getScaleHelper();
        final GRect defaultBoundsInBd = scaleHelper.rectOsToBdContained(defaultBoundsInOs);
        // Doing it last, for it might throw,
        // either from backing library code
        // or user callbacks called synchronously.
        if (this.bindingConfig.getMustUseDefaultBoundsForClientElseWindow()) {
            host.setClientBounds(defaultBoundsInBd);
        } else {
            host.setWindowBounds(defaultBoundsInBd);
        }
    }

    /**
     * Overriding implementations must call super.
     */
    protected void onHostClosingImpl(AbstractBwdHost host) {
        final Object forCheck = this.hostByWindow.remove(host.getBackingWindow());
        if (forCheck != host) {
            throw new BindingError(forCheck + " != " + host);
        }
        
        /*
         * NB: O(n), but could be O(1) if keeping track
         * of index and swapping.
         */
        final boolean didRemove = this.hostList.remove(host);
        if (!didRemove) {
            throw new BindingError("could not remove host");
        }
    }
    
    /**
     * Overriding implementations must call super.
     */
    protected void onHostClosedEventFiringImpl(AbstractBwdHost host) {
        if (DEBUG) {
            Dbg.log("removing host " + host);
        }
        final boolean didRemove = this.eventLogicHostDeque.remove(host);
        if (!didRemove) {
            throw new IllegalArgumentException("host unknown");
        }
        if (this.eventLogicHostDeque.size() == 0) {
            // No need to keep it running now.
            this.eventLogicProcess.stop();
        }
    }
    
    /*
     * Screen info.
     */
    
    protected abstract GRect getScreenBounds_rawInOs();
    
    /*
     * Mouse info.
     */
    
    protected abstract GPoint getMousePosInScreen_rawInOs();
    
    /*
     * Images.
     */
    
    protected abstract InterfaceBwdImage newImageImpl(
            String filePath,
            InterfaceBwdImageDisposalListener disposalListener);
    
    protected abstract InterfaceBwdWritableImage newWritableImageImpl(
            int width,
            int height,
            InterfaceBwdImageDisposalListener disposalListener);
    
    /*
     * Shutdown.
     */
    
    /**
     * Implement binding-specific shutdown treatments here.
     * 
     * Hosts closing, fonts and images disposal,
     * and UI thread parallelizer (not scheduler) shutdown
     * are already done before this method gets called.
     */
    protected abstract void shutdownAbruptly_bindingSpecific();

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /*
     * Hosts.
     */
    
    private void closeAllHosts() {
        final Object[] hostArr = this.hostByWindow.values().toArray();
        for (Object host_ : hostArr) {
            final InterfaceBwdHost host = (InterfaceBwdHost) host_;
            host.close();
        }
    }
    
    /*
     * Images.
     */
    
    private void addImage(InterfaceBwdImage image) {
        synchronized (this.imageRepoMutex) {
            final int index = this.imageList.size();
            this.imageList.add(image);
            this.indexByImage.put(image, index);
        }
    }
    
    private void removeImage(InterfaceBwdImage image) {
        synchronized (this.imageRepoMutex) {
            final Integer indexRef = this.indexByImage.remove(image);
            if (indexRef == null) {
                // Already disposed.
                // Not supposed to happen, since current method
                // must only be called once per image.
                throw new AssertionError("image unknown : " + image);
            }
            final int index = indexRef.intValue();
            final int lastIndex = this.imageList.size() - 1;
            if (index == lastIndex) {
                this.imageList.remove(index);
            } else {
                // Moving last image to removed image index,
                // for O(1) removal.
                final InterfaceBwdImage lastImage = imageList.remove(lastIndex);
                this.imageList.set(index, lastImage);
                this.indexByImage.put(lastImage, indexRef);
            }
        }
    }
    
    private void disposeAllImages() {
        final InterfaceBwdImage[] imageArr;
        synchronized (this.imageRepoMutex) {
            imageArr = this.imageList.toArray(new InterfaceBwdImage[this.imageList.size()]);
        }
        for (InterfaceBwdImage image : imageArr) {
            image.dispose();
        }
    }
    
    /*
     * Shutdown.
     */
    
    private void shutdownParallelizerNow() {
        /*
         * Best effort.
         */
        if (this.parallelizer instanceof ExecutorParallelizer) {
            final ExecutorParallelizer prlzrImpl =
                (ExecutorParallelizer) this.parallelizer;
            final Executor executor = prlzrImpl.getExecutor();
            
            if (executor instanceof ExecutorService) {
                final ExecutorService es = (ExecutorService) executor;
                es.shutdownNow();
                
            } else if (executor instanceof HardScheduler) {
                final HardScheduler scheduler = (HardScheduler) executor;
                final boolean mustInterruptWorkingWorkers = false;
                scheduler.shutdownNow(mustInterruptWorkingWorkers);
                
            } else {
                // Unknown executor.
            }
        } else {
            // Unknown parallelizer.
        }
    }
    
    /**
     * Must be called on shutdown.
     * 
     * Closes hosts, disposes fonts and images,
     * and shuts down UI thread parallelizer.
     * 
     * Things to typically also shut down aside from that:
     * - The UI thread scheduler.
     * - The cursor repository if any.
     */
    private void shutdownAbruptly_generic() {
        this.isShutdown = true;
        
        if (false) {
            /*
             * Useless due to UI thread scheduler abrupt shutdown.
             */
            this.eventLogicProcess.stop();
        }
        
        this.closeAllHosts();
        
        final AbstractBwdFontHome<?,?> fontHome =
                (AbstractBwdFontHome<?,?>) this.getFontHome();
        fontHome.dispose();
        
        this.disposeAllImages();
        
        this.shutdownParallelizerNow();
    }
}
