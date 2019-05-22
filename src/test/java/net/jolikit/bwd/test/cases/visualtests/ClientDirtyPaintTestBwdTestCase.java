package net.jolikit.bwd.test.cases.visualtests;

import java.util.ArrayList;
import java.util.List;

import net.jolikit.bwd.api.InterfaceBwdBinding;
import net.jolikit.bwd.api.events.BwdMouseEvent;
import net.jolikit.bwd.api.graphics.BwdColor;
import net.jolikit.bwd.api.graphics.GPoint;
import net.jolikit.bwd.api.graphics.GRect;
import net.jolikit.bwd.api.graphics.InterfaceBwdGraphics;
import net.jolikit.bwd.test.cases.utils.AbstractBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCase;
import net.jolikit.bwd.test.utils.InterfaceBwdTestCaseClient;

/**
 * To check that only dirty areas are properly propagated to the client
 * through base clip, and that only them are paint when the client
 * only paints them.
 * 
 * Dividing the client area in cells.
 * Pressing the mouse on a cell causes a dirty painting to be ensured for it.
 * A cycle of colors is used for successive paintings, so as to be able
 * to figure out the clip corresponding to each dirty painting.
 */
public class ClientDirtyPaintTestBwdTestCase extends AbstractBwdTestCase {

    //--------------------------------------------------------------------------
    // CONFIGURATION
    //--------------------------------------------------------------------------

    private static final int CELL_SPANS = 50;

    /**
     * Must not contain black (used for lines drawing).
     */
    private static final BwdColor[] COLORS = new BwdColor[]{
        BwdColor.BLUE,
        BwdColor.YELLOW,
        BwdColor.GREEN,
        BwdColor.RED,
        BwdColor.BROWN,
        BwdColor.CHOCOLATE,
        BwdColor.CYAN,
        BwdColor.LAVENDER,
        BwdColor.FUCHSIA,
        BwdColor.DEEPPINK,
        BwdColor.DEEPSKYBLUE,
    };
    
    private static final int INITIAL_WIDTH = 300;
    private static final int INITIAL_HEIGHT = 200;
    private static final GPoint INITIAL_CLIENT_SPANS = GPoint.valueOf(INITIAL_WIDTH, INITIAL_HEIGHT);

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private int paintCount = 0;
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------

    public ClientDirtyPaintTestBwdTestCase() {
    }

    public ClientDirtyPaintTestBwdTestCase(InterfaceBwdBinding binding) {
        super(binding);
    }
    
    @Override
    public InterfaceBwdTestCase newTestCase(InterfaceBwdBinding binding) {
        return new ClientDirtyPaintTestBwdTestCase(binding);
    }
    
    @Override
    public InterfaceBwdTestCaseClient newClient() {
        return new ClientDirtyPaintTestBwdTestCase(this.getBinding());
    }

    @Override
    public GPoint getInitialClientSpans() {
        return INITIAL_CLIENT_SPANS;
    }

    /*
     * 
     */
    
    @Override
    public void onMousePressed(BwdMouseEvent event) {
        super.onMousePressed(event);
        
        final int x = event.xInClient();
        final int y = event.yInClient();
        
        final int cellXIndex = x / CELL_SPANS;
        final int cellYIndex = y / CELL_SPANS;
        
        final int cellX = cellXIndex * CELL_SPANS;
        final int cellY = cellYIndex * CELL_SPANS;
        
        final GRect dirtyRect = GRect.valueOf(cellX, cellY, CELL_SPANS, CELL_SPANS);
        this.getHost().makeDirtyAndEnsurePendingClientPainting(dirtyRect);
    }

    //--------------------------------------------------------------------------
    // PROTECTED METHODS
    //--------------------------------------------------------------------------
    
    @Override
    protected List<GRect> paint_initDone(
            InterfaceBwdGraphics g,
            GRect dirtyRect) {
        
        final ArrayList<GRect> paintedRectList = new ArrayList<GRect>();
        if (dirtyRect.isEmpty()) {
            /*
             * Nothing dirty: painting nothing.
             */
            return paintedRectList;
        } else {
            /*
             * Dirty painting : we only paint, and indicate to have painted,
             * the dirty parts, to check that the binding properly computes
             * them when full repaint is needed (during or after moves,
             * during or after resizes, after show-up, and after focus gain
             * or focus loss).
             */
            g.addClipInClient(dirtyRect);
            paintedRectList.add(dirtyRect);
        }

        /*
         * Filling box: only the clip part should be paint.
         */
        
        final int pc = ++this.paintCount;
        final BwdColor color = COLORS[(pc - 1) % COLORS.length];
        g.setColor(color);
        
        final GRect box = g.getBoxInClient();
        g.fillRect(box);

        /*
         * Painting various lines so that we can check
         * which parts are paint.
         */
        
        g.setColor(BwdColor.BLACK);
        // Half cell span.
        final int hcs = CELL_SPANS/2;
        for (int x = box.x() + hcs; x <= box.xMax(); x += CELL_SPANS) {
            g.drawLine(box.x() + hcs, box.y() + hcs, x, box.yMax());
        }
        for (int y = box.y() + hcs; y <= box.yMax(); y += CELL_SPANS) {
            g.drawLine(box.x() + hcs, box.y() + hcs, box.xMax(), y);
        }
        
        /*
         * Indicating paint count in clip part.
         */
        
        final GRect clip = g.getClipInUser();
        
        final BwdColor other = BwdColor.valueOfRgb8(
                (color.getRed8() + 128) % 256,
                (color.getGreen8() + 128) % 256,
                (color.getBlue8() + 128) % 256);
        g.setColor(other);
        g.drawText(clip.x(), clip.y(), "pc=" + pc);
        
        return paintedRectList;
    }
}
