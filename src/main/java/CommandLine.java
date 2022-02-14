import org.lwjgl.nanovg.NVGColor;

import java.nio.charset.Charset;

import static org.lwjgl.nanovg.NanoVG.*;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Basic command line implementation
 */
public class CommandLine {

    // Current content of command line
    static String content = "";
    // Is it shown
    static boolean show = false;

    // Cursor blinking stuff
    static Timer cursorBlink = new Timer();
    static boolean cursorIsShown = false;
    static double cursorBlinkAccum = 0.0;

    // Settings
    static int HEIGHT = 30;
    static float CURSOR_BLINK_PERIOD = 0.5f;

    static void init() {
        cursorBlink.init();
    }

    /**
     * Called when glfw calls a character input callback
     * for text input
     * @param code
     */
    static void processCharInput(int code) {
        if (!show) return;
        String str = new String(new byte[]{(byte) code}, Charset.forName("UTF-8"));
        content += str;
    }

    /**
     * Called if command line is shown
     * used for example for backspace functionality
     */
    static void processInput() {
        if (InputController.keyPressed(GLFW_KEY_BACKSPACE)) {
            if (content.length() > 0) {
                content = content.substring(0, content.length() - 1);
            }
        }
    }

    /**
     * Draw the command line in its current state, only called if show = true
     * @param app
     */
    static void draw(App app) {
        // Draw background box
        nvgBeginPath(app.vg);
        nvgRect(app.vg, 0, app.WINDOW_HEIGHT - HEIGHT, app.WINDOW_WIDTH, HEIGHT);
        nvgFillColor(app.vg, nvgRGBAf(0, 0, 0, 0.35f, NVGColor.create()));
        nvgFill(app.vg);
        // Draw contents
        cursorBlinkAccum += cursorBlink.dt();
        if (cursorBlinkAccum > CURSOR_BLINK_PERIOD) {
            cursorBlinkAccum -= CURSOR_BLINK_PERIOD;
            cursorIsShown = !cursorIsShown;
        }
        nvgBeginPath(app.vg);
        nvgFontSize(app.vg, HEIGHT);
        nvgFontFace(app.vg, "sans");
        nvgFillColor(app.vg, nvgRGBAf(1, 1, 1, 0.8f, NVGColor.create()));
        nvgText(app.vg, 0, app.WINDOW_HEIGHT - 5, "> " + content+ (cursorIsShown ? "|" : ""));
    }

}
