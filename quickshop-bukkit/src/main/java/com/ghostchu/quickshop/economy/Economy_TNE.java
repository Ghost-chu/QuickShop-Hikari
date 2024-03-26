package com.ghostchu.quickshop.economy;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.economy.NonSeparateAbstractEconomy;
import com.ghostchu.simplereloadlib.ReloadResult;
import com.ghostchu.simplereloadlib.ReloadStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.tnemc.core.EconomyManager;
import net.tnemc.core.TNECore;
import net.tnemc.core.account.Account;
import net.tnemc.core.account.holdings.HoldingsEntry;
import net.tnemc.core.account.holdings.modify.HoldingsModifier;
import net.tnemc.core.actions.source.PluginSource;
import net.tnemc.core.api.TNEAPI;
import net.tnemc.core.currency.Currency;
import net.tnemc.core.currency.format.CurrencyFormatter;
import net.tnemc.core.transaction.Transaction;
import net.tnemc.core.transaction.TransactionResult;
import net.tnemc.core.utils.exceptions.InvalidTransactionException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@ToString
public class Economy_TNE extends NonSeparateAbstractEconomy {

    private final QuickShop plugin;
    private boolean allowLoan;

    @Getter
    @Setter
    private TNEAPI api;

    public Economy_TNE(@NotNull QuickShop plugin) {
        super();
        this.plugin = plugin;
        plugin.getReloadManager().register(this);
        init();
        setupEconomy();
    }

    private void init() {
        this.allowLoan = plugin.getConfig().getBoolean("shop.allow-economy-loan");
    }

    private void setupEconomy() {
        this.api = TNECore.api();
    }

    @Nullable
    private Currency getCurrency(@NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return null;
        }

