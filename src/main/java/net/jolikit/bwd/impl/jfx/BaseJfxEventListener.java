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
package net.jolikit.bwd.impl.jfx;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import net.jolikit.lang.LangUtils;
import net.jolikit.lang.ObjectWrapper;

/**
 * Base class to listen to JavaFX events of interest, from a single class,
 * and without having to bother with events types and related generics.
 * 
 * Works for listening from events coming from either a Stage or a Node.
 * Window events callbacks are only called in case of a Stage.
 * 
 * Some events don't have actual JavaFX events associated with,
 * but have corresponding properties which can be listened to,
 * such as Stage.iconifiedProperty().
 * 
 * onXXX methods do nothing by default.
 */
public class BaseJfxEventListener {
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    /*
     * Only one of stage/node must be non-null.
     */

    private final Stage stage;

    private final Node node;

    /*
     * Not using a list of events handlers (and corresponding event type),
     * not to mess with generics hell.
     */
    
    /*
     * Window events.
     */

    private final ObjectWrapper<EventHandler<WindowEvent>> eventHandlerHolder_WINDOW_SHOWN = new ObjectWrapper<EventHandler<WindowEvent>>();
    private final ObjectWrapper<EventHandler<WindowEvent>> eventHandlerHolder_WINDOW_HIDDEN = new ObjectWrapper<EventHandler<WindowEvent>>();

    private final ObjectWrapper<ChangeListener<Boolean>> changeListener_iconified = new ObjectWrapper<ChangeListener<Boolean>>();
    private final ObjectWrapper<ChangeListener<Boolean>> changeListener_maximized = new ObjectWrapper<ChangeListener<Boolean>>();

    private final ObjectWrapper<ChangeListener<Number>> changeListener_x = new ObjectWrapper<ChangeListener<Number>>();
    private final ObjectWrapper<ChangeListener<Number>> changeListener_y = new ObjectWrapper<ChangeListener<Number>>();
    private final ObjectWrapper<ChangeListener<Number>> changeListener_width = new ObjectWrapper<ChangeListener<Number>>();
    private final ObjectWrapper<ChangeListener<Number>> changeListener_height = new ObjectWrapper<ChangeListener<Number>>();

    private final ObjectWrapper<EventHandler<WindowEvent>> eventHandlerHolder_WINDOW_CLOSE_REQUEST = new ObjectWrapper<EventHandler<WindowEvent>>();
    
    /*
     * Focus events.
     */
    
    private final ObjectWrapper<ChangeListener<Boolean>> changeListener_focused = new ObjectWrapper<ChangeListener<Boolean>>();
    
    /*
     * Key events.
     */
    
    private final ObjectWrapper<EventHandler<KeyEvent>> eventHandlerHolder_KEY_PRESSED = new ObjectWrapper<EventHandler<KeyEvent>>();
    private final ObjectWrapper<EventHandler<KeyEvent>> eventHandlerHolder_KEY_RELEASED = new ObjectWrapper<EventHandler<KeyEvent>>();
    private final ObjectWrapper<EventHandler<KeyEvent>> eventHandlerHolder_KEY_TYPED = new ObjectWrapper<EventHandler<KeyEvent>>();

    /*
     * Mouse events.
     */
    
    private final ObjectWrapper<EventHandler<MouseEvent>> eventHandlerHolder_MOUSE_PRESSED = new ObjectWrapper<EventHandler<MouseEvent>>();
    private final ObjectWrapper<EventHandler<MouseEvent>> eventHandlerHolder_MOUSE_RELEASED = new ObjectWrapper<EventHandler<MouseEvent>>();
    private final ObjectWrapper<EventHandler<MouseEvent>> eventHandlerHolder_MOUSE_CLICKED = new ObjectWrapper<EventHandler<MouseEvent>>();
    private final ObjectWrapper<EventHandler<MouseEvent>> eventHandlerHolder_MOUSE_ENTERED = new ObjectWrapper<EventHandler<MouseEvent>>();
    private final ObjectWrapper<EventHandler<MouseEvent>> eventHandlerHolder_MOUSE_EXITED = new ObjectWrapper<EventHandler<MouseEvent>>();
    private final ObjectWrapper<EventHandler<MouseEvent>> eventHandlerHolder_MOUSE_MOVED = new ObjectWrapper<EventHandler<MouseEvent>>();
    private final ObjectWrapper<EventHandler<MouseEvent>> eventHandlerHolder_MOUSE_DRAGGED = new ObjectWrapper<EventHandler<MouseEvent>>();

