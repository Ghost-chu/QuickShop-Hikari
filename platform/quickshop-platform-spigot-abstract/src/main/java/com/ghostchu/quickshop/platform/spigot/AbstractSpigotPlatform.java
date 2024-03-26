package com.ghostchu.quickshop.platform.spigot;

import com.ghostchu.quickshop.common.util.QuickSLF4JLogger;
import com.ghostchu.quickshop.platform.Platform;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTList;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class AbstractSpigotPlatform implements Platform {
    protected final Logger logger = Logger.getLogger("QuickShop-Hikari");
    private final Plugin plugin;
    protected Map<String, String> translationMapping;
    private BukkitAudiences audience;

    public AbstractSpigotPlatform(@NotNull Plugin instance) {
        this.plugin = instance;
        //TODO use method to replace
        if (Bukkit.getPluginManager().getPlugin("NBTAPI") == null) {
            throw new IllegalStateException("Must install NBT-API if you're running on Spigot server");
        }
        //this.translationMapping = mapping;
    }

    @NotNull
    @ApiStatus.Internal
    @Deprecated
    public static String getNMSVersion() {
        // Should only works on Spigot, Paper are breaking this, do not use if possible
        String name = Bukkit.getServer().getClass().getPackage().getName();
        return name.substring(name.lastIndexOf('.') + 1);
    }

    @Override
    public @NotNull Component getDisplayName(@NotNull ItemStack stack) {
        if (stack.getItemMeta() != null) {
            return LegacyComponentSerializer.legacySection().deserialize(stack.getItemMeta().getDisplayName());
        }
        return Component.empty();
    }

    @Override
    public @NotNull Component getDisplayName(@NotNull ItemMeta meta) {
        if (meta.hasDisplayName()) {
            return LegacyComponentSerializer.legacySection().deserialize(meta.getDisplayName());
        }
        return Component.empty();
    }

    @Override
    public @NotNull Component getLine(@NotNull Sign sign, int line) {
        return LegacyComponentSerializer.legacySection().deserialize(sign.getLine(line));
    }

    @Override
    public @Nullable List<Component> getLore(@NotNull ItemStack stack) {
        if (!stack.hasItemMeta()) {
            return null;
        }
        if (!stack.getItemMeta().hasLore()) {
            return null;
        }
        return stack.getItemMeta().getLore().stream().map(LegacyComponentSerializer.legacySection()::deserialize).collect(Collectors.toList());
    }

    @Override
    public @Nullable List<Component> getLore(@NotNull ItemMeta meta) {
        if (!meta.hasLore()) {
            return null;
        }
        return meta.getLore().stream().map(LegacyComponentSerializer.legacySection()::deserialize).collect(Collectors.toList());
    }

    @Override
    public @NotNull Component getTranslation(@NotNull Material material) {
        return Component.translatable(getTranslationKey(material));
    }

    @Override
    public @NotNull Component getTranslation(@NotNull EntityType entity) {
        return Component.translatable(getTranslationKey(entity));
    }

    @Override
    public @NotNull Component getTranslation(@NotNull PotionEffectType potionEffectType) {
        return Component.translatable(getTranslationKey(potionEffectType));
    }

    @Override
    public @NotNull Component getTranslation(@NotNull Enchantment enchantment) {
        return Component.translatable(getTranslationKey(enchantment));
    }

    @Override
    public @NotNull Component getTranslation(@NotNull ItemStack itemStack) {
        return Component.translatable(getTranslationKey(itemStack));
    }

    @Override
    public @NotNull MiniMessage miniMessage() {
        return MiniMessage.miniMessage();
    }


    @Override
    public void sendMessage(@NotNull CommandSender sender, @NotNull Component component) {
        if (this.audience == null) {
            this.audience = BukkitAudiences.create(this.plugin);
        }
        //this.audience.sender(sender).sendMessage(component);
        sender.spigot().sendMessage(BungeeComponentSerializer.get().serialize(component));
    }

    @Override
    public void shutdown() {
        if (this.audience != null) {
            this.audience.close();
        }
    }

    @Override
    public void sendSignTextChange(@NotNull Player player, @NotNull Sign sign, boolean glowing, @NotNull List<Component> components) {
        //player.sendSignChange(sign.getLocation(), components.stream().map(com -> LegacyComponentSerializer.legacySection().serialize(com)).toArray(String[]::new));
    }

    @Override
    public void setDisplayName(@NotNull ItemStack stack, @Nullable Component component) {
        if (stack.getItemMeta() == null) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (component == null) {
            meta.setDisplayName(null);
        } else {
            meta.setDisplayName(LegacyComponentSerializer.legacySection().serialize(component));
        }
        stack.setItemMeta(meta);
    }

    @Override
    public void setDisplayName(@NotNull Item stack, @Nullable Component component) {
        if (component == null) {
            stack.setCustomName(null);
        } else {
            stack.setCustomName(LegacyComponentSerializer.legacySection().serialize(component));
        }
    }

    @Override
    public void setLines(@NotNull Sign sign, @NotNull List<Component> component) {
        String EMPTY_LINE_NBT = "{\"text\":\"\"}";
        ReadWriteNBT root = NBT.createNBTObject();
        ReadWriteNBT front_text = root.getOrCreateCompound("front_text"); // > 1.20
        ReadWriteNBTList<String> messages = front_text.getStringList("messages"); // > 1.20
        for (int i = 0; i < 4; i++) {
            Component com = component.get(i);
            String json = com == null ? EMPTY_LINE_NBT : GsonComponentSerializer.gson().serialize(com);
            root.setString("Text" + (i + 1), json);
            messages.add(json); // > 1.20
        }
        // ==== Apply the changes ====
        NBT.modify(sign, nbt -> {
            nbt.mergeCompound(root);
        });
    }

    @Override
    public void setLore(@NotNull ItemStack stack, @NotNull Collection<Component> components) {
        if (!stack.hasItemMeta()) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        meta.setLore(components.stream().map(LegacyComponentSerializer.legacySection()::serialize).collect(Collectors.toList()));
        stack.setItemMeta(meta);
    }

    @Override
    public void updateTranslationMappingSection(@NotNull Map<String, String> mapping) {
        this.translationMapping = mapping;
    }

    @Override
    @NotNull
    public org.slf4j.Logger getSlf4jLogger(@NotNull Plugin parent) {
        return QuickSLF4JLogger.initializeLoggerService(parent.getLogger());
    }

    private String postProcessingTranslationKey(String key) {
        return this.translationMapping.getOrDefault(key, key);
    }
}
