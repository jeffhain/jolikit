package net.jolikit.bwd.impl.sdl2.jlib;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

/**
 * \brief Dollar Gesture Event (event.dgesture.*)
 * typedef struct SDL_DollarGestureEvent
 * {
 *     Uint32 type;        // ::SDL_DOLLARGESTURE or ::SDL_DOLLARRECORD
 *     Uint32 timestamp;
 *     SDL_TouchID touchId; // The touch device id
 *     SDL_GestureID gestureId;
 *     Uint32 numFingers;
 *     float error;
 *     float x;            // Normalized center of gesture
 *     float y;            // Normalized center of gesture
 * } SDL_DollarGestureEvent;
 */
public class SDL_DollarGestureEvent extends Structure {
    public static class ByReference extends SDL_DollarGestureEvent implements Structure.ByReference {
    }
    public static class ByValue extends SDL_DollarGestureEvent implements Structure.ByValue {
    }
    public int type;
    public int timestamp;
    public long touchId;
    public long gestureId;
    public int numFingers;
    public float error;
    public float x;
    public float y;
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
                "type", "timestamp",
                "touchId", "gestureId", "numFingers",
                "error", "x", "y");
    }
}
