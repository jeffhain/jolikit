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

import java.util.List;

import net.jolikit.bwd.api.events.InterfaceBwdEventListener;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;

/**
 * Interface for BWD clients, each of which occupies
 * the client area of a BWD host.
 */
public interface InterfaceBwdClient extends InterfaceBwdEventListener {

    /**
     * The host must not be used in this method, only the reference,
     * for it might not be fully constructed yet.
     * 
     * NB: Using Object type to avoid a cyclic dependency with host type.
     * 
     * @param host The host which receives this client in its client area.
     *        Must not be null.
     */
    public void setHost(Object host);

    /*
     * 
     */
    
    /**
     * Useful for clients that buffer potentially spammy events,
     * such as mouse moves, to reduce their frequency by undersampling
     * them and only forwarding the latest one of each burst.
     * 
     * To be called before paintClient(), not to buffer events across
     * paintings, which would make it harder to grasp what's going on
     * and could cause some inconsistencies.
     * 
     * Note that processing of events such as drag events might cause
     * client bounds modifications, so these bounds must be retrieved
     * AFTER this call, when computing the box for the painting.
     * 
     * Similarly, processing of events might change window state,
     * so deciding whether window state allows for painting
     * should be done AFTER this call.
     */
    public void processEventualBufferedEvents();
    
    /**
     * Must paint client, synchronously.
     * 
     * Might do nothing if dirtyRect is empty and painting was already
     * up to date according to the client.
     * 
     * A single painting might correspond to multiple calls to
     * host.makeDirtyAndEnsurePendingClientPainting(...), but we only specify
     * a single dirty rectangle here, not a list, for multiple reasons.
     * First, a single enclosing dirty rectangle can easily be computed from
     * a list of dirty rectangles.
     * Second, the list would most often only contain a single dirty rectangle.
     * Third, it's much easier to deal with on the client side.
     * 
     * @param g The root graphics for drawing on the client,
     *        with box and initial clip identical to client area,
     *        and already initialized.
     * @param dirtyRect A rectangle that is dirty and must be repaint even if
     *        things to display in it didn't change on client side.
     *        Must not cover pixels outside client area, to make it easier
     *        to deal with from client side.
     *        Must not be null, but can be empty (the general case).
     * @return A list of rectangles corresponding to the areas that were
     *         actually painted, to indicate to the binding which pixels
     *         might have changed, in case that could help to optimize rendering.
     *         Should cover the pixels of the specified dirty rectangle
     *         if it's not empty, and possibly more.
     *         Rectangles are allowed to cover pixels outside client area
     *         (the binding has to clip them properly before use).
     *         Can be empty (if dirtyRect was empty, and painting was already
     *         up to date).
     *         Can also be null, in which case the whole client area
     *         must be considered as having been painted.
     */
    public List<GRect> paintClient(
            InterfaceBwdGraphics g,
            GRect dirtyRect);
}
