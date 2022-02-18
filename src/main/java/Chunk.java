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

    // List of all blocks in the chunk, for easy iteration
    List<Block> blockList;

    // VAO that holds current mesh
    public boolean meshReady;
    public int mesh;
    public int vertexCount;
    private List<Integer> vbos;

    // Hold mesh data during the time between calculating and loading to gpu
    private List<Float> positions;
    private List<Float> textureCoords;
    private List<Float> normals;

    public Chunk(World world, int x, int y, int z) {
        this.world = world;
        this.origin = new Vector3i(x, y, z);
        this.blocks = new Block[WIDTH][WIDTH][HEIGHT];
        this.vbos = new ArrayList<>();
        this.blockList = new ArrayList<>();
        this.meshReady = false;
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
        blockList.remove(block);
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
        // Add to list
        blockList.add(block);
    }

    /**
     * Unload any mesh belonging to this chunk from the gpu
     */
    public void unloadMesh() {
        // Delete previous mesh
        GL30.glDeleteVertexArrays(mesh);
        for (int vbo : vbos) {
            GL30.glDeleteBuffers(vbo);
        }
        vbos.clear();
        meshReady = false;
    }

    /**
     * Calculate all the mesh data, do not load into GPU yet,
     * must be done by main thread
     */
    public void calculateMesh() {
        positions = new ArrayList<>();
        textureCoords = new ArrayList<>();
        normals = new ArrayList<>();
        // Go over all blocks
        for (Block block : blockList) {
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
        }
        vertexCount = positions.size() / 3;
    }

    /**
     * Load the calculated mesh into gpu memory
     * DANGER!!!! MESH DATA MUST BE CALCULATED
     */
    public void loadCalculatedMesh() {
        unloadMesh();
        mesh = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(mesh);
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
        meshReady = true;
        positions.clear();
        normals.clear();
        textureCoords.clear();
    }

    /**
     * Given all block data, create a single mesh for efficient rendering
     */
    public void regenerateMesh() {
        calculateMesh();
        loadCalculatedMesh();
    }

}
