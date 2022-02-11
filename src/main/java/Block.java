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

    /**
     * Make transformation matrix from the Block's attributes
     * @return
     */
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

        // Create the vertex attribute object
        vao = GL30.glGenVertexArrays();
        // Bind it, so that we perform edits on this vao
        GL30.glBindVertexArray(vao);
        // Make a vertex buffer object, attached to the above vao
        int vbo = GL15.glGenBuffers();
        // Bind it, so that we perform edits on this vbo
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        // Put the vertex data into a read-ready buffer
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices);
        buffer.flip();
        // Pipe the vertex data into the bound vbo
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        // This vbo will be at index 0 of the earlier vao
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        // Unbind everything, for safety
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

}
