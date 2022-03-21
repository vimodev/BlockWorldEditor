import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.*;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
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

    public boolean modified;

    // List of all blocks in the chunk, for easy iteration
    List<Block> blockList;

    // List of all lights in the chunk
    public HashMap<Vector3f, Light> lightsMap = new HashMap<>();

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
        modified = true;
        Block block = blocks[x][z][y];
        if (block == null) return null;

        // Remove light for illuminating blocks (TODO: replace GOLD blocks with illuminating type?)
        if (block.type == BlockType.GOLD) {
            lightsMap.remove(new Vector3f(x, y, z));
        }

        blockList.remove(block);
        blocks[x][z][y] = null;
        if (x - 1 >= 0 && blocks[x-1][z][y] != null) {
            blocks[x-1][z][y].setFace(1, true);
        }
        if (x + 1 < Chunk.WIDTH && blocks[x+1][z][y] != null) {
            blocks[x + 1][z][y].setFace(3, true);
        }
        if (z - 1 >= 0 && blocks[x][z-1][y] != null) {
            blocks[x][z-1][y].setFace(2, true);
        }
        if (z + 1 < Chunk.WIDTH && blocks[x][z+1][y] != null) {
            blocks[x][z+1][y].setFace(0, true);
        }
        if (y - 1 >= 0 && blocks[x][z][y-1] != null) {
            blocks[x][z][y-1].setFace(4, true);
        }
        if (y + 1 < Chunk.HEIGHT && blocks[x][z][y+1] != null) {
            blocks[x][z][y+1].setFace(5, true);
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
        modified = true;
        blocks[x][z][y] = block;
        block.inChunkX = (short) x; block.inChunkY = (short) y; block.inChunkZ = (short) z;
        block.chunk = this;
        // Set face data, which face faces another block? we dont need to render that one!
        if (x - 1 >= 0 && blocks[x-1][z][y] != null) {
            block.setFace(3, false);
            blocks[x-1][z][y].setFace(1, false);
        }
        if (x + 1 < Chunk.WIDTH && blocks[x+1][z][y] != null) {
            block.setFace(1, false);
            blocks[x + 1][z][y].setFace(3, false);
        }
        if (z - 1 >= 0 && blocks[x][z-1][y] != null) {
            block.setFace(0, false);
            blocks[x][z-1][y].setFace(2, false);
        }
        if (z + 1 < Chunk.WIDTH && blocks[x][z+1][y] != null) {
            block.setFace(2, false);
            blocks[x][z+1][y].setFace(0, false);
        }
        if (y - 1 >= 0 && blocks[x][z][y-1] != null) {
            block.setFace(5, false);
            blocks[x][z][y-1].setFace(4, false);
        }
        if (y + 1 < Chunk.HEIGHT && blocks[x][z][y+1] != null) {
            block.setFace(4, false);
            blocks[x][z][y+1].setFace(5, false);
        }
        // We dont need the faces at the bottom of a chunk
        if (y == 0) {
            block.setFace(5, false);
        }
        // Add to list
        blockList.add(block);

        // Add light for illuminating blocks (TODO: replace GOLD blocks with illuminating type?)
        if (block.type == BlockType.GOLD) {
            lightsMap.put(
                    new Vector3f(x, y, z),
                    new Light(new Vector3f(x + origin.x + 0.5f, y + origin.y + 0.5f, z + origin.z + 0.5f)));
        }
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
        float inc = (float) Block.increment / (float) Block.size;
        HashMap<BlockType, Vector2f> texLoc = new HashMap<>();
        for (BlockType t : BlockType.values()) {
            texLoc.put(t, new Vector2f(inc * Block.textureLocation.get(t).x, inc * Block.textureLocation.get(t).y));
        }
        // Go over all blocks
        for (Block block : blockList) {
            // Calculate texture based on block type
            Vector2f leftTop = texLoc.get(block.type);
            if (block == world.select1Block || block == world.select2Block) {
                leftTop = new Vector2f(inc * Block.selectTextureLocation.x, inc * Block.selectTextureLocation.y);
            }
            // If the entire face field is 0, then no faces need be rendered
            if (block.faceField == (byte) 0) continue;
            float x = block.inChunkX; float y = block.inChunkY; float z = block.inChunkZ;
            // Go over all faces that need drawing
            for (int f = 0; f < 6; f++) {
                // Render only faces that ought to be rendered
                if (!block.getFace(f)) continue;
                // Add all the vertex positions, textureCoords and normals for each face's vertices
                for (int v = 0; v < 6; v++) {
                    positions.add(Block.faceVertices[f][v * 3] + x);
                    positions.add(Block.faceVertices[f][v * 3 + 1] + y);
                    positions.add(Block.faceVertices[f][v * 3 + 2] + z);
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
        positions = null;
        normals = null;
        textureCoords = null;
    }

    /**
     * Given all block data, create a single mesh for efficient rendering
     */
    public void regenerateMesh() {
        calculateMesh();
        loadCalculatedMesh();
    }

    public static File toFile(Chunk chunk) {
        try {
            File file = Files.createTempFile("bwe", ".chunk").toFile();
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            // Create array of blocktype ids, which has very small serialization size
            byte[][][] blockIds = new byte[Chunk.WIDTH][Chunk.WIDTH][Chunk.HEIGHT];
            for (int x = 0; x < Chunk.WIDTH; x++) {
                for (int z = 0; z < Chunk.WIDTH; z++) {
                    for (int y = 0; y < Chunk.HEIGHT; y++) {
                        blockIds[x][z][y] = (chunk.blocks[x][z][y] == null) ? ((byte) 0) : (chunk.blocks[x][z][y].type.id());
                    }
                }
            }
            oos.writeObject(blockIds);
            fos.close();
            oos.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Chunk fromFile(World world, Vector3i origin, File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Chunk chunk = new Chunk(world, origin.x, origin.y, origin.z);
            // Parse array of blocktype ids and fill chunk
            byte[][][] blockIds = (byte[][][]) ois.readObject();
            for (int x = 0; x < Chunk.WIDTH; x++) {
                for (int z = 0; z < Chunk.WIDTH; z++) {
                    for (int y = 0; y < Chunk.HEIGHT; y++) {
                        if (blockIds[x][z][y] != 0) {
                            chunk.setBlock(x, y, z, new Block(BlockType.type(blockIds[x][z][y])));
                        }
                    }
                }
            }
            fis.close();
            ois.close();
            return chunk;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}
