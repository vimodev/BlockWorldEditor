import org.joml.Vector3f;
import org.joml.Vector3i;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
        // Ask user which file to output to
        try {
            File file = promptFileLocation();
            List<File> chunkFiles = new ArrayList<>();
            File worldFile = null;
            // Make a temporary directory to extract to
            Path extractDir = Files.createTempDirectory("blockworldeditor");
            extractDir.toFile().deleteOnExit();
            // Read the zip and extract files
            byte[] buffer = new byte[1024];
            ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = new File(extractDir.toFile(), zipEntry.getName());
                newFile.deleteOnExit();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                if (zipEntry.getName().endsWith(".chunk")) chunkFiles.add(newFile);
                else if (zipEntry.getName().equals("world.json")) worldFile = newFile;
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
            // Create the world
            JSONObject worldJSON = new JSONObject(Files.readString(worldFile.toPath()));
            JSONObject genJSON = worldJSON.getJSONObject("generator");
            World world = new World(app,
                    new HillWorldGenerator(
                            genJSON.getLong("seed"),
                            genJSON.getInt("height"),
                            genJSON.getInt("amplitude"),
                            genJSON.getFloat("frequency")
                    )
            );
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
            // Reset the archiver and repopulate with the newly loaded stuff
            ChunkArchiver.reset();
            JSONArray chunkIndex = worldJSON.getJSONArray("chunks");
            // Go over all indexed chunks
            for (int i = 0; i < chunkIndex.length(); i++) {
                // Get position and name
                JSONObject c = chunkIndex.getJSONObject(i);
                JSONArray p = c.getJSONArray("p");
                Vector3i origin = new Vector3i(p.getInt(0), p.getInt(1), p.getInt(2));
                String n = c.getString("n");
                // Find the appropriate file
                for (File f : chunkFiles) {
                    if (f.getName().equals(n)) {
                        ChunkArchiver.unloadedChunksLock.lock();
                        ChunkArchiver.unloadedChunks.put(origin, f);
                        ChunkArchiver.unloadedChunksLock.unlock();
                        break;
                    }
                }
            }
            // Make sure to load the chunks that need to be loaded from archive / generator
            int generating = world.manageChunks();
            while (world.chunks.size() < generating) {
                world.gatherChunks();
                Thread.sleep(50);
            }
            return world;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
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
        } catch (IOException e) {
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
        fileChooser.setFileFilter(new FileNameExtensionFilter("BlockWorldEditor files", "bwe"));
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
    static File exportToFile(World world, File file) throws IOException {
        // The destination zip file
        file = new File(file + ".bwe");
        FileOutputStream fos = new FileOutputStream(file);
        ZipOutputStream zipOut = new ZipOutputStream(fos);

        // JSON object builder for world index
        JSONObject worldJSON = new JSONObject();
        // Export camera state to json
        JSONObject cameraJSON = world.camera.toJSON();
        worldJSON.put("camera", cameraJSON);
        // Export dir light to json
        JSONObject dirLight = world.dirLight.toJSON();
        worldJSON.put("dirLight", dirLight);
        // Set generator
        JSONObject genJSON = new JSONObject(world.worldGenerator.config);
        worldJSON.put("generator", genJSON);
        // Chunk file indexing
        JSONArray chunks = new JSONArray();
        ChunkArchiver.unloadedChunksLock.lock();
        for (Vector3i key : ChunkArchiver.unloadedChunks.keySet()) {
            JSONObject chunkJSON = new JSONObject();
            chunkJSON.put("p", new JSONArray(new int[]{key.x, key.y, key.z}));
            chunkJSON.put("n", ChunkArchiver.unloadedChunks.get(key).getName());
            chunks.put(chunkJSON);
            FileInputStream fis = new FileInputStream(ChunkArchiver.unloadedChunks.get(key));
            ZipEntry zipEntry = new ZipEntry(ChunkArchiver.unloadedChunks.get(key).getName());
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024]; int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
        }
        for (Chunk c : world.chunks) {
            if (!c.modified) continue;
            File f = Chunk.toFile(c);
            JSONObject chunkJSON = new JSONObject();
            chunkJSON.put("p", new JSONArray(new int[]{c.origin.x, c.origin.y, c.origin.z}));
            chunkJSON.put("n", f.getName());
            chunks.put(chunkJSON);
            FileInputStream fis = new FileInputStream(f);
            ZipEntry zipEntry = new ZipEntry(f.getName());
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024]; int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
            f.delete();
        }
        worldJSON.put("chunks", chunks);
        ChunkArchiver.unloadedChunksLock.unlock();
        // Write to file
        File tempFile = new File(file + ".json");
        try (PrintWriter out = new PrintWriter(tempFile)) {
            out.print(worldJSON);
        }
        // Apply zip to compress
        FileInputStream fis = new FileInputStream(tempFile);
        ZipEntry zipEntry = new ZipEntry("world.json");
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024]; int length;
        while((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        zipOut.close();
        fis.close();
        fos.close();
        tempFile.delete();
        return file;
    }

}
