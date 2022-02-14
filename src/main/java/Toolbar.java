import org.joml.Vector2i;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;

import static org.lwjgl.nanovg.NanoVG.*;

public class Toolbar {

    static final int NUMBER_ITEMS = 10;
    static final float HEIGHT_PERCENTAGE = 0.8f;
    static final int BORDER = 5;

    static BlockType[] items = new BlockType[NUMBER_ITEMS];
    static int selected = 0;

    /**
     * What do do based on scroll ofset
     * @param yoffset
     */
    public static void processScroll(double yoffset) {
        // Move selection up or down
        if (yoffset < 0) {
            selected = selected + 1;
            if (selected >= NUMBER_ITEMS) selected -= NUMBER_ITEMS;
        } else {
            selected = selected - 1;
            if (selected < 0) selected += NUMBER_ITEMS;
        }
    }

    // Just put in some generic block types
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

    /**
     * Draw the toolbar
     * @param app
     */
    public static void draw(App app) {
        // Margin to top and bottom of screen
        float margin = app.WINDOW_HEIGHT * (1 - HEIGHT_PERCENTAGE) / 2;
        float iconSize = app.WINDOW_HEIGHT * HEIGHT_PERCENTAGE / (float) NUMBER_ITEMS;
        // Draw background box
        nvgBeginPath(app.vg);
        nvgRect(app.vg, app.WINDOW_WIDTH - iconSize, margin, iconSize, app.WINDOW_HEIGHT * HEIGHT_PERCENTAGE);
        nvgFillColor(app.vg, nvgRGBAf(0, 0, 0, 0.35f, NVGColor.create()));
        nvgFill(app.vg);
        // Draw block icons
        // DISCLAIMER I HAVE NO IDEA HOW THE IMAGE PATTERN FUNCTION WORKS
        // THAT THIS WORKS IS ALL PURE BRUTE FORCE TRYING STUFF AND LUCK
        float aspectRatio = iconSize / (float) Block.size;
        for (int i = 0; i < NUMBER_ITEMS; i++) {
            if (items[i] == null) continue;
            float screenX = app.WINDOW_WIDTH - iconSize;
            float screenY = margin + i * iconSize;
            Vector2i loc = Block.textureLocation.get(items[i]);
            float imgX = loc.x * Block.size;
            float imgY = loc.y * Block.size;
            NVGPaint paint = nvgImagePattern(app.vg, screenX - imgX * aspectRatio, screenY - imgY * aspectRatio,
                    Block.size * Block.increment * aspectRatio, Block.size * Block.increment * aspectRatio, 0, app.textureImg, 1, NVGPaint.create());
            nvgBeginPath(app.vg);
            nvgRect(app.vg, app.WINDOW_WIDTH - iconSize, margin + i * iconSize, iconSize, iconSize);
            nvgFillPaint(app.vg, paint);
            nvgFill(app.vg);
        }
        // Draw selection
        nvgBeginPath(app.vg);
        nvgRect(app.vg, app.WINDOW_WIDTH - iconSize, margin + selected * iconSize, iconSize, iconSize);
        nvgStrokeColor(app.vg, nvgRGBAf(1, 1, 1, 0.85f, NVGColor.create()));
        nvgStrokeWidth(app.vg, BORDER);
        nvgStroke(app.vg);
    }

}
