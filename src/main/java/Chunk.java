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

/**
 * Block container with a single mesh
 */
public class Chunk {

    // Chunk dimensions WIDTH x WIDTH x HEIGHT
    public static final int WIDTH = 32;
    public static final int HEIGHT = 256;

    // Chunk parent world
    public World world;

    // 3d grid of blocks: x z y
    public Block[][][] blocks;
    // Origin of the chunk in the world
    public Vector3i origin;

    // First block in the chunk
    public Block first;
    // Last block
    public Block last;
    // We use this chain of pointers for efficient iteration over all blocks

    // VAO that holds current mesh
    public int mesh;
    public int vertexCount;
    private List<Integer> vbos;

    public Chunk(World world, int x, int y, int z) {
        this.world = world;
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

    public Vector3i getLocalPosition(Vector3f loc) {
        int x = (int) Math.floor(loc.x) % Chunk.WIDTH;
        int y = (int) Math.floor(loc.y) % Chunk.HEIGHT;
        int z = (int) Math.floor(loc.z) % Chunk.WIDTH;
        if (x < 0) x += Chunk.WIDTH;
        if (z < 0) z += Chunk.WIDTH;
        return new Vector3i(x, y, z);
    }

    public Block removeBlock(int x, int y, int z) {
        Block block = blocks[x][z][y];
        if (block == null) return null;
        if (block == first && block == last) {
            first = null; last = null;
        } else if (block == first) {
            first = block.next;
            block.next.previous = null;
        } else if (block == last) {
            last = block.previous;
            block.previous.next = null;
        } else {
            block.next.previous = block.previous;
            block.previous.next = block.next;
        }
        blocks[x][z][y] = null;
        if (x - 1 >= 0 && blocks[x-1][z][y] != null) {
            blocks[x-1][z][y].faces[1] = true;
        }
        if (x + 1 < Chunk.WIDTH && blocks[x+1][z][y] != null) {
            blocks[x + 1][z][y].faces[3] = true;
        }
        if (z - 1 >= 0 && blocks[x][z-1][y] != null) {
            blocks[x][z-1][y].faces[2] = true;
        }
        if (z + 1 < Chunk.WIDTH && blocks[x][z+1][y] != null) {
            blocks[x][z+1][y].faces[0] = true;
        }
        if (y - 1 >= 0 && blocks[x][z][y-1] != null) {
            blocks[x][z][y-1].faces[4] = true;
        }
        if (y + 1 < Chunk.HEIGHT && blocks[x][z][y+1] != null) {
            blocks[x][z][y+1].faces[5] = true;
        }
        return block;
    }

    /**
     * Set the block at local coords x y z to Block
     * @param x
     * @param y
     * @param z
     * @param block
     */
    public void setBlock(int x, int y, int z, Block block) {
        blocks[x][z][y] = block;
        block.positionInChunk = new Vector3i(x, y, z);
        block.chunk = this;
        // Set face data, which face faces another block? we dont need to render that one!
        if (x - 1 >= 0 && blocks[x-1][z][y] != null) {
            block.faces[3] = false;
            blocks[x-1][z][y].faces[1] = false;
        }
        if (x + 1 < Chunk.WIDTH && blocks[x+1][z][y] != null) {
            block.faces[1] = false;
            blocks[x + 1][z][y].faces[3] = false;
        }
        if (z - 1 >= 0 && blocks[x][z-1][y] != null) {
            block.faces[0] = false;
            blocks[x][z-1][y].faces[2] = false;
        }
        if (z + 1 < Chunk.WIDTH && blocks[x][z+1][y] != null) {
            block.faces[2] = false;
            blocks[x][z+1][y].faces[0] = false;
        }
        if (y - 1 >= 0 && blocks[x][z][y-1] != null) {
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

    /**
     * Given all block data, create a single mesh for efficient rendering
     */
    public void regenerateMesh() {
        // Delete previous mesh
        GL30.glDeleteVertexArrays(mesh);
        for (int vbo : vbos) {
            GL30.glDeleteBuffers(vbo);
        }
        vbos.clear();
        // Create a new one
        mesh = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(mesh);
        List<Float> positions = new ArrayList<>();
        List<Float> textureCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        // Go over all blocks
        Block block = first;
        while (block != null) {
            // Calculate texture based on block type
            float inc = (float) Block.increment / (float) Block.size;
            Vector2f leftTop = new Vector2f(inc * Block.textureLocation.get(block.type).x, inc * Block.textureLocation.get(block.type).y);
            if (block == world.select1Block || block == world.select2Block) {
                leftTop = new Vector2f(inc * Block.selectTextureLocation.x, inc * Block.selectTextureLocation.y);
            }
            // Go over all faces that need drawing
            for (int f = 0; f < 6; f++) {
                if (!block.faces[f]) continue;
                // Add all the vertex positions, textureCoords and normals for each face's vertices
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
        // And proceed to load all that data into gpu memory
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
