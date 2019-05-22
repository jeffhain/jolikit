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

enum TestPixelStatus {
    /**
     * Pixels out of clip are of course not to be painted.
     */
    OUT_OF_CLIP_NOT_PAINTED,
    OUT_OF_CLIP_PAINTED_ONCE,
    OUT_OF_CLIP_PAINTED_MULTIPLE_TIMES,
    /*
     * Remaining values for pixels in clip.
     */
    NOT_ALLOWED_NOT_PAINTED,
    NOT_ALLOWED_PAINTED_ONCE,
    NOT_ALLOWED_PAINTED_MULTIPLE_TIMES,
    ALLOWED_NOT_PAINTED,
    ALLOWED_PAINTED_ONCE,
    ALLOWED_PAINTED_MULTIPLE_TIMES,
    /**
     * Supersedes ALLOWED_NOT_PAINTED.
     */
    REQUIRED_NOT_PAINTED,
    /**
     * Supersedes ALLOWED_PAINTED_ONCE.
     */
    REQUIRED_PAINTED_ONCE,
    /**
     * Supersedes ALLOWED_PAINTED_ONCE.
     */
    REQUIRED_PAINTED_MULTIPLE_TIMES;
}
