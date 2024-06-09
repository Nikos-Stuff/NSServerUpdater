package nsj.nsj;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class NSServerUpdater extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Server Updater Enabled...");

        // Get the path to the root directory of the server
        Path serverDir = Paths.get("").toAbsolutePath();

        String downloadUrl = "https://api.nikostuff.com/v1/get_jar/purpur?getlatest";
        String fileName = serverDir.resolve("server_u.jar").toString();

        try {
            downloadFile(downloadUrl, fileName);
            getLogger().info("Download completed. File saved as: " + fileName);
        } catch (IOException e) {
            getLogger().warning("Failed to download the file: " + e.getMessage());
        }
    }

    private void downloadFile(String url, String fileName) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
    }


    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
