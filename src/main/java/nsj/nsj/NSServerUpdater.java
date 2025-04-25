package nsj.nsj;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.Properties;

public final class NSServerUpdater extends JavaPlugin implements Listener {

    private static final String CONFIG_FILE_NAME = "NSSU.properties";
    private static final String DEFAULT_JAR_TYPE = "purpur";
    private static final String DEFAULT_JAR_VERSION = "latest";
    private static final String DEFAULT_JAR_NAME = "server_u";

    private String jarType;
    private String jarVersion;
    private String jarName;

    private WatchService watchService;
    private Thread watchThread;
    private long lastModified = 0;

    @Override
    public void onEnable() {
        getLogger().info("Loading configuration...");

        Path serverDir = Paths.get("").toAbsolutePath();
        String configFilePath = serverDir.resolve(CONFIG_FILE_NAME).toString();

        Properties config = loadOrCreateConfig(configFilePath);
        applyConfig(config);

        getServer().getPluginManager().registerEvents(this, this);

        startConfigWatcher(serverDir, configFilePath);
    }

    @Override
    public void onDisable() {
        if (watchThread != null && watchThread.isAlive()) {
            watchThread.interrupt();
        }

        getLogger().info("Upgrading jar...");

        Path serverDir = Paths.get("").toAbsolutePath();
        String downloadUrl = getDownloadUrl(jarType, jarVersion);
        String fileName = serverDir.resolve(jarName + ".jar").toString();

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

        try (InputStream input = Files.newInputStream(configPath)) {
            config.load(input);
        } catch (IOException e) {
            getLogger().warning("Failed to load configuration file: " + e.getMessage());
        }

        return config;
    }

    private void applyConfig(Properties config) {
        jarType = config.getProperty("jar_type", DEFAULT_JAR_TYPE);
        jarVersion = config.getProperty("jar_version", DEFAULT_JAR_VERSION);
        jarName = config.getProperty("save_to", DEFAULT_JAR_NAME);
    }

    private void startConfigWatcher(Path serverDir, String configFilePath) {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            serverDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            Path configPath = serverDir.resolve(CONFIG_FILE_NAME);
            lastModified = Files.getLastModifiedTime(configPath).toMillis();

            watchThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            Path changed = (Path) event.context();
                            if (changed.toString().equals(CONFIG_FILE_NAME)) {
                                long newModified = Files.getLastModifiedTime(configPath).toMillis();
                                if (newModified != lastModified) {
                                    lastModified = newModified;

                                    Properties newConfig = loadOrCreateConfig(configFilePath);
                                    applyConfig(newConfig);

                                    getLogger().info("Detected config change! Updated jar settings.");
                                }
                            }
                        }
                        key.reset();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        getLogger().warning("Error watching config file: " + e.getMessage());
                    }
                }
            });
            watchThread.start();
        } catch (IOException e) {
            getLogger().warning("Failed to start config file watcher: " + e.getMessage());
        }
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
        int totalBars = 20;
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
