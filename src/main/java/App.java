import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.opengl.GL;

import javax.swing.*;
import java.nio.FloatBuffer;

import static java.sql.Types.NULL;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;


/**
 * Main runnable application class
 */
public class App {

    public Window window;
    public int WINDOW_WIDTH = 1920;
    public int WINDOW_HEIGHT = 1080;
    public String WINDOW_TITLE = "BlockWorldEditor";

    public long vg;
    public int font;
    public int textureImg;
    public float contentScaleX;
    public float contentScaleY;

    public Timer fps;
    public World world;

    public boolean wireframe = false;

    /**
     * Run the application
     */
    public void run() {
        initialize();
        loop();
        terminate();
    }

    /**
     * Initialize the app
     */
    public void initialize() {
        System.out.println("LWJGL Version: " + Version.getVersion());
        // Out GLFW errors to std err
        GLFWErrorCallback.createPrint(System.err).set();
        // Init glfw
        if (!glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }
        // Get the resolution of the primary monitor
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        // Create the game window
        window = new Window(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_TITLE);
        // Center the window
        glfwSetWindowPos(
                window.getWindow(),
                (vidmode.width() - WINDOW_WIDTH) / 2,
                (vidmode.height() - WINDOW_HEIGHT) / 2
        );
        // Escape closes window
        glfwSetKeyCallback(window.getWindow(), (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });
        // Make the OpenGL context current
        glfwMakeContextCurrent(window.getWindow());
        // Make the window visible
        glfwShowWindow(window.getWindow());
        // Enable vsync
        glfwSwapInterval(1);
        // Allows us to use OpenGL
        GL.createCapabilities();
        // Set the background clear color
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Initialize input controller
        InputController.initialize(this);
        // Intitialize fps timer
        fps = new Timer();
        // Load Block model and texture
        Block.loadTexture();

        // Hide the cursor
        glfwSetInputMode(window.getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

        // Nano VG stuff
        glEnable(GL_STENCIL_TEST);
        // Create nano vg context
        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == NULL) {
            throw new IllegalStateException("Failed to initialize NanoVG");
        }
        // Get the content scaling factors
        FloatBuffer sx = BufferUtils.createFloatBuffer(1);
        FloatBuffer sy = BufferUtils.createFloatBuffer(1);
        glfwGetWindowContentScale(window.getWindow(), sx, sy);
        contentScaleX = sx.get(0);
        contentScaleY = sy.get(0);

        // Load font
        String path = this.getClass().getResource("OpenSans-Bold.ttf").getPath().toString().substring(1);
        font = nvgCreateFont(vg, "sans", path);

        // Load texture img for UI
        path = this.getClass().getResource("textures.png").getPath().toString().substring(1);
        textureImg = nvgCreateImage(vg, path, NVG_IMAGE_NEAREST | NVG_IMAGE_PREMULTIPLIED);

        // Initialize command line
        CommandLine.init();
        // And bind character input callback
        glfwSetCharCallback(window.getWindow(), (long window, int code) -> {
            CommandLine.processCharInput(code);
        });

        // Initialize toolbar
        Toolbar.init();
        glfwSetScrollCallback(window.getWindow(), (long window, double xoffset, double yoffset) -> {
            Toolbar.processScroll(yoffset);
        });
    }

    /**
     * Contains the main game loop
     */
    public void loop() {

        // Make a world instance with some blocks
        world = new World(this);

        // Generate some blocks of all types
        float offset = 0;
        for (BlockType type : BlockType.values()) {
            for (int i = 0; i < 1000; i++) {
                world.addBlock(new Block(offset, 15f, i * -2.0f, type));
            }
            offset += 2.0f;
        }
        for (int x = -150; x < 150; x++) {
            for (int z = -150; z < 150; z++) {
                for (int y = 0; y < 15; y++) {
                    if (y < 10) world.addBlock(new Block(x, y, z, BlockType.STONE));
                    else if (y < 14) world.addBlock(new Block(x, y, z, BlockType.DIRT));
                    else world.addBlock(new Block(x, y, z, BlockType.GRASS));
                }
            }
        }

        world.camera.position.y = 55f;

        // After editing all the chunks, we generate their mesh
        for (Chunk c : world.chunks) c.regenerateMesh();

        // Main game loop
        fps.dt();
        double accumulatedTime = 0;
        while ( !glfwWindowShouldClose(window.getWindow()) ) {
            // Get time delta
            double dt = fps.dt();
            accumulatedTime += dt;

            // Open and close command line
            if (InputController.keyPressed(GLFW_KEY_ENTER)) {
                if (CommandLine.show) executeCommand(CommandLine.content);
                CommandLine.show = !CommandLine.show;
                CommandLine.historyIndex = -1;
                CommandLine.content = "";
            }

            // All nano vg rendering must occur after this call
            nvgBeginFrame(vg, WINDOW_WIDTH, WINDOW_HEIGHT, contentScaleY);
            
            // Apply input to the world or command line
            if (!CommandLine.show) world.tick(this, dt);
            else CommandLine.processInput();

            // Render the world
            if (wireframe) glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            world.render();
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

            // Render the UI over the rest
            renderUI(world);

            if (accumulatedTime > 1) {
                accumulatedTime -= 1;
            }

            // All nano vg rendering must occur before this call
            nvgEndFrame(vg);
            // Swap the front and back buffers
            glfwSwapBuffers(window.getWindow());
            glfwPollEvents();
        }

    }

