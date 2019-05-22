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
package net.jolikit.bwd.impl.qtj4;

import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdKeyLocations;
import net.jolikit.bwd.api.events.BwdKeys;
import net.jolikit.bwd.api.events.BwdMouseButtons;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.events.AbstractEventConverter;
import net.jolikit.bwd.impl.utils.events.CmnInputConvState;
import net.jolikit.lang.Dbg;

import com.trolltech.qt.core.QEvent;
import com.trolltech.qt.core.QEvent.Type;
import com.trolltech.qt.core.QPoint;
import com.trolltech.qt.core.QPointF;
import com.trolltech.qt.core.Qt;
import com.trolltech.qt.core.Qt.Key;
import com.trolltech.qt.core.Qt.KeyboardModifier;
import com.trolltech.qt.core.Qt.KeyboardModifiers;
import com.trolltech.qt.core.Qt.MouseButton;
import com.trolltech.qt.core.Qt.MouseButtons;
import com.trolltech.qt.core.Qt.TouchPointState;
import com.trolltech.qt.gui.QCursor;
import com.trolltech.qt.gui.QKeyEvent;
import com.trolltech.qt.gui.QMouseEvent;
import com.trolltech.qt.gui.QTouchEvent;
import com.trolltech.qt.gui.QTouchEvent_TouchPoint;
import com.trolltech.qt.gui.QWheelEvent;

/**
 * For key events: QKeyEvent
 * For mouse events: QMouseEvent, QEvent (for enter and leave).
 * For wheel events: QWheelEvent, QTouchEvent
 */
public class QtjEventConverter extends AbstractEventConverter {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final boolean DEBUG = false;
    
    /**
     * TODO qtj At some point, when dragging window, we had trouble with
     * QMouseEvent's x() and y() both alternating between two possibly far away
     * positions: a "random" old one, and current one (???).
     * As a result, we had to compute mouse position in client from
     * client position and QMouseEvent's globalX() and globalY().
     * Lately we couldn't reproduce the issue, but for safety we still
     * distrust QMouseEvent's x() and y(), and use the workaround.
     */
    private static final boolean CAN_RELY_ON_EVENTS_MOUSE_POS_IN_CLIENT = false;
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private final QtjKeyConverter keyConverter = new QtjKeyConverter();
    
    private final boolean mustSynthesizeAltGraph;
    private final int altGraphNativeScanCode;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    public QtjEventConverter(
            CmnInputConvState commonState,
            AbstractBwdHost host,
            boolean mustSynthesizeAltGraph,
            int altGraphNativeScanCode) {
        super(commonState, host);
        this.mustSynthesizeAltGraph = mustSynthesizeAltGraph;
        this.altGraphNativeScanCode = altGraphNativeScanCode;
    }
    
    /*
     * Wheel events.
     */
    
