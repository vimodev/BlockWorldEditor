import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glClearColor;

/**
 * Main runnable application class
 */
public class App {

    private Window window;
    private int WINDOW_WIDTH = 1920;
    private int WINDOW_HEIGHT = 1080;
    private String WINDOW_TITLE = "BlockWorldEditor";

    private Timer fps;

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

        // Intitialize fps timer
        fps = new Timer();
    }

    /**
     * Contains the main game loop
     */
    public void loop() {

        double accumulatedTime = 0;
        while ( !glfwWindowShouldClose(window.getWindow()) ) {
            double dt = fps.dt();
            accumulatedTime += dt;

            if (accumulatedTime > 1) {
                accumulatedTime -= 1;
                window.setTitle(WINDOW_TITLE + " " + String.format("%.2f", fps.getFrequency()) + " fps");
            }
            glfwSwapBuffers(window.getWindow());
            glfwPollEvents();
        }

    }

    /**
     * Terminate the app, doing a proper clean up
     */
    public void terminate() {
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
