import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Central static controller to gather all
 * user input functionality for the game in
 * one place
 */
public class InputController {

    protected static final double KEY_BOUNCE_TIME = 0.1f; // in seconds
    protected static Map<Integer, Timer> keyTimers; // indexed by key code
    protected static App app;

    /**
     * Initializes this controller to set its
     * initial state
     */
    public static void initialize(App app) {
        InputController.app = app;
        keyTimers = new HashMap<>();
        for (int i = 32; i < 349; i++) {
            keyTimers.put(i, new Timer());
        }
    }

    /**
     * Has the key been pressed for the first time
     * in x seconds? Used for debounced toggled input
     * @param key
     * @return
     */
    public static boolean keyPressed(int key) {
        // Get the appropriate timer
        Timer timer = keyTimers.get(key);
        // Read the delta without triggering
        double dt = timer.readDt();
        // If debounce necessary, return false
        if (dt < KEY_BOUNCE_TIME) return false;
        // Otherwise fetch result
        boolean result = keyHeld(key);
        // If it was pressed, we trigger the timer again
        if (result) timer.dt();
        return result;
    }

    /**
     * Was the key down this frame?
     * @param key
     * @return
     */
    public static boolean keyHeld(int key) {
        return keyHeldInt(key) == 1;
    }

    /**
     * Was the key down this frame, return int representation of boolean
     * @param key
     * @return
     */
    public static int keyHeldInt(int key) {
        return glfwGetKey(app.window.getWindow(), key);
    }

    /**
     * Was the given mouse button held this frame?
     * @param button
     * @return
     */
    public static boolean mouseButtonHeld(int button) {
        return glfwGetMouseButton(app.window.getWindow(), button) == GLFW_PRESS;
    }

    /**
     * Was the primary mouse button held this frame
     * @return
     */
    public static boolean primaryMouseButtonHeld() {
        return mouseButtonHeld(GLFW_MOUSE_BUTTON_1);
    }

    /**
     * Was the secondary mouse button held this frame
     * @return
     */
    public static boolean secondaryMouseButtonHeld() {
        return mouseButtonHeld(GLFW_MOUSE_BUTTON_2);
    }

}