        if (currency != null) {
            final Optional<Currency> currencyOpt = TNECore.eco().currency().findCurrency(currency);
            return currencyOpt.orElseGet(() -> TNECore.eco().currency().getDefaultCurrency(world.getName()));
        }
        return TNECore.eco().currency().getDefaultCurrency(world.getName());
    }

    private boolean runTransaction(final Account account, final Currency currency, final String world, final BigDecimal amount, final String type, final boolean counter) {

        //Set up our holdings' modifier.
        final HoldingsModifier modifier = new HoldingsModifier(world,
                currency.getUid(),
                amount);

        //Setup our transaction.
        final Transaction transaction = new Transaction(type)
                .to(account, ((counter) ? modifier.counter() : modifier))
                .processor(EconomyManager.baseProcessor())
                .source(new PluginSource("QuickShop"));
        try {

            //Process our transaction.
            final TransactionResult result = transaction.process();
            if (result.isSuccessful()) {
                return true;
            }

        } catch (InvalidTransactionException ignore) {
            return false;
        }
        return false;
    }

    /**
     * Deposits a given amount of money from thin air to the account with the given name.
     *
     * @param name     The exact (case-insensitive) username to give money to
     * @param amount   The amount to give them
     * @param currency The currency name
     * @return True if success (Should be almost always)
     */
    @Override
    public boolean deposit(@NotNull String name, double amount, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return false;
        }
        final Optional<Account> account = TNECore.api().getAccount(name);
        final Optional<Currency> currencyOpt = TNECore.eco().currency().findCurrency(currency);

        if (account.isPresent() && currencyOpt.isPresent()) {

            return runTransaction(account.get(), currencyOpt.get(), world.getName(), BigDecimal.valueOf(amount), "give", false);
        }
        return false;
    }

    /**
     * Deposits a given amount of money from thin air to the account with the given {@link UUID unique identifier}.
     *
     * @param name     The {@link UUID unique identifier} to give money to
     * @param amount   The amount to give them
     * @param currency The currency name
     * @return True if success (Should be almost always)
     */
    @Override
    public boolean deposit(@NotNull UUID name, double amount, @NotNull World world, @Nullable String currency) {
        deposit(Bukkit.getOfflinePlayer(name), amount, world, currency);
        return false;
    }

    /**
     * Deposits a given amount of money from thin air to the given {@link OfflinePlayer player}.
     *
     * @param trader   The {@link OfflinePlayer player} to give money to
     * @param amount   The amount to give them
     * @param currency The currency name
     * @return True if success (Should be almost always)
     */
    @Override
    public boolean deposit(@NotNull OfflinePlayer trader, double amount, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return false;
        }
        //We should forward this method to the UUID one instead of doing another OfflinePlayer lookup here just to send it to the TNE UUID method.
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

        final Currency currencyObj = getCurrency(world, currency);

        if (currencyObj == null) {
            return "Error";
        }

        return CurrencyFormatter.format(null, new HoldingsEntry(TNECore.eco().region().defaultRegion(),
                currencyObj.getUid(), BigDecimal.valueOf(balance), EconomyManager.NORMAL));
    }

    @Override
    public double getBalance(@NotNull String name, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return 0.0;
        }
        final Optional<Account> account = TNECore.api().getAccount(name);
        final Optional<Currency> currencyOpt = TNECore.eco().currency().findCurrency(currency);

        if (account.isPresent() && currencyOpt.isPresent()) {
            return account.get().getHoldingsTotal(world.getName(), currencyOpt.get().getUid()).doubleValue();
        }
        return 0.0;
    }


    /**
     * Fetches the balance of the given account {@link UUID unique identifier}.
     *
     * @param name     The {@link UUID unique identifier} of the account
     * @param currency The currency name
     * @return Their current balance.
     */
    @Override
    public double getBalance(@NotNull UUID name, @NotNull World world, @Nullable String currency) {
        return getBalance(name.toString(), world, currency);
    }

    /**
     * Fetches the balance of the given {@link OfflinePlayer player}
     *
     * @param player   The {@link OfflinePlayer player}
     * @param currency The currency name
     * @return Their current balance.
     */
    @Override
    public double getBalance(@NotNull OfflinePlayer player, @NotNull World world, @Nullable String currency) {
        return getBalance(player.getUniqueId().toString(), world, currency);
    }

    @Override
    public @Nullable String getLastError() {
        return "Cannot provide: TNE not supports enhanced error tracing.";
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return this.plugin.getJavaPlugin();
    }

    /**
     * Checks if the currency exists.
     *
     * @param currency Currency name
     * @return exists
     */
    @Override
    public boolean hasCurrency(@NotNull World world, @NotNull String currency) {
        return TNECore.eco().currency().findCurrency(currency).isPresent();
    }

    /**
     * Checks that this economy is valid. Returns false if it is not valid.
     *
     * @return True if this economy will work, false if it will not.
     */
    @Override
    public boolean isValid() {
        return this.api != null && TNECore.instance() != null;
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

    /**
     * Withdraws a given amount of money from the given username and turns it to thin air.
     *
     * @param name     The exact (case-insensitive) username to take money from
     * @param amount   The amount to take from them
     * @param currency The currency name
     * @return True if success, false if they didn't have enough cash
     */
    @Override
    public boolean withdraw(@NotNull String name, double amount, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return false;
        }

        final Optional<Account> account = TNECore.api().getAccount(name);
        final Optional<Currency> currencyOpt = TNECore.eco().currency().findCurrency(currency);

        if (account.isPresent() && currencyOpt.isPresent()) {

            return runTransaction(account.get(), currencyOpt.get(), world.getName(), BigDecimal.valueOf(amount), "take", true);
        }
        return false;
    }

    /**
     * Withdraws a given amount of money from the given {@link UUID unique identifier} and turns it to thin air.
     *
     * @param name     The exact {@link UUID unique identifier} of the user.
     * @param amount   The amount to take from them
     * @param currency The currency name
     * @return True if success, false if they didn't have enough cash
     */
    @Override
    public boolean withdraw(@NotNull UUID name, double amount, @NotNull World world, @Nullable String currency) {
        return withdraw(name.toString(), amount, world, currency);
    }

    /**
     * Withdraws a given amount of money from the given {@link OfflinePlayer player} and turns it to thin air.
     *
     * @param trader   The {@link OfflinePlayer player} to take money from
     * @param amount   The amount to take from them
     * @param currency The currency name
     * @return True if success, false if they didn't have enough cash
     */
    @Override
    public boolean withdraw(@NotNull OfflinePlayer trader, double amount, @NotNull World world, @Nullable String currency) {
        return withdraw(trader.getUniqueId().toString(), amount, world, currency);
    }

    @Override
    public @NotNull String getName() {
        return "BuiltIn-TNE";
    }

    @Override
    public String getProviderName() {
        return "TNE";
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
}
