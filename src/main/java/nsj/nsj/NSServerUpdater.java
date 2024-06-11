package nsj.nsj;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class NSServerUpdater extends JavaPlugin implements Listener {

    private static final String CONFIG_FILE_NAME = "NSSU.properties";
    private static final String DEFAULT_JAR_TYPE = "purpur";
    private static final String DEFAULT_JAR_VERSION = "latest";
    private static final String DEFAULT_JAR_NAME = "server_u";

    private String jarType;
    private String jarVersion;
    private String jarName;

    @Override
    public void onEnable() {
        // Enable and load configuration
        getLogger().info("Configuration loaded!");

        // Get the path to the root directory of the server
        Path serverDir = Paths.get("").toAbsolutePath();
        String configFilePath = serverDir.resolve(CONFIG_FILE_NAME).toString();

        // Load or create configuration
        Properties config = loadOrCreateConfig(configFilePath);

        jarType = config.getProperty("jar_type", DEFAULT_JAR_TYPE);
        jarVersion = config.getProperty("jar_version", DEFAULT_JAR_VERSION);
        jarName = config.getProperty("save_to", DEFAULT_JAR_NAME);

        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // Disable and update
        getLogger().info("Upgrading jar..");

        // Get the path to the root directory of the server
        Path serverDir = Paths.get("").toAbsolutePath();
        String downloadUrl = getDownloadUrl(jarType, jarVersion);
        String fileName = serverDir.resolve(jarName +".jar").toString();

        getLogger().info("Downloading " + jarType + " version " + jarVersion + "...");

        try {
            downloadFile(downloadUrl, fileName);
            getLogger().info("Download completed. File saved as: " + fileName);
        } catch (IOException e) {
            getLogger().warning("Failed to download the file: " + e.getMessage());
        }
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        getLogger().warning("-----------------------------------------------------");
        getLogger().warning("Server will be automatically updated to:");
        getLogger().warning("Version: " + jarVersion);
        getLogger().warning("Type: " + jarType);
        getLogger().warning("Save Name: " + jarName);
        getLogger().warning("-----------------------------------------------------");
    }

    private Properties loadOrCreateConfig(String configFilePath) {
        Properties config = new Properties();

        // Create default config if it doesn't exist or required properties are missing
        Path configPath = Paths.get(configFilePath);
        if (!Files.exists(configPath)) {
            try (OutputStream output = Files.newOutputStream(configPath)) {
                config.setProperty("0w1", "Please note - don't include .jar in the save_to input");
                config.setProperty("0w2", "More info on github.com/Nikos-Stuff/NSServerUpdater");
                config.setProperty("0w3", "For questions or support | Discord - nikodaproot");
                config.setProperty("0w4", "----------------------------------------------------------");
                config.setProperty("jar_type", DEFAULT_JAR_TYPE);
                config.setProperty("jar_version", DEFAULT_JAR_VERSION);
                config.setProperty("save_to", DEFAULT_JAR_NAME);
                config.store(output, "NikoStuff Server Updater Config");
                getLogger().info("Created default configuration file.");
            } catch (IOException e) {
                getLogger().warning("Failed to create configuration file: " + e.getMessage());
            }
        }

        // Load the config file
        try (InputStream input = Files.newInputStream(configPath)) {
            config.load(input);
        } catch (IOException e) {
            getLogger().warning("Failed to load configuration file: " + e.getMessage());
        }

        return config;
    }



    private String getDownloadUrl(String jarType, String jarVersion) {
        if ("latest".equalsIgnoreCase(jarVersion)) {
            return String.format("https://api.nikostuff.com/v1/get_jar/%s?getlatest", jarType);
        } else {
            return String.format("https://api.nikostuff.com/v1/get_jar/%s?download=%s", jarType, jarVersion);
        }
    }

    private void downloadFile(String url, String fileName) throws IOException {
        URL downloadUrl = new URL(url);
        long lastLogTime = System.currentTimeMillis();
        try (BufferedInputStream in = new BufferedInputStream(downloadUrl.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(fileName)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            long totalBytesRead = 0;
            long fileSize = downloadUrl.openConnection().getContentLengthLong();

            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                // Log progress every 0.3 seconds
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLogTime >= 300) {
                    lastLogTime = currentTime;
                    int progress = (int) ((totalBytesRead * 100) / fileSize);
                    getLogger().info("Downloading jar: " + getProgressBar(progress) + " " + progress + "%");
                }
            }
        }
    }

    private String getProgressBar(int progress) {
        int totalBars = 20;  // Total number of bars in the progress bar
        int filledBars = (progress * totalBars) / 100;
        StringBuilder progressBar = new StringBuilder("[");
        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                progressBar.append("=");
            } else {
                progressBar.append(" ");
            }
        }
        progressBar.append("]");
        return progressBar.toString();
    }
}
