import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;

/**
 * Representation of a block in game
 */
public class Block {

    // Pointer to the block mesh in gpu memory, needed for rendering
    static int vao = -1;
    static int vertexCount = -1;

    Vector3f position;

    public Block(float x, float y, float z) {
        this.position = new Vector3f(x, y, z);
    }

    public Matrix4f getTransformationMatrix() {
        Matrix4f M = new Matrix4f();
        M.identity();
        M.translate(this.position);
        return M;
    }

    /**
     * Load cube model into GPU memory
     */
    public static void loadVAO() {
        float[] vertices = new float[] {
                0.0f,0.0f,0.0f,
                0.0f,0.0f, 1.0f,
                0.0f, 1.0f, 1.0f,
                1.0f, 1.0f,0.0f,
                0.0f,0.0f,0.0f,
                0.0f, 1.0f,0.0f,
                1.0f,0.0f, 1.0f,
                0.0f,0.0f,0.0f,
                1.0f,0.0f,0.0f,
                1.0f, 1.0f,0.0f,
                1.0f,0.0f,0.0f,
                0.0f,0.0f,0.0f,
                0.0f,0.0f,0.0f,
                0.0f, 1.0f, 1.0f,
                0.0f, 1.0f,0.0f,
                1.0f,0.0f, 1.0f,
                0.0f,0.0f, 1.0f,
                0.0f,0.0f,0.0f,
                0.0f, 1.0f, 1.0f,
                0.0f,0.0f, 1.0f,
                1.0f,0.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f,0.0f,0.0f,
                1.0f, 1.0f,0.0f,
                1.0f,0.0f,0.0f,
                1.0f, 1.0f, 1.0f,
                1.0f,0.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                1.0f, 1.0f,0.0f,
                0.0f, 1.0f,0.0f,
                1.0f, 1.0f, 1.0f,
                0.0f, 1.0f,0.0f,
                0.0f, 1.0f, 1.0f,
                1.0f, 1.0f, 1.0f,
                0.0f, 1.0f, 1.0f,
                1.0f,0.0f, 1.0f
        };
        vertexCount = vertices.length;

        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices);
        buffer.flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

}
