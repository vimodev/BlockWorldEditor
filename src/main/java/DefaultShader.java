/**
 * Basic color gradient shader for testing purposesTestShader
 */
public class DefaultShader extends Shader {

    private static final String VERTEX_FILE = "default_vertex.glsl";
    private static final String FRAGMENT_FILE = "default_fragment.glsl";

    public DefaultShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
    }

    @Override
    protected void bindAttributes() {
        super.bindAttribute(0, "position");
    }
}