    /**
     * Render the UI
     * @param world world to fetch information from
     */
    public void renderUI(World world) {
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        // Keybinds
        nvgBeginPath(vg);
        nvgFontSize(vg, 15);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 10, 20, "ESC to quit, F to fly, 1/2 for selecting, ENTER to open command line, type 'help' for commands");
        // FPS counter
        nvgBeginPath(vg);
        nvgFontSize(vg, 15);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 10, 35, "fps: " + String.format("%.0f", fps.getFrequency()));
        // Camera coordinates
        nvgBeginPath(vg);
        nvgFontSize(vg, 15);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 10, 50, "pos: " + world.camera.position);
        // Camera dir
        nvgBeginPath(vg);
        nvgFontSize(vg, 15);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 10, 65, "dir: " + world.camera.getDirection());
        // Time
        nvgBeginPath(vg);
        nvgFontSize(vg, 15);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 10, 80, "time: " + String.format("%.0f (%.0f/s)",  world.time, world.timeRate));
        // Selected block type
        nvgBeginPath(vg);
        nvgFontSize(vg, 15);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        if (Toolbar.getSelectedBlock() != null) {
            nvgText(vg, 10, 95, "holding block: " + Toolbar.getSelectedBlock().name());
        } else {
            nvgText(vg, 10, 95, "holding block: ");
        }


        // Render crosshair
        int crossHairLength = 35;
        int crossHairThickness = 3;
        nvgBeginPath(vg);
        nvgRect(vg, WINDOW_WIDTH / 2 - crossHairLength / 2, WINDOW_HEIGHT / 2 - crossHairThickness / 2, crossHairLength, crossHairThickness);
        nvgRect(vg, WINDOW_WIDTH / 2 - crossHairThickness / 2, WINDOW_HEIGHT / 2 - crossHairLength / 2, crossHairThickness, crossHairLength);
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgFill(vg);

        // Draw toolbar
        Toolbar.draw(this);

        // Draw command line
        if (CommandLine.show) {
            CommandLine.draw(this);
        }

        glEnable(GL_CULL_FACE);
    }

    /**
     * Attempt to execute the functionality of the given command
     * @param command string command contents
     */
    public void executeCommand(String command) {
        // Clean input
        command = command.trim().toLowerCase();
        if (command.length() == 0) return;
        // Note in history
        CommandLine.history.add(command);
        // Help command lists all command and functionality
        if (command.equals("help")) {
            StringBuilder h = new StringBuilder();
            h.append("HELP\n");
            h.append("import    Imports a saved world from file.\n");
            h.append("export    Exports current world to file\n");
            h.append("render wireframe <on/off>    Toggle wireframe rendering\n");
            h.append("render distance <distance>    Set render distance [0,-]\n");
            h.append("vsync <on/off>    Toggle vsync\n");
            h.append("time <time>    Set time to <time> [0,2399]\n");
            h.append("time rate <rate>    Set time rate to <rate> [0,-]\n");
            JOptionPane.showMessageDialog(new JDialog(), h.toString());
        } else if (command.equals("import")) {
            // Import a world from a file
            world = WorldManager.importWorld(this);
        } else if (command.equals("export")) {
            // Export current world to a file
            WorldManager.exportWorld(world);
        } else if (command.startsWith("render")) {
            // Edit rendering settings
            // Wireframe toggle
            if (command.equals("render wireframe on")) wireframe = true;
            else if (command.equals("render wireframe off")) wireframe = false;
            // Set render distance
            else if (command.startsWith("render distance")) {
                String[] split = command.split(" ");
                try {
                    int dist = Integer.parseInt(split[split.length - 1]);
                    if (dist >= 0) Renderer.RENDER_DISTANCE = dist;
                } catch (NumberFormatException e) {};
            }
        } else if (command.startsWith("vsync")) {
            // Toggle vsync
            if (command.equals("vsync off")) glfwSwapInterval(0);
            else if (command.equals("vsync on")) glfwSwapInterval(1);
            // Set time rate and value
        } else if (command.startsWith("time")) {
            if (command.startsWith("time rate")) {
                String[] split = command.split(" ");
                try {
                    int rateValue = Integer.parseInt(split[split.length - 1]);
                    if (rateValue >= 0 && rateValue < 2400) world.timeRate = rateValue;
                } catch (NumberFormatException e) {};
            } else {
                String[] split = command.split(" ");
                try {
                    int timeValue = Integer.parseInt(split[split.length - 1]);
                    if (timeValue >= 0 && timeValue < 2400) world.time = timeValue;
                } catch (NumberFormatException e) {};
            }
        } else if (command.startsWith("set")) {
            // Set selection to given block type
            if (world.select1 != null & world.select2 != null) {
                try {
                    String[] split = command.split(" ");
                    BlockType type = BlockType.valueOf(split[1].toUpperCase());
                    world.setBlocks(world.select1, world.select2, type);
                } catch (IllegalArgumentException e) {};
            }
        } else if (command.startsWith("replace")) {
            // Replace given blocktype with other blocktype in selection
            if (world.select1 != null & world.select2 != null) {
                try {
                    String[] split = command.split(" ");
                    BlockType oType = BlockType.valueOf(split[1].toUpperCase());
                    BlockType nType = BlockType.valueOf(split[2].toUpperCase());
                    world.replaceBlocks(world.select1, world.select2, oType, nType);
                } catch (IllegalArgumentException e) {};
            }
        } else if (command.equals("remove")) {
            // Remove all blocks in selection
            if (world.select1 != null & world.select2 != null) {
                world.removeBlocks(world.select1, world.select2);
            }
        }
        fps.dt();
    }

    /**
     * Terminate the app, doing a proper clean up
     */
    public void terminate() {
        nvgDelete(vg);
        // Clear any window callbacks
        glfwFreeCallbacks(window.getWindow());
        // Destroy the window
        glfwDestroyWindow(window.getWindow());
        // Terminate GLFW
        glfwTerminate();
        // Exit the process
        System.exit(0);
    }

    public static void main(String[] args) {
        App main = new App();
        main.run();
    }

}
