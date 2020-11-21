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
package net.jolikit.test;

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

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private JlkTestConfig() {
    }
}
