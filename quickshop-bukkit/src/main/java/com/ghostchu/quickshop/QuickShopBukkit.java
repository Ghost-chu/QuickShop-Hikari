package com.ghostchu.quickshop;

import com.alessiodp.libby.BukkitLibraryManager;
import com.alessiodp.libby.Library;
import com.alessiodp.libby.LibraryManager;
import com.alessiodp.libby.logging.adapters.JDKLogAdapter;
import com.alessiodp.libby.logging.adapters.LogAdapter;
import com.comphenix.protocol.utility.Util;
import com.ghostchu.quickshop.common.util.GeoUtil;
import com.ghostchu.quickshop.platform.Platform;
import com.ghostchu.quickshop.platform.paper.PaperPlatform;
import com.ghostchu.quickshop.platform.spigot.AbstractSpigotPlatform;
import com.ghostchu.quickshop.platform.spigot.v1_18_1.Spigot1181Platform;
import com.ghostchu.quickshop.platform.spigot.v1_18_2.Spigot1182Platform;
import com.ghostchu.quickshop.platform.spigot.v1_19_1.Spigot1191Platform;
import com.ghostchu.quickshop.platform.spigot.v1_19_2.Spigot1193Platform;
import com.ghostchu.quickshop.platform.spigot.v1_19_3.Spigot1194Platform;
import com.ghostchu.quickshop.platform.spigot.v1_20_1.Spigot1201Platform;
import com.ghostchu.quickshop.platform.spigot.v1_20_2.Spigot1202Platform;
import com.ghostchu.quickshop.platform.spigot.v1_20_3.Spigot1203Platform;
import com.ghostchu.quickshop.util.PackageUtil;
import com.vdurmont.semver4j.Semver;
import io.papermc.lib.PaperLib;
import kong.unirest.Unirest;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class QuickShopBukkit extends JavaPlugin {
    @Getter
    private final java.util.logging.Logger bootstrapLogger = java.util.logging.Logger.getLogger("QuickShop-Hikari/Bootstrap");
    private Platform platform;
    private Logger logger;
    private QuickShop quickShop;
    private Throwable abortLoading;
    private BukkitLibraryManager bukkitLibraryManager;

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        this.quickShop.reloadConfigSubModule();
    }

    @Override
    public void onLoad() {
        long startLoadAt = System.currentTimeMillis();
        try {
            bootstrapLogger.info("QuickShop-" + getFork() + " - Bootstrap -> Execute the initialization sequence");
            bootstrapLogger.info("Bootloader preparing for startup, please wait...");
            bootstrapLogger.info("Initializing libraries...");
            loadLibraries();
            bootstrapLogger.info("Initializing platform...");
            loadPlatform();
            bootstrapLogger.info("Boot QuickShop instance...");
            initQuickShop();
            // SLF4J now should available
            bootstrapLogger.info("QuickShop-" + getFork() + " - Bootstrap -> Complete (" + (System.currentTimeMillis() - startLoadAt) + "ms). Waiting for enable...");
        } catch (Throwable e) {
            bootstrapLogger.log(Level.SEVERE, "Failed to startup the QuickShop-Hikari due unexpected exception!", e);
            Bukkit.getPluginManager().disablePlugin(this);
            abortLoading = e;
            throw new IllegalStateException("Boot failure", e);
        }
    }

    @Override
    public void onDisable() {
        long shutdownAtTime = System.currentTimeMillis();
        bootstrapLogger.info("QuickShop-" + getFork() + " - Bootstrap -> Execute the shutdown sequence");
        this.quickShop.onDisable();
        bootstrapLogger.info("Cleaning up resources...");
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
        Bukkit.getServicesManager().unregisterAll(this);
        Unirest.shutDown(true);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
        this.platform.shutdown();
        bootstrapLogger.info("QuickShop-" + getFork() + " - Bootstrap -> All Complete (" + (System.currentTimeMillis() - shutdownAtTime) + "ms)");
    }

    @Override
    public void onEnable() {
        if (abortLoading != null) {
            throw new IllegalStateException("Plugin is disabled due an loading error", abortLoading);
        }
        long enableAtTime = System.currentTimeMillis();
        bootstrapLogger.info("QuickShop-" + getFork() + " - Bootstrap -> Execute the enable sequence");
        this.quickShop.onEnable();
        bootstrapLogger.info("QuickShop-" + getFork() + " - Bootstrap -> All Complete. (" + (System.currentTimeMillis() - enableAtTime) + "ms)");
    }

    /**
     * Return the QuickShop fork name.
     *
     * @return The fork name.
     */
    @NotNull
    public String getFork() {
        return "Hikari";
    }

    private void loadLibraries() {
        resolveLibraries(this);
        new UnirestLibLoader(this);
        new AdventureLibLoader(this);
    }

    private void resolveLibraries(QuickShopBukkit quickShopBukkit) {
        if (Boolean.parseBoolean(System.getProperty("com.ghostchu.quickshop.QuickShopBukkit.doNotResolveLibraries"))) {
            bootstrapLogger.warning("Warning! You have disabled libraries resolver! Make sure you added libraries in plugin.yml!");
            return;
        }
//        LogAdapter adapter = new LogAdapter() {
//            @Override
//            public void log(@NotNull LogLevel logLevel, @Nullable String s) {
//                // silent
//            }
//
//            @Override
//            public void log(@NotNull LogLevel logLevel, @Nullable String s, @Nullable Throwable throwable) {
//                // silent
//            }
//        };
        LogAdapter adapter = new JDKLogAdapter(getLogger());
//        if (Boolean.parseBoolean(System.getProperty("com.ghostchu.quickshop.QuickShopBukkit.verboseLibraryManager"))) {
//            adapter = new JDKLogAdapter(getLogger());
//        }
        this.bukkitLibraryManager = new BukkitLibraryManager(quickShopBukkit, "lib", adapter);
        if (!Boolean.parseBoolean(System.getProperty("com.ghostchu.quickshop.QuickShopBukkit.disableMavenLocal"))) {
            this.bukkitLibraryManager.addMavenLocal();
        }
        if (!Boolean.parseBoolean(System.getProperty("com.ghostchu.quickshop.QuickShopBukkit.disableSpigotLocal"))) {
            File relative = new File(getDataFolder().getParentFile().getParentFile(), "libraries");
            if (relative.exists()) {
                this.bukkitLibraryManager.addRepository(relative.toPath().toUri().toString());
            }
        }
        if (!Boolean.parseBoolean(System.getProperty("com.ghostchu.quickshop.QuickShopBukkit.disableMirrorTesting"))) {
            GeoUtil.determineBestMirrorServer(getLogger()).forEach(m -> this.bukkitLibraryManager.addRepository(m.getRepoUrl()));
        }
        //this.bukkitLibraryManager.addMavenCentral();
        this.bukkitLibraryManager.getRepositories().forEach(r -> {
            if (Boolean.parseBoolean(System.getProperty("com.ghostchu.quickshop.QuickShopBukkit.verboseLibraryManager"))) {
                bootstrapLogger.info("Registered repository: " + r);
            }
        });
        try {
            loadLibraries(this.bukkitLibraryManager);
        } catch (IllegalStateException e) {
            bootstrapLogger.log(Level.SEVERE, e.getMessage() + " The startup cannot continue.", e);
        }
    }

    private void loadLibraries(LibraryManager manager) {
        try (InputStream stream = getResource("libraries.maven");) {
            if (stream == null) {
                throw new IllegalStateException("Jar file doesn't include a valid libraries.maven file");
            }
            String dat = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            String[] libraries = dat.split("\n");
            List<Library> libraryList = new ArrayList<>();
            int skipped = 0;
            for (String library : libraries) {
                if (library.isBlank() || library.startsWith("#") || library.startsWith("//")) continue;
                library = library.trim();
                String[] cases = library.split("@");
                String testClass = null;
                if (cases.length == 2) {
                    testClass = cases[1];
                }
                if (testClass != null) {
                    if (Util.classExists(testClass) && Boolean.parseBoolean(System.getProperty("com.ghostchu.quickshop.QuickShopBukkit.reuseDependencies", "true"))) {
                        skipped++;
                        continue;
                    }
                }

                String[] libExplode = cases[0].split(":");
                if (libExplode.length < 3) {
                    throw new IllegalArgumentException("[" + library + "] not a valid maven dependency syntax");
                }
                String groupId = libExplode[0];
                String artifactId = libExplode[1];
                String version = libExplode[2];
                String classifier = null;
                if (libExplode.length >= 4) {
                    classifier = libExplode[3];
                }
                Library.Builder libBuilder = Library.builder()
                        .groupId(groupId)
                        .artifactId(artifactId)
                        .version(version)
                        .resolveTransitiveDependencies(true)
                        .isolatedLoad(false);
                if (classifier != null) {
                    libBuilder = libBuilder.classifier(classifier);
                }
                Library lib = libBuilder.build();
                libraryList.add(lib);
            }
            bootstrapLogger.info("Loading " + libraryList.size() + " libraries (" + skipped + " skipped libraries)...");
            for (int i = 0; i < libraryList.size(); i++) {
                Library load = libraryList.get(i);
                bootstrapLogger.info("Loading library " + load.toString() + " [" + (i + 1) + "/" + libraryList.size() + "]");
                if (Boolean.parseBoolean(System.getProperty("com.ghostchu.quickshop.QuickShopBukkit.verboseLibraryManager"))) {
                    for (String url : load.getUrls()) {
                        bootstrapLogger.info(load + " url selected: " + url);
                    }
                }
                manager.loadLibrary(load);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot download the libraries, the first time install/upgrade need the Internet connection.", e);
        }
    }


    private void loadPlatform() throws Exception {
        int platformId = 0;
        if (PaperLib.isSpigot()) {
            platformId = 1;
        }
        if (PaperLib.isPaper()) {
            platformId = 2;
        }

        platformId = PackageUtil.parsePackageProperly("forcePlatform").asInteger(platformId);
        try {
            switch (platformId) {
                case 1 -> {
                    bootstrapLogger.info("Platform detected: Spigot");
                    bootstrapLogger.warning("Use Paper or Paper's fork to get best performance and enhanced features!");

                    initNbtApi();

                    //noinspection deprecation
                    String internalNMSVersion = AbstractSpigotPlatform.getNMSVersion();
                    this.platform = switch (internalNMSVersion) {
                        case "v1_18_R1" -> new Spigot1181Platform(this);
                        case "v1_18_R2" -> new Spigot1182Platform(this);
                        case "v1_19_R1" -> new Spigot1191Platform(this);
                        case "v1_19_R2" -> new Spigot1193Platform(this);
                        case "v1_19_R3" -> new Spigot1194Platform(this);
                        case "v1_20_R1" -> new Spigot1201Platform(this);
                        case "v1_20_R2" -> new Spigot1202Platform(this);
                        case "v1_20_R3" -> new Spigot1203Platform(this);
                        default -> {
                            bootstrapLogger.warning("This server running " + internalNMSVersion + " not supported by Hikari. (Try update? or Use Paper's fork to get cross-platform compatibility.)");
                            Bukkit.getPluginManager().disablePlugin(this);
                            throw new IllegalStateException("This server running " + internalNMSVersion + " not supported by Hikari. (Try update? or Use Paper's fork to get cross-platform compatibility.)");
                        }
                    };
                }
                case 2 -> {
                    bootstrapLogger.info("Platform detected: Paper");
                    this.platform = new PaperPlatform();
                }
                default -> throw new UnsupportedOperationException("Unsupported platform");
            }
            try {
                this.logger = this.platform.getSlf4jLogger(this);
            } catch (Throwable th) {
                this.logger = LoggerFactory.getLogger(getDescription().getName());
            }
            logger.info("Slf4jLogger initialized");
            bootstrapLogger.info("Platform initialized: " + this.platform.getClass().getName());
        } catch (Throwable e) {
            throw new Exception("Failed to initialize the platform", e);
        }
    }

    private void initNbtApi() {
        new NbtApiInitializer(bootstrapLogger);
    }

    private void initQuickShop() {
        bootstrapLogger.info("Creating QuickShop instance...");
        this.quickShop = new QuickShop(this, logger, platform);
        this.quickShop.onLoad();
    }

    @NotNull
    public Logger logger() {
        return this.logger;
    }

    @NotNull
    public Platform platform() {
        return this.platform;
    }

    /**
     * Returns QS version, this method only exist on QuickShop forks If running other QuickShop forks, result
     * may not is "Reremake x.x.x" If running QS official, Will throw exception.
     *
     * @return Plugin Version
     */
    @NotNull
    public String getVersion() {
        return getDescription().getVersion();
    }

    @NotNull
    public Semver getSemVersion() {
        try {
            return new Semver(getDescription().getVersion());
        } catch (Exception e) {
            return new Semver("0.0.0.0");
        }
    }

    public QuickShop getQuickShop() {
        return quickShop;
    }

    static class UnirestLibLoader {
        public UnirestLibLoader(QuickShopBukkit plugin) {
            plugin.getBootstrapLogger().info("Initialing Unirest...");
            Unirest.config()
                    .concurrency(10, 5)
                    .setDefaultHeader("User-Agent", "QuickShop/" + plugin.getFork() + "-" + plugin.getDescription().getVersion() + " Java/" + System.getProperty("java.version"));
            Unirest.config().verifySsl(PackageUtil.parsePackageProperly("verifySSL").asBoolean());
            if (PackageUtil.parsePackageProperly("proxyHost").isPresent()) {
                plugin.getBootstrapLogger().info("Unirest proxy feature has been enabled.");
                Unirest.config().proxy(PackageUtil.parsePackageProperly("proxyHost").asString("127.0.0.1"), PackageUtil.parsePackageProperly("proxyPort").asInteger(1080));
            }
            if (PackageUtil.parsePackageProperly("proxyUsername").isPresent()) {
                plugin.getBootstrapLogger().info("Unirest proxy authentication activated.");
                Unirest.config().proxy(PackageUtil.parsePackageProperly("proxyHost").asString("127.0.0.1"), PackageUtil.parsePackageProperly("proxyPort").asInteger(1080), PackageUtil.parsePackageProperly("proxyUsername").asString(""), PackageUtil.parsePackageProperly("proxyPassword").asString(""));
            }
        }
    }

    static class AdventureLibLoader {
        public AdventureLibLoader(QuickShopBukkit plugin) {
//            plugin.getBootstrapLogger().info("Loading the Adventure Chat Processor...");
//            plugin.getBootstrapLogger().info("Adventure API loaded from: " + CommonUtil.getClassPath(Adventure.class));
//            plugin.getBootstrapLogger().info("Adventure Bukkit Platform loaded from: " + CommonUtil.getClassPath(BukkitAudiences.class));
//            plugin.getBootstrapLogger().info("Adventure Text Serializer (Legacy) loaded from: " + CommonUtil.getClassPath(LegacyComponentSerializer.class));
//            plugin.getBootstrapLogger().info("Adventure Text Serializer (Gson) loaded from: " + CommonUtil.getClassPath(GsonComponentSerializer.class));
//            plugin.getBootstrapLogger().info("Adventure Text Serializer (Json) loaded from: " + CommonUtil.getClassPath(JSONComponentSerializer.class));
//            plugin.getBootstrapLogger().info("Adventure Text Serializer (BungeeChat) loaded from: " + CommonUtil.getClassPath(BungeeComponentSerializer.class));
//            plugin.getBootstrapLogger().info("Adventure Text Serializer (ViaVersion Facet) loaded from: " + CommonUtil.getClassPath(ViaFacet.class));
//            plugin.getBootstrapLogger().info("Adventure Text Serializer (ANSI) loaded from: " + CommonUtil.getClassPath(ANSIComponentSerializer.class));
//            plugin.getBootstrapLogger().info("Adventure Text Serializer (Plain) loaded from: " + CommonUtil.getClassPath(PlainTextComponentSerializer.class));
//            plugin.getBootstrapLogger().info("Adventure MiniMessage Lib loaded from: " + CommonUtil.getClassPath(MiniMessage.class));

        }
    }
}