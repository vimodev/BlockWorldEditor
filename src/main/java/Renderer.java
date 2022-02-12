import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.*;

import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class Renderer {

    public static Shader defaultShader = new DefaultShader();

    public static float RENDER_DISTANCE = 100f;

    public static void render(World world) {
        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_LEQUAL);
//        glFrontFace(GL_CW);
        // Enable backface culling
        glEnable(GL_CULL_FACE);
//        GL11.glEnable(GL11.GL_BLEND);
        glCullFace(GL_BACK);
        // Set clear color to sky color and clear
        GL11.glClearColor(world.skyColor.x, world.skyColor.y, world.skyColor.z, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Use default shader
        Shader shader = defaultShader;
        shader.use();

        // Set camera matrices
        shader.setUniform("projectionMatrix", world.camera.getProjection());
        shader.setUniform("viewMatrix", world.camera.getTransformation());

        // Set view/camera position
        shader.setUniform("viewPosition", world.camera.position);

        // Enable the block texture
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL_TEXTURE_2D, Block.texture);

        // Add directional light from world
        world.dirLight.addToShaderAsDirLight(shader);

        // Add point lights from world
        for (int i = 0; i < world.pointLights.size(); i++) {
            world.pointLights.get(i).addToShaderAsPointLight(shader, i);
        }

        // Render each chunk's mesh
        for (Chunk c : world.chunks) {
            if (new Vector2f(world.camera.position.x, world.camera.position.z).distance(new Vector2f(c.origin.x, c.origin.z)) > RENDER_DISTANCE) continue;
            shader.setUniform("transformationMatrix", c.getTransformationMatrix());
            GL30.glBindVertexArray(c.mesh);
            GL20.glEnableVertexAttribArray(0); // Vertices
            GL20.glEnableVertexAttribArray(1); // Texture coords
            GL20.glEnableVertexAttribArray(2); // Normals
            GL11.glDrawArrays(GL_TRIANGLES, 0, c.vertexCount);
        }

        // Unbind everything for safety
        GL30.glBindVertexArray(0);
        GL20.glDisableVertexAttribArray(0);
        shader.unuse();
    }

}
