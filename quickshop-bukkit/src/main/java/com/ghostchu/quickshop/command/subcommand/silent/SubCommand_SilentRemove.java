package com.ghostchu.quickshop.command.subcommand.silent;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.command.CommandParser;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.permission.BuiltInShopPermission;
import com.ghostchu.quickshop.obj.QUserImpl;
import com.ghostchu.quickshop.util.PackageUtil;
import com.ghostchu.quickshop.util.logging.container.ShopRemoveLog;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SubCommand_SilentRemove extends SubCommand_SilentBase {
    private static final int CONFIRM_TIMEOUT = 5;
    private Cache<UUID, UUID> deleteConfirmation = CacheBuilder.newBuilder()
            .expireAfterWrite(CONFIRM_TIMEOUT, TimeUnit.SECONDS)
            .build();

    public SubCommand_SilentRemove(QuickShop plugin) {
        super(plugin);
    }

    @Override
    protected void doSilentCommand(@NotNull Player sender, @NotNull Shop shop, @NotNull CommandParser parser) {
        if (!shop.playerAuthorize(sender.getUniqueId(), BuiltInShopPermission.DELETE)
                && !plugin.perm().hasPermission(sender, "quickshop.other.destroy")) {
            plugin.text().of(sender, "no-permission").send();
            return;
        }
        boolean skipConfirmation = PackageUtil.parsePackageProperly("skipConfirmation").asBoolean(false);
        if (sender.getUniqueId().equals(deleteConfirmation.getIfPresent(shop.getRuntimeRandomUniqueId())) || skipConfirmation) {
            plugin.logEvent(new ShopRemoveLog(QUserImpl.createFullFilled(sender), "/quickshop silentremove command", shop.saveToInfoStorage()));
            plugin.getShopManager().deleteShop(shop);
        }else{
            deleteConfirmation.put(shop.getRuntimeRandomUniqueId(), sender.getUniqueId());
            plugin.text().of(sender,"delete-controlpanel-button-confirm", CONFIRM_TIMEOUT).send();
        }

    }
}
