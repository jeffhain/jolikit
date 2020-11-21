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
package net.jolikit.bwd.test.mains.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.jolikit.build.JlkBinConfig;
import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.basics.ScreenBoundsType;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBindingMainLaunchInfo;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseHome;
import net.jolikit.bwd.test.utils.InterfaceMainLaunchInfo;
import net.jolikit.lang.Dbg;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.OsUtils;
import net.jolikit.lang.RuntimeExecHelper;
import net.jolikit.lang.RuntimeExecHelper.InterfaceExecution;

/**
 * Utilities for our mains to decide whether or not they must
 * relaunch themselves, typically to ensure having a clean and/or
 * properly setup classpath, and proper JVM options.
 * 
 * Designed for use:
 * - In binding-bound mains, so that they can relaunch themselves
 *   with required binding-specific classpath and/or JVM options
 *   (such as when launched from the IDE with no specific options).
 * - In test cases choices GUI main, so that it can relaunch itself
 *   to get rid of an eventual presence of SWT in the classpath,
 *   which can conflict with the Swing GUI (yes, it's not bootstrapped).
 * - When launching a binding-bound main on a test case
 *   from test cases GUI, to indicate that proper classpath
 *   and/or options have been set and that it must not relaunch itself.
 */
public class BwdMainLaunchUtils {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    private static final String JOLIKIT_AUTO_COMP_DIR_PATH = JlkBinConfig.getAutoCompDirPath();

    /**
     * Class path for non binding-specific dependencies.
     */
    private static final List<String> JAVA_COMMON_DEPS_CLASSPATH_LIST =
            JlkBinConfig.getCommonClasspathList();
    
    /**
     * Separator for classpath entries.
     */
    private static final String CP_SEP = (OsUtils.isWindows() ? ";" : ":");
    
    protected static final int KILL_AREA_SPANS = 50;

    /**
     * Somehow large, to make sure eventual host decorations
     * won't overlap each other.
     */
    private static final int CLIENT_SPACING = 40;

    /*
     * 
     */
    
    /**
     * If present, means that the main must not relaunch itself,
     * and do best effort with current JVM options, classpath,
     * and main's arguments.
     */
    public static final String MAIN_ARG_MRITJ = "-mustRunInThisJvm";

    /**
     * Test case home class name.
     */
    public static final String MAIN_ARG_TCHCN = "-testCaseHomeClassName";
    
    /**
     * Initial client position.
     */
    public static final String MAIN_ARG_ICP = "-initialClientPosition";
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final String javaPath;

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @param javaPath Path of java executable.
     */
    public BwdMainLaunchUtils(String javaPath) {
        this.javaPath = LangUtils.requireNonNull(javaPath);
    }
    
    /**
     * By default, launching in a new JVM, with just what we need
     * on the classpath, to avoid interferences from jars in IDE classpath,
     * such as swt.jar which on Mac prevents AWT/Swing to work properly.
     * 
     * @param args Must not be null.
     */
    public static boolean mustRunInThisJvm(String[] args) {
        final boolean ret = containsMritjArg(args);
        if (ret) {
            if (DEBUG) {
                Dbg.log("MUST RUN IN THIS JVM");
            }
        } else {
            if (DEBUG) {
                Dbg.log("MUST RUN IN ANOTHER JVM");
            }
        }
        return ret;
    }

    /**
     * @param args Must not be null.
     */
    public InterfaceExecution launchMainInANewJvmWithMritjAdded(
            InterfaceMainLaunchInfo mainLaunchInfo,
            String[] args) {
        
        final List<String> progArgList = Arrays.asList(args);
        
        return this.launchMainInANewJvmWithMritjAdded_argList(mainLaunchInfo, progArgList);
    }

    /**
     * @param args Must not be null.
     */
    public void launchMainInANewJvmWithMritjAddedAndKillGui(
            InterfaceMainLaunchInfo mainLaunchInfo,
            String[] args) {
        
        final InterfaceExecution execution = this.launchMainInANewJvmWithMritjAdded(mainLaunchInfo, args);
        
        final List<InterfaceExecution> executionList = Arrays.asList(execution);
        launchKillGuiAndWaitForNoExecNoIE(executionList);
    }

