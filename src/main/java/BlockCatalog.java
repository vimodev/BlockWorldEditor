import org.joml.Vector2i;
import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.nanovg.NanoVG.*;

// Inventory type catalog of blocks
public class BlockCatalog {

    // Should the catalog be shown?
    public static boolean show = false;

    private static float HEIGHT_PERCENTAGE = 0.6f;
    private static int N_ROWS = 4;
    private static int N_COLS = 5;

    public static void processInput() {
        // On mouse click
        if (InputController.primaryMouseClicked()) {
            DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
            DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
            glfwGetCursorPos(App.instance.window.getWindow(), xBuffer, yBuffer);
            xBuffer.rewind(); double x = xBuffer.get();
            yBuffer.rewind(); double y = yBuffer.get();
            // Is it confined to block catalog?
            int gridHeight = (int) (HEIGHT_PERCENTAGE * App.instance.window.getHeight());
            int gridWidth = (int) (gridHeight * ((float) N_COLS / N_ROWS));
            int leftTopX = (App.instance.window.getWidth() - gridWidth) / 2;
            int leftTopY = (App.instance.window.getHeight() - gridHeight) / 2;
            // If so, try to find the appropriate block and replace in toolbar
            if (x > leftTopX && x < leftTopX + gridWidth && y > leftTopY && y < leftTopY + gridHeight) {
                int yLoc = (int) Math.floor(((float) (y - leftTopY) / (float) gridHeight) * N_ROWS);
                int xLoc = (int) Math.floor(((float) (x - leftTopX) / (float) gridWidth) * N_COLS);
                int index = xLoc + yLoc * N_COLS;
                if (index >= 0 && index < BlockType.values().length) {
                    Toolbar.items[Toolbar.selected] = BlockType.values()[index];
                }
            }
        }
    }

    public static void draw() {
        // Background
        int gridHeight = (int) (HEIGHT_PERCENTAGE * App.instance.WINDOW_HEIGHT);
        int gridWidth = (int) (gridHeight * ((float) N_COLS / N_ROWS));
        int leftTopX = (App.instance.WINDOW_WIDTH - gridWidth) / 2;
        int leftTopY = (App.instance.WINDOW_HEIGHT - gridHeight) / 2;
        nvgBeginPath(App.instance.vg);
        nvgRect(App.instance.vg, leftTopX, leftTopY, gridWidth, gridHeight);
        nvgFillColor(App.instance.vg, nvgRGBAf(0, 0, 0, 0.35f, NVGColor.create()));
        nvgFill(App.instance.vg);

        // Block textures
        float iconSize = gridHeight / N_ROWS;
        float aspectRatio = iconSize / (float) Block.size;
        int i = 0;
        for (BlockType type : BlockType.values()) {
            float screenX = leftTopX + (i % N_COLS) * iconSize;
            float screenY = leftTopY + (i / N_COLS) * iconSize;
            Vector2i loc = Block.textureLocation.get(type);
            float imgX = loc.x * Block.size;
            float imgY = loc.y * Block.size;
            NVGPaint paint = nvgImagePattern(App.instance.vg, screenX - imgX * aspectRatio, screenY - imgY * aspectRatio,
                    Block.size * Block.increment * aspectRatio, Block.size * Block.increment * aspectRatio, 0, App.instance.textureImg, 1, NVGPaint.create());
            nvgBeginPath(App.instance.vg);
            nvgRect(App.instance.vg, screenX, screenY, iconSize, iconSize);
            nvgFillPaint(App.instance.vg, paint);
            nvgFill(App.instance.vg);
            i++;
        }

    }

}
