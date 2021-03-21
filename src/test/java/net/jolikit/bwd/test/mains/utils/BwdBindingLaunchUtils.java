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
package net.jolikit.bwd.test.mains.utils;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.SortedSet;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractBwdBinding;
import net.jolikit.bwd.impl.utils.BaseBwdBindingConfig;
import net.jolikit.bwd.test.utils.ArgsHelper;
import net.jolikit.bwd.test.utils.BwdTestUtils;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseHome;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseHomeProvider;
import net.jolikit.lang.Dbg;
import net.jolikit.time.sched.AbstractProcess;

public class BwdBindingLaunchUtils {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final ArgsHelper AH = new ArgsHelper();
    private static final String OPT_MRITJ = BwdMainLaunchUtils.MAIN_ARG_MRITJ;
    private static final String OPT_TCHCN = BwdMainLaunchUtils.MAIN_ARG_TCHCN;
    private static final String OPT_ICP = BwdMainLaunchUtils.MAIN_ARG_ICP;
    static {
        AH.registedKey(
                OPT_MRITJ, 0, false,
                "not to relaunch in another JVM");
        AH.registedKey(
                OPT_TCHCN, 1, false,
                "<test_case_home_class_name> (must implement "
                        + InterfaceBwdTestCase.class.getSimpleName(
                                ) + ") (if not present, using a default test case home)");
        AH.registedKey(
                OPT_ICP, 2, false,
                "<x> <y> (initial client position) (if not present, centering)");
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * To be called first.
     */
    public static InterfaceBwdTestCaseHome getTestCaseHome(
        String[] args,
        InterfaceBwdTestCaseHomeProvider testCaseHomeProvider) {
        
        if (DEBUG) {
            Dbg.log("getTestCaseHome(...)");
        }

        // Doing complete args check only once, here.
        AH.checkArgs(args, System.out);

        final InterfaceBwdTestCaseHome testCaseHome;
        final String homeClassName = AH.getArgN(args, OPT_TCHCN, 1);
        if (homeClassName == null) {
            testCaseHome = testCaseHomeProvider.getDefaultHome();
        } else {
            testCaseHome = testCaseHomeProvider.getHome(homeClassName);
            if (testCaseHome == null) {
                throw new IllegalArgumentException("unknown home class name: " + homeClassName);
            }
        }
        return testCaseHome;
    }
    
    /**
     * To be called second.
     */
    public static void setParallelizerParallelism(
        InterfaceBwdTestCaseHome testCaseHome,
        BaseBwdBindingConfig bindingConfig) {
        
        if (DEBUG) {
            Dbg.log("setParallelizerParallelism(...)");
        }
        
        final Integer prlRef = testCaseHome.getParallelizerParallelismElseNull();
        if (prlRef != null) {
            bindingConfig.setParallelizerParallelism(prlRef.intValue());
        }
    }
    
    /**
     * To be called third.
     */
    public static void launchBindingWithTestCase(
            String[] args,
            final InterfaceBwdBinding binding,
            String bindingName,
            InterfaceBwdTestCaseHome testCaseHome) {

        if (DEBUG) {
            Dbg.log("launchBindingWithTestCase(...)");
            Dbg.log("bindingName = " + bindingName);
        }

        final AbstractBwdBinding bindingImpl = (AbstractBwdBinding) binding;
        
        final BaseBwdBindingConfig bindingConfig = bindingImpl.getBindingConfig();
        
        if (DEBUG) {
            Dbg.log("configuring binding from test case home...");
        }
        
        {
            final Integer iRef = testCaseHome.getScaleElseNull();
            if (iRef != null) {
                bindingConfig.setScale(iRef.intValue());
            }
        }
        {
            final Double dRef = testCaseHome.getClientPaintDelaySElseNull();
            if (dRef != null) {
                bindingConfig.setClientPaintingDelayS(dRef.doubleValue());
            }
        }
        bindingConfig.setBonusSystemFontFilePathList(testCaseHome.getBonusSystemFontFilePathList());
        bindingConfig.setDefaultFontInfoComputer(testCaseHome.getDefaultFontInfoComputer());
        {
            final Boolean bRef = testCaseHome.getMustUseFontBoxForFontKindElseNull();
            if (bRef != null) {
                bindingConfig.setMustUseFontBoxForFontKind(bRef);
            }
        }
        {
            final Boolean bRef = testCaseHome.getMustUseFontBoxForCanDisplayElseNull();
            if (bRef != null) {
                bindingConfig.setMustUseFontBoxForCanDisplay(bRef);
            }
        }

        if (DEBUG) {
            Dbg.log("loading fonts...");
        }
        
        // Loading fonts before creating test case,
        // in case its creation would make use of default font
        // or other fonts to inititialize stuffs.
        final SortedSet<String> loadedFontFilePathSet =
                binding.getFontHome().loadSystemAndUserFonts(
                        testCaseHome.getUserFontFilePathListElseNull());
        
        if (DEBUG) {
            Dbg.log("creating test case...");
        }
        
        final InterfaceBwdTestCase testCase = testCaseHome.newTestCase(binding);
        
        if (DEBUG) {
            Dbg.log("getting initial client bounds...");
        }
        
        // Using default if null.
        GRect initialClientBounds = getInitialClientBounds(args, testCaseHome);
        if (initialClientBounds == null) {
            if (DEBUG) {
                Dbg.log("computing default initial client bounds...");
            }
            initialClientBounds = BwdTestUtils.computeDefaultInitialClientBounds(binding, testCaseHome);
        }

        if (DEBUG) {
            Dbg.log("initialClientBounds = " + initialClientBounds);
        }

        if (DEBUG) {
            Dbg.log("configuring binding from test case...");
        }
        
        bindingConfig.setMustImplementBestEffortPixelReading(
                testCase.getMustImplementBestEffortPixelReading());
        
        if (DEBUG) {
            Dbg.log("creating client...");
        }

        final boolean noHostDeco = !testCase.getHostDecorated();
        final double windowAlphaFp = testCase.getWindowAlphaFp();

        final String baseTitle = "BWD-" + bindingName + "-" + testCase.getClass().getSimpleName();
        final String title = baseTitle + "-" + 1;

        final InterfaceBwdTestCaseClient client = testCase.newClient();
        if (client instanceof InterfaceBwdTestCase) {
            final UncaughtExceptionHandler exceptionHandler = client.getExceptionHandlerElseNull();
            if (exceptionHandler != null) {
                if (DEBUG) {
                    Dbg.log("configuring binding from client as test aase...");
                }
                bindingConfig.setExceptionHandler(exceptionHandler);
            }
            client.setLoadedFontFilePathSet(loadedFontFilePathSet);
        }
        
        if (DEBUG) {
            Dbg.log("creating host...");
        }

        final InterfaceBwdHost host = binding.newHost(
                title,
                !noHostDeco,
                //
                client);
        host.setWindowAlphaFp(windowAlphaFp);

        if (DEBUG) {
            Dbg.log("setting initial client bounds...");
        }
        
        host.setClientBounds(initialClientBounds);
        
        if (DEBUG) {
            Dbg.log("showing host...");
        }
        
        host.show();

        /*
         * Doing abrupt shutdown when no more hosts.
         */

        final double pollPeriodS = 0.1;
        final AbstractProcess shutdownProcess = new AbstractProcess(binding.getUiThreadScheduler()) {
            @Override
            protected long process(long theoreticalTimeNs, long actualTimeNs) {
                final int hostCount = binding.getHostCount();
                if (hostCount == 0) {
                    if (DEBUG) {
                        Dbg.log("doing shutdown because no more hosts");
                        Dbg.flush();
                    }
                    binding.shutdownAbruptly();
                    // Doesn't matter much to stop here, since shutdown is abrupt,
                    // but it's a good practice.
                    this.stop();
                    return 0;
                } else {
                    return actualTimeNs + sToNs(pollPeriodS);
                }
            }
        };
        
        if (DEBUG) {
            Dbg.log("starting shutdown process...");
        }

        shutdownProcess.start();
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------

    private BwdBindingLaunchUtils() {
    }
    
    private static GRect getInitialClientBounds(
            String[] args,
            InterfaceBwdTestCaseHome home) {
        final String xStr = AH.getArgN(args, OPT_ICP, 1);
        if (xStr == null) {
            // Key not in args.
            return null;
        }

        final int x = Integer.parseInt(xStr);
        final int y = Integer.parseInt(AH.getArgN(args, OPT_ICP, 2));
        
        final GPoint spans = home.getInitialClientSpans();
        final int xSpan = spans.x();
        final int ySpan = spans.y();

        return GRect.valueOf(x, y, xSpan, ySpan);
    }
}
