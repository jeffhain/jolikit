package net.jolikit.bwd.impl.lwjgl3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.lwjgl.glfw.GLFW;

/**
 * Keys defined in LWJGL.
 * 
 * @see #org.lwjgl.glfw.GLFW
 */
public class LwjglKeys {

    //--------------------------------------------------------------------------
    // FIELDS
    //--------------------------------------------------------------------------
    
    private static final List<Integer> KEY_LIST = Collections.unmodifiableList(newKeyList());
    
    //--------------------------------------------------------------------------
    // PUBLIC METHODS
    //--------------------------------------------------------------------------
    
    /**
     * @return An unmodifiable list of all keys defined in LWJGL.
     */
    public static List<Integer> keyList() {
        return KEY_LIST;
    }
    
    //--------------------------------------------------------------------------
    // PRIVATE METHODS
    //--------------------------------------------------------------------------
    
    private LwjglKeys() {
    }
    
    private static List<Integer> newKeyList() {
        final List<Integer> list = new ArrayList<Integer>();
        list.add(GLFW.GLFW_KEY_UNKNOWN);
        list.add(GLFW.GLFW_KEY_SPACE);
        list.add(GLFW.GLFW_KEY_APOSTROPHE);
        list.add(GLFW.GLFW_KEY_COMMA);
        list.add(GLFW.GLFW_KEY_MINUS);
        list.add(GLFW.GLFW_KEY_PERIOD);
        list.add(GLFW.GLFW_KEY_SLASH);
        list.add(GLFW.GLFW_KEY_0);
        list.add(GLFW.GLFW_KEY_1);
        list.add(GLFW.GLFW_KEY_2);
        list.add(GLFW.GLFW_KEY_3);
        list.add(GLFW.GLFW_KEY_4);
        list.add(GLFW.GLFW_KEY_5);
        list.add(GLFW.GLFW_KEY_6);
        list.add(GLFW.GLFW_KEY_7);
        list.add(GLFW.GLFW_KEY_8);
        list.add(GLFW.GLFW_KEY_9);
        list.add(GLFW.GLFW_KEY_SEMICOLON);
        list.add(GLFW.GLFW_KEY_EQUAL);
        list.add(GLFW.GLFW_KEY_A);
        list.add(GLFW.GLFW_KEY_B);
        list.add(GLFW.GLFW_KEY_C);
        list.add(GLFW.GLFW_KEY_D);
        list.add(GLFW.GLFW_KEY_E);
        list.add(GLFW.GLFW_KEY_F);
        list.add(GLFW.GLFW_KEY_G);
        list.add(GLFW.GLFW_KEY_H);
        list.add(GLFW.GLFW_KEY_I);
        list.add(GLFW.GLFW_KEY_J);
        list.add(GLFW.GLFW_KEY_K);
        list.add(GLFW.GLFW_KEY_L);
        list.add(GLFW.GLFW_KEY_M);
        list.add(GLFW.GLFW_KEY_N);
        list.add(GLFW.GLFW_KEY_O);
        list.add(GLFW.GLFW_KEY_P);
        list.add(GLFW.GLFW_KEY_Q);
        list.add(GLFW.GLFW_KEY_R);
        list.add(GLFW.GLFW_KEY_S);
        list.add(GLFW.GLFW_KEY_T);
        list.add(GLFW.GLFW_KEY_U);
        list.add(GLFW.GLFW_KEY_V);
        list.add(GLFW.GLFW_KEY_W);
        list.add(GLFW.GLFW_KEY_X);
        list.add(GLFW.GLFW_KEY_Y);
        list.add(GLFW.GLFW_KEY_Z);
        list.add(GLFW.GLFW_KEY_LEFT_BRACKET);
        list.add(GLFW.GLFW_KEY_BACKSLASH);
        list.add(GLFW.GLFW_KEY_RIGHT_BRACKET);
        list.add(GLFW.GLFW_KEY_GRAVE_ACCENT);
        list.add(GLFW.GLFW_KEY_WORLD_1);
        list.add(GLFW.GLFW_KEY_WORLD_2);
        list.add(GLFW.GLFW_KEY_ESCAPE);
        list.add(GLFW.GLFW_KEY_ENTER);
        list.add(GLFW.GLFW_KEY_TAB);
        list.add(GLFW.GLFW_KEY_BACKSPACE);
        list.add(GLFW.GLFW_KEY_INSERT);
        list.add(GLFW.GLFW_KEY_DELETE);
        list.add(GLFW.GLFW_KEY_RIGHT);
        list.add(GLFW.GLFW_KEY_LEFT);
        list.add(GLFW.GLFW_KEY_DOWN);
        list.add(GLFW.GLFW_KEY_UP);
        list.add(GLFW.GLFW_KEY_PAGE_UP);
        list.add(GLFW.GLFW_KEY_PAGE_DOWN);
        list.add(GLFW.GLFW_KEY_HOME);
        list.add(GLFW.GLFW_KEY_END);
        list.add(GLFW.GLFW_KEY_CAPS_LOCK);
        list.add(GLFW.GLFW_KEY_SCROLL_LOCK);
        list.add(GLFW.GLFW_KEY_NUM_LOCK);
        list.add(GLFW.GLFW_KEY_PRINT_SCREEN);
        list.add(GLFW.GLFW_KEY_PAUSE);
        list.add(GLFW.GLFW_KEY_F1);
        list.add(GLFW.GLFW_KEY_F2);
        list.add(GLFW.GLFW_KEY_F3);
        list.add(GLFW.GLFW_KEY_F4);
        list.add(GLFW.GLFW_KEY_F5);
        list.add(GLFW.GLFW_KEY_F6);
        list.add(GLFW.GLFW_KEY_F7);
        list.add(GLFW.GLFW_KEY_F8);
        list.add(GLFW.GLFW_KEY_F9);
        list.add(GLFW.GLFW_KEY_F10);
        list.add(GLFW.GLFW_KEY_F11);
        list.add(GLFW.GLFW_KEY_F12);
        list.add(GLFW.GLFW_KEY_F13);
        list.add(GLFW.GLFW_KEY_F14);
        list.add(GLFW.GLFW_KEY_F15);
        list.add(GLFW.GLFW_KEY_F16);
        list.add(GLFW.GLFW_KEY_F17);
        list.add(GLFW.GLFW_KEY_F18);
        list.add(GLFW.GLFW_KEY_F19);
        list.add(GLFW.GLFW_KEY_F20);
        list.add(GLFW.GLFW_KEY_F21);
        list.add(GLFW.GLFW_KEY_F22);
        list.add(GLFW.GLFW_KEY_F23);
        list.add(GLFW.GLFW_KEY_F24);
        list.add(GLFW.GLFW_KEY_F25);
        list.add(GLFW.GLFW_KEY_KP_0);
        list.add(GLFW.GLFW_KEY_KP_1);
        list.add(GLFW.GLFW_KEY_KP_2);
        list.add(GLFW.GLFW_KEY_KP_3);
        list.add(GLFW.GLFW_KEY_KP_4);
        list.add(GLFW.GLFW_KEY_KP_5);
        list.add(GLFW.GLFW_KEY_KP_6);
        list.add(GLFW.GLFW_KEY_KP_7);
        list.add(GLFW.GLFW_KEY_KP_8);
        list.add(GLFW.GLFW_KEY_KP_9);
        list.add(GLFW.GLFW_KEY_KP_DECIMAL);
        list.add(GLFW.GLFW_KEY_KP_DIVIDE);
        list.add(GLFW.GLFW_KEY_KP_MULTIPLY);
        list.add(GLFW.GLFW_KEY_KP_SUBTRACT);
        list.add(GLFW.GLFW_KEY_KP_ADD);
        list.add(GLFW.GLFW_KEY_KP_ENTER);
        list.add(GLFW.GLFW_KEY_KP_EQUAL);
        list.add(GLFW.GLFW_KEY_LEFT_SHIFT);
        list.add(GLFW.GLFW_KEY_LEFT_CONTROL);
        list.add(GLFW.GLFW_KEY_LEFT_ALT);
        list.add(GLFW.GLFW_KEY_LEFT_SUPER);
        list.add(GLFW.GLFW_KEY_RIGHT_SHIFT);
        list.add(GLFW.GLFW_KEY_RIGHT_CONTROL);
        list.add(GLFW.GLFW_KEY_RIGHT_ALT);
        list.add(GLFW.GLFW_KEY_RIGHT_SUPER);
        list.add(GLFW.GLFW_KEY_MENU);
        list.add(GLFW.GLFW_KEY_LAST);
        return list;
    }
}