    /**
     * @param backingEvent A QWheelEvent or a QTouchEvent.
     * @return A corresponding wheel event, or null if the specified event was
     *         a touch event that had no primary touch point or did not suit
     *         for some other reason.
     */
    @Override
    public BwdWheelEvent newWheelEvent(Object backingEvent) {
        
        if (backingEvent instanceof QTouchEvent) {
            final QTouchEvent touchEvent = (QTouchEvent) backingEvent;
            final QTouchEvent_TouchPoint point = computeFirstPrimary(touchEvent.touchPoints());
            
            final boolean mustUseTouchEvent =
                    (point != null)
                    && (point.state() == TouchPointState.TouchPointMoved)
                    && (touchEvent.type() == Type.TouchUpdate);
            if (!mustUseTouchEvent) {
                return null;
            }
        }
        
        return super.newWheelEvent(backingEvent);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected void updateFromBackingEvent(Object backingEvent) {
        
        {
            final QEvent event = (QEvent) backingEvent;
            /*
             * TODO qtj Doesn't seem to have any visible effect,
             * but we are told:
             * "You should call accept() if your widget handles those events."
             */
            event.accept();
        }
        
        /*
         * Updating common state.
         */
        
        final CmnInputConvState commonState = this.getCommonState();
        
        GPoint mousePosInScreen = null;
        
        if (backingEvent instanceof QKeyEvent) {
            final QKeyEvent keyEvent = (QKeyEvent) backingEvent;
            
            final KeyboardModifiers modifiers = keyEvent.modifiers();
            commonState.setShiftDown(modifiers.isSet(KeyboardModifier.ShiftModifier));
            commonState.setControlDown(modifiers.isSet(KeyboardModifier.ControlModifier));
            if (!this.mustSynthesizeAltGraph) {
                commonState.setAltDown(modifiers.isSet(KeyboardModifier.AltModifier));
            }
            commonState.setMetaDown(modifiers.isSet(KeyboardModifier.MetaModifier));
        }
        
        if (backingEvent instanceof QMouseEvent) {
            final QMouseEvent mouseEvent = (QMouseEvent) backingEvent;
            
            mousePosInScreen = GPoint.valueOf(
                    mouseEvent.globalX(),
                    mouseEvent.globalY());
            commonState.setMousePosInScreen(mousePosInScreen);
            if (DEBUG) {
                Dbg.log("reading event mouse pos in screen:");
                Dbg.log("mousePosInScreen = " + mousePosInScreen);
            }

            final MouseButtons buttons = mouseEvent.buttons();
            commonState.setPrimaryButtonDown(buttons.isSet(MouseButton.LeftButton));
            commonState.setMiddleButtonDown(buttons.isSet(MouseButton.MidButton));
            commonState.setSecondaryButtonDown(buttons.isSet(MouseButton.RightButton));

            final KeyboardModifiers modifiers = mouseEvent.modifiers();
            commonState.setShiftDown(modifiers.isSet(KeyboardModifier.ShiftModifier));
            commonState.setControlDown(modifiers.isSet(KeyboardModifier.ControlModifier));
            if (!this.mustSynthesizeAltGraph) {
                commonState.setAltDown(modifiers.isSet(KeyboardModifier.AltModifier));
            }
            commonState.setMetaDown(modifiers.isSet(KeyboardModifier.MetaModifier));
            
        }
        
        /*
         * 
         */
        
        if (mousePosInScreen == null) {
            // TODO qtj Best effort (we need this pos
            // for the corresponding BWD event).
            final QPoint qMousePosInScreen = QCursor.pos();
            mousePosInScreen = GPoint.valueOf(
                    qMousePosInScreen.x(),
                    qMousePosInScreen.y());
            commonState.setMousePosInScreen(mousePosInScreen);
            if (DEBUG) {
                Dbg.log("reading global mouse pos in screen:");
                Dbg.log("mousePosInScreen = " + mousePosInScreen);
            }
        }
        
        /*
         * Updating host-specific state.
         */
        
        GPoint mousePosInClient = null;
        if (CAN_RELY_ON_EVENTS_MOUSE_POS_IN_CLIENT) {
            if (backingEvent instanceof QMouseEvent) {
                final QMouseEvent mouseEvent = (QMouseEvent) backingEvent;
                mousePosInClient = GPoint.valueOf(
                        mouseEvent.x(),
                        mouseEvent.y());
            } else {
                final QEvent event = (QEvent) backingEvent;
                // No pos available in this event.
            }
        }
        
        if (mousePosInClient == null) {
            final InterfaceBwdHost host = this.getHost();
            final GRect clientBounds = host.getClientBounds();
            if (!clientBounds.isEmpty()) {
                mousePosInClient = GPoint.valueOf(
                        mousePosInScreen.x() - clientBounds.x(),
                        mousePosInScreen.y() - clientBounds.y());

                if (DEBUG) {
                    Dbg.log("computed mouse pos in client:");
                    Dbg.log("mousePosInScreen = " + mousePosInScreen);
                    Dbg.log("clientBounds = " + clientBounds);
                    Dbg.log("mousePosInClient = " + mousePosInClient);
                }
            }
        }
        
        if (mousePosInClient != null) {
            this.setMousePosInClient(mousePosInClient);
        }
        
        /*
         * 
         */
        
        if (this.mustSynthesizeAltGraph
                && (backingEvent instanceof QKeyEvent)) {
            final QKeyEvent keyEvent = (QKeyEvent) backingEvent;
            
            if (keyEvent.type() == Type.KeyPress) {
                final int key = getKey(backingEvent);
                if (key == BwdKeys.ALT) {
                    this.getCommonState().setAltDown(true);
                } else if (key == BwdKeys.ALT_GRAPH) {
                    this.getCommonState().setAltGraphDown(true);
                }
                
            } else if (keyEvent.type() == Type.KeyRelease) {
                final int key = getKey(backingEvent);
                if (key == BwdKeys.ALT) {
                    this.getCommonState().setAltDown(false);
                } else if (key == BwdKeys.ALT_GRAPH) {
                    this.getCommonState().setAltGraphDown(false);
                }
            }
        }
    }

    /*
     * Key events.
     */
    
    @Override
    protected int getKey(Object backingEvent) {
        final QKeyEvent keyEvent = (QKeyEvent) backingEvent;
        
        if (DEBUG) {
            Dbg.log("QKeyEvent = " + keyEvent);
        }
        
        final Key backingKey = this.getBackingKey(keyEvent);
        
        return this.keyConverter.get(backingKey);
    }

    @Override
    protected int getKeyLocation(Object backingEvent) {
        final QKeyEvent keyEvent = (QKeyEvent) backingEvent;

        final Key backingKey = this.getBackingKey(keyEvent);
        if (backingKey == Key.Key_unknown) {
            // Unknown key, better not try to read its location.
            return BwdKeyLocations.NO_STATEMENT;
        }

        /*
         * TODO qtj Don't have much location info.
         */
        
        final KeyboardModifiers mods = keyEvent.modifiers();
        final boolean isKeypadKey = mods.isSet(KeyboardModifier.KeypadModifier);
        if (isKeypadKey) {
            return BwdKeyLocations.NUMPAD;
        }

        switch (backingKey) {
        case Key_ApplicationLeft:
        case Key_Super_L:
            //
            return BwdKeyLocations.LEFT;
            //
        case Key_ApplicationRight:
        case Key_Super_R:
            //
            return BwdKeyLocations.RIGHT;
            //
        default:
            return BwdKeyLocations.NO_STATEMENT;
        }
    }

    @Override
    protected int getCodePoint(Object backingEvent) {
        final QKeyEvent keyEvent = (QKeyEvent) backingEvent;
        /*
         * NB: Qt doc says it's a Unicode "text",
         * but here we just expect a Unicode character
         * (equivalent to a code point).
         */
        final String cpStr = keyEvent.text();
        if (cpStr.length() == 0) {
            return 0;
        }
        return cpStr.codePointAt(0);
    }
    
    /*
     * Mouse events.
     */
    
    @Override
    protected int getButton(Object backingEvent) {
        final int button;
        if (backingEvent instanceof QMouseEvent) {
            final QMouseEvent mouseEvent = (QMouseEvent) backingEvent;
            button = computeButton(mouseEvent);
        } else {
            // QEvent
            button = BwdMouseButtons.NO_STATEMENT;
        }
        return button;
    }
    
    /*
     * Wheel events.
     */

    @Override
    protected int getWheelXRoll(Object backingEvent) {
        final int xRoll;
        if (backingEvent instanceof QWheelEvent) {
            xRoll = 0;
        } else {
            final QTouchEvent touchEvent = (QTouchEvent) backingEvent;
            
            final QTouchEvent_TouchPoint point = computeFirstPrimary(touchEvent.touchPoints());
            
            final QPointF pos = point.pos();
            final QPointF lastPos = point.lastPos();
            xRoll = computeBwdRollAmount(pos.x() - lastPos.x());
        }
        return xRoll;
    }

    @Override
    protected int getWheelYRoll(Object backingEvent) {
        final int yRoll;
        if (backingEvent instanceof QWheelEvent) {
            final QWheelEvent wheelEvent = (QWheelEvent) backingEvent;
            /*
             * TODO qtj In Qt5, delta() is deprecated.
             * Shall then use pixelDelta() if non-zero, else angleDelta().
             */
            yRoll = computeBwdRollAmount(wheelEvent.delta());
        } else {
            final QTouchEvent touchEvent = (QTouchEvent) backingEvent;
            
            final QTouchEvent_TouchPoint point = computeFirstPrimary(touchEvent.touchPoints());
            
            final QPointF pos = point.pos();
            final QPointF lastPos = point.lastPos();
            // Minus because we negate delta inside (for wheel even delta).
            final int delta = (int) -(pos.y() - lastPos.y());
            yRoll = computeBwdRollAmount(delta);
        }
        return yRoll;
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private static int computeButton(QMouseEvent event) {
        final MouseButton backingButton = event.button();
        switch (backingButton) {
        case LeftButton: return BwdMouseButtons.PRIMARY;
        case MidButton: return BwdMouseButtons.MIDDLE;
        case RightButton: return BwdMouseButtons.SECONDARY;
        default:
            return BwdMouseButtons.NO_STATEMENT;
        }
    }
    
    /**
     * @return The first primary point found, else null.
     */
    private static QTouchEvent_TouchPoint computeFirstPrimary(
            List<QTouchEvent_TouchPoint> points) {
        final int size = points.size();
        for (int i = 0; i < size; i++) {
            final QTouchEvent_TouchPoint point = points.get(i);
            if (point.isPrimary()) {
                return point;
            }
        }
        return null;
    }
    
    private static int computeBwdRollAmount(double delta) {
        /*
         * TODO qtj Maybe we should accumulate.
         */
        return (int) Math.signum(-delta);
    }
    
    /**
     * Never uses "alt hack".
     */
    private static Key getBackingKey_raw(QKeyEvent keyEvent) {
        final int keyCode = keyEvent.key();
        if (false) {
            // TODO qtj Doesn't seem appropriate for any of our usages.
            Qt.Key.Key_unknown.value();
        }
        if (keyCode == 0) {
            /*
             * "for example, it may be the result of a compose sequence,
             * a keyboard macro, or due to key event compression."
             * 
             * Key.resolve(0) throws.
             */
            return Key.Key_unknown;
        }
        
        final Key backingKey = Key.resolve(keyCode);
        return backingKey;
    }

    private Key getBackingKey(QKeyEvent keyEvent) {
        if (this.mustSynthesizeAltGraph
                && (keyEvent.nativeScanCode() == this.altGraphNativeScanCode)) {
            return Key.Key_AltGr;
        }
        
        final Key backingKey = getBackingKey_raw(keyEvent);
        return backingKey;
    }
}
