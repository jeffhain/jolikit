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
package net.jolikit.bwd.test.mains.utils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.jolikit.lang.Dbg;
import net.jolikit.lang.RuntimeExecHelper.InterfaceExecution;

/**
 * A little Swing GUI to close, or on which to click,
 * to kill a list of processes.
 * Processes are also killed on GUI's JVM shutdown,
 * if the GUI has been launched.
 */
public class KillGui {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------
    
    private static final boolean DEBUG = false;
    
    private static final int INITIAL_WIDTH = 200;
    private static final int INITIAL_HEIGHT = 100;
    
    private static final String TITLE = "Kill Gui";
    private static final String MESSAGE = "Click To Kill All";
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final List<InterfaceExecution> executionList;
    
    private final JFrame frame = new JFrame(TITLE);
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public KillGui(List<InterfaceExecution> executionList) {
        // Defensive copy.
        this.executionList = new ArrayList<InterfaceExecution>(executionList);
        
        final Container contentPane = this.frame.getContentPane();
        
        final JPanel panel1 = new JPanel();
        panel1.setBackground(Color.BLACK);
        contentPane.add(panel1, BorderLayout.CENTER);
        
        final JLabel label = new JLabel(MESSAGE);
        label.setForeground(Color.WHITE);
        panel1.add(label);
    }
    
    public void launchGui() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                configureFrame(frame);
                
                // Centering (looks OK to do it before setting visible).
                final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                final GraphicsDevice gd = ge.getDefaultScreenDevice();
                final GraphicsConfiguration gc = gd.getDefaultConfiguration();
                final Rectangle psb = gc.getBounds();
                frame.setLocation(psb.x, psb.y);
                
                frame.setVisible(true);
            }
        });
        
        addShutdownHook();
    }
    
    /**
     * Disposes the frame.
     * No more kills can be done after this.
     */
    public void stop() {
        synchronized (executionList) {
            executionList.clear();
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.dispose();
            }
        });
    }

    /**
     * For programmatic destruction.
     * Does nothing if GUI has been stopped.
     */
    public void destroyAllExecutionsProcesses() {
        final InterfaceExecution[] executionArr;
        synchronized (executionList) {
            executionArr = executionList.toArray(new InterfaceExecution[executionList.size()]);
        }
        if (executionArr.length == 0) {
            return;
        }
        
        if (DEBUG) {
            Dbg.log("destroys...");
            Dbg.flush();
        }
        
        for (InterfaceExecution execution : executionArr) {
            final Process process = execution.getProcess();
            
            if (DEBUG) {
                Dbg.log("Process.destroy() on " + process + "...");
                Dbg.flush();
            }
            
            /*
             * TODO jdk With Java 8+, might want to use destroyForcibly().
             */
            process.destroy();
        }
        
        if (DEBUG) {
            Dbg.log("...destroys");
            Dbg.flush();
        }
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private void configureFrame(JFrame frame) {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setMinimumSize(new Dimension(INITIAL_WIDTH, INITIAL_HEIGHT));
        
        frame.setBackground(Color.BLACK);
        
        /*
         * 
         */
        
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                killAll();
            }
        });

        /*
         * 
         */
        
        final WindowListener windowListener = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                killAll();
            }
        };
        frame.addWindowListener(windowListener);
        
        frame.pack();
    }
    
    /**
     * Must be called in UI thread, for disposal of the frame.
     */
    private void killAll() {
        
        /*
         * Killing others.
         */

        this.destroyAllExecutionsProcesses();

        /*
         * Killing ourselves.
         */

        this.frame.dispose();
    }
    
    /**
     * To fulfill our killing purpose on JVM normal or abnormal exit.
     * NB: Won't work if JVM is forcibly killed.
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(KillGui.class.getSimpleName() + "-allDieWithMe") {
            @Override
            public void run() {
                destroyAllExecutionsProcesses();
            }
        });
    }
}
