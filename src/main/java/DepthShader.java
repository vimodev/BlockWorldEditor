/**
 * Depth shader used for shadow mapping
 */
public class DepthShader extends Shader {

    private static final String VERTEX_FILE = "depth_vertex.glsl";
    private static final String FRAGMENT_FILE = "depth_fragment.glsl";

    public DepthShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
    }

    @Override
    protected void bindAttributes() {
        super.bindAttribute(0, "position");
        super.bindAttribute(1, "textureCoords");
        super.bindAttribute(2, "normal");
    }
}