    /**
     * Launches L*H JVMs, L being the number of main launch info,
     * and H the number of test case homes.
     * To avoid issues, a good practice is to only allow
     * for one list to contain more than one element.
     * 
     * For simplicity, and because when wanting to use and compare
     * different test cases they should most likely be similar,
     * some properties are only read on first test case home,
     * and applied to all:
     * - getMustSequenceLaunches()
     * - getInitialClientSpans()
     * If a test case home doesn't have the same value than first home
     * for getMustSequenceLaunches(), it's ignored.
     * 
     * @param superimposed Can be overridden to true.
     */
    public void launchBindingMainsWithTestCasesInNewJvmsWithMritjAdded(
            List<InterfaceBindingMainLaunchInfo> mainLaunchInfoList,
            List<InterfaceBwdTestCaseHome> testCaseHomeList,
            boolean superimposed) {
        
        final InterfaceBwdTestCaseHome firstHome = testCaseHomeList.get(0);
        
        final boolean oneAtATime = firstHome.getMustSequenceLaunches();
        if (oneAtATime) {
            // Forcibly superimposed if one at a time
            // (no need to use different locations).
            superimposed = true;
        }
        
        final GPoint initialClientSpans = firstHome.getInitialClientSpans();
        
        /*
         * 
         */
        
        final List<InterfaceExecution> executionList = new ArrayList<InterfaceExecution>();
        
        /*
         * 
         */
        
        final int mainCount = mainLaunchInfoList.size() * testCaseHomeList.size();
        final int colCount = (int) Math.ceil(Math.sqrt(mainCount));
        final int rowCount = mainCount / colCount;
        
        final GRect screenBounds = BwdTestUtils.getScreenBoundsFromDefaultBinding(
                ScreenBoundsType.PRIMARY_SCREEN_AVAILABLE);
        {
            final InterfaceBwdBinding bindingForScreenBounds = BwdTestUtils.getDefaultBinding();
            bindingForScreenBounds.getUiThreadScheduler().execute(new Runnable() {
                @Override
                public void run() {
                    bindingForScreenBounds.shutdownAbruptly();
                }
            });
        }
        
        int k = -1;
        for (InterfaceBwdTestCaseHome testCaseHome : testCaseHomeList) {
            if (testCaseHome.getMustSequenceLaunches() != oneAtATime) {
                // Ignoring it.
                continue;
            }
            
            final String testCaseHomeClassName = testCaseHome.getClass().getName();

            for (InterfaceBindingMainLaunchInfo mainLaunchInfo : mainLaunchInfoList) {
                k++;
                
                final String mainClassName = mainLaunchInfo.getMainClassName();

                final List<String> progArgList = new ArrayList<String>();
                
                progArgList.add(MAIN_ARG_TCHCN);
                progArgList.add(testCaseHomeClassName);
                
                if (superimposed) {
                    // No bounds arg: all will be centered.
                } else {
                    final int row = k / colCount;
                    final int col = k % colCount;

                    final boolean centeredElseNearTopLeft = true;
                    final int xOffset;
                    final int yOffset;
                    if (centeredElseNearTopLeft) {
                        final int totalWidth = colCount * (initialClientSpans.x() + CLIENT_SPACING) - CLIENT_SPACING;
                        final int totalHeight = rowCount * (initialClientSpans.y() + CLIENT_SPACING) - CLIENT_SPACING;
                        xOffset = (screenBounds.xSpan() - totalWidth)/2;
                        yOffset = (screenBounds.ySpan() - totalHeight)/2;
                    } else {
                        xOffset = screenBounds.x() + KILL_AREA_SPANS + CLIENT_SPACING;
                        yOffset = screenBounds.y() + KILL_AREA_SPANS + CLIENT_SPACING;
                    }
                    final int x =
                            xOffset
                            + col * (initialClientSpans.x() + CLIENT_SPACING);
                    final int y =
                            yOffset
                            + row * (initialClientSpans.y() + CLIENT_SPACING);
                    progArgList.add(MAIN_ARG_ICP);
                    progArgList.add(Integer.toString(x));
                    progArgList.add(Integer.toString(y));
                }
                
                if (DEBUG) {
                    if (oneAtATime) {
                        Dbg.log("begin [" + mainClassName + "]");
                    }
                }
                
                final InterfaceExecution execution =
                        launchMainInANewJvmWithMritjAdded_argList(
                                mainLaunchInfo,
                                progArgList);
                
                if (oneAtATime) {
                    RuntimeExecHelper.waitForNoIE(execution);
                    
                    if (DEBUG) {
                        Dbg.log("end [" + mainClassName + "]");
                    }
                } else {
                    executionList.add(execution);
                }
            }
        }
        
        if (!oneAtATime) {
            launchKillGuiAndWaitForNoExecNoIE(executionList);
        }
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static void launchKillGuiAndWaitForNoExecNoIE(List<InterfaceExecution> executionList) {
        final KillGui killGui = new KillGui(executionList);
        killGui.launchGui();
        
        // Waiting for all executions to be done.
        final InterfaceExecution[] executionArr = executionList.toArray(new InterfaceExecution[executionList.size()]);
        for (InterfaceExecution execution : executionArr) {
            RuntimeExecHelper.waitForNoIE(execution);
        }

        // Don't need kill GUI anymore.
        killGui.stop();
    }
    
    /*
     * 
     */
    
    private InterfaceExecution launchMainInANewJvmWithMritjAdded_argList(
            InterfaceMainLaunchInfo mainLaunchInfo,
            List<String> progArgList) {
        
        final String mainClassName = mainLaunchInfo.getMainClassName();
        
        final String jvmArgsStr = mainLaunchInfo.getSpecificJvmArgs();

        final String cp;
        {
            final StringBuilder sb = new StringBuilder();
            
            final String commonCp = getCommonCp();
            sb.append(commonCp);
            
            final List<String> specificCpList = mainLaunchInfo.getSpecificClasspathList();
            for (String str : specificCpList) {
                sb.append(CP_SEP);
                sb.append(str);
            }

            cp = sb.toString();
        }
        
        final String newProgArgsStr;
        {
            final List<String> newProgArgList = new ArrayList<String>();
            
            newProgArgList.add(MAIN_ARG_MRITJ);
            
            newProgArgList.addAll(progArgList);
            
            newProgArgsStr = toArgsStr(newProgArgList);
        }
        
        final String cmd = this.javaPath
                + ((jvmArgsStr.length() == 0) ? "" : " " + jvmArgsStr)
                + " -cp " + cp
                + " " + mainClassName
                + ((newProgArgsStr.length() == 0) ? "" : " " + newProgArgsStr);
        if (DEBUG) {
            Dbg.log("cmd = " + cmd);
        }
    
        final InterfaceExecution execution =
                RuntimeExecHelper.execAsync(
                        cmd,
                        System.out,
                        System.out);
        
        return execution;
    }
    
    /*
     * 
     */
    
    private static String getCommonCp() {
        final String jolikitClassesCp = JOLIKIT_AUTO_COMP_DIR_PATH;

        final String commonCp;
        {
            final StringBuilder sb = new StringBuilder();
            
            sb.append(jolikitClassesCp);
            
            for (String str : JAVA_COMMON_DEPS_CLASSPATH_LIST) {
                sb.append(CP_SEP);
                sb.append(str);
            }
            commonCp = sb.toString();
        }

        return commonCp;
    }
    
    private static boolean containsMritjArg(String[] args) {
        boolean ret = false;
        for (String arg : args) {
            if (arg.equals(MAIN_ARG_MRITJ)) {
                ret = true;
                break;
            }
        }
        return ret;
    }
    
    private static String toArgsStr(List<String> argList) {
        final StringBuilder sb = new StringBuilder();

        for (String arg : argList) {
            if (sb.length() != 0) {
                sb.append(" ");
            }
            sb.append(arg);
        }

        return sb.toString();
    }
}
