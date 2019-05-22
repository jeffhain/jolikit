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
package net.jolikit.bwd.test.mains;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.jolikit.bwd.test.utils.InterfaceBindingMainLaunchInfo;

/**
 * Contains BWD main launch infos for tests.
 */
public class XxxBoundBwdMainLaunchInfo {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final List<List<InterfaceBindingMainLaunchInfo>> MAIN_LAUNCH_INFO_LIST_LIST;
    static {
        final List<List<InterfaceBindingMainLaunchInfo>> listList =
                new ArrayList<List<InterfaceBindingMainLaunchInfo>>();
        listList.add(Collections.unmodifiableList(Arrays.asList(
                new InterfaceBindingMainLaunchInfo[]{
                        new AwtBoundBwdMain(),
                        new SwingBoundBwdMain(),
                        new JfxBoundBwdMain.JavaFxMain(),
                })));
        listList.add(Collections.unmodifiableList(Arrays.asList(
                new InterfaceBindingMainLaunchInfo[]{
                        new SwtBoundBwdMain(),
                        new Lwjgl3BoundBwdMain(),
                        new JoglBoundBwdMain(),
                })));
        listList.add(Collections.unmodifiableList(Arrays.asList(
                new InterfaceBindingMainLaunchInfo[]{
                        new Qtj4BoundBwdMain(),
                        new Algr5BoundBwdMain(),
                        new Sdl2BoundBwdMain(),
                })));
        MAIN_LAUNCH_INFO_LIST_LIST = Collections.unmodifiableList(listList);
    }

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    /**
     * @return An immutable list of lists of BWD bindings main launch info.
     */
    public static List<List<InterfaceBindingMainLaunchInfo>> getAllMainLaunchInfoListList() {
        return MAIN_LAUNCH_INFO_LIST_LIST;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private XxxBoundBwdMainLaunchInfo() {
    }
}
