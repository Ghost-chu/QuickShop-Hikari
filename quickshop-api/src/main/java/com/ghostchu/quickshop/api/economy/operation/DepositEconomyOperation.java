package com.ghostchu.quickshop.api.economy.operation;

import com.ghostchu.quickshop.api.economy.EconomyCore;
import com.ghostchu.quickshop.api.obj.QUser;
import com.ghostchu.quickshop.api.operation.Operation;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Operation about deposit money.
 */
public class DepositEconomyOperation implements Operation {
    private final QUser account;
    private final double amount;
    private final EconomyCore economyCore;
    private final World world;
    private final String currency;
    private boolean committed = false;
    private boolean rollback = false;

    public DepositEconomyOperation(@NotNull QUser account, double amount, @NotNull World world, @Nullable String currency, @NotNull EconomyCore economyCore) {
        this.account = account;
        this.amount = amount;
        this.world = world;
        this.currency = currency;
        this.economyCore = economyCore;
    }

    @Override
    public boolean commit() {
        boolean result = economyCore.deposit(account, amount, world, currency);
        if (result) {
            committed = true;
        }
        return result;
    }

    @Override
    public boolean isCommitted() {
        return this.committed;
    }

    @Override
    public boolean isRollback() {
        return this.rollback;
    }

    @Override
    public boolean rollback() {
        boolean result = economyCore.withdraw(account, amount, world, currency);
        if (result) {
            rollback = true;
        }
        return result;
    }
}
