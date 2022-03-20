import org.joml.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.lang.Math;

import static org.lwjgl.opengl.GL11.*;

import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

public class Renderer {

    public static Shader defaultShader = new DefaultShader();

    public static float RENDER_DISTANCE = 200f;
    public static int numberRendered = 0;
    public static float LIGHT_RENDER_DISTANCE = 50f;
    public static int lightsRendered = 0;

    public static void render(World world) {
        // Enable antialiasing
        glDisable(GL_MULTISAMPLE);
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
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

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

        // Get all point light from world (only from chunks to be rendered)
        Light.clearShaderOfPointLights(shader, 1000);
        lightsRendered = 0;
        for (Chunk c : world.chunks) {
            for (Light light : c.lightsMap.values()) {
                if (!shouldLightRender(light, world.camera)) continue;
                light.addToShaderAsPointLight(shader, lightsRendered);
                lightsRendered++;
            }
        }

        // Render each chunk's mesh
        numberRendered = 0;
        for (Chunk c : world.chunks) {
            // Check if we should render the chunk
            if (!shouldChunkRender(c, world.camera)) continue;
            // Otherwise we render the chunk
            numberRendered++;
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

    private static boolean shouldChunkRender(Chunk chunk, Camera camera) {
        // Outside render distance should not render
        float hDistance = new Vector2f(camera.position.x, camera.position.z).distance(
                new Vector2f(chunk.origin.x + Chunk.WIDTH / 2, chunk.origin.z + Chunk.WIDTH / 2));
        if (hDistance > RENDER_DISTANCE) return false;
        if (hDistance < Chunk.WIDTH) return true;
        // Empty chunk should not render
        if (chunk.blockList.isEmpty()) return false;
        // Frustum culling
        Matrix4f cameraMatrix = camera.getProjection().mul(camera.getTransformation(), new Matrix4f());
        for (int x = 0; x <= Chunk.WIDTH; x += Chunk.WIDTH / 4) {
            for (int z = 0; z <= Chunk.WIDTH; z += Chunk.WIDTH / 4) {
                for (int y = 0; y <= Chunk.HEIGHT; y += Chunk.HEIGHT / 8) {
                    Vector4f pos = new Vector4f(
                            chunk.origin.x + x,
                            chunk.origin.y + y,
                            chunk.origin.z + z,
                            1
                    );
                    Vector4f result = cameraMatrix.transform(pos, new Vector4f());
                    float sx = result.x / result.w;
                    float sy = result.y / result.w;
                    if (Math.abs(sx) <= 1.1f && Math.abs(sy) <= 1.1f) return true;
                }
            }
        }
        return false;
    }

    private static boolean shouldLightRender(Light light, Camera camera) {
        // Outside light render distance should not render
        float hDistance = camera.position.distance(light.position);
        if (hDistance > LIGHT_RENDER_DISTANCE) return false;
        return true;
    }

}
