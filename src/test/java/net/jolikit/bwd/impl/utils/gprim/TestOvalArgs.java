/*
 * Copyright 2020 Jeff Hain
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

import net.jolikit.bwd.api.graphics.GRect;

/**
 * Definition of an oval for tests.
 */
final class TestOvalArgs {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final GRect oval;
    
    private final boolean mustUsePolyAlgo;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public TestOvalArgs(
            GRect oval,
            boolean mustUsePolyAlgo) {
        this.oval = oval;
        
        this.mustUsePolyAlgo = mustUsePolyAlgo;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[oval = ").append(this.oval);
        sb.append(", mustUsePolyAlgo = ").append(this.mustUsePolyAlgo);
        sb.append("]");
        return sb.toString();
    }
    
    public GRect getOval() {
        return this.oval;
    }

    public boolean getMustUsePolyAlgo() {
        return this.mustUsePolyAlgo;
    }
}
