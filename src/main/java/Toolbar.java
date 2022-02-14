import org.lwjgl.nanovg.NVGColor;

import static org.lwjgl.nanovg.NanoVG.*;

public class Toolbar {

    static final int NUMBER_ITEMS = 10;
    static final float HEIGHT_PERCENTAGE = 0.8f;
    static final int BORDER = 5;

    static BlockType[] items = new BlockType[NUMBER_ITEMS];
    static int selected = 0;

    public static void processScroll(double yoffset) {
        if (yoffset < 0) {
            selected = selected + 1;
            if (selected >= NUMBER_ITEMS) selected -= NUMBER_ITEMS;
        } else {
            selected = selected - 1;
            if (selected < 0) selected += NUMBER_ITEMS;
        }
    }

    public static void init() {
        int i = 0;
        for (BlockType type : BlockType.values()) {
            items[i] = type;
            i++;
            if (i >= NUMBER_ITEMS) break;
        }
    }

    public static BlockType getSelectedBlock() {
        return items[selected];
    }

    public static void draw(App app) {
        float margin = app.WINDOW_HEIGHT * (1 - HEIGHT_PERCENTAGE) / 2;
        float iconSize = app.WINDOW_HEIGHT * HEIGHT_PERCENTAGE / (float) NUMBER_ITEMS;
        // Draw background box
        nvgBeginPath(app.vg);
        nvgRect(app.vg, app.WINDOW_WIDTH - iconSize, margin, iconSize, app.WINDOW_HEIGHT * HEIGHT_PERCENTAGE);
        nvgFillColor(app.vg, nvgRGBAf(0, 0, 0, 0.35f, NVGColor.create()));
        nvgFill(app.vg);
        // Draw selection
        nvgBeginPath(app.vg);
        nvgRect(app.vg, app.WINDOW_WIDTH - iconSize, margin + selected * iconSize, iconSize, iconSize);
        nvgStrokeColor(app.vg, nvgRGBAf(1, 1, 1, 0.85f, NVGColor.create()));
        nvgStrokeWidth(app.vg, BORDER);
        nvgStroke(app.vg);
    }

}
