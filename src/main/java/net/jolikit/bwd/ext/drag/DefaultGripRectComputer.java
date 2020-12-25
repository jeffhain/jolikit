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
package net.jolikit.bwd.ext.drag;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.lang.NbrsUtils;

public class DefaultGripRectComputer implements InterfaceGripRectComputer {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final int gripSpan;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param gripSpan Must be >= 0. Zero means center grip occupies
     *        the whole client area.
     */
    public DefaultGripRectComputer(int gripSpan) {
        NbrsUtils.requireSupOrEq(0, gripSpan, "gripSpan");
        this.gripSpan = gripSpan;
    }
    
    @Override
    public GRect computeGripRectInClientBox(
            GRect clientBox,
            GripType gripType) {
        
        final int gripSpan = this.gripSpan;
        
        final int gtx = gripType.xSide();
        final int gty = gripType.ySide();
        
        final int cbx = clientBox.x();
        final int cby = clientBox.y();
        final int cbxs = clientBox.xSpan();
        final int cbys = clientBox.ySpan();

        final int centerXSpan = Math.max(0, cbxs - 2 * gripSpan);
        final int centerYSpan = Math.max(0, cbys - 2 * gripSpan);
        
        final GRect gripBox = GRect.valueOf(
                cbx + ((gtx < 0) ? 0 : ((gtx == 0) ? gripSpan : gripSpan + centerXSpan)),
                cby + ((gty < 0) ? 0 : ((gty == 0) ? gripSpan : gripSpan + centerYSpan)),
                ((gtx != 0) ? gripSpan : centerXSpan),
                ((gty != 0) ? gripSpan : centerYSpan));
        return gripBox;
    }
}
