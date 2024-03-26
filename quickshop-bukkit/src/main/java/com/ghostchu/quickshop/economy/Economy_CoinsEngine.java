package com.ghostchu.quickshop.economy;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.economy.NonSeparateAbstractEconomy;
import com.ghostchu.quickshop.util.economyformatter.BuiltInEconomyFormatter;
import com.ghostchu.quickshop.util.logger.Log;
import com.ghostchu.simplereloadlib.ReloadResult;
import com.ghostchu.simplereloadlib.ReloadStatus;
import lombok.ToString;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.coinsengine.api.CoinsEngineAPI;
import su.nightexpress.coinsengine.api.currency.Currency;
import su.nightexpress.coinsengine.data.impl.CoinsUser;

import java.util.Iterator;
import java.util.UUID;

@ToString
public class Economy_CoinsEngine extends NonSeparateAbstractEconomy {
    private final QuickShop plugin;
    private final BuiltInEconomyFormatter formatter;
    private boolean allowLoan;

    public Economy_CoinsEngine(@NotNull QuickShop plugin) {
        super();
        this.plugin = plugin;
        this.formatter = new BuiltInEconomyFormatter(plugin);
        plugin.getReloadManager().register(this);
        init();
        setupEconomy();
    }


    private void init() {
        this.allowLoan = plugin.getConfig().getBoolean("shop.allow-economy-loan");
    }

    private void setupEconomy() {

    }

    @Override
    public @NotNull String getName() {
        return "BuiltIn-CoinsEngine";
    }

    @Override
    public String getProviderName() {
        return "CoinsEngine";
    }

    /**
     * Callback for reloading
     *
     * @return Reloading success
     */
    @Override
    public ReloadResult reloadModule() {
        init();
        return ReloadResult.builder().status(ReloadStatus.SUCCESS).build();
    }

