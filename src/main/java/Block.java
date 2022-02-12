import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

/**
 * All types of blocks
 */
enum BlockType {
    NONE,
    STONE,
    DIRT,
    PLANKS,
    BRICK,
    COBBLE,
}

/**
 * Representation of a block in game
 */
public class Block {

    static Map<BlockType, Integer[]> vao;

    // Texture stuff
    static final String textureFile = "textures.png";
    static final int size = 256;
    static final int increment = 16;
    static int texture = -1;
    // Locations of block type textures in the sprite map
    static final Map<BlockType, Vector2i> textureLocation = Map.of(
            BlockType.NONE, new Vector2i(9, 1),
            BlockType.STONE, new Vector2i(1, 0),
            BlockType.DIRT, new Vector2i(2, 0),
            BlockType.PLANKS, new Vector2i(4, 0),
            BlockType.BRICK, new Vector2i(7, 0),
            BlockType.COBBLE, new Vector2i(0, 1)
    );

    public Vector3f position;
    public BlockType type;

    // Chunk stuff
    public boolean[] faces;
    public Block previous;
    public Block next;
    public Vector3i positionInChunk;

    public Block(float x, float y, float z, BlockType type) {
        this.position = new Vector3f(x, y, z);
        this.type = type;
        this.faces = new boolean[6];
        for (int f = 0; f < 6; f++) this.faces[f] = true;
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
     * Load cube model into GPU memory
     */
    public static void loadVAO() {
        vao = new HashMap<>();
        for (BlockType type : BlockType.values()) {
            vao.put(type, new Integer[6]);
            // Loop over each face
            for (int f = 0; f < 6; f++) {
                // Vertex positions
                vao.get(type)[f] = glGenVertexArrays();
                GL30.glBindVertexArray(vao.get(type)[f]);
                int vbo = GL15.glGenBuffers();
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
                FloatBuffer buffer = BufferUtils.createFloatBuffer(6 * 3);
                buffer.put(faceVertices[f]);
                buffer.flip();
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
                GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

                // Texture coordinates
                float inc = (float) increment / (float) size;
                Vector2f leftTop = new Vector2f(inc * textureLocation.get(type).x, inc * textureLocation.get(type).y);
                float[] textureCoords = new float[6 * 2];
                Vector2f coords = new Vector2f(0);
                for (int o = 0; o < 6; o++) {
                    if (o == 2 || o == 3 || o == 4) {
                        coords.x = leftTop.x;
                    } else {
                        coords.x = leftTop.x + inc;
                    }
                    if (o == 1 || o == 2 || o == 3) {
                        coords.y = leftTop.y + inc;
                    } else {
                        coords.y = leftTop.y;
                    }
                    textureCoords[2 * o] = coords.x;
                    textureCoords[2 * o + 1] = coords.y;
                }
                vbo = GL15.glGenBuffers();
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
                buffer = BufferUtils.createFloatBuffer(6 * 2);
                buffer.put(textureCoords);
                buffer.flip();
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
                GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 0, 0);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

                // Normals
                vbo = GL15.glGenBuffers();
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
                buffer = BufferUtils.createFloatBuffer(6 * 3);
                buffer.put(faceNormals[f]);
                buffer.flip();
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
                GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, 0, 0);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
                GL30.glBindVertexArray(0);
            }
        }
    }

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
        glGenerateMipmap(GL_TEXTURE_2D);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, 0);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

}
