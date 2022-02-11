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
        // Enable backface culling
//        glEnable(GL_CULL_FACE);
//        GL11.glEnable(GL11.GL_BLEND);
//        glCullFace(GL_BACK);
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

        // Enable the block texture
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL_TEXTURE_2D, Block.texture);

        // For every block in the world, set its properties and draw
        for (BlockType type : BlockType.values()) {
            // Bind properties
            GL30.glBindVertexArray(Block.vaos.get(type));
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            // Draw every block of this type
            for (Block block : world.blocks.get(type)) {
                // If in render distance
                if (world.camera.position.distance(block.position) > RENDER_DISTANCE) continue;
                shader.setUniform("transformationMatrix", block.getTransformationMatrix());
                GL11.glDrawArrays(GL_TRIANGLES, 0, Block.vertexCount);
            }
        }

        // Unbind everything for safety
        GL30.glBindVertexArray(0);
        GL20.glDisableVertexAttribArray(0);
        shader.unuse();
    }

}
