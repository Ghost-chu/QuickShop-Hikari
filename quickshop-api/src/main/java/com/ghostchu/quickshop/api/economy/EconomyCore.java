package com.ghostchu.quickshop.api.economy;

import com.ghostchu.quickshop.api.obj.QUser;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author netherfoam Represents an economy.
 */
public interface EconomyCore {
    /**
     * Deposits a given amount of money from thin air to the given username.
     *
     * @param obj      The object of one of type: UUID, OfflinePlayer, String
     * @param amount   The amount to give them
     * @param currency The currency name
     * @param world    The transaction world
     * @return True if success (Should be almost always)
     */
    boolean deposit(@NotNull QUser obj, double amount, @NotNull World world, @Nullable String currency);

    /**
     * Formats the given number... E.g. 50.5 becomes $50.5 Dollars, or 50 Dollars 5 Cents
     *
     * @param balance  The given number
     * @param currency The currency name
     * @param world    The transaction world
     * @return The balance in human readable text.
     */
    String format(double balance, @NotNull World world, @Nullable String currency);

    /**
     * Fetches the balance of the given account name
     *
     * @param obj      The object of one of type: UUID, OfflinePlayer, String
     * @param currency The currency name
     * @param world    The transaction world
     * @return Their current balance.
     */
    double getBalance(@NotNull QUser obj, @NotNull World world, @Nullable String currency);

    /**
     * Gets the economy processor last error message
     *
     * @return Error message or null if never happens
     */
    @Nullable
    String getLastError();

    /**
     * Getting Economy impl name
     *
     * @return Impl name
     */
    @NotNull String getName();

    /**
     * Getting Economy impl owned by
     *
     * @return Owned by
     */
    @NotNull Plugin getPlugin();

    /**
     * Gets the currency does exists
     *
     * @param currency Currency name
     * @param world    The transaction world
     * @return exists
     */
    boolean hasCurrency(@NotNull World world, @NotNull String currency);

    /**
     * Checks that this economy is valid. Returns false if it is not valid.
     *
     * @return True if this economy will work, false if it will not.
     */
    boolean isValid();

    /**
     * Gets currency supports status
     *
     * @return true if supports
     */
    boolean supportCurrency();

    /**
     * Transfers the given amount of money from Player1 to Player2
     *
     * @param from     The player who is paying money, The object of one of type: UUID, OfflinePlayer, String
     * @param to       The player who is receiving money, The object of one of type: UUID, OfflinePlayer, String
     * @param amount   The amount to transfer
     * @param currency The currency name
     * @param world    The transaction world
     * @return true if success (Payer had enough cash, receiver was able to receive the funds)
     */
    boolean transfer(@NotNull QUser from, @NotNull QUser to, double amount, @NotNull World world, @Nullable String currency);

    /**
     * Withdraws a given amount of money from the given username and turns it to thin air.
     *
     * @param obj      The object of one of type: UUID, OfflinePlayer, String
     * @param amount   The amount to take from them
     * @param currency The currency name
     * @param world    The transaction world
     * @return True if success, false if they didn't have enough cash
     */
    boolean withdraw(@NotNull QUser obj, double amount, @NotNull World world, @Nullable String currency);

}
