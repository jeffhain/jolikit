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
package net.jolikit.bwd.test.mains.launcher.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;

import net.jolikit.bwd.test.mains.utils.BwdMainLaunchUtils;
import net.jolikit.bwd.test.utils.BwdTestCaseHomesColumn;
import net.jolikit.bwd.test.utils.InterfaceBindingMainLaunchInfo;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseHome;

/**
 * A Swing GUI to choose which binding(s) and test case(s) to launch.
 */
public class BwdTestLauncherGui {
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final String TOP_LABEL =
            "Make sure to have xxx_config properties files configured for your installation, including screen physical resolution";
    
    static {
        /*
         * Default font is too large for our taste (and screens!).
         */
        final Font font = new Font("Lucida Console", 0, 11);
        UIManager.put("Button.font", font);
        UIManager.put("ToggleButton.font", font);
    }
    
    private static final String HORIZONTAL_SEPARATOR = "    ";
    private static final String VERTICAL_SEPARATOR = " ";
    
    /**
     * Small so that we can have more test cases on screen.
     */
    private static final int TEST_CASE_BUTTON_BORDER_INSETS = 2;
    
    /**
     * Same as for test cases, for homogeneity and users not having
     * to have to mentally switch between multiple inter-button spaces.
     */
    private static final int BINDING_BUTTON_BORDER_INSETS = TEST_CASE_BUTTON_BORDER_INSETS;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    /**
     * Custom border for buttons to take less space.
     */
    private static class MySelectableBorder extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final int insets;
        public MySelectableBorder(int insets) {
            this.insets = insets;
        }
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(Color.GRAY);
            
