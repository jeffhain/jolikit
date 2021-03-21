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
package net.jolikit.bwd.api;

import java.util.ConcurrentModificationException;
import java.util.List;

import net.jolikit.bwd.api.fonts.InterfaceBwdFontHome;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdImage;
import net.jolikit.bwd.api.graphics.InterfaceBwdWritableImage;
import net.jolikit.threading.prl.InterfaceParallelizer;
import net.jolikit.time.sched.InterfaceWorkerAwareScheduler;

/**
 * Interface for BWD bindings.
 * 
 * The entry point of BWD API.
 */
public interface InterfaceBwdBinding {
    
    /*
     * Hosts.
     */
    
    /**
     * Creates a non-dialog host.
     * See host API to create a dialog host.
     * 
     * Where the host appears when shown is undefined,
     * so you should set window or client bounds before showing it
     * (or after if you prefer, but it increases the chance of flickering).
     * 
     * Title must not be null as it can be used even if not decorated,
     * such as in task bar.
     * 
     * Decoration flag is here to be able to use native decoration,
     * but it could also be implemented in the binding.
     * 
     * Initial position is not specified at creation time, to keep things
     * simple, to allow for host creation before it is known, and because
     * it would be redundant with the possibility to specify target bounds
     * after creation and before showing.
     * The bounds to use for a host which target bounds have not been set,
     * are not specified, but for consistency at least across runs it should
     * be good to make it a configurable default value, or a function of
     * screen bounds.
     * 
     * @param title Must not be null.
     * @param decorated Whether the created host should be decorated.
     * @param client The client to use for the created host.
     * @return A new non-host.
     * @throws IllegalStateException if this binding is shut down.
     */
    public InterfaceBwdHost newHost(
            String title,
            boolean decorated,
            //
            InterfaceBwdClient client);
    
    /**
     * @return A new mutable list containing all created
     *         and not yet closed hosts.
     */
    public List<InterfaceBwdHost> getAllHostList();

    /**
     * @return A new mutable list containing all created
     *         and not yet closed root hosts (non-dialog hosts).
     */
    public List<InterfaceBwdHost> getRootHostList();

    /**
     * @return The number of created and not yet closed hosts.
     */
    public int getHostCount();

    /*
     * Threading.
     */
    
    /**
     * This method can be used form any thread.
     * 
     * @return A scheduler executing runnables in the UI thread.
     */
    public InterfaceWorkerAwareScheduler getUiThreadScheduler();
    
    /**
     * Not splitting it into two booleans, one for clients
     * and one for writable images, because in practice
     * they have strong reasons to be the same.
     * 
     * @return True if graphics (for clients and for writable images)
     *         descending from a same root graphics (and root graphics itself)
     *         can be used in parallel to paint non-overlapping areas,
     *         false otherwise.
     */
    public boolean isParallelPaintingSupported();

    /**
     * @return True if fonts creation and disposal is thread-safe,
     *         false otherwise, in which case it should only be
     *         done in UI thread (which could be required).
     */
    public boolean isConcurrentFontManagementSupported();

    /**
     * @return True if images from file creation and disposal is thread-safe,
     *         false otherwise, in which case it should only be
     *         done in UI thread (which could be required).
     */
    public boolean isConcurrentImageFromFileManagementSupported();
    
    /**
     * @return True if writable images can be created, drawn (graphics usage)
     *         and disposed from another thread than UI thread,
     *         false otherwise.
     */
    public boolean isConcurrentWritableImageManagementSupported();

    /**
     * The parallelizer must be reentrant, which allows for parallelization
     * from treatments possibly already called from parallelized treatments.
     * To avoid deadlocks in case of multiple reentrant calls, the parallelizer
     * must also use calling/user thread as worker thread.
     * 
     * This parallelizer is meant to be used from UI thread, or from one of its
     * worker threads, either for parallel painting if supported, or to
     * parallelize treatments otherwise done in UI thread, but usually there is
     * no reason why it would not also be usable from any other thread, other
     * than not wanting to steal parallelism from parallelizations done from
     * UI thread.
     * 
     * @return A reentrant parallelizer, meant to be used from UI thread
     *         or from one of its worker threads.
     */
    public InterfaceParallelizer getParallelizer();
    