    /*
     * Scroll events.
     */
    
    private final ObjectWrapper<EventHandler<ScrollEvent>> eventHandlerHolder_SCROLL = new ObjectWrapper<EventHandler<ScrollEvent>>();

    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public BaseJfxEventListener(Stage stage) {
        this.stage = LangUtils.requireNonNull(stage);
        this.node = null;
    }

    public BaseJfxEventListener(Node node) {
        this.stage = null;
        this.node = LangUtils.requireNonNull(node);
    }

    public void addEventHandlers() {

        /*
         * Window events.
         * 
         * Some window events can be listened at either as window events or as properties,
         * like WINDOW_SHOWN/WINDOW_HIDDEN versus visibleProperty(),
         * and WINDOW_CLOSE_REQUEST versus onCloseRequestProperty() (or even setOnCloseRequest(...)).
         * As a general rule, we use the window event way when available,
         * because it's precisely what we want to listen to, and because
         * the property might not be available on the Stage.
         */
        
        if (this.stage != null) {
            this.setNewEventHandler(this.eventHandlerHolder_WINDOW_SHOWN, WindowEvent.WINDOW_SHOWN, new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    onWindowShown(event);
                }
            });

            this.setNewEventHandler(this.eventHandlerHolder_WINDOW_HIDDEN, WindowEvent.WINDOW_HIDDEN, new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    onWindowHidden(event);
                }
            });

            this.setNewChangeListener(this.changeListener_iconified, this.stage.iconifiedProperty(), new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    final boolean was = oldValue.booleanValue();
                    final boolean is = newValue.booleanValue();
                    if (was) {
                        if (!is) {
                            onWindowDeiconified();
                        }
                    } else {
                        if (is) {
                            onWindowIconified();
                        }
                    }
                }
            });

            this.setNewChangeListener(this.changeListener_maximized, this.stage.maximizedProperty(), new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    final boolean was = oldValue.booleanValue();
                    final boolean is = newValue.booleanValue();
                    if (was) {
                        if (!is) {
                            onWindowDemaximized();
                        }
                    } else {
                        if (is) {
                            onWindowMaximized();
                        }
                    }
                }
            });

            this.setNewChangeListener(this.changeListener_x, this.stage.xProperty(), new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    onWindowMoved();
                }
            });
            
            this.setNewChangeListener(this.changeListener_y, this.stage.yProperty(), new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    onWindowMoved();
                }
            });
            
            this.setNewChangeListener(this.changeListener_width, this.stage.widthProperty(), new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    onWindowResized();
                }
            });
            
            this.setNewChangeListener(this.changeListener_height, this.stage.heightProperty(), new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                    onWindowResized();
                }
            });

            this.setNewEventHandler(this.eventHandlerHolder_WINDOW_CLOSE_REQUEST, WindowEvent.WINDOW_CLOSE_REQUEST, new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    onWindowCloseRequest(event);
                }
            });
        }

        /*
         * Focus events.
         */

        this.setNewChangeListener(this.changeListener_focused, this.focusedProperty(), new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                final boolean was = oldValue.booleanValue();
                final boolean is = newValue.booleanValue();
                if (was) {
                    if (!is) {
                        onFocusLost();
                    }
                } else {
                    if (is) {
                        onFocusGained();
                    }
                }
            }
        });
        
        /*
         * Key events.
         */

        this.setNewEventHandler(this.eventHandlerHolder_KEY_PRESSED, KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                onKeyPressed(event);
            }
        });

        this.setNewEventHandler(this.eventHandlerHolder_KEY_RELEASED, KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                onKeyReleased(event);
            }
        });

        this.setNewEventHandler(this.eventHandlerHolder_KEY_TYPED, KeyEvent.KEY_TYPED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                onKeyTyped(event);
            }
        });

        /*
         * Mouse events.
         */

        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_PRESSED, MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                onMousePressed(event);
            }
        });

        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_RELEASED, MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                onMouseReleased(event);
            }
        });

        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_CLICKED, MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                onMouseClicked(event);
            }
        });

        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_ENTERED, MouseEvent.MOUSE_ENTERED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                onMouseEntered(event);
            }
        });

        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_EXITED, MouseEvent.MOUSE_EXITED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                onMouseExited(event);
            }
        });

        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_MOVED, MouseEvent.MOUSE_MOVED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                onMouseMoved(event);
            }
        });

        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_DRAGGED, MouseEvent.MOUSE_DRAGGED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                onMouseDragged(event);
            }
        });

        /*
         * Scroll events.
         * 
         * TODO jfx Could also listen to swipe events
         * for converting them to wheel events,
         * if generated instead of scroll events for quick gestures.
         */

        this.setNewEventHandler(this.eventHandlerHolder_SCROLL, ScrollEvent.SCROLL, new EventHandler<ScrollEvent>() {
            @Override
            public void handle(ScrollEvent event) {
                onScroll(event);
            }
        });
    }

    public void removeEventHandlers() {
        if (this.stage != null) {
            this.setNewEventHandler(this.eventHandlerHolder_WINDOW_SHOWN, WindowEvent.WINDOW_SHOWN, null);
            this.setNewEventHandler(this.eventHandlerHolder_WINDOW_HIDDEN, WindowEvent.WINDOW_HIDDEN, null);
            
            this.setNewChangeListener(this.changeListener_iconified, this.stage.iconifiedProperty(), null);
            this.setNewChangeListener(this.changeListener_maximized, this.stage.maximizedProperty(), null);
            
            this.setNewChangeListener(this.changeListener_x, this.stage.xProperty(), null);
            this.setNewChangeListener(this.changeListener_y, this.stage.yProperty(), null);
            this.setNewChangeListener(this.changeListener_width, this.stage.widthProperty(), null);
            this.setNewChangeListener(this.changeListener_height, this.stage.heightProperty(), null);
            
            this.setNewEventHandler(this.eventHandlerHolder_WINDOW_CLOSE_REQUEST, WindowEvent.WINDOW_CLOSE_REQUEST, null);
        }
        
        /*
         * 
         */
        
        this.setNewChangeListener(this.changeListener_focused, this.focusedProperty(), null);
        
        /*
         * 
         */

        this.setNewEventHandler(this.eventHandlerHolder_KEY_PRESSED, KeyEvent.KEY_PRESSED, null);
        this.setNewEventHandler(this.eventHandlerHolder_KEY_RELEASED, KeyEvent.KEY_RELEASED, null);
        this.setNewEventHandler(this.eventHandlerHolder_KEY_TYPED, KeyEvent.KEY_TYPED, null);

        /*
         * 
         */

        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_PRESSED, MouseEvent.MOUSE_PRESSED, null);
        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_RELEASED, MouseEvent.MOUSE_RELEASED, null);
        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_CLICKED, MouseEvent.MOUSE_CLICKED, null);
        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_ENTERED, MouseEvent.MOUSE_ENTERED, null);
        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_EXITED, MouseEvent.MOUSE_EXITED, null);
        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_MOVED, MouseEvent.MOUSE_MOVED, null);
        this.setNewEventHandler(this.eventHandlerHolder_MOUSE_DRAGGED, MouseEvent.MOUSE_DRAGGED, null);

        /*
         * 
         */

        this.setNewEventHandler(this.eventHandlerHolder_SCROLL, ScrollEvent.SCROLL, null);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    /**
     * @return The Window or Node.
     */
    protected Object getEventSource() {
        if (this.stage != null) {
            return this.stage;
        } else {
            return this.node;
        }
    }

    /*
     * Window events.
     */

    protected void onWindowShown(WindowEvent event) {
    }

    protected void onWindowHidden(WindowEvent event) {
    }

    protected void onWindowIconified() {
    }

    protected void onWindowDeiconified() {
    }

    protected void onWindowMaximized() {
    }

    protected void onWindowDemaximized() {
    }
    
    protected void onWindowMoved() {
    }
    
    protected void onWindowResized() {
    }
    
    protected void onWindowCloseRequest(WindowEvent event) {
    }
    
    /*
     * Focus events.
     */

    protected void onFocusGained() {
    }

    protected void onFocusLost() {
    }

    /*
     * Key events.
     */

    protected void onKeyPressed(KeyEvent event) {
    }

    protected void onKeyReleased(KeyEvent event) {
    }

    protected void onKeyTyped(KeyEvent event) {
    }

    /*
     * Mouse events.
     */

    protected void onMousePressed(MouseEvent event) {
    }

    protected void onMouseReleased(MouseEvent event) {
    }

    protected void onMouseClicked(MouseEvent event) {
    }

    protected void onMouseEntered(MouseEvent event) {
    }

    protected void onMouseExited(MouseEvent event) {
    }

    protected void onMouseMoved(MouseEvent event) {
    }

    protected void onMouseDragged(MouseEvent event) {
    }

    /*
     * Scroll events.
     */

    protected void onScroll(ScrollEvent event) {
    }

    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private ReadOnlyBooleanProperty focusedProperty() {
        if (this.stage != null) {
            return this.stage.focusedProperty();
        } else {
            return this.node.focusedProperty();
        }
    }
    
    /*
     * 
     */

    /**
     * Removes previous event handler, if any, for the specified event type,
     * and then adds the specified one, if any.
     * 
     * @param eventHandlerHolder Holds currently registered event handler, if any.
     * @param eventType Must not be null.
     * @param eventHandler Can be null.
     */
    private <ST extends Event, T extends ST> void setNewEventHandler(
            ObjectWrapper<EventHandler<ST>> eventHandlerHolder,
            //
            EventType<T> eventType,
            EventHandler<ST> eventHandler) {
        /*
         * Removing old handler if any.
         */
        {
            final EventHandler<ST> oldEventHandler = eventHandlerHolder.value;
            if (oldEventHandler != null) {
                if (this.stage != null) {
                    this.stage.removeEventHandler(eventType, oldEventHandler);
                } else {
                    this.node.removeEventHandler(eventType, oldEventHandler);
                }
                eventHandlerHolder.value = null;
            }
        }
        /*
         * Adding new handler if any.
         */
        if (eventHandler != null) {
            if (this.stage != null) {
                this.stage.addEventHandler(eventType, eventHandler);
            } else {
                this.node.addEventHandler(eventType, eventHandler);
            }
            eventHandlerHolder.value = eventHandler;
        }
    }

    /**
     * Removes previous change listener, if any, on the specified observable value,
     * and then adds the specified one, if any.
     * 
     * @param changeListenerHolder Holds currently registered change listener, if any.
     * @param observableValue Value to observe. Must not be null.
     * @param changeListener Can be null.
     */
    private <T> void setNewChangeListener(
            ObjectWrapper<ChangeListener<T>> changeListenerHolder,
            //
            ObservableValue<T> observableValue,
            ChangeListener<T> changeListener) {
        /*
         * Removing old listener if any.
         */
        {
            final ChangeListener<T> oldChangeListener = changeListenerHolder.value;
            if (oldChangeListener != null) {
                observableValue.removeListener(oldChangeListener);
                changeListenerHolder.value = null;
            }
        }
        /*
         * Adding new listener if any.
         */
        if (changeListener != null) {
            observableValue.addListener(changeListener);
            changeListenerHolder.value = changeListener;
        }
    }
}