            /*
             * Span depending on selected state,
             * else on Mac we see no difference
             * between selected and not selected state,
             * when using such a border, due to component's
             * being paint without the usual white or blue
             * background.
             */
            final int span;
            if (isSelected(c)) {
                span = 2;
            } else {
                span = 1;
            }
            for (int i = 0; i < span; i++) {
                // Need -1 else bottom/right are out.
                g.drawRect(
                        x + i,
                        y + i,
                        width-1 - 2*i,
                        height-1 - 2*i);
            }
        }
        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = this.insets;
            return insets;
        }
        private static boolean isSelected(Component c) {
            if (c instanceof JToggleButton) {
                return ((JToggleButton) c).isSelected();
            } else {
                // We don't care.
                return false;
            }
        }
    }
    
    private class MyAfterAnyActionRunnable implements Runnable {
        @Override
        public void run() {
            // Values snapshots, to use them in another thread.
            final List<InterfaceBindingMainLaunchInfo> selectedMainLaunchInfoList =
                    toOrderedMliList(mainLaunchInfoSet);
            final List<InterfaceBwdTestCaseHome> selectedTestCaseHomeList =
                    toOrderedHomeList(testCaseHomeSet);
            final boolean selectedSmartGrid = smartGridWrapper[0];
            final boolean selectedSuperimposed = superimposedWrapper[0];
            
            /*
             * Triggering launch as soon as at least one MLI
             * and one home are selected, for multiple MLIs
             * and multiples homes would cause a quadratic number
             * of JVMs to be launched.
             * NB: User can still select a few test cases,
             * then hit "select all" to select all MLIs. 
             */
            if ((selectedMainLaunchInfoList.size() != 0)
                    && (selectedTestCaseHomeList.size() != 0)) {
                /*
                 * Enough stuffs selected for launch: let's go.
                 */
                final Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        mainLaunchUtils.launchBindingMainsWithTestCasesInNewJvmsWithMritjAdded(
                                selectedMainLaunchInfoList,
                                selectedTestCaseHomeList,
                                selectedSmartGrid,
                                selectedSuperimposed);
                        /*
                         * We're done launching the mock,
                         * now we can shut down the service
                         * (which thread is not a daemon).
                         */
                        executor.shutdownNow();
                    }
                };
                executor.execute(runnable);
                // We're done with this GUI.
                frame.dispose();
            }
        }
    };

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final String title;
    private final List<List<InterfaceBindingMainLaunchInfo>> mainLaunchInfoListList;
    private final List<BwdTestCaseHomesColumn> testCaseHomesColumnList;
    
    private final BwdMainLaunchUtils mainLaunchUtils;
    
    /*
     * 
     */
    
    /**
     * Executor to do stuffs outside UI thread
     * (we don't want mains to eventually block UI thread).
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            final Thread thread = new Thread(runnable);
            /*
             * Making sure it's not a daemon,
             * else it will poof at some point
             * while launching mains.
             */
            thread.setDaemon(false);
            return thread;
        }
    });
    
    private final Set<InterfaceBindingMainLaunchInfo> mainLaunchInfoSet = new HashSet<InterfaceBindingMainLaunchInfo>();
    private final Set<InterfaceBwdTestCaseHome> testCaseHomeSet = new HashSet<InterfaceBwdTestCaseHome>();
    private final boolean[] smartGridWrapper = new boolean[1];
    private final boolean[] superimposedWrapper = new boolean[1];

    private final Runnable afterAnyAction = new MyAfterAnyActionRunnable();

    private JFrame frame;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public BwdTestLauncherGui(
            String title,
            List<List<InterfaceBindingMainLaunchInfo>> mainLaunchInfoListList,
            List<BwdTestCaseHomesColumn> testCaseHomesColumnList,
            //
            String javaPath) {
        
        this.title = title;
        this.mainLaunchInfoListList = mainLaunchInfoListList;
        this.testCaseHomesColumnList = testCaseHomesColumnList;
        
        this.mainLaunchUtils = new BwdMainLaunchUtils(javaPath);
    }
    
    public void launchGui() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final JFrame theFrame = buildFrame(executor);
                frame = theFrame;
                
                // Centering (looks OK to do it before setting visible).
                final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                final GraphicsDevice gd = ge.getDefaultScreenDevice();
                final GraphicsConfiguration gc = gd.getDefaultConfiguration();
                final Rectangle psb = gc.getBounds();
                theFrame.setLocation(
                        psb.x + (psb.width - theFrame.getWidth()) / 2,
                        psb.y + (psb.height - theFrame.getHeight()) / 2);
                
                theFrame.setVisible(true);
            }
        });
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param executor Executor to do stuffs outside UI thread.
     */
    private JFrame buildFrame(ExecutorService executor) {
        final JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setMinimumSize(new Dimension(250, 100));
        
        {
            final JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            {
                final JLabel label = new JLabel(TOP_LABEL);
                panel.add(label);
            }
            {
                final JPanel testPanel = new JPanel();
                testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.X_AXIS));
                {
                    this.addBindingsPanel(testPanel);
                    
                    this.addTestCasesPanel(testPanel);
                }
                
                final JScrollPane scrollPane = new JScrollPane(testPanel);
                scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                
                panel.add(scrollPane);
            }
            frame.add(panel);
        }
        
        frame.pack();
        
        return frame;
    }
    
    /*
     * 
     */
    
    private void addBindingsPanel(JPanel panel) {
        final JPanel panel_1 = new JPanel();
        panel_1.setLayout(new BorderLayout());
        {
            final JPanel panel_1_1 = new JPanel();
            panel_1_1.setLayout(new BorderLayout());
            {
                // Don't know how to center "properly".
                final String leftPadding = "                                    ";
                final JLabel titleLabel = new JLabel(leftPadding + "Bindings");
                panel_1_1.add(titleLabel, BorderLayout.CENTER);
            }
            {
                panel_1_1.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.SOUTH);
            }
            panel_1.add(panel_1_1, BorderLayout.NORTH);
        }
        {
            final JPanel panel_1_2 = new JPanel();
            panel_1_2.setLayout(new BoxLayout(panel_1_2, BoxLayout.X_AXIS));
            this.addBindingsSubPanels(panel_1_2);
            panel_1.add(panel_1_2, BorderLayout.CENTER);
        }
        panel.add(panel_1);
    }
    
    private void addBindingsSubPanels(JPanel panel) {
        final Map<JToggleButton,InterfaceBindingMainLaunchInfo> mliByButton =
                new HashMap<JToggleButton,InterfaceBindingMainLaunchInfo>();
        {
            final JPanel panel_1 = new JPanel();
            panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));
            
            boolean didAList = false;
            for (List<InterfaceBindingMainLaunchInfo> mliList : this.mainLaunchInfoListList) {
                if (didAList) {
                    panel_1.add(new JLabel(VERTICAL_SEPARATOR));
                }
                for (InterfaceBindingMainLaunchInfo mli : mliList) {
                    final InterfaceBindingMainLaunchInfo mli_final = mli;
                    
                    final JToggleButton mliButton = newJToggleButton(
                            mli.getBindingName(),
                            new MySelectableBorder(BINDING_BUTTON_BORDER_INSETS));
                    mliByButton.put(mliButton, mli);
                    mliButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (mliButton.isSelected()) {
                                mainLaunchInfoSet.add(mli_final);
                            } else {
                                mainLaunchInfoSet.remove(mli_final);
                            }
                            afterAnyAction.run();
                        }
                    });
                    
                    panel_1.add(mliButton);
                }
                didAList = true;
            }
            
            panel.add(panel_1);
        }
        {
            panel.add(new JLabel(HORIZONTAL_SEPARATOR));
        }
        {
            final JPanel panel_2 = new JPanel();
            panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.Y_AXIS));
            
            {
                final JButton selectAllButton = newJButton(
                        "Select All",
                        new MySelectableBorder(BINDING_BUTTON_BORDER_INSETS));
                selectAllButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        for (Map.Entry<JToggleButton,InterfaceBindingMainLaunchInfo> entry : mliByButton.entrySet()) {
                            final JToggleButton mliButton = entry.getKey();
                            final InterfaceBindingMainLaunchInfo mli = entry.getValue();
                            if (!mliButton.isSelected()) {
                                mliButton.setSelected(true);
                                mainLaunchInfoSet.add(mli);
                            }
                        }
                        afterAnyAction.run();
                    }
                });
                
                panel_2.add(selectAllButton);
            }
            {
                final JButton selectNoneButton = newJButton(
                        "Select None",
                        new MySelectableBorder(BINDING_BUTTON_BORDER_INSETS));
                selectNoneButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        for (Map.Entry<JToggleButton,InterfaceBindingMainLaunchInfo> entry : mliByButton.entrySet()) {
                            final JToggleButton mliButton = entry.getKey();
                            final InterfaceBindingMainLaunchInfo mli = entry.getValue();
                            if (mliButton.isSelected()) {
                                mliButton.setSelected(false);
                                mainLaunchInfoSet.remove(mli);
                            }
                        }
                        afterAnyAction.run();
                    }
                });
                
                panel_2.add(selectNoneButton);
            }
            panel_2.add(new JLabel(VERTICAL_SEPARATOR));
            {
                final JToggleButton smartGridButton = newJToggleButton(
                        "SmartGrid",
                        new MySelectableBorder(BINDING_BUTTON_BORDER_INSETS));
                smartGridButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        smartGridWrapper[0] = smartGridButton.isSelected();
                        afterAnyAction.run();
                    }
                });
                
                panel_2.add(smartGridButton);
            }
            {
                final JToggleButton superimposedButton = newJToggleButton(
                        "Superimposed",
                        new MySelectableBorder(BINDING_BUTTON_BORDER_INSETS));
                superimposedButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        superimposedWrapper[0] = superimposedButton.isSelected();
                        afterAnyAction.run();
                    }
                });
                
                panel_2.add(superimposedButton);
            }
            
            panel.add(panel_2);
        }
    }
    
    /*
     * 
     */
    
    private void addTestCasesPanel(JPanel panel) {
        final JPanel panel_1 = new JPanel();
        panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.X_AXIS));
        
        final List<JToggleButton> testCaseButtonList = new ArrayList<JToggleButton>();
        final int colCount = this.testCaseHomesColumnList.size();
        for (int col = 0; col < colCount; col++) {
            final BwdTestCaseHomesColumn homesColumn =
                    this.testCaseHomesColumnList.get(col);
            {
                panel_1.add(new JLabel(HORIZONTAL_SEPARATOR));
                panel_1.add(new JSeparator(JSeparator.VERTICAL));
                panel_1.add(new JLabel(HORIZONTAL_SEPARATOR));
            }
            
            final String title = homesColumn.getTitle();
            
            this.addTestCasesPanel_column(
                    panel_1,
                    title,
                    testCaseButtonList,
                    homesColumn);
        }
        
        panel.add(panel_1);
    }
    
    private void addTestCasesPanel_column(
            JPanel panel,
            String title,
            List<JToggleButton> testCaseButtonList,
            BwdTestCaseHomesColumn homesColumn) {
        final JPanel panel_1 = new JPanel();
        panel_1.setLayout(new BorderLayout());
        {
            final JPanel panel_1_1 = new JPanel();
            panel_1_1.setLayout(new BorderLayout());
            {
                final JLabel titleLabel = new JLabel(title);
                panel_1_1.add(titleLabel, BorderLayout.CENTER);
            }
            {
                panel_1_1.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.SOUTH);
            }
            panel_1.add(panel_1_1, BorderLayout.NORTH);
        }
        {
            final JPanel panel_1_2 = new JPanel();
            panel_1_2.setLayout(new BoxLayout(panel_1_2, BoxLayout.X_AXIS));
            this.addTestCasesSubPanel_column(
                    panel_1_2,
                    testCaseButtonList,
                    homesColumn);
            panel_1.add(panel_1_2, BorderLayout.CENTER);
        }
        panel.add(panel_1);
    }

    private void addTestCasesSubPanel_column(
            JPanel panel_1,
            final List<JToggleButton> testCaseButtonList,
            BwdTestCaseHomesColumn homesColumn) {
        final JPanel panel_1_x = new JPanel();
        panel_1_x.setLayout(new BoxLayout(panel_1_x, BoxLayout.Y_AXIS));

        for (List<InterfaceBwdTestCaseHome> homeGroup : homesColumn.getHomeListList()) {

            panel_1_x.add(new JLabel(VERTICAL_SEPARATOR));

            for (InterfaceBwdTestCaseHome home : homeGroup) {
                final InterfaceBwdTestCaseHome homeFinal = home;

                final JToggleButton testCaseButton = newJToggleButton(
                        home.getClass().getSimpleName(),
                        new MySelectableBorder(TEST_CASE_BUTTON_BORDER_INSETS));
                testCaseButtonList.add(testCaseButton);
                testCaseButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (testCaseButton.isSelected()) {
                            testCaseHomeSet.add(homeFinal);
                            if (false) {
                                /*
                                 * Old code, when only had a single home selection.
                                 */
                                // De-selecting all other eventually selected mocks.
                                for (JToggleButton b : testCaseButtonList) {
                                    if ((b != testCaseButton) && b.isSelected()) {
                                        b.setSelected(false);
                                    }
                                }
                            }
                        } else {
                            testCaseHomeSet.remove(homeFinal);
                        }
                        afterAnyAction.run();
                    }
                });

                panel_1_x.add(testCaseButton);
            }
        }
        
        panel_1.add(panel_1_x);
    }
    
    /*
     * 
     */
    
    private static JButton newJButton(String text, Border border) {
        final JButton button = new JButton(text);
        button.setBorder(border);
        return button;
    }
    
    private static JToggleButton newJToggleButton(String text, Border border) {
        final JToggleButton button = new JToggleButton(text);
        button.setBorder(border);
        return button;
    }
    
    /**
     * MLI ordering according to the internal and user-specified list,
     * for consistency across runs, and to allow user to specify the order.
     * 
     * @param mliSet (in) Set of MLI.
     * @return An ordered list of the specified MLI.
     */
    private List<InterfaceBindingMainLaunchInfo> toOrderedMliList(Set<InterfaceBindingMainLaunchInfo> mliSet) {
        
        final List<InterfaceBindingMainLaunchInfo> orderedList = new ArrayList<InterfaceBindingMainLaunchInfo>();
        
        for (List<InterfaceBindingMainLaunchInfo> mliList : this.mainLaunchInfoListList) {
            for (InterfaceBindingMainLaunchInfo mliInfo : mliList) {
                if (mliSet.contains(mliInfo)) {
                    orderedList.add(mliInfo);
                }
            }
        }
        
        return orderedList;
    }
    
    /**
     * Test case homes ordering according to the internal and user-specified list,
     * for consistency across runs, and to allow user to specify the order.
     * 
     * @param homeSet (in) Set of homes.
     * @return An ordered list of the specified homes.
     */
    private List<InterfaceBwdTestCaseHome> toOrderedHomeList(Set<InterfaceBwdTestCaseHome> homeSet) {
        
        // Making sure we don't add a home twice when it appears multiple times
        // in user-specified lists.
        final HashSet<InterfaceBwdTestCaseHome> remainingHomeSet =
                new HashSet<InterfaceBwdTestCaseHome>(homeSet);
        
        final List<InterfaceBwdTestCaseHome> orderedList = new ArrayList<InterfaceBwdTestCaseHome>();
        
        for (BwdTestCaseHomesColumn homesColumn : this.testCaseHomesColumnList) {
            for (List<InterfaceBwdTestCaseHome> homeList : homesColumn.getHomeListList()) {
                for (InterfaceBwdTestCaseHome home : homeList) {
                    if (remainingHomeSet.remove(home)) {
                        orderedList.add(home);
                    }
                }
            }
        }
        
        return orderedList;
    }
}
