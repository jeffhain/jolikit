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
package net.jolikit.bwd.impl.jfx;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import net.jolikit.bwd.api.InterfaceBwdClient;
import net.jolikit.bwd.api.InterfaceBwdHost;
import net.jolikit.bwd.api.events.BwdKeyEventPr;
import net.jolikit.bwd.api.events.BwdKeyEventT;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.events.BwdWheelEvent;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.impl.utils.AbstractBwdHost;
import net.jolikit.bwd.impl.utils.InterfaceHostLifecycleListener;
import net.jolikit.bwd.impl.utils.basics.ScaleHelper;
import net.jolikit.bwd.impl.utils.graphics.IntArrayGraphicBuffer;

public class JfxBwdHost extends AbstractBwdHost {

    /*
     * Stage ~= window
     * Scene ~= panel
     * Root node: put in scene.
     * 
     * For dialogs, we can't use javafx.scene.control.Dialog, since it's not a window,
     * nor javafx.stage.Popup, since its modality cannot be set.
     * Fortunately, we can just use a Stage instead, since it's possible to
     * init a Stage with an owner and modality.
     * Also, using Stage allows to have decorated dialogs, since javafx.stage.Popup
     * cannot be decorated.
     * 
     * If client painting delay is the same as JavaFX tick delay (1.0/60 s),
     * we still don't do painting from animation timer, for consistency with
     * behavior for other values, and because on resize animation timer
     * seems to speed up (for a smoother experience, but we don't want
     * to burn too much CPU when user requested a max PPS rate).
     * Also, for some reason, painting synchronously from within animation timer
     * seems to limit very much the PPS rate (like 100ish instead of 10000's).
     * 
     * Events in JavaFX (https://docs.oracle.com/javafx/2/events/processing.htm):
     * 1) Event capturing phase: any event filter is called, from root node to
     *    target node. If any filter consumes the event, the phase stops and
     *    the next phase does not occur.
     * 2) Event bubbling phase: any event handler is called, going from target
     *    node back to root node.
     * The primary difference between a filter and a handler is when each one
     * is executed.
     * Whether we use filers or handlers makes no difference, since the
     * host is at the end of the chain.
     * We use handlers for naming consistency.
     * 
     * Focus events:
     * In some versions of JavaFX, focus events always come
     * in the following order: canvas, then window, which is good
     * when losing focus but not appropriate when gaining it.
     * But we don't care, since we just use the window state,
     * and canvas focus state should always match it, unless
     * during eventual transient states, which should not hurt
     * any treatment.
     * 
     * We process move/resize related events asynchronously,
     * which allows to merge intermediary events (since window bounds
     * are split into 4 properties), and to cancel processing of a
     * move if we figure out it's actually a resize.
     * 
     * TODO jfx Principal difficulty during conception:
     * keeping stage/parent/canvas bounds consistent during resizes,
     * because they are not kept consistent right-away
     * (seems to need to wait for rendering or so).
     */
    
    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    /*
     * In case of scaling, we want to use a canvas in binding coordinates,
     * and not regular client canvas with a scale for transform, because:
     * - For direct drawing on client graphics, it would sometimes be
     *   OS-pixel precise, but for that user can just keep a scale of 1.
     * - It wouldn't help much with performances
     *   (graphics canvas is dimensions still correspond to scale of 1).
     * - It would cause inconsistencies between redefined
     *   drawing treatments, which pixels appear enlarged,
     *   and drawing treatments delegated to the backing library
     *   (font rendering, image drawing, etc.)
     * - It would be inconsistent with what happens with writable images,
     *   which are fully defined in binding graphics.
     */
    private static final boolean MUST_DRAW_ON_A_CANVAS_IN_BD = true;
    
    //--------------------------------------------------------------------------
    // PRIVATE CLASSES
    //--------------------------------------------------------------------------
    
    private class MyStageEl extends BaseJfxEventListener {
        public MyStageEl(Stage stage) {
            super(stage);
        }

        /*
         * Window events.
         */
        
