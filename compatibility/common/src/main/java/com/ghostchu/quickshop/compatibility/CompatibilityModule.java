package com.ghostchu.quickshop.compatibility;

import com.ghostchu.quickshop.api.QuickShopAPI;
import com.ghostchu.quickshop.api.event.QSConfigurationReloadEvent;
import com.ghostchu.quickshop.api.obj.QUser;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.common.util.CommonUtil;
import com.ghostchu.quickshop.obj.QUserImpl;
import com.ghostchu.quickshop.util.logging.container.ShopRemoveLog;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class CompatibilityModule extends JavaPlugin implements Listener {
    private QuickShopAPI api;

    public QuickShopAPI getApi() {
        return api;
    }

    public List<Shop> getShops(@NotNull String worldName, int minX, int minZ, int maxX, int maxZ) {
        List<Shop> shopsList = new ArrayList<>();
        for (int x = minX >> 4; x <= maxX >> 4; x++) {
            for (int z = minZ >> 4; z <= maxZ >> 4; z++) {
                Map<Location, Shop> shops = this.api.getShopManager().getShops(worldName, x, z);
                if (shops != null) {
                    shopsList.addAll(shops.values());
                }
            }
        }
        BoundingBox boundingBox = new BoundingBox(minX, Integer.MIN_VALUE, minZ, maxX, Integer.MAX_VALUE, maxZ);
        return shopsList.stream().filter(s -> boundingBox.contains(s.getLocation().toVector())).toList();
    }

    public List<Shop> getShops(@NotNull String worldName, int chunkX, int chunkZ) {
        List<Shop> shopsList = new ArrayList<>();
        for (int x = chunkX; x <= chunkX; x++) {
            for (int z = chunkZ; z <= chunkZ; z++) {
                Map<Location, Shop> shops = this.api.getShopManager().getShops(worldName, x, z);
                if (shops != null) {
                    shopsList.addAll(shops.values());
                }
            }
        }
        return shopsList;
    }

    @Override
    public void onLoad() {
        try {
            saveDefaultConfig();
        } catch (IllegalArgumentException ignored) {
        }
        this.api = QuickShopAPI.getInstance();
        getLogger().info("Loading up...");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        HandlerList.unregisterAll((Plugin) this);
        getLogger().info("Unloading...");
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        try {
            saveDefaultConfig();
        } catch (IllegalArgumentException ignored) {
        }
        this.api = QuickShopAPI.getInstance();
        Bukkit.getPluginManager().registerEvents(this, this);
        init();
        getLogger().info("Enabling...");
    }

    public abstract void init();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onQuickShopReload(QSConfigurationReloadEvent event) {
        reloadConfig();
        init();
        getLogger().info("Reloading configuration...");
    }

    public void recordDeletion(@Nullable QUser qUser, @NotNull Shop shop, @NotNull String reason) {
        if (qUser == null) {
            qUser = QUserImpl.createFullFilled(CommonUtil.getNilUniqueId(), "UNKNOWN", false);
        }
        this.api.logEvent(new ShopRemoveLog(qUser, reason, shop.saveToInfoStorage()));
    }
}
