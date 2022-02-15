import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.opengl.GL;

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

        glfwSetInputMode(window.getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

        // Nano VG stuff
        glEnable(GL_STENCIL_TEST);
        vg = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vg == NULL) {
            throw new IllegalStateException("Failed to initialize NanoVG");
        }
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
                world.addBlock(new Block(offset, 50f, i * -2.0f, type));
            }
            offset += 2.0f;
        }
        for (int x = -50; x < 50; x++) {
            for (int z = -50; z < 50; z++) {
                for (int y = 0; y < 50; y++) {
                    world.addBlock(new Block(x, y, z, BlockType.STONE));
                }
            }
        }

        world.camera.position.y = 55f;

//        World world = WorldManager.importWorld(this);

        // After editing all the chunks, we generate their mesh
        for (Chunk c : world.chunks) c.regenerateMesh();

        // Main game loop
        fps.dt();
        double accumulatedTime = 0;
        while ( !glfwWindowShouldClose(window.getWindow()) ) {
            // Get time delta
            double dt = fps.dt();
            accumulatedTime += dt;

            // Toggle wireframe
            if (InputController.keyPressed(GLFW_KEY_F1)) {
                wireframe = !wireframe;
            }

            // Open and close command line
            if (InputController.keyPressed(GLFW_KEY_ENTER)) {
                if (CommandLine.show) executeCommand(CommandLine.content);
                CommandLine.show = !CommandLine.show;
                CommandLine.content = "";
            }

            nvgBeginFrame(vg, WINDOW_WIDTH, WINDOW_HEIGHT, contentScaleY);

            
            // Rotate sun for day/night cycle
            world.dirLight.position.rotateX((float) dt * 0.2f).normalize();
            world.dirLight.position.rotateZ((float) dt * 0.1f).normalize();
            
            // Apply input to the world or command line
            if (!CommandLine.show) world.applyInput(this, dt);
            else CommandLine.processInput();

            // Render the world
            if (wireframe) glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
            world.render();
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);

            renderUI(world);

            if (accumulatedTime > 1) {
                accumulatedTime -= 1;
            }
            // Frame is ready
            nvgEndFrame(vg);
            glfwSwapBuffers(window.getWindow());
            glfwPollEvents();
        }

    }

    public void renderUI(World world) {
        glDisable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);
        // Keybinds
        nvgBeginPath(vg);
        nvgFontSize(vg, 15);
        nvgFontFace(vg, "sans");
        nvgFillColor(vg, nvgRGBAf(1, 1, 1, 0.5f, NVGColor.create()));
        nvgText(vg, 10, 20, "ESC to quit, F to fly, F1 to toggle wireframe, ENTER to open command line");
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

    public void executeCommand(String command) {
        command = command.trim().toLowerCase();
        if (command.length() == 0) return;
        System.out.println("Running command: " + command);
        if (command.equals("import")) {
            world = WorldManager.importWorld(this);
        } else if (command.equals("export")) {
            WorldManager.exportWorld(world);
        }
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
