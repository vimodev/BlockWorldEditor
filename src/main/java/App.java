import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.opengl.GL;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

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

    static App instance;

    public Window window;
    public int WINDOW_WIDTH = 1920;
    public int WINDOW_HEIGHT = 1080;
    public String WINDOW_TITLE = "BlockWorldEditor";

    public boolean previousFrameHadCursor;

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
        // Enable multi sample buffer
        glfwWindowHint(GLFW_SAMPLES, 8);
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

        String prefix = System.getProperty("user.dir").startsWith("/") ? "/" : "" ;
        String path = "";
        // Load font
        try {
            path = App.resourceToFileSystem("OpenSans-Bold.ttf");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        font = nvgCreateFont(vg, "sans", prefix + path);

        // Load texture img for UI
        try {
            path = App.resourceToFileSystem("textures.png");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        textureImg = nvgCreateImage(vg, prefix + path, NVG_IMAGE_NEAREST | NVG_IMAGE_PREMULTIPLIED);

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

        // Set window resize callback
        glfwSetFramebufferSizeCallback(window.getWindow(), (long window, int width, int height) -> {
            glViewport(0, 0, width, height);
            App.instance.window.setDimensions(width, height);
            App.instance.world.camera.setProjection(
                    (float) width / height,
                    App.instance.world.camera.fieldOfView,
                    App.instance.world.camera.zNear,
                    App.instance.world.camera.zFar);
        });

        // Window focus callback
        glfwSetWindowFocusCallback(window.getWindow(), (long window, boolean focused) -> {
           if (App.instance.window.getWindow() == window) {
               App.instance.window.isFocused = focused;
               if (focused) {
                   glfwSetCursorPos(App.instance.window.getWindow(), WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2);
               }
           }
        });

        previousFrameHadCursor = true;
    }

    /**
     * Contains the main game loop
     */
    public void loop() {

        // Make a world instance with some blocks
        world = new World(this, new HillWorldGenerator(System.currentTimeMillis(), 30, 15, 100f));

        // Make sure initial world is rendered, because spawning without chunks is bad
        int generating = world.manageChunks();
        // Just loop disgustingly until all dispatches are done
        while (world.chunks.size() < generating) {
            world.gatherChunks();
        }

        world.camera.position.y = 250f;

        glfwMaximizeWindow(window.getWindow());

        // Main game loop
        fps.dt();
        double accumulatedTime = 0;
        while ( !glfwWindowShouldClose(window.getWindow()) ) {
            // Get time delta
            double dt = fps.dt();
            accumulatedTime += dt;

            if (CommandLine.show || BlockCatalog.show) glfwSetInputMode(window.getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);

            // Open and close command line
            if (InputController.keyPressed(GLFW_KEY_ENTER)) {
                if (CommandLine.show) {
                    executeCommand(CommandLine.content);
                    glfwSetInputMode(window.getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
                } else {
                    glfwSetInputMode(window.getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                }
                CommandLine.show = !CommandLine.show;
                CommandLine.historyIndex = -1;
                CommandLine.content = "";
            }

            int step = 5;
            float difference = Renderer.NEW_RENDER_DISTANCE - Renderer.RENDER_DISTANCE;
            Renderer.RENDER_DISTANCE += Math.signum(difference) * step;
            World.chunkLoadRange = Renderer.RENDER_DISTANCE * 1.25f;
            World.chunkUnloadRange = World.chunkLoadRange + 128f;

            if (InputController.keyPressed(GLFW_KEY_E) && !CommandLine.show) {
                BlockCatalog.show = !BlockCatalog.show;
                if (BlockCatalog.show) glfwSetInputMode(window.getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
                else glfwSetInputMode(window.getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
            }

            // All nano vg rendering must occur after this call
            nvgBeginFrame(vg, WINDOW_WIDTH, WINDOW_HEIGHT, contentScaleY);
            
            // Apply input to the world or command line
            if (window.isFocused) {
                if (!CommandLine.show && !BlockCatalog.show) {
                    if (!previousFrameHadCursor) world.tick(this, dt);
                    else glfwSetCursorPos(window.getWindow(), WINDOW_WIDTH / 2, WINDOW_HEIGHT / 2);
                    previousFrameHadCursor = false;
                } else if (CommandLine.show){
                    CommandLine.processInput();
                    previousFrameHadCursor = true;
                } else if (BlockCatalog.show) {
                    BlockCatalog.processInput();
                    previousFrameHadCursor = true;
                }
            }

            // Render the world
            if (wireframe) glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            world.render();
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

            // Render the UI over the rest
            renderUI(world);

            if (accumulatedTime > 0.1) {
                accumulatedTime -= 0.1;
                world.gatherChunks();
                // Make sure chunks around the player are generated
                world.manageChunks();
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
        int y = 100; int fontSize = 15;
        // Keybinds
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 10, y, "ESC to quit, F to fly, 1/2 for selecting, E to open catalog, ENTER to open command line, type 'help' for commands");
        y += 15;

        // Spacer
        y += 15;
        // -- RENDERING
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 10, y, "RENDERING");
        y += 15;
        // FPS counter
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "FPS: " + String.format("%.0f", fps.getFrequency()));
        y += 15;
        // Render distance
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "Render distance: " + Renderer.RENDER_DISTANCE);
        y += 15;
        // Blocks rendered
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "Blocks rendered: " + Renderer.blocksRendered);
        y += 15;
        // Blocks rendered
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "Vertices rendered: " + Renderer.verticesRendered);
        y += 15;
        // # Rendered chunks
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "Chunks rendered: " + Renderer.numberRendered);
        y += 15;
        // # Loaded chunks
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "Chunks loaded: " + world.chunks.size());
        y += 15;
        // # Chunks currently generating
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        int nrChunks = (world.worldGenerator != null) ? world.worldGenerator.jobs.size() : 0;
        nvgText(vg, 20, y, "Chunks loading: " + ((world.worldGenerator != null) ? world.worldGenerator.jobs.size() : "-"));
        y += 15;
        // # lights rendered
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "Lights rendered: " + Renderer.lightsRendered);
        y += 15;

        // Spacer
        y += 15;
        // -- WORLD
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 10, y, "WORLD");
        y += 15;
        // Time
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "Time: " + String.format("%.0f (%.0f/s)",  world.time, world.timeRate));
        y += 15;
        // Sun position
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "Sun position: " + String.format("(X:%.1f  Y:%.1f  Z:%.1f)",  world.sun.getPosition().x, world.sun.getPosition().y, world.sun.getPosition().z));
        y += 15;
        // Sun direction
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "Sun direction: " + String.format("(X:%.1f  Y:%.1f  Z:%.1f) (%.2fpi)",  world.sun.getDirection().x, world.sun.getDirection().y, world.sun.getDirection().z, ((world.time/1200f)+1)%2));
        y += 15;
        // Darkness
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "Darkness: " + String.format("%.2f", world.sun.getTimeMultiplier(world.time)));
        y += 15;

        // Spacer
        y += 15;
        // -- CAMERA
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 10, y, "PLAYER (CAMERA)");
        y += 15;
        // Camera coordinates
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "Position: " + String.format("(X:%.1f  Y:%.1f  Z:%.1f)",  world.camera.position.x, world.camera.position.y, world.camera.position.z));
        y += 15;
        // Camera direction
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "Direction: " + String.format("(X:%.1f  Y:%.1f  Z:%.1f)",   world.camera.getDirection().x,  world.camera.getDirection().y,  world.camera.getDirection().z));
        y += 15;
        // Selected block type
        nvgBeginPath(vg);
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 20, y, "Currently holding block: " + ((Toolbar.getSelectedBlock() != null) ? Toolbar.getSelectedBlock().name() : "-"));
        y += 15;


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

        if (BlockCatalog.show) {
            BlockCatalog.draw();
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
            h.append("World Management:\n");
            h.append("import    Imports a saved world from file.\n");
            h.append("export    Exports current world to file\n");
            h.append("Graphics:\n");
            h.append("render wireframe <on/off>    Toggle wireframe rendering\n");
            h.append("render distance <distance>    Set render distance [0,-]\n");
            h.append("vsync <on/off>    Toggle vsync\n");
            h.append("Editing:\n");
            h.append("time <time>    Set time to <time> [0,2399]\n");
            h.append("time rate <rate>    Set time rate to <rate> [0,-]\n");
            h.append("select <1/2>     Set selection 1/2 to current position\n");
            h.append("copy     Copy contents of current selection\n");
            h.append("paste     Paste contents at selection 1\n");
            h.append("set <type>    Set selection to <type>\n");
            h.append("replace <old_type> <type>    Set selection <old_type> to <type>\n");
            h.append("remove    Remove selection blocks\n");
            h.append("sphere <r> <type> [hollow]   Spawn sphere of <type> with radius <r> at selection 1\n");
            h.append("line <type>    Trace line from selection 1 to selection 2\n");
            JOptionPane.showMessageDialog(new JDialog(), h.toString());
        } else if (command.equals("import")) {
            // Import a world from a file
            World newWorld = WorldManager.importWorld(this);
            if (newWorld != null) world = newWorld;
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
                    if (dist >= 0) {
                        Renderer.NEW_RENDER_DISTANCE = dist;
                    }
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
        } else if (command.startsWith("sphere")) {
            if (world.select1 != null) {
                try {
                    String[] split = command.split(" ");
                    int r = Integer.parseInt(split[1]);
                    BlockType type = BlockType.valueOf(split[2].toUpperCase());
                    world.setSphere(world.select1, r, type, command.contains("hollow"));
                } catch (IllegalArgumentException e) {};
            }
        } else if (command.startsWith("line")) {
            if (world.select1 != null & world.select2 != null) {
                try {
                    String[] split = command.split(" ");
                    BlockType type = BlockType.valueOf(split[1].toUpperCase());
                    world.setLine(world.select1, world.select2, type);
                } catch (IllegalArgumentException e) {};
            }
        } else if (command.equals("copy")) {
            if (world.select1 != null & world.select2 != null) {
                world.toClipboard(world.select1, world.select2);
            }
        } else if (command.equals("paste")) {
            if (world.select1 != null) {
                world.fromClipboard(world.select1);
            }
        } else if (command.equals("select 1")) {
            world.select1 = world.camera.position.floor(new Vector3f());
        } else if (command.equals("select 2")) {
            world.select2 = world.camera.position.floor(new Vector3f());
        }
        fps.dt();
    }

    public static String resourceToFileSystem(String resource) throws IOException {
        File temp = File.createTempFile("temp", ".temp");
        Path tempPath = temp.toPath();
        temp.delete();
        try (InputStream is = App.class.getResourceAsStream(resource)) {
            Files.copy(is, tempPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempPath.toString();
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
        App.instance = main;
        main.run();
    }

}
