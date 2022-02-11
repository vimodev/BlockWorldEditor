import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class World {

    private App app;

    public Camera camera;
    public List<Block> blocks;
    public Vector3f skyColor;

    public World(App app) {
        this.app = app;
        camera = new Camera();
        blocks = new ArrayList<>();
        skyColor = new Vector3f(0, 0.6f, 1f);
    }

    public void applyInput(double dt) {
        camera.freeMove(app, dt);
//        camera.position.z -= 1.0 * dt;
//        blocks.get(0).position.z -= 0.1 * dt;
    }

    public void render() {
        Renderer.render(this);
    }

    public void fromFile(String filename) {

    }

    public void toFile(String filename) {

    }

}
