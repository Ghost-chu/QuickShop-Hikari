package com.ghostchu.quickshop.command.subcommand;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.command.CommandHandler;
import com.ghostchu.quickshop.api.command.CommandParser;
import com.ghostchu.quickshop.api.shop.Shop;
import com.ghostchu.quickshop.api.shop.ShopType;
import com.ghostchu.quickshop.api.shop.permission.BuiltInShopPermission;
import com.ghostchu.quickshop.util.Util;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SubCommand_Buy implements CommandHandler<Player> {

    private final QuickShop plugin;

    public SubCommand_Buy(QuickShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onCommand(@NotNull Player sender, @NotNull String commandLabel, @NotNull CommandParser parser) {
        final Shop shop = getLookingShop(sender);
        if (shop != null) {
            if (shop.playerAuthorize(sender.getUniqueId(), BuiltInShopPermission.SET_SHOPTYPE)
                    || plugin.perm().hasPermission(sender, "quickshop.other.buy")) {
                shop.setShopType(ShopType.BUYING);
                shop.setSignText(plugin.text().findRelativeLanguages(sender));
                plugin.text().of(sender, "command.now-buying", Util.getItemStackName(shop.getItem())).send();
            } else {
                plugin.text().of(sender, "not-managed-shop").send();
            }
            return;
        }
        plugin.text().of(sender, "not-looking-at-shop").send();
    }


}
