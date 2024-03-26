package com.ghostchu.quickshop.platform.spigot.v1_19_3;

import com.ghostchu.quickshop.platform.Platform;
import com.ghostchu.quickshop.platform.Util;
import com.ghostchu.quickshop.platform.spigot.AbstractSpigotPlatform;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.event.HoverEvent;
import net.minecraft.nbt.CompoundTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.craftbukkit.v1_19_R3.CraftServer;
import org.bukkit.craftbukkit.v1_19_R3.enchantments.CraftEnchantment;
import org.bukkit.craftbukkit.v1_19_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_19_R3.potion.CraftPotionEffectType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeWrapper;
import org.jetbrains.annotations.NotNull;

public class Spigot1194Platform extends AbstractSpigotPlatform implements Platform {

    public Spigot1194Platform(@NotNull Plugin plugin) {
        super(plugin);
    }

    @Override
    public @NotNull HoverEvent<HoverEvent.ShowItem> getItemStackHoverEvent(@NotNull ItemStack stack) {
        NamespacedKey namespacedKey = stack.getType().getKey();
        Key key = Key.key(namespacedKey.toString());
        BinaryTagHolder holder;
        if (Util.methodExists(BinaryTagHolder.class, "binaryTagHolder")) {
            holder = BinaryTagHolder.binaryTagHolder(CraftItemStack.asNMSCopy(stack).save(new CompoundTag()).toString());
        } else {
            //noinspection UnstableApiUsage
            holder = BinaryTagHolder.of(CraftItemStack.asNMSCopy(stack).save(new CompoundTag()).toString());
        }
        return HoverEvent.showItem(key, stack.getAmount(), holder);
    }

    @Override
    public @NotNull String getMinecraftVersion() {
        try {
            return ((CraftServer) Bukkit.getServer()).getServer().getServerVersion();
        } catch (Exception e) {
            return super.getMinecraftVersion();
        }
    }

    @Override
    public void registerCommand(@NotNull String prefix, @NotNull Command command) {
        ((CraftServer) Bukkit.getServer()).getCommandMap().register(prefix, command);
        command.register(((CraftServer) Bukkit.getServer()).getCommandMap());
        ((CraftServer) Bukkit.getServer()).syncCommands();
    }

    @Override
    public @NotNull String getTranslationKey(@NotNull Material material) {
        if (material.isBlock()) {
            //noinspection deprecation
            return postProcessingTranslationKey(Bukkit.getUnsafe().getBlockTranslationKey(material));
        } else {
            //noinspection deprecation
            return postProcessingTranslationKey(Bukkit.getUnsafe().getItemTranslationKey(material));
        }
    }

    private String postProcessingTranslationKey(String key) {
        return this.translationMapping.getOrDefault(key, key);
    }

    @Override
    public @NotNull String getTranslationKey(@NotNull EntityType type) {
        //noinspection deprecation
        return postProcessingTranslationKey(Bukkit.getUnsafe().getTranslationKey(type));
    }

    @Override
    public @NotNull String getTranslationKey(@NotNull PotionEffectType potionEffectType) {
        if(potionEffectType instanceof PotionEffectTypeWrapper wrapper){
            potionEffectType = wrapper.getType();
        }
        CraftPotionEffectType craftPotionEffectType = (CraftPotionEffectType) potionEffectType;
        return postProcessingTranslationKey(craftPotionEffectType.getHandle().getDescriptionId());
    }

    @Override
    public @NotNull String getTranslationKey(@NotNull Enchantment enchantment) {
        CraftEnchantment craftEnchantment = (CraftEnchantment) enchantment;
        return postProcessingTranslationKey(craftEnchantment.getHandle().getDescriptionId());
    }
    @Override
    public @NotNull String getTranslationKey(@NotNull ItemStack stack) {
        return postProcessingTranslationKey(stack.getTranslationKey());
    }
}
