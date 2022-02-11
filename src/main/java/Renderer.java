import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11.*;

import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;

public class Renderer {

    public static Shader defaultShader = new DefaultShader();

    public static void render(World world) {
//        glEnable(GL_DEPTH_TEST);
//        glEnable(GL_LEQUAL);
//        glEnable(GL_CULL_FACE);
//        GL11.glEnable(GL11.GL_BLEND);
//        glCullFace(GL_BACK);
        GL11.glClearColor(world.skyColor.x, world.skyColor.y, world.skyColor.z, 1);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Shader shader = defaultShader;
        shader.use();

        shader.setUniform("projectionMatrix", world.camera.getProjection());
        shader.setUniform("viewMatrix", world.camera.getTransformation());

        GL30.glBindVertexArray(Block.vao);
        GL20.glEnableVertexAttribArray(0);

        for (Block block : world.blocks) {
            shader.setUniform("transformationMatrix", block.getTransformationMatrix());
            GL11.glDrawArrays(GL_TRIANGLES, 0, Block.vertexCount);
        }

        GL30.glBindVertexArray(0);
        GL20.glDisableVertexAttribArray(0);
        shader.unuse();
    }

}