    @Nullable
    private Currency getCurrency(@NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return null;
        }
        if (currency == null) {
            Currency backupCur = null;
            Iterator<Currency> currencyIterator = CoinsEngineAPI.getCurrencyManager().getCurrencies().iterator();
            if (currencyIterator.hasNext()) {
                backupCur = currencyIterator.next();
            }
            return CoinsEngineAPI.getCurrencyManager().getVaultCurrency().orElse(backupCur);
        }
        return CoinsEngineAPI.getCurrency(currency);
    }


    @Override
    public boolean deposit(@NotNull String name, double amount, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return false;
        }
        CoinsUser user = CoinsEngineAPI.getUserData(name);
        if (user == null) {
            return false;
        }
        Currency cur = getCurrency(world, currency);
        if (cur == null) {
            return false;
        }
        user.addBalance(cur, amount);
        return true;
    }

    /**
     * Deposits a given amount of money from thin air to the given username.
     *
     * @param name     The exact (case insensitive) username to give money to
     * @param amount   The amount to give them
     * @param currency The currency name
     * @return True if success (Should be almost always)
     */
    @Override
    public boolean deposit(@NotNull UUID name, double amount, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return false;
        }
        CoinsUser user = CoinsEngineAPI.getUserData(name);
        if (user == null) {
            return false;
        }
        Currency cur = getCurrency(world, currency);
        if (cur == null) {
            return false;
        }
        user.addBalance(cur, amount);
        return true;
    }

    /**
     * Deposits a given amount of money from thin air to the given username.
     *
     * @param trader   The player to give money to
     * @param amount   The amount to give them
     * @param currency The currency name
     * @return True if success (Should be almost always)
     */
    @Override
    public boolean deposit(@NotNull OfflinePlayer trader, double amount, @NotNull World world, @Nullable String currency) {
        return deposit(trader.getUniqueId(), amount, world, currency);
    }

    /**
     * Formats the given number... E.g. 50.5 becomes $50.5 Dollars, or 50 Dollars 5 Cents
     *
     * @param balance  The given number
     * @param currency The currency name
     * @return The balance in human readable text.
     */
    @Override
    public String format(double balance, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return "Error";
        }
        return formatInternal(balance, currency);
    }

    @Override
    public double getBalance(@NotNull String name, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return 0.0;
        }
        CoinsUser user = CoinsEngineAPI.getUserData(name);
        if (user == null) {
            Log.debug("User " + name + " not exists in CoinsEngine database, return for 0 balance");
            return 0.0;
        }
        Currency cur = getCurrency(world, currency);
        if (cur == null) {
            Log.debug("Currency " + currency + " not exists in CoinsEngine database, return for 0 balance");
            return 0.0;
        }
        return user.getBalance(cur);
    }

    private String formatInternal(double balance, @Nullable String currency) {
        if (!isValid()) {
            return "Error";
        }

        return this.formatter.getInternalFormat(balance, currency);
    }

    /**
     * Fetches the balance of the given account name
     *
     * @param name     The name of the account
     * @param currency The currency name
     * @return Their current balance.
     */
    @Override
    public double getBalance(@NotNull UUID name, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return 0.0;
        }
        CoinsUser user = CoinsEngineAPI.getUserData(name);
        if (user == null) {
            Log.debug("User " + name + " not exists in CoinsEngine database, return for 0 balance");
            return 0.0;
        }
        Currency cur = getCurrency(world, currency);
        if (cur == null) {
            Log.debug("Currency " + currency + " not exists in CoinsEngine database, return for 0 balance");
            return 0.0;
        }
        return user.getBalance(cur);
    }

    /**
     * Fetches the balance of the given player
     *
     * @param player   The name of the account
     * @param currency The currency name
     * @return Their current balance.
     */
    @Override
    public double getBalance(@NotNull OfflinePlayer player, @NotNull World world, @Nullable String currency) {
        return getBalance(player.getUniqueId(), world, currency);
    }

    /**
     * Withdraws a given amount of money from the given username and turns it to thin air.
     *
     * @param name     The exact (case insensitive) username to take money from
     * @param amount   The amount to take from them
     * @param currency The currency name
     * @return True if success, false if they didn't have enough cash
     */
    @Override
    public boolean withdraw(@NotNull UUID name, double amount, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return false;
        }
        if (!allowLoan) {
            if (getBalance(name, world, currency) < amount) {
                return false;
            }
        }
        CoinsUser user = CoinsEngineAPI.getUserData(name);
        if (user == null) {
            return false;
        }
        Currency cur = getCurrency(world, currency);
        if (cur == null) {
            return false;
        }
        user.removeBalance(cur, amount);
        return true;
    }

    /**
     * Withdraws a given amount of money from the given username and turns it to thin air.
     *
     * @param trader   The player to take money from
     * @param amount   The amount to take from them
     * @param currency The currency name
     * @return True if success, false if they didn't have enough cash
     */
    @Override
    public boolean withdraw(@NotNull OfflinePlayer trader, double amount, @NotNull World world, @Nullable String currency) {
        return withdraw(trader.getUniqueId(), amount, world, currency);
    }

    /**
     * Gets the currency does exists
     *
     * @param currency Currency name
     * @return exists
     */
    @Override
    public boolean hasCurrency(@NotNull World world, @NotNull String currency) {
        return getCurrency(world, currency) != null;
    }

    /**
     * Gets currency supports status
     *
     * @return true if supports
     */
    @Override
    public boolean supportCurrency() {
        return true;
    }

    @Override
    public boolean withdraw(@NotNull String name, double amount, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return false;
        }
        if (!allowLoan) {
            if (getBalance(name, world, currency) < amount) {
                return false;
            }
        }
        CoinsUser user = CoinsEngineAPI.getUserData(name);
        if (user == null) {
            return false;
        }
        Currency cur = getCurrency(world, currency);
        if (cur == null) {
            return false;
        }
        user.addBalance(cur, amount);
        return true;
    }

    @Override
    public @Nullable String getLastError() {
        return "Cannot provide: CoinsEngine doesn't support enhanced error tracing.";
    }

    /**
     * Checks that this economy is valid. Returns false if it is not valid.
     *
     * @return True if this economy will work, false if it will not.
     */
    @Override
    public boolean isValid() {
        return Bukkit.getPluginManager().isPluginEnabled("CoinsEngine");
    }


    @Override
    public @NotNull Plugin getPlugin() {
        return this.plugin.getJavaPlugin();
    }


}
