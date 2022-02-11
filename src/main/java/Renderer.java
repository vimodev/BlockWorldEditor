import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.*;

import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class Renderer {

    public static Shader defaultShader = new DefaultShader();

    public static void render(World world) {
        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_LEQUAL);
        // Enable backface culling
        glEnable(GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
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

        // We will draw blocks, so enable the block vao
        GL30.glBindVertexArray(Block.vao);
        GL20.glEnableVertexAttribArray(0);

        // For every block in the world, set the transformation matrix and draw
        for (Block block : world.blocks) {
            shader.setUniform("transformationMatrix", block.getTransformationMatrix());
            GL11.glDrawArrays(GL_TRIANGLES, 0, Block.vertexCount);
        }

        // Unbind everything for safety
        GL30.glBindVertexArray(0);
        GL20.glDisableVertexAttribArray(0);
        shader.unuse();
    }

}
