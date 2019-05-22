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
package net.jolikit.bwd.impl.utils.gprim;

/**
 * Interface to decide whether a figure must be painted with
 * an algorithm specific for huge spans (typically iterating
 * on clip pixels and deciding whether they must be painted,
 * which should never take too long as clips are always smaller than
 * (because included in) the client area), instead of with
 * a regular figure drawing algorithm (typically iterating on points
 * of the curve to draw or fill, which can take too long if the figure
 * is too large).
 * 
 * Not using the clip as argument, to make sure painted pixels
 * don't change when just the clip changes, which would be surprising.
 */
public interface InterfaceHugeAlgoSwitch {

    /**
     * @param xSpan Figure bounding box x span.
     * @param ySpan Figure bounding box y span.
     * @return True if must huge drawing specific algorithm,
     *         false otherwise. 
     */
    public boolean mustUseHugeAlgorithm(int xSpan, int ySpan);
}
