import org.joml.Vector3f;

public class Skybox {

    Mesh mesh;

    private final Vector3f position;

    private float scale;

    private final Vector3f rotation;

    public Skybox(String objModel, String textureFile) {
        scale = 500;
        rotation = new Vector3f();

        Mesh skyBoxMesh = ObjectLoader.loadMesh(objModel);
        Texture skyBoxtexture = new Texture(textureFile);
        skyBoxMesh.setTexture(skyBoxtexture);
        mesh = skyBoxMesh;
        position = new Vector3f(0, 0, 0);
    }

    public void setPosition(float x, float y, float z) {
        this.position.x = x;
        this.position.y = y;
        this.position.z = z;
    }

    public void setRotation(float x, float y, float z) {
        this.rotation.x = x;
        this.rotation.y = y;
        this.rotation.z = z;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getScale() {
        return scale;
    }
}