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
package net.jolikit.test;

import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.lang.OsUtils;
import net.jolikit.lang.PropertiesFileUtils;

/**
 * Installation related test configuration.
 */
public class JlkTestConfig {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private static final String CONFIG_FILE_PATH;
    static {
        if (OsUtils.isWindows()) {
            CONFIG_FILE_PATH = "src/test/resources/test_config-win.properties";
        } else if (OsUtils.isMac()) {
            CONFIG_FILE_PATH = "src/test/resources/test_config-mac.properties";
        } else {
            CONFIG_FILE_PATH = "src/test/resources/test_config-fallback.properties";
        }
    }
    
    private static final PropertiesFileUtils PROPERTIES_FILE_UTILS =
            new PropertiesFileUtils(CONFIG_FILE_PATH);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @return Resolution of the device (number of physical pixels) along width.
     */
    public static int getDeviceResolutionWidth() {
        final String str = PROPERTIES_FILE_UTILS.getRequiredProperty("DEVICE_RESOLUTION_WIDTH");
        return Integer.parseInt(str);
    }

    /**
     * @return Resolution of the device (number of physical pixels) along height.
     */
    public static int getDeviceResolutionHeight() {
        final String str = PROPERTIES_FILE_UTILS.getRequiredProperty("DEVICE_RESOLUTION_HEIGHT");
        return Integer.parseInt(str);
    }
    
    /**
     * @return Decoration insets, for all bindings except Allegro5 binding.
     */
    public static GRect getDecorationInsets() {
        final String str = PROPERTIES_FILE_UTILS.getRequiredProperty("DECORATION_INSETS");
        return parseInsets(str);
    }
    
    /**
     * @return Decoration insets, specific for Allegro5 binding.
     */
    public static GRect getDecorationInsetsAlgr5() {
        final String str = PROPERTIES_FILE_UTILS.getRequiredProperty("DECORATION_INSETS_ALGR5");
        return parseInsets(str);
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private JlkTestConfig() {
    }
    
    private static GRect parseInsets(String str) {
        final String[] arr = str.split(";");
        final int left = Integer.parseInt(arr[0].trim());
        final int top = Integer.parseInt(arr[1].trim());
        final int right = Integer.parseInt(arr[2].trim());
        final int bottom = Integer.parseInt(arr[3].trim());
        return GRect.valueOf(left, top, right, bottom);
    }
}
