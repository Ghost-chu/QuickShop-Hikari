package com.ghostchu.quickshop.command.subcommand;

import cc.carm.lib.easysql.EasySQL;
import cc.carm.lib.easysql.api.SQLQuery;
import cc.carm.lib.easysql.hikari.HikariDataSource;
import cc.carm.lib.easysql.hikari.pool.HikariPool;
import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.command.CommandHandler;
import com.ghostchu.quickshop.api.command.CommandParser;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.common.util.QuickExecutor;
import com.ghostchu.quickshop.shop.SimpleShopManager;
import com.ghostchu.quickshop.shop.cache.SimpleShopCache;
import com.ghostchu.quickshop.util.MsgUtil;
import com.ghostchu.quickshop.util.Util;
import com.ghostchu.quickshop.util.performance.BatchBukkitExecutor;
import com.google.common.cache.Cache;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SubCommand_Debug implements CommandHandler<CommandSender> {

    private final QuickShop plugin;

    public SubCommand_Debug(QuickShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onCommand(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull CommandParser parser) {
        if (parser.getArgs().isEmpty()) {
            switchDebug(sender);
            return;
        }
        List<String> subParams = new ArrayList<>(parser.getArgs());
        subParams.remove(0);
        switch (parser.getArgs().get(0)) {
            case "debug", "dev", "devmode" -> switchDebug(sender);
            case "handlerlist" -> handleHandlerList(sender, subParams);
            case "signs" -> handleSigns(sender);
            case "database" -> handleDatabase(sender, subParams);
            case "updateplayersigns" -> handleSignsUpdate(sender, subParams);
            case "force-shops-reload" -> handleShopsReload(sender, subParams);
            case "force-shoploader-reload" -> handleShopsLoaderReload(sender, subParams);
            case "check-shop-status" -> handleShopDebug(sender, subParams);
            case "toggle-shop-load-status" -> handleShopLoading(sender, subParams);
            case "check-shop-debug" -> handleShopInfo(sender, subParams);
            case "set-property" -> handleProperty(sender, subParams);
            case "reset-shop-caches" -> handleShopCacheResetting(sender, subParams);
            case "reset-dbmanager" -> handleDbManagerReset(sender, subParams);
            case "dump-db-connections" -> handleDumpDbConnections(sender, subParams);
            case "stop-db-any-queries" -> handleStopDbQueries(sender, subParams);
            case "toggle-db-debugmode" -> handleToggleDbDebugMode(sender, subParams);
            case "dump-hikaricp-status" -> handleDumpHikariCPStatus(sender, subParams);
            case "set-hikaricp-capacity" -> handleSetHikariCPCapacity(sender, subParams);
            default -> plugin.text().of(sender, "debug.arguments-invalid", parser.getArgs().get(0)).send();
        }
    }

    private void handleSetHikariCPCapacity(CommandSender sender, List<String> subParams) {
        int size = Integer.parseInt(subParams.get(0));
        HikariDataSource hikariDataSource = (HikariDataSource) plugin.getSqlManager().getDataSource();
        hikariDataSource.setMaximumPoolSize(size);
        hikariDataSource.setMinimumIdle(size);
        sender.sendMessage("MaximumPoolSize and MinimumIdle was set to "+size);
    }

    private void handleDbConnectionTest(CommandSender sender, List<String> subParams) {
        sender.sendMessage("Please wait...");
        try {
            CompletableFuture.supplyAsync(() -> {
                try (Connection connection = plugin.getSqlManager().getConnection()) {
                    if (connection.isValid(1000)) {
                        sender.sendMessage("HikariCP working!");
                    } else {
                        sender.sendMessage("HikariCP returned a dead connection!");
                    }
                } catch (SQLException e) {
                    sender.sendMessage("Failed to test HikariCP");
                    e.printStackTrace();
                }
                return null;
            }).get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            sender.sendMessage("HikariCP seems stop working, please clean up all active queries to release connection resources.");
        } catch (ExecutionException | InterruptedException e) {
            sender.sendMessage("Failed to test HikariCP");
            e.printStackTrace();
        }
    }

    private void handleStopDbQueries(CommandSender sender, List<String> subParams) {
        long stopped = plugin.getSqlManager().getActiveQuery().values().stream().map(s -> {
            s.close();
            return null;
        }).count();
        sender.sendMessage("Stopped " + stopped + " active queries.");
    }

    private void handleDumpHikariCPStatus(CommandSender sender, List<String> subParams) {
        HikariDataSource hikariDataSource = (HikariDataSource) plugin.getSqlManager().getDataSource();

        sender.sendMessage("Catalog: " + hikariDataSource.getCatalog());
        sender.sendMessage("PoolName: " + hikariDataSource.getPoolName());
        sender.sendMessage("Connection Timeout: " + hikariDataSource.getConnectionTimeout());
        sender.sendMessage("Idle Timeout: " + hikariDataSource.getIdleTimeout());
        sender.sendMessage("Leak Detection Threshold: " + hikariDataSource.getLeakDetectionThreshold());
        sender.sendMessage("MaximumPoolSize: " + hikariDataSource.getMaximumPoolSize());
        sender.sendMessage("MinimumIdle: " + hikariDataSource.getMinimumIdle());
        sender.sendMessage("MaxLifeTime: " + hikariDataSource.getMaxLifetime());
        sender.sendMessage("ValidationTimeout: " + hikariDataSource.getValidationTimeout());
        try {
            Field poolField = hikariDataSource.getClass().getDeclaredField("pool");
            poolField.setAccessible(true);
            HikariPool hikariPool = (HikariPool) poolField.get(hikariDataSource);
            sender.sendMessage("Active connections: " + hikariPool.getActiveConnections());
            sender.sendMessage("Idle connections: " + hikariPool.getIdleConnections());
            sender.sendMessage("Total connections: " + hikariPool.getTotalConnections());
            sender.sendMessage("Threads Awaiting connections: " + hikariPool.getThreadsAwaitingConnection());
        } catch (Exception e) {
            plugin.logger().warn("Failed retrieve HikariPool internal state.", e);
        }
    }

    private void handleToggleDbDebugMode(CommandSender sender, List<String> subParams) {
        plugin.getSqlManager().setDebugMode(!plugin.getSqlManager().isDebugMode());
        sender.sendMessage("Db Debug Mode: " + plugin.getSqlManager().isDebugMode());
    }

    private void handleDumpDbConnections(CommandSender sender, List<String> subParams) {
        sender.sendMessage("Dumping active queries...");
        for (Map.Entry<UUID, SQLQuery> e : plugin.getSqlManager().getActiveQuery().entrySet()) {
            sender.sendMessage(e.getKey().toString() + ": " + e.getValue());
        }
    }

    private void handleDbManagerReset(CommandSender sender, List<String> subParams) {
        sender.sendMessage("Please wait...");
        sender.sendMessage("Shutting down sql manager");
        EasySQL.shutdownManager(this.plugin.getSqlManager());
        sender.sendMessage("Clear executors...");
        QuickExecutor.getHikaricpExecutor().shutdownNow().forEach(r -> sender.sendMessage("Unfinished HikariCP task: " + r));
        QuickExecutor.setHikaricpExecutor(QuickExecutor.provideHikariCPExecutor());
        QuickExecutor.getShopHistoryQueryExecutor().shutdownNow().forEach(r -> sender.sendMessage("Unfinished HistoryQuery task: " + r));
        QuickExecutor.setShopHistoryQueryExecutor(QuickExecutor.provideShopHistoryQueryExecutor());
        sender.sendMessage("Re-launch database connect progress!");
        Util.asyncThreadRun(plugin::setupDatabase);
    }

    private void handleShopCacheResetting(CommandSender sender, List<String> subParams) {
        SimpleShopManager shopManager = (SimpleShopManager) plugin.getShopManager();
        SimpleShopCache simpleShopCache = (SimpleShopCache) shopManager.getShopCache();
        simpleShopCache.getCaches().values().forEach(Cache::invalidateAll);
        sender.sendMessage("Cleared!");
    }

    private void handleProperty(CommandSender sender, List<String> subParams) {
        if (subParams.isEmpty()) {
            sender.sendMessage("Error: You must enter a property key=value set.");
            return;
        }
        if (subParams.size() > 1) {
            sender.sendMessage("Error: You must enter a (and only one) property key=value set. E.g   aaa=bbb");
            return;
        }
        String[] split = subParams.get(0).split("=");
        if (split.length < 1) {
            sender.sendMessage("Error: You must enter a (and only one) property key=value set. E.g   aaa=bbb");
            return;
        }
        String key = split[0];
        String value = null;
        if (split.length > 1) {
            value = split[1];
        }
        if (!key.startsWith("com.ghostchu.quickshop") && !key.startsWith("quickshop")) {
            sender.sendMessage("Error: You can only set the quickshop related properties for safety.");
            return;
        }
        if (value == null) {
            System.clearProperty(key);
            sender.sendMessage("Property " + key + " has been deleted.");
        } else {
            String oldOne = System.setProperty(key, value);
            sender.sendMessage("Property " + key + " has been changed from " + oldOne + " to " + value);
        }
    }

    private void handleShopInfo(CommandSender sender, List<String> subParams) {
        Shop shop = getLookingShop(sender);
        if (shop == null) {
            plugin.text().of(sender, "not-looking-at-shop").send();
            return;
        }
        MsgUtil.sendDirectMessage(sender, shop.toString());
    }

    private void handleShopLoading(CommandSender sender, List<String> remove) {
        Shop shop = getLookingShop(sender);
        if (shop == null) {
            plugin.text().of(sender, "not-looking-at-shop").send();
            return;
        }
        if (shop.isLoaded()) {
            plugin.getShopManager().unloadShop(shop);
        } else {
            plugin.getShopManager().loadShop(shop);
        }
        plugin.text().of(sender, "debug.toggle-shop-loaded-status", shop.isLoaded());
    }

    private void handleShopDebug(CommandSender sender, List<String> remove) {
        Shop shop = getLookingShop(sender);
        if (shop == null) {
            plugin.text().of(sender, "not-looking-at-shop").send();
            return;
        }
        plugin.text().of(sender, "debug.shop-internal-data", shop.toString()).send();
    }

    private void handleShopsLoaderReload(CommandSender sender, List<String> remove) {
        plugin.text().of(sender, "debug.force-shop-loader-reload").send();
        List<Shop> allShops = plugin.getShopManager().getAllShops();
        plugin.text().of(sender, "debug.force-shop-loader-reload-unloading-shops-from-memory", allShops.size()).send();
        plugin.getShopManager().getAllShops().forEach(shop -> plugin.getShopManager().unloadShop(shop));
        plugin.text().of(sender, "debug.force-shop-loader-reload-reloading-shop-loader").send();
        plugin.getShopLoader().loadShops();
        plugin.text().of(sender, "debug.force-shop-loader-reload-complete").send();
    }

    private void handleShopsReload(CommandSender sender, List<String> remove) {
        plugin.text().of(sender, "debug.force-shop-reload").send();
        List<Shop> shops = new ArrayList<>(plugin.getShopManager().getLoadedShops());
        shops.forEach(s -> plugin.getShopManager().unloadShop(s));
        shops.forEach(s -> plugin.getShopManager().loadShop(s));
        plugin.text().of(sender, "debug.force-shop-reload-complete", shops.size()).send();
    }


    public void switchDebug(@NotNull CommandSender sender) {
        final boolean debug = plugin.getConfig().getBoolean("dev-mode");

        if (debug) {
            plugin.getJavaPlugin().reloadConfig();
            plugin.getConfig().set("dev-mode", false);
            plugin.getJavaPlugin().saveConfig();
            plugin.getReloadManager().reload();
            plugin.text().of(sender, "command.now-nolonger-debuging").send();
            return;
        }

        plugin.getJavaPlugin().reloadConfig();
        plugin.getConfig().set("dev-mode", true);
        plugin.getJavaPlugin().saveConfig();
        plugin.getReloadManager().reload();
        plugin.text().of(sender, "command.now-debuging").send();
    }

    private void handleHandlerList(@NotNull CommandSender sender, List<String> remove) {
        if (remove.isEmpty()) {
            MsgUtil.sendDirectMessage(sender, "You must enter an Bukkit Event class");
            plugin.text().of(sender, "debug.handler-list-not-valid-bukkit-event-class", "null");
            return;
        }
        printHandlerList(sender, remove.get(0));
    }

    private void handleSigns(@NotNull CommandSender sender) {
        Shop shop = getLookingShop(sender);
        if (shop == null) {
            plugin.text().of(sender, "not-looking-at-shop").send();
            return;
        }
        shop.getSigns().forEach(sign -> plugin.text().of(sender, "debug.sign-located", sign.getLocation()).send());
    }

    private void handleDatabase(@NotNull CommandSender sender, @NotNull List<String> remove) {
        if (remove.isEmpty()) {
            plugin.text().of("debug.operation-missing");
            return;
        }
        plugin.text().of(sender, "debug.operation-invalid", remove.get(0)).send();
    }

    private void handleSignsUpdate(CommandSender sender, List<String> remove) {
        if (remove.isEmpty()) {
            plugin.text().of(sender, "debug.update-player-shops-signs-no-username-given").send();
            return;
        }
        plugin.text().of(sender, "debug.update-player-shops-signs-create-async-task").send();
        plugin.getPlayerFinder().name2UuidFuture(remove.get(0)).whenComplete((uuid, throwable) -> {
            if (throwable != null) {
                plugin.text().of(sender, "internal-error", throwable.getMessage()).send();
                return;
            }
            plugin.text().of(sender, "debug.update-player-shops-player-selected", uuid).send();
            List<Shop> shops = plugin.getShopManager().getAllShops(uuid);
            plugin.text().of(sender, "debug.update-player-shops-player-shops", shops.size()).send();
            BatchBukkitExecutor<Shop> updateExecutor = new BatchBukkitExecutor<>();
            updateExecutor.addTasks(shops);
            plugin.text().of(sender, "debug.update-player-shops-task-started", shops.size()).send();
            updateExecutor.startHandle(plugin.getJavaPlugin(), Shop::setSignText).whenComplete((aVoid, th) -> {
                if (th != null) {
                    plugin.text().of(sender, "internal-error", th.getMessage()).send();
                    return;
                }
                long usedTime = updateExecutor.getStartTime().until(Instant.now(), java.time.temporal.ChronoUnit.MILLIS);
                plugin.text().of(sender, "debug.update-player-shops-complete", usedTime);
            });
        });

    }

    public void printHandlerList(@NotNull CommandSender sender, String event) {
        try {
            final Class<?> clazz = Class.forName(event);
            final Method method = clazz.getMethod("getHandlerList");
            final Object[] obj = new Object[0];
            final HandlerList list = (HandlerList) method.invoke(null, obj);

            for (RegisteredListener listener1 : list.getRegisteredListeners()) {
                MsgUtil.sendDirectMessage(sender,
                        LegacyComponentSerializer.legacySection().deserialize(ChatColor.AQUA
                                + listener1.getPlugin().getName()
                                + ChatColor.YELLOW
                                + " # "
                                + ChatColor.GREEN
                                + listener1.getListener().getClass().getCanonicalName()));
            }
        } catch (Exception th) {
            MsgUtil.sendDirectMessage(sender, Component.text("ERR " + th.getMessage()).color(NamedTextColor.RED));
            plugin.logger().warn("An error has occurred while getting the HandlerList", th);
        }
    }

    @NotNull
    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender, @NotNull String commandLabel, @NotNull CommandParser parser) {
        return Collections.emptyList();
    }

}