        @Override
        protected void onWindowShown(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "onWindowShown(" + backingEvent + ")");
            }
            makeAllDirtyAndEnsurePendingClientPainting();
            onBackingWindowShown();
        }

        @Override
        protected void onWindowHidden(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "onWindowHidden(" + backingEvent + ")");
            }
            onBackingWindowHidden();
        }
        
        @Override
        protected void onWindowIconified() {
            if (DEBUG) {
                hostLog(this, "onWindowIconified()");
            }
            onBackingWindowIconified();
        }

        @Override
        protected void onWindowDeiconified() {
            if (DEBUG) {
                hostLog(this, "onWindowDeiconified()");
            }

            onBackingWindowDeiconified();
        }

        @Override
        protected void onWindowMaximized() {
            if (DEBUG) {
                hostLog(this, "onWindowMaximized()");
            }
            
            onBackingWindowMaximized();
        }

        @Override
        protected void onWindowDemaximized() {
            if (DEBUG) {
                hostLog(this, "onWindowDemaximized()");
            }
            
            onBackingWindowDemaximized();
        }
        
        @Override
        protected void onWindowMoved() {
            if (DEBUG) {
                hostLog(this, "onWindowMoved()");
            }
            
            /*
             * TODO jfx When window is iconified, (x,y) position is changed to
             * (-32000.0, -32000.0), but we don't want to consider these as special values
             * to ignore (since they aren't illegal values), so even then we might
             * trigger a (useless) painting.
             */
            onBackingWindowMoved();
        }
        
        @Override
        protected void onWindowResized() {
            if (DEBUG) {
                hostLog(this, "onWindowResized()");
            }
            
            /*
             * Canvas bounds change events come after window spans change events,
             * but we're fine since triggering painting here still makes it execute
             * after canvas bounds change events, when canvas is properly resized.
             */
            onBackingWindowResized();
        }
        
        @Override
        protected void onWindowCloseRequest(WindowEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "onWindowCloseRequest(" + backingEvent + ")");
            }
            
            onBackingWindowClosing();
        }

        /*
         * Focus events.
         */
        
        @Override
        protected void onFocusGained() {
            if (DEBUG) {
                hostLog(this, "onFocusGained()");
            }
            onBackingWindowFocusGained();
        }
        
        @Override
        protected void onFocusLost() {
            if (DEBUG) {
                hostLog(this, "onFocusLost()");
            }
            onBackingWindowFocusLost();
        }
    }
    
    /*
     * Canvas (client area) events listener.
     */

    private class MyCanvasEl extends BaseJfxEventListener {
        public MyCanvasEl(Node eventNode) {
            super(eventNode);
        }
        
        /*
         * Focus events.
         */

        @Override
        protected void onFocusGained() {
            if (DEBUG) {
                hostLog(this, "onFocusGained()");
            }
        }
        
        @Override
        protected void onFocusLost() {
            if (DEBUG) {
                hostLog(this, "onFocusLost()");
            }
        }

        /*
         * Key events.
         */
        
        @Override
        protected void onKeyPressed(KeyEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "onKeyPressed(" + backingEvent + ")");
            }
            
            final BwdKeyEventPr event = eventConverter.newKeyPressedEvent(backingEvent);
            onBackingKeyPressed(event);
        }

        @Override
        protected void onKeyTyped(KeyEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "onKeyTyped(" + backingEvent + ")");
            }
            final BwdKeyEventT event = eventConverter.newKeyTypedEventElseNull(backingEvent);
            if (event != null) {
                onBackingKeyTyped(event);
            }
        }

        @Override
        protected void onKeyReleased(KeyEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "onKeyReleased(" + backingEvent + ")");
            }
            final BwdKeyEventPr event = eventConverter.newKeyReleasedEvent(backingEvent);
            onBackingKeyReleased(event);
        }
        
        /*
         * Mouse events.
         */

        @Override
        protected void onMousePressed(MouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "onMousePressed(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMousePressedEvent(backingEvent);
            onBackingMousePressed(event);
        }

        @Override
        protected void onMouseReleased(MouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "onMouseReleased(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMouseReleasedEvent(backingEvent);
            onBackingMouseReleased(event);
        }

        @Override
        protected void onMouseClicked(MouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "onMouseClicked(" + backingEvent + ")");
            }
            // Bindings must not generate mouse clicked events.
        }

        @Override
        protected void onMouseEntered(MouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "onMouseEntered(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMouseEnteredClientEvent(backingEvent);
            onBackingMouseEnteredClient(event);
        }

        @Override
        protected void onMouseExited(MouseEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "onMouseExited(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMouseExitedClientEvent(backingEvent);
            onBackingMouseExitedClient(event);
        }

        @Override
        protected void onMouseMoved(MouseEvent backingEvent) {
            if (DEBUG_SPAM) {
                hostLog(this, "onMouseMoved(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMouseMovedEvent(backingEvent);
            onBackingMouseMoved(event);
        }

        @Override
        protected void onMouseDragged(MouseEvent backingEvent) {
            if (DEBUG_SPAM) {
                hostLog(this, "onMouseDragged(" + backingEvent + ")");
            }
            final BwdMouseEvent event = eventConverter.newMouseMovedEvent(backingEvent);
            onBackingMouseMoved(event);
        }
        
        /*
         * Scroll events.
         */
        
        @Override
        protected void onScroll(ScrollEvent backingEvent) {
            if (DEBUG) {
                hostLog(this, "onScroll(" + backingEvent + ")");
            }
            final BwdWheelEvent event = eventConverter.newWheelEventElseNull(backingEvent);
            if (event != null) {
                onBackingWheelEvent(event);
            }
        }
    }
    
    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------

    private final JfxEventConverter eventConverter;
    
    private final Stage window;
    
    /**
     * A kind of panel.
     */
    private final Scene scene;

    /**
     * Tied to the scene (same width/height).
     */
    private final Pane pane;
    
    /**
     * Occupies the whole pane.
     */
    private final Canvas canvas;
    
    private final JfxDirtySnapshotHelper dirtySnapshotHelper;
    
    private final JfxHostBoundsHelper hostBoundsHelper;
    
    private final JfxBwdCursorManager cursorManager;
    
    private final MyCanvasEl canvasEventListener;
    private final MyStageEl windowEventListener;
    
    private final IntArrayGraphicBuffer offscreenIntArrayBuffer =
        new IntArrayGraphicBuffer(
            JfxPaintUtils.MUST_PRESERVE_OB_CONTENT_ON_RESIZE,
            JfxPaintUtils.ALLOW_OB_SHRINKING);
    
    private final JfxImgDrawingUtils imgDrawingUtilsForHost =
        new JfxImgDrawingUtils(
            JfxPaintUtils.MUST_REUSE_IMG_FOR_HOST_UTILS);
    
    private final JfxImgDrawingUtils imgDrawingUtilsForGraphics =
        new JfxImgDrawingUtils(
            JfxPaintUtils.MUST_REUSE_IMG_FOR_GRAPHICS_UTILS);
    
    /*
     * 
     */
    
    /**
     * Reused to preserve non-dirty (non-painted) areas.
     */
    private Canvas lastOffscreenCanvas;
    
    private boolean currentPainting_mustUseIntArrG;
    private Canvas currentPainting_graphicsCanvas;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @param owner If not null, creates a dialog host, else a basic host.
     * @param title Must not be null (used in task bar even if not decorated).
     * @param modal Only used if creating a dialog host.
     */
    public JfxBwdHost(
            AbstractJfxBwdBinding binding,
            InterfaceHostLifecycleListener<AbstractBwdHost> hostLifecycleListener,
            JfxBwdHost owner,
            //
            String title,
            boolean decorated,
            boolean modal,
            //
            InterfaceBwdClient client) {
        super(
                binding.getBindingConfig(),
                binding,
                hostLifecycleListener,
                binding.getHostOfFocusedClientHolder(),
                owner,
                title,
                decorated,
                modal,
                client);
        
        final JfxBwdBindingConfig bindingConfig = binding.getBindingConfig();
        
        /*
         * Canvas.
         */
        
        final Canvas canvas = JfxUtils.newCanvas(0, 0);
        this.canvas = canvas;
        
        // Else can't gain focus.
        canvas.setFocusTraversable(true);
        
        // Needs to be false else no mouse events.
        canvas.mouseTransparentProperty().set(false);
        
        /*
         * Scene/Pane.
         */
        
        final Scene scene;
        final Pane pane;
        if (false) {
            // What we would have to do if using a Popup as window.
            final Popup popup = new Popup();
            scene = popup.getScene();
            pane = (Pane) scene.getRoot(); // Thank God it's a Pane.
        } else {
            pane = new Pane();
            
            final boolean depthBuffer = false;
            /*
             * TODO jfx Disabling does not seem to disable.
             */
            final SceneAntialiasing antialiasing = SceneAntialiasing.DISABLED;
            /*
             * TODO jfx Need NOT to use 0 as initial width/height,
             * else Canvas.snapshot(...) causes some kind of freeze,
             * cf. bug JDK-8187618.
             */
            final double initSceneWH = -1.0;
            scene = new Scene(
                    pane,
                    initSceneWH,
                    initSceneWH,
                    depthBuffer,
                    antialiasing);
        }
        this.scene = scene;
        this.pane = pane;

        /*
         * JavaFX uses white background by default, but most other
         * libraries use black by default, or as fall back when clearing
         * with "transparent" color when window alpha is not 0,
         * so we align on that.
         * Format: #RRGGBB
         */
        pane.setStyle("-fx-background-color: #000000");
        
        /*
         * TODO jfx Wanting to disable filling here to avoid
         * useless paintings.
         * Using "null" doesn't work, because it causes fallback
         * to a default opaque fill, but TRANSPARENT works.
         */
        scene.setFill(Color.TRANSPARENT);
        
        /*
         * Window (Stage).
         */
        
        final Stage window = new Stage();
        this.window = window;
        
        /*
         * TODO jfx Need to use tiny bounds here, and not default bounds,
         * because these initial bounds are used for first show up,
         * even if we set new backing bounds later before showing.
         * That way, on first show up, instead of window jumping (possibly slowly)
         * from default bounds to default-or-user-specified bounds,
         * it jumps from tiny bounds to default-or-user-specified bounds,
         * which is much less noticeable.
         */
        window.setX(0.0);
        window.setY(0.0);
        window.setWidth(1.0);
        window.setHeight(1.0);
        
        final boolean isDialog = (owner != null);
        if (isDialog) {
            window.initOwner(owner.getBackingWindow());
            if (modal) {
                window.initModality(Modality.WINDOW_MODAL);
            } else {
                window.initModality(Modality.NONE);
            }
        }
        
        if (decorated) {
            /*
             * "DECORATED" means decorated AND opaque, AND with
             * a default white background.
             */
            window.initStyle(StageStyle.DECORATED);
        } else {
            /*
             * "UNDECORATED" means undecorated AND opaque, AND with
             * a default white background.
             * 
             * "TRANSPARENT" means transparent AND undecorated.
             * 
             * With UNDECORATED, setting window alpha still allows
             * for transparency, so we could just use that,
             * but we prefer to use TRANSPARENT to be more explicit
             * and avoid eventual anti-transparency change for UNDECORATED.
             */
            window.initStyle(StageStyle.TRANSPARENT);
        }
        window.setTitle(title);
        
        /*
         * Plugging things together.
         */

        // Binding canvas spans to pane spans, after which can't
        // explicitly set canvas spans anymore.
        canvas.widthProperty().bind(pane.widthProperty());
        canvas.heightProperty().bind(pane.heightProperty());

        pane.getChildren().add(canvas);
        
        window.setScene(scene);

        /*
         * 
         */
        
        final AbstractBwdHost host = this;
        
        this.eventConverter = new JfxEventConverter(
                binding.getEventsConverterCommonState(),
                host);

        this.cursorManager = new JfxBwdCursorManager(canvas);
        
        this.hostBoundsHelper = new JfxHostBoundsHelper(
                host,
                bindingConfig.getDecorationInsets(),
                bindingConfig.getMinWindowWidthIfDecorated(),
                bindingConfig.getMinWindowHeightIfDecorated());
        
        this.dirtySnapshotHelper =
                new JfxDirtySnapshotHelper(
                        canvas,
                        JfxPaintUtils.ALLOW_OB_SHRINKING) {
            @Override
            protected boolean isDisabled() {
                return !bindingConfig.getMustImplementBestEffortPixelReading();
            }
        };
        
        /*
         * 
         */
        
        this.canvasEventListener = new MyCanvasEl(canvas);
        this.windowEventListener = new MyStageEl(window);
        
        /*
         * Dangerous parts ("this" publication) last.
         */
        
        this.canvasEventListener.addEventHandlers();
        this.windowEventListener.addEventHandlers();

        hostLifecycleListener.onHostCreated(this);
    }
    
    /*
     * 
     */
    
    @Override
    public JfxBwdBindingConfig getBindingConfig() {
        return (JfxBwdBindingConfig) super.getBindingConfig();
    }

    @Override
    public AbstractJfxBwdBinding getBinding() {
        return (AbstractJfxBwdBinding) super.getBinding();
    }
    
    /*
     * 
     */

    @Override
    public JfxBwdCursorManager getCursorManager() {
        return this.cursorManager;
    }

    /*
     * 
     */

    @Override
    public Window getBackingWindow() {
        return this.window;
    }
    
    public Scene getScene() {
        return this.scene;
    }
    
    public Pane getPane() {
        return this.pane;
    }
    
    public Canvas getCanvas() {
        return this.canvas;
    }

    /*
     * 
     */

    @Override
    public JfxBwdHost getOwner() {
        return (JfxBwdHost) super.getOwner();
    }

    /*
     * 
     */
    
    @Override
    public InterfaceBwdHost newDialog(
            String title,
            boolean decorated,
            boolean modal,
            //
            InterfaceBwdClient client) {
        final JfxBwdHost owner = this;
        return new JfxBwdHost(
                this.getBinding(),
                this.getDialogLifecycleListener(),
                owner,
                //
                title,
                decorated,
                modal,
                //
                client);
    }
    
    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------

    @Override
    protected void paintClientNowOrLater() {
        this.paintBwdClientNowAndBackingClient(
            getBindingConfig(),
            this.hostBoundsHelper);
    }

    @Override
    protected void setWindowAlphaFp_backing(double windowAlphaFp) {
        /*
         * TODO jfx Doesn't seem possible to have fully transparent background,
         * and to paint with fully opaque pixels: this alpha seem to forces itself
         * upon each final pixel, even those of "transparent" background.
         */
        this.window.setOpacity(windowAlphaFp);
    }
    
    /*
     * 
     */

    @Override
    protected boolean isBackingWindowShowing() {
        return this.window.isShowing();
    }

    @Override
    protected void showBackingWindow() {
        this.window.show();
    }

    @Override
    protected void hideBackingWindow() {
        this.window.hide();
    }

    /*
     * Focus.
     */

    @Override
    protected boolean isBackingWindowFocused() {
        if (!this.window.isShowing()) {
            /*
             * TODO jfx On Mac, isFocused() might stay true for dialogs
             * that become hidden due to their parent becoming hidden.
             */
            return false;
        }
        if (this.window.isIconified()) {
            /*
             * TODO jfx On Mac, isFocused() might stay true after
             * iconification (whether isBackingWindowMaximized() is true or not)
             * (and even after another window actually gets focused).
             */
            return false;
        }
        return this.window.isFocused();
    }
    
    @Override
    protected void requestBackingWindowFocusGain() {
        this.window.requestFocus();
    }

    /*
     * Iconified.
     */
    
    @Override
    protected boolean doesSetBackingWindowIconifiedOrNotWork() {
        return true;
    }

    @Override
    protected boolean isBackingWindowIconified() {
        return this.window.isIconified();
    }
    
    @Override
    protected void setBackingWindowIconifiedOrNot(boolean iconified) {
        this.window.setIconified(iconified);
    }

    /*
     * Maximized.
     */

    @Override
    protected boolean doesSetBackingWindowMaximizedOrNotWork() {
        return !this.getBinding().getBindingConfig().getMustNotImplementMaxDemax();
    }
    
    @Override
    protected boolean isBackingWindowMaximized() {
        /*
         * TODO jfx On Windows, can have a decorated dialog
         * (or regular host as well?) state stay "maximized"
         * after demaximization, even though the bounds
         * get properly demaximized.
         * Maybe tied to window position being out of screen,
         * which itself might be due to a JavaFX issue with decoration
         * since position is minus decoration span.
         * Ex.:
206_443361291  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; oldHostState = [sho, unf, dei, max, ope]
206_443361952  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; oldClientState = [sho, unf, dei, max, ope]
206_443362942  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; oldInsets = [4, 23, 4, 4]
206_443363603  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; oldClientBounds = [0, 19, 2560, 1377]
206_443364594  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; oldWindowBounds = [-4, -4, 2568, 1404]
206_443367237  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; hostMethod = DEMAXIMIZE
206_443376155  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r : setting expected maxed window bounds : [-4, -4, 2568, 1404]
206_443383092  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main]
206_443422400  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main]
206_443423391  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; newHostState = [sho, unf, dei, dem, ope]
206_443424382  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; newClientState = [sho, unf, dei, dem, ope]
206_443425043  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; newInsets = [4, 23, 4, 4]
206_443425703  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; newClientBounds = [0, 19, 2560, 1377]
206_443426694  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; newWindowBounds = [-4, -4, 2568, 1404]
206_443437264  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main]
206_443437925  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; expectedFinalHostState = [sho, foc, dei, dem, ope]
206_443438916  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; expectedFinalClientState = [sho, foc, dei, dem, ope]
206_443439577  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; expected demaxed client bounds = [40, 778, 150, 50]
206_443440568  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; expected demaxed window bounds = null
206_443441228  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r ; expected maxed window bounds = [-4, -4, 2568, 1404]
(...)
206_721402021  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r : (decorated = true)
206_721404994  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r : host state =   [sho, foc, dei, max, ope]
206_721407306  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r : client state = [sho, foc, dei, max, ope]
206_721408958  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r : (test state) shadowed maximized state =  false
206_721412261  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r : insets =  [4, 23, 4, 4]
206_721416225  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r : client bounds = [40, 778, 150, 50]
206_721419528  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r : window bounds = [36, 755, 158, 77]
206_721421840  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r : (test state) expected demaxed client bounds = [40, 778, 150, 50]
206_721423492  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r : (test state) expected demaxed window bounds = null
206_721426465  1 7860D5FF@Thread[14,JavaFX Application Thread,5,main] ; H0D3U2r : (test state) expected maxed window bounds = [-4, -4, 2568, 1404]
         */
        return this.window.isMaximized();
    }

    @Override
    protected void setBackingWindowMaximizedOrNot(boolean maximized) {
        this.window.setMaximized(maximized);
    }

    /*
     * Bounds.
     */
    
    @Override
    protected GRect getBackingInsetsInOs() {
        return this.hostBoundsHelper.getInsetsInOs();
    }

    @Override
    protected GRect getBackingClientBoundsInOs() {
        return this.hostBoundsHelper.getClientBoundsInOs();
    }
    
    @Override
    protected GRect getBackingWindowBoundsInOs() {
        return this.hostBoundsHelper.getWindowBoundsInOs();
    }
    
    @Override
    protected void setBackingClientBoundsInOs(GRect targetClientBoundsInOs) {
        this.hostBoundsHelper.setClientBoundsInOs(targetClientBoundsInOs);
    }

    @Override
    protected void setBackingWindowBoundsInOs(GRect targetWindowBoundsInOs) {
        this.hostBoundsHelper.setWindowBoundsInOs(targetWindowBoundsInOs);
    }

    /*
     * Closing.
     */
    
    @Override
    protected void closeBackingWindow() {
        // Same as Stage.hide(), but more consistent to use close() here.
        this.window.close();
    }
    
    /*
     * Painting.
     */
    
    @Override
    protected InterfaceBwdGraphics newRootGraphics(GRect boxWithBorder) {
        
        final boolean isImageGraphics = false;
        
        final boolean mustUseIntArrayGraphics =
            getBindingConfig().getMustUseIntArrayGraphicsForClients();
        this.currentPainting_mustUseIntArrG = mustUseIntArrayGraphics;
        
        final int scale = getScaleHelper().getScale();
        
        final InterfaceBwdGraphics gForBorder;
        if (mustUseIntArrayGraphics) {
            this.currentPainting_graphicsCanvas = null;
            this.offscreenIntArrayBuffer.setSize(
                boxWithBorder.xSpan(),
                boxWithBorder.ySpan());
            final int[] pixelArr =
                this.offscreenIntArrayBuffer.getPixelArr();
            final int pixelArrScanlineStride =
                this.offscreenIntArrayBuffer.getScanlineStride();
            gForBorder = new JfxBwdGraphicsWithIntArr(
                this.getBinding(),
                boxWithBorder,
                isImageGraphics,
                pixelArr,
                pixelArrScanlineStride);
        } else {
            final int graphicsGcScale;
            if (MUST_DRAW_ON_A_CANVAS_IN_BD
                && (scale != 1)) {
                /*
                 * Painting at scale 1 on a smaller and offscreen canvas,
                 * and then will do a scaled copy of it into displayed canvas.
                 */
                Canvas offscreenCanvas = this.lastOffscreenCanvas;
                if ((offscreenCanvas == null)
                    || (offscreenCanvas.getWidth() != boxWithBorder.xSpan())
                    || (offscreenCanvas.getHeight() != boxWithBorder.ySpan())) {
                    offscreenCanvas = new Canvas(
                        boxWithBorder.xSpan(),
                        boxWithBorder.ySpan());
                    this.lastOffscreenCanvas = offscreenCanvas;
                }
                this.currentPainting_graphicsCanvas = offscreenCanvas;
                graphicsGcScale = 1;
            } else {
                /*
                 * Directly painting at target scale.
                 */
                this.currentPainting_graphicsCanvas = this.canvas;
                graphicsGcScale = scale;
            }
            gForBorder = new JfxBwdGraphicsWithGc(
                this.getBinding(),
                boxWithBorder,
                //
                this.currentPainting_graphicsCanvas.getGraphicsContext2D(),
                graphicsGcScale,
                this.imgDrawingUtilsForGraphics,
                this.dirtySnapshotHelper);
        }
        return gForBorder;
    }
    
    @Override
    protected void paintBackingClient(
        ScaleHelper scaleHelper,
        GPoint clientSpansInOs,
        GPoint bufferPosInCliInOs,
        GPoint bufferSpansInBd,
        List<GRect> paintedRectList) {
        
        if (paintedRectList.isEmpty()) {
            return;
        }
        
        final GraphicsContext gc = this.canvas.getGraphicsContext2D();
        
        final int bufferXSpanInOs =
            scaleHelper.spanBdToOs(
                bufferSpansInBd.x());
        final int bufferYSpanInOs =
            scaleHelper.spanBdToOs(
                bufferSpansInBd.y());
        
        if (this.currentPainting_mustUseIntArrG) {
            this.imgDrawingUtilsForHost.drawIntArrBufferOnGc(
                bufferPosInCliInOs.x(),
                bufferPosInCliInOs.y(),
                bufferXSpanInOs,
                bufferYSpanInOs,
                //
                this.offscreenIntArrayBuffer,
                //
                0,
                0,
                bufferSpansInBd.x(),
                bufferSpansInBd.y(),
                //
                this.getBinding().getInternalParallelizer(),
                this.getAccurateClientScaling(),
                //
                gc);
        } else {
            if (this.currentPainting_graphicsCanvas != this.canvas) {
                final WritableImage graphicsImage =
                    JfxSnapshotHelper.takeSnapshot(
                        this.currentPainting_graphicsCanvas);
                
                this.imgDrawingUtilsForHost.drawBackingImageOnGc(
                    bufferPosInCliInOs.x(),
                    bufferPosInCliInOs.y(),
                    bufferXSpanInOs,
                    bufferYSpanInOs,
                    //
                    graphicsImage,
                    //
                    0,
                    0,
                    bufferSpansInBd.x(),
                    bufferSpansInBd.y(),
                    //
                    this.getBinding().getInternalParallelizer(),
                    this.getAccurateClientScaling(),
                    //
                    gc);
            }
        }
    }
}
