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
package net.jolikit.bwd.impl.qtj4;

import net.jolikit.bwd.api.graphics.GRect;

import com.trolltech.qt.core.QRect;
import com.trolltech.qt.gui.QPolygon;

public class QtjUtils {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static GRect toGRect(QRect rect) {
        return GRect.valueOf(
                rect.x(),
                rect.y(),
                rect.width(),
                rect.height());
    }
    
    public static QPolygon toQPolygon(
            int[] xArr,
            int[] yArr,
            int pointCount) {
        // We guess the constructor arg is initial point capacity.
        // Qt docs say it's the "size",
        // but don't tell what that means.
        final QPolygon polygon = new QPolygon(pointCount);
        for (int i = 0; i < pointCount; i++) {
            polygon.add(xArr[i], yArr[i]);
        }
        return polygon;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private QtjUtils() {
    }
}
