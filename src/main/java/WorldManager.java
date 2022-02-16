import org.joml.Vector3f;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages imports / exports of worlds
 */
public class WorldManager {

    /**
     * Let user pick a file and import world
     * @param app
     * @return
     */
    static World importWorld(App app) {
        File file = promptFileLocation();
        String result = "";
        try {
            result = Files.readString(file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (result == "") return null;
        JSONObject worldJSON = new JSONObject(result);
        World world = new World(app);
        // Set camera state
        world.camera.position.x = worldJSON.getJSONObject("camera").getJSONObject("position").getFloat("x");
        world.camera.position.y = worldJSON.getJSONObject("camera").getJSONObject("position").getFloat("y");
        world.camera.position.z = worldJSON.getJSONObject("camera").getJSONObject("position").getFloat("z");
        world.camera.pitch = worldJSON.getJSONObject("camera").getJSONObject("rotation").getFloat("pitch");
        world.camera.yaw = worldJSON.getJSONObject("camera").getJSONObject("rotation").getFloat("yaw");
        world.camera.roll = worldJSON.getJSONObject("camera").getJSONObject("rotation").getFloat("roll");
        // Set directional light state
        world.dirLight.position.x = worldJSON.getJSONObject("dirLight").getJSONObject("position").getFloat("x");
        world.dirLight.position.y = worldJSON.getJSONObject("dirLight").getJSONObject("position").getFloat("y");
        world.dirLight.position.z = worldJSON.getJSONObject("dirLight").getJSONObject("position").getFloat("z");
        world.dirLight.ambient.x = worldJSON.getJSONObject("dirLight").getJSONObject("ambient").getFloat("r");
        world.dirLight.ambient.y = worldJSON.getJSONObject("dirLight").getJSONObject("ambient").getFloat("g");
        world.dirLight.ambient.z = worldJSON.getJSONObject("dirLight").getJSONObject("ambient").getFloat("b");
        world.dirLight.diffuse.x = worldJSON.getJSONObject("dirLight").getJSONObject("diffuse").getFloat("r");
        world.dirLight.diffuse.y = worldJSON.getJSONObject("dirLight").getJSONObject("diffuse").getFloat("g");
        world.dirLight.diffuse.z = worldJSON.getJSONObject("dirLight").getJSONObject("diffuse").getFloat("b");
        world.dirLight.specular.x = worldJSON.getJSONObject("dirLight").getJSONObject("specular").getFloat("r");
        world.dirLight.specular.y = worldJSON.getJSONObject("dirLight").getJSONObject("specular").getFloat("g");
        world.dirLight.specular.z = worldJSON.getJSONObject("dirLight").getJSONObject("specular").getFloat("b");
        // Set point lights state
        world.pointLights.clear();
        for (int i = 0; i < worldJSON.getJSONArray("pointLights").length(); i++) {
            Vector3f pos = new Vector3f();
            pos.x = worldJSON.getJSONArray("pointLights").getJSONObject(i).getJSONObject("position").getFloat("x");
            pos.y = worldJSON.getJSONArray("pointLights").getJSONObject(i).getJSONObject("position").getFloat("y");
            pos.z = worldJSON.getJSONArray("pointLights").getJSONObject(i).getJSONObject("position").getFloat("z");
            Vector3f ambient = new Vector3f();
            ambient.x = worldJSON.getJSONArray("pointLights").getJSONObject(i).getJSONObject("ambient").getFloat("r");
            ambient.y = worldJSON.getJSONArray("pointLights").getJSONObject(i).getJSONObject("ambient").getFloat("g");
            ambient.z = worldJSON.getJSONArray("pointLights").getJSONObject(i).getJSONObject("ambient").getFloat("b");
            Vector3f diffuse = new Vector3f();
            diffuse.x = worldJSON.getJSONArray("pointLights").getJSONObject(i).getJSONObject("diffuse").getFloat("r");
            diffuse.y = worldJSON.getJSONArray("pointLights").getJSONObject(i).getJSONObject("diffuse").getFloat("g");
            diffuse.z = worldJSON.getJSONArray("pointLights").getJSONObject(i).getJSONObject("diffuse").getFloat("b");
            Vector3f specular = new Vector3f();
            specular.x = worldJSON.getJSONArray("pointLights").getJSONObject(i).getJSONObject("specular").getFloat("r");
            specular.y = worldJSON.getJSONArray("pointLights").getJSONObject(i).getJSONObject("specular").getFloat("g");
            specular.z = worldJSON.getJSONArray("pointLights").getJSONObject(i).getJSONObject("specular").getFloat("b");
            world.pointLights.add(new Light(pos, ambient, diffuse, specular));
        }
        // Set blocks
        JSONObject blocks = worldJSON.getJSONObject("blocks");
        for (BlockType type : BlockType.values()) {
            JSONArray list = blocks.getJSONArray(type.name());
            for (int i = 0; i < list.length(); i++) {
                JSONArray pos = list.getJSONArray(i);
                world.addBlock(new Block(pos.getFloat(0), pos.getFloat(1), pos.getFloat(2), type));
            }
        }
        for (Chunk c : world.chunks) c.regenerateMesh();
        return world;
    }

    /**
     * Export the world to the users prefered file
     * @param world
     * @return
     */
    static File exportWorld(World world) {
        File file = promptFileLocation();
        try {
            return exportToFile(world, file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Prompt the user to specify a file
     * @return
     */
    static File promptFileLocation() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setCurrentDirectory(new java.io.File(System.getProperty("user.home")));
        int result = fileChooser.showOpenDialog(new JDialog());
        if (result != JFileChooser.APPROVE_OPTION) {
            System.exit(1);
        }
        return fileChooser.getSelectedFile();
    }

    /**
     * Export the world to the specified file
     * @param world
     * @param file
     * @return
     * @throws FileNotFoundException
     */
    static File exportToFile(World world, File file) throws FileNotFoundException {
        JSONObject worldJSON = new JSONObject();
        // Export camera state to json
        JSONObject cameraJSON = world.camera.toJSON();
        worldJSON.put("camera", cameraJSON);
        // Export dir light to json
        JSONObject dirLight = world.dirLight.toJSON();
        worldJSON.put("dirLight", dirLight);
        // Export point lights to json
        JSONArray lightsJSON = new JSONArray();
        for (Light l : world.pointLights) {
            lightsJSON.put(l.toJSON());
        }
        worldJSON.put("pointLights", lightsJSON);
        // Export blocks to json
        Map<BlockType, JSONArray> blocksJSON = new HashMap<>();
        for (BlockType type : BlockType.values()) blocksJSON.put(type, new JSONArray());
        for (Chunk c : world.chunks) {
            for (Block block : c.blockList) {
                JSONArray pos = new JSONArray();
                pos.put(block.position.x);
                pos.put(block.position.y);
                pos.put(block.position.z);
                blocksJSON.get(block.type).put(pos);
            }
        }
        JSONObject blocks = new JSONObject();
        for (BlockType type : BlockType.values()) {
            blocks.put(type.name(), blocksJSON.get(type));
        }
        worldJSON.put("blocks", blocks);
        // Write to file
        try (PrintWriter out = new PrintWriter(file)) {
            out.print(worldJSON);
        }
        return null;
    }

}
