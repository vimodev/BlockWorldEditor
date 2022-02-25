import static org.lwjgl.glfw.GLFW.*;

/**
 * Wrapper for glfw window
 */
public class Window {

    // Holds reference to glfw window
    private long window;

    // Dimensions and window title
    private int width;
    private int height;
    private String title;

    /**
     * Creates a window with given properties
     * @param width of the window
     * @param height of the window
     * @param title of the window
     */
    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;
        this.title = title;

        // Create the window in glfw
        window = glfwCreateWindow(width, height, title, 0, 0);

        // Error detection
        if (window == 0) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public long getWindow() {
        return window;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        glfwSetWindowTitle(window, title);
    }
}