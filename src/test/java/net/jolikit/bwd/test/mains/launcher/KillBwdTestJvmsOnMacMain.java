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
package net.jolikit.bwd.test.mains.launcher;

import net.jadecy.utils.MemPrintStream;
import net.jolikit.lang.OsUtils;
import net.jolikit.lang.RuntimeExecHelper;
import net.jolikit.lang.Unchecked;

public class KillBwdTestJvmsOnMacMain {

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public static void main(String[] args) {
        if (!OsUtils.isMac()) {
            System.out.println("not MAC: doing nothing");
            return;
        }
        
        final MemPrintStream stream = new MemPrintStream();
        RuntimeExecHelper.execSyncNoIE("ps -ef", stream);
        
        // Added that because at some point thought it could help
        // debug or fix an issue I don't remember about much,
        // which was something like not seeing any output.
        // Keeping it in case it helps.
        Unchecked.sleepMs(100L);
        
        boolean foundToKill = false;
        for (String line : stream.getLines()) {
            final String mainPackageStart = "net.jolikit";
            final boolean mustKillIt =
                    line.contains(mainPackageStart)
                    // Not killing self.
                    && !line.contains(KillBwdTestJvmsOnMacMain.class.getName());
            if (mustKillIt) {
                foundToKill = true;
                
                line = line.replaceAll("  ", "");
                
                final String killCmd;
                {
                    final int from = line.indexOf(' ') + 1;
                    final int to = line.indexOf(' ', from);
                    final String pidStr = line.substring(from, to);
                    killCmd = "kill -9 " + pidStr;
                }
                
                final String mainClassName;
                {
                    final int from = line.lastIndexOf(mainPackageStart);
                    int to = line.indexOf(' ', from);
                    if (to < 0) {
                        to = line.length();
                    }
                    mainClassName = line.substring(from, to);
                }
                
                System.out.println(killCmd + " (" + mainClassName + ")");
                RuntimeExecHelper.execAsync(killCmd);
                
            }
        }
        
        if (!foundToKill) {
            System.out.println("nothing to kill");
        }
    }
}