    /*
     * Screen info.
     */
    
    /**
     * Must only be called in UI thread.
     * 
     * Bounds within which drawing is supposed to occur.
     * 
     * Whether these bounds are those of a particular physical screen,
     * those of a virtual screen composed of multiple physical screens,
     * or those of the "available" (minus task bars and such) part of
     * some screen, is not defined.
     * 
     * By default it should return the "available" bounds of primary screen,
     * so as not to mess with virtual screen subtleties and so as not
     * to accidentally hide task bars and such (for example when
     * maximizing a window).
     * 
     * Design note: Some libraries don't provide a reliable way of
     * retrieving such bounds, and others don't provide access to
     * "available" bounds.
     * As a result, we keep it deliberately loose, which allows for
     * versatility without imposing the burden of having to implement
     * a rich API.
     * Typically, which kind of bounds are returned could be a configuration
     * parameter of the binding, or the bounds value could be just read
     * from configuration.
     * If methods for specific or virtual screen bounds were to be added,
     * conflict with this method could be avoided by using signatures like
     * "getPhysicalScreenBounds(int screenIndex)",
     * "getPhysicalScreenAvailableBounds(int screenIndex)",
     * or "getVirtualScreenBounds()".
     * 
     * NB: This method should recompute bounds on every call if possible,
     * in case bounds would change dynamically.
     * 
     * @return Bounds within which drawing is supposed to occur,
     *         in screen coordinates.
     */
    public GRect getScreenBounds();
    
    /*
     * Mouse info.
     */
    
    /**
     * Note that as it's a best effort computation, it might not only be
     * more or less obsolete, but it might also be out of screen, due to
     * the eventual asynchronous nature of the best effort computation,
     * for example if it's just the last position computed upon processing
     * an event.
     * 
     * @return Coordinates of the mouse pointer in screen coordinates,
     *         or a best effort computation of it.
     */
    public GPoint getMousePosInScreen();

    /*
     * Images.
     */
    
    /**
     * Supported image types depend on the implementation.
     * 
     * The image should be fully loaded when this method returns.
     * If wanting to draw images obtained from streams, the download should be
     * taken care of aside, before calling this method, eventually by showing
     * progress information in the mean time, or by calling this method with
     * files corresponding to the image loaded so far.
     * If wanting to rely on bindings that allow for asynchronous images
     * downloads, what drawing a partially loaded image should do is not
     * defined here.
     * 
     * @param filePath Path of an image file.
     * @return The corresponding image.
     * @throws IllegalArgumentException if could not load the image due to
     *         format support issue (not internal errors, which should cause
     *         an error).
     */
    public InterfaceBwdImage newImage(String filePath);
    
    /**
     * The created image has its graphics initialized by default,
     * and the initial color of its pixels is zero (fully transparent).
     * 
     * @param width Width for the image to create. Must be > 0.
     * @param height Height for the image to create. Must be > 0.
     * @return A new writable image with the specified width and height.
     */
    public InterfaceBwdWritableImage newWritableImage(int width, int height);
    
    /*
     * Fonts.
     */
    
    public InterfaceBwdFontHome getFontHome();
    
    /*
     * Shutdown.
     */
    
    /**
     * @return Whether this binding shutdown has been initiated.
     */
    public boolean isShutdown();
    
    /**
     * Initiates an orderly shut down of all the resources
     * associated with this binding.
     * 
     * Must close all hosts, dispose all fonts and images,
     * and any other backing library related resources,
     * and then shut down UI thread scheduler, abruptly if possible,
     * which means that schedules can be aborted without cancellable's
     * onCancel() method being called.
     * 
     * Note that all resources are not necessarily released
     * when this method completes, since with some backing libraries
     * there is no way to know when they are.
     * In particular, even though the UI thread scheduler must be shut down,
     * some treatments might still execute themselves in it, for example
     * if they have been scheduled, possibly by the backing library,
     * before shutdown call.
     * 
     * For a more graceful shut down, first cancel/stop all your tasks that run
     * in UI thread, eventually also close your hosts yourself and process
     * related window events, and then, when everything is quiescent enough
     * for you, schedule a call to this method.
     * 
     * @throws ConcurrentModificationException if not called in UI thread.
     */
    public void shutdownAbruptly();
}
