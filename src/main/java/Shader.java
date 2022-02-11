import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

/**
 * Abstract class to load and manage shaders
 */
public abstract class Shader {

    private int shaderProgram;
    private int vertexShaderID;
    private int fragmentShaderID;

    /**
     * Creates a shader without geometry shader
     * @param vertexShaderFile
     * @param fragmentShaderFile
     */
    public Shader(String vertexShaderFile, String fragmentShaderFile) {
        // Create the shaders in opengl
        shaderProgram = glCreateProgram();
        vertexShaderID = glCreateShader(GL_VERTEX_SHADER);
        fragmentShaderID = glCreateShader(GL_FRAGMENT_SHADER);

        // Load and compile the shaders
        glShaderSource(vertexShaderID, readShaderFile(vertexShaderFile));
        glShaderSource(fragmentShaderID, readShaderFile(fragmentShaderFile));
        glCompileShader(vertexShaderID);
        glCompileShader(fragmentShaderID);

        // Check for errors
        if (glGetShaderi(vertexShaderID, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println(glGetShaderInfoLog(vertexShaderID));
        }
        if (glGetShaderi(fragmentShaderID, GL_COMPILE_STATUS) == GL_FALSE) {
            System.err.println(glGetShaderInfoLog(fragmentShaderID));
        }

        // Attach the shaders to the program
        glAttachShader(shaderProgram, vertexShaderID);
        glAttachShader(shaderProgram, fragmentShaderID);
        bindAttributes();
        glLinkProgram(shaderProgram);
        glValidateProgram(shaderProgram);
    }

    public void use() {
        GL20.glUseProgram(shaderProgram);
    }

    public void unuse() {
        GL20.glUseProgram(0);
    }

    /**
     * Various uniform setters
     * to set uniforms in shader of different types
     */
    public void setUniform(String uniform, float value) {
        glUniform1f(getUniformLocation(uniform), value);
    }

    public void setUniform(String uniform, int value) {
        glUniform1i(getUniformLocation(uniform), value);
    }

    public void setUniform(String uniform, Vector3f value) {
        glUniform3f(getUniformLocation(uniform), value.x, value.y, value.z);
    }

    public void setUniform(String uniform, Vector2f value) {
        glUniform2f(getUniformLocation(uniform), value.x, value.y);
    }

    public void setUniform(String uniform, boolean value) {
        glUniform1f(getUniformLocation(uniform), value ? 1 : 0);
    }

    public void setUniform(String uniform, Matrix4f value) {
        FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
        value.get(matrixBuffer);
        glUniformMatrix4fv(getUniformLocation(uniform), false, matrixBuffer);
    }

    /**
     * Given the name of a uniform get the location in the shader program
     * @param uniformName of the uniform
     * @return the location
     */
    public int getUniformLocation(String uniformName) {
        return glGetUniformLocation(shaderProgram, uniformName);
    }

    /**
     * Clean up this shader
     */
    public void cleanUp() {
        unuse();
        GL20.glDetachShader(shaderProgram, vertexShaderID);
        GL20.glDetachShader(shaderProgram, fragmentShaderID);
        GL20.glDeleteShader(vertexShaderID);
        GL20.glDeleteShader(fragmentShaderID);
        GL20.glDeleteProgram(shaderProgram);
    }

    protected abstract void bindAttributes();

    /**
     * Bind the vbo at the given attribute number to a variable in glsl
     * @param attribute
     * @param variableName
     */
    protected void bindAttribute(int attribute, String variableName) {
        GL20.glBindAttribLocation(shaderProgram, attribute, variableName);
    }

    /**
     * Read a shader file into a OpenGL shader
     * @param filename of shader file
     * @return String contents of shader
     */
    String readShaderFile(String filename) {
        // We use a string builder to easily compose a string
        StringBuilder sb = new StringBuilder();
        // We try to read the file
        try {
            // Read the file
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            getClass().getResource("/shaders/" + filename).openStream()));
            // Read the first line
            String line = reader.readLine();
            // If it is not empty we start looping
            while (line != null) {
                sb.append(line); sb.append("\n");
                line = reader.readLine();
            }
        } catch (IOException e) {
            System.err.println("Could not read shader file.");
            e.printStackTrace();
        }
        // Convert the string builder instance to a string
        return sb.toString();
    }

}