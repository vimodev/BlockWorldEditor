/**
 * Depth shader used for shadow mapping
 */
public class SkyboxShader extends Shader {

    private static final String VERTEX_FILE = "skybox_vertex.glsl";
    private static final String FRAGMENT_FILE = "skybox_fragment.glsl";

    public SkyboxShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
    }

    @Override
    protected void bindAttributes() {
        super.bindAttribute(0, "position");
        super.bindAttribute(1, "textureCoords");
        super.bindAttribute(2, "normal");
    }
}