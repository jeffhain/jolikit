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
package net.jolikit.bwd.impl.utils.basics;

public class OsUtils {

    /*
     * Derived from org.apache.commons.lang.SystemUtils.
     */
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * For lazy initialization.
     */
    private static class MyLazy {
        /**
         * os.name property, or "unknown" if not defined.
         */
        private static final String OS_NAME;
        static {
            String osName = System.getProperty("os.name");
            if (osName == null) {
                osName = "unknown";
            }
            OS_NAME = osName;
        }
        
        /*
         * Not necessarily exclusive (ex.: if MAC_OSX is true, Mac is true too).
         */
        
        private static final boolean IS_OS_WINDOWS = OS_NAME.startsWith("Windows");
        private static final boolean IS_OS_AIX = OS_NAME.startsWith("AIX");
        private static final boolean IS_OS_HP_UX = OS_NAME.startsWith("HP-UX");
        private static final boolean IS_OS_IRIX = OS_NAME.startsWith("Irix");
        private static final boolean IS_OS_LINUX = OS_NAME.startsWith("Linux") || OS_NAME.startsWith("LINUX");
        private static final boolean IS_OS_MAC = OS_NAME.startsWith("Mac");
        private static final boolean IS_OS_MAC_OSX = OS_NAME.startsWith("Mac OS X");
        private static final boolean IS_OS_OS2 = OS_NAME.startsWith("OS/2");
        private static final boolean IS_OS_SOLARIS = OS_NAME.startsWith("Solaris");
        private static final boolean IS_OS_SUN_OS = OS_NAME.startsWith("SunOS");

        private static final boolean IS_OS_UNIX =
            IS_OS_AIX || IS_OS_HP_UX || IS_OS_IRIX || IS_OS_LINUX ||
            IS_OS_MAC_OSX || IS_OS_SOLARIS || IS_OS_SUN_OS;
    }
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return os.name property, or "unknown" if it's not defined.
     */
    public static String getOsName() {
        return MyLazy.OS_NAME;
    }
    
    /*
     * 
     */
    
    public static boolean isWindows() {
        return MyLazy.IS_OS_WINDOWS;
    }
    
    public static boolean isAix() {
        return MyLazy.IS_OS_AIX;
    }
    
    public static boolean isHpUx() {
        return MyLazy.IS_OS_HP_UX;
    }
    
    public static boolean isIrix() {
        return MyLazy.IS_OS_IRIX;
    }
    
    public static boolean isLinux() {
        return MyLazy.IS_OS_LINUX;
    }

    /**
     * @return True if OS is Mac (OS X or not).
     */
    public static boolean isMac() {
        return MyLazy.IS_OS_MAC;
    }
    
    public static boolean isMacOsX() {
        return MyLazy.IS_OS_MAC_OSX;
    }
    
    public static boolean isOs2() {
        return MyLazy.IS_OS_OS2;
    }
    
    public static boolean isSolaris() {
        return MyLazy.IS_OS_SOLARIS;
    }
    
    public static boolean isSunOs() {
        return MyLazy.IS_OS_SUN_OS;
    }
    
    /*
     * 
     */
    
    /**
     * @return True if this is a UNIX like system, as in any of AIX,
     *         HP-UX, Irix, Linux, MacOSX, Solaris or SUN OS.
     */
    public static boolean isUnix() {
        return MyLazy.IS_OS_UNIX;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private OsUtils() {
    }
}
