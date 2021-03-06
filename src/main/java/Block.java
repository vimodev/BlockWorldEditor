import org.joml.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

/**
 * All types of blocks
 */
enum BlockType {
//    NONE,
    GRASS(1),
    STONE(2),
    DIRT(3),
    PLANKS(4),
    BRICK(5),
    COBBLE(6),
    SAND(7),
    WOOL_WHITE(8),
    LOG(9),
    IRON(10),
    GOLD(11),
    SAND_STONE(12),
    MUD(13);

    // Blocktype id, is a byte for small serialization size
    private final byte id;
    // Map to get blocktype from id
    private static Map<Byte, BlockType> map = new HashMap<>();

    // Populate id->blocktype mapping
    static {
        for (BlockType type : BlockType.values()) {
            map.put(type.id, type);
        }
    }

    BlockType(int id) {
        this.id = (byte) id;
    }

    public byte id() {
        return this.id;
    }

    public static BlockType type(byte id) {
        return map.get(id);
    }


}

/**
 * Representation of a block in game
 */
public class Block {

    // Texture stuff
    static final String textureFile = "textures.png";
    static final int size = 256;
    static final int increment = 16;
    static int texture = -1;
    // Locations of block type textures in the sprite map
    static final Map<BlockType, Vector2i> textureLocation = new HashMap<>() {{
        put(BlockType.GRASS, new Vector2i(1, 1));
        put(BlockType.STONE, new Vector2i(1, 0));
        put(BlockType.DIRT, new Vector2i(2, 0));
        put(BlockType.PLANKS, new Vector2i(4, 0));
        put(BlockType.BRICK, new Vector2i(7, 0));
        put(BlockType.COBBLE, new Vector2i(0, 1));
        put(BlockType.SAND, new Vector2i(2, 1));
        put(BlockType.WOOL_WHITE, new Vector2i(0, 4));
        put(BlockType.LOG, new Vector2i(4, 1));
        put(BlockType.IRON, new Vector2i(6, 1));
        put(BlockType.GOLD, new Vector2i(7, 1));
        put(BlockType.SAND_STONE, new Vector2i(0, 11));
        put(BlockType.MUD, new Vector2i(8, 6));
    }};
    // A selected block has this texture coordinate
    static final Vector2i selectTextureLocation = new Vector2i(9, 1);

    // Type of block determines texture
    public BlockType type;

    // Chunk stuff
    // Which faces are rendering
    public byte faceField;
    // Block position inside chunk
    public short inChunkX;
    public short inChunkY;
    public short inChunkZ;
    // Parent chunk
    public Chunk chunk;

    /**
     * Create a block at position with type
     * @param type type of block
     */
    public Block(BlockType type) {
        this.faceField = (byte) 0;
        this.type = type;
        for (int f = 0; f < 6; f++) setFace(f, true);
    }

    public void setFace(int index, boolean value) {
        if (value) {
            faceField |= 1 << index;
        } else {
            faceField &= ~(1 << index);
        }
    }

    public boolean getFace(int index) {
        return ((faceField >> index) & 1) == 1;
    }

    public Vector3f getPosition() {
        Vector3i c = this.chunk.origin;
        return new Vector3f(c.x + inChunkX, c.y + inChunkY, c.z + inChunkZ);
    }

    // Vertex positions per face
    static float[][] faceVertices = new float[][]{
            {1, 1, 0,    1, 0, 0,    0, 0, 0, // -z face
                    0, 0, 0,    0, 1, 0,    1, 1, 0},
            {1, 1, 1,    1, 0, 1,    1, 0, 0, // +x face
                    1, 0, 0,    1, 1, 0,    1, 1, 1},
            {0, 1, 1,    0, 0, 1,    1, 0, 1, // +z face
                    1, 0, 1,    1, 1, 1,    0, 1, 1},
            {0, 1, 0,    0, 0, 0,    0, 0, 1, // -x face
                    0, 0, 1,    0, 1, 1,    0, 1, 0},
            {1, 1, 1,    1, 1, 0,    0, 1, 0, // Top face
                    0, 1, 0,    0, 1, 1,    1, 1, 1},
            {0, 0, 0,    1, 0, 0,    1, 0, 1, // Bottom face
                    1, 0, 1,    0, 0, 1,    0, 0, 0}
    };
    // Vertex normals per face
    static float[][] faceNormals = new float[][]{
            {0, 0, -1,   0, 0, -1,   0, 0, -1, // -z
                    0, 0, -1,   0, 0, -1,   0, 0, -1},
            {1, 0, 0,    1, 0, 0,    1, 0, 0, // +x
                    1, 0, 0,    1, 0, 0,    1, 0, 0},
            {0, 0, 1,    0, 0, 1,    0, 0, 1, // +z
                    0, 0, 1,    0, 0, 1,    0, 0, 1},
            {-1, 0, 0,   -1, 0, 0,   -1, 0, 0, // -x
                    -1, 0, 0,   -1, 0, 0,   -1, 0, 0},
            {0, 1, 0,    0, 1, 0,    0, 1, 0, // Top
                    0, 1, 0,    0, 1, 0,    0, 1, 0},
            {0, -1, 0,   0, -1, 0,   0, -1, 0, // Bottom
                    0, -1, 0,   0, -1, 0,   0, -1, 0}
    };

    /**
     * Load the 2^k*2^k texture png into gpu memory
     */
    public static void loadTexture() {
        // Intialize array of pixel properties
        int[] pixels = null;
        // Try to read image file
        try {
            // Load IO
            BufferedImage image = ImageIO.read(Block.class.getResource(textureFile));
            // Read RGB values
            pixels = new int[size * size];
            image.getRGB(0, 0, size, size, pixels, 0, size);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Extract all the rgb info seperated
        int[] data = new int[size * size];
        for (int i = 0; i < size * size; i++) {
            int a = (pixels[i] & 0xff000000) >> 24;
            int r = (pixels[i] & 0xff0000) >> 16;
            int g = (pixels[i] & 0xff00) >> 8;
            int b = (pixels[i] & 0xff);
            data[i] = a << 24 | b << 16 | g << 8 | r;
        }

        // Create a texture in opengl
        texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        // Load image data into a buffer
        IntBuffer buffer = ByteBuffer.allocateDirect(data.length << 2)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        buffer.put(data).flip();

        // Create the texture from the image data buffer
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size, size, 0, GL_RGBA,
                GL_UNSIGNED_BYTE, buffer);

        // Enable mipmapping to decrease resolution over distance
//        glGenerateMipmap(GL_TEXTURE_2D);
//        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
//        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

}
