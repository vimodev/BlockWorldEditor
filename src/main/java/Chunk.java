import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class Chunk {

    public static final int WIDTH = 32;
    public static final int HEIGHT = 256;

    public Block[][][] blocks;
    public Vector3i origin;

    public Block first;
    public Block last;

    public int mesh;
    public int vertexCount;
    private List<Integer> vbos;

    public Chunk(int x, int y, int z) {
        this.origin = new Vector3i(x, y, z);
        this.blocks = new Block[WIDTH][WIDTH][HEIGHT];
        this.vbos = new ArrayList<>();
    }

    public Matrix4f getTransformationMatrix() {
        Matrix4f M = new Matrix4f();
        M.identity();
        M.translate(origin.x, origin.y, origin.z);
        return M;
    }

    public void setBlock(int x, int y, int z, Block block) {
        blocks[x][z][y] = block;
        block.positionInChunk = new Vector3i(x, y, z);
        // Set face data
        if (x - 1 > 0 && blocks[x-1][z][y] != null) {
            block.faces[3] = false;
            blocks[x-1][z][y].faces[1] = false;
        }
        if (x + 1 < Chunk.WIDTH && blocks[x+1][z][y] != null) {
            block.faces[1] = false;
            blocks[x + 1][z][y].faces[3] = false;
        }
        if (z - 1 > 0 && blocks[x][z-1][y] != null) {
            block.faces[2] = false;
            blocks[x][z-1][y].faces[0] = false;
        }
        if (z + 1 < Chunk.WIDTH && blocks[x][z+1][y] != null) {
            block.faces[0] = false;
            blocks[x][z+1][y].faces[2] = false;
        }
        if (y - 1 > 0 && blocks[x][z][y-1] != null) {
            block.faces[5] = false;
            blocks[x][z][y-1].faces[4] = false;
        }
        if (y + 1 < Chunk.HEIGHT && blocks[x][z][y+1] != null) {
            block.faces[4] = false;
            blocks[x][z][y+1].faces[5] = false;
        }
        // Update rendering chain
        if (first == null) {
            first = block; last = block;
        } else {
            last.next = block;
            block.previous = last;
            last = block;
        }
    }

    public void regenerateMesh() {
        GL30.glDeleteVertexArrays(mesh);
        for (int vbo : vbos) {
            GL30.glDeleteBuffers(vbo);
        }
        vbos.clear();
        mesh = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(mesh);
        List<Float> positions = new ArrayList<>();
        List<Float> textureCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        Block block = first;
        while (block != null) {
            float inc = (float) Block.increment / (float) Block.size;
            Vector2f leftTop = new Vector2f(inc * Block.textureLocation.get(block.type).x, inc * Block.textureLocation.get(block.type).y);
            for (int f = 0; f < 6; f++) {
                if (!block.faces[f]) continue;
                for (int v = 0; v < 6; v++) {
                    positions.add(Block.faceVertices[f][v * 3] + block.positionInChunk.x);
                    positions.add(Block.faceVertices[f][v * 3 + 1] + block.positionInChunk.y);
                    positions.add(Block.faceVertices[f][v * 3 + 2] + block.positionInChunk.z);
                    if (v == 2 || v == 3 || v == 4) textureCoords.add(leftTop.x);
                    else textureCoords.add(leftTop.x + inc);
                    if (v == 1 || v == 2 || v == 3) textureCoords.add(leftTop.y + inc);
                    else textureCoords.add(leftTop.y);
                    normals.add(Block.faceNormals[f][v * 3]);
                    normals.add(Block.faceNormals[f][v * 3 + 1]);
                    normals.add(Block.faceNormals[f][v * 3 + 2]);
                }
            }
            block = block.next;
        }
        vertexCount = positions.size() / 3;
        // Positions
        int vbo = GL15.glGenBuffers();
        vbos.add(vbo);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(positions.size());
        float[] positionsArray = new float[positions.size()];
        for (int i = 0; i < positions.size(); i++) positionsArray[i] = positions.get(i);
        buffer.put(positionsArray);
        buffer.flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        // Texture coords
        vbo = GL15.glGenBuffers();
        vbos.add(vbo);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        buffer = BufferUtils.createFloatBuffer(textureCoords.size());
        float[] textureArray = new float[textureCoords.size()];
        for (int i = 0; i < textureCoords.size(); i++) textureArray[i] = textureCoords.get(i);
        buffer.put(textureArray);
        buffer.flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 0, 0);
        // Normals
        vbo = GL15.glGenBuffers();
        vbos.add(vbo);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        buffer = BufferUtils.createFloatBuffer(normals.size());
        float[] normalsArray = new float[normals.size()];
        for (int i = 0; i < normals.size(); i++) normalsArray[i] = normals.get(i);
        buffer.put(normalsArray);
        buffer.flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, 0, 0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

}
