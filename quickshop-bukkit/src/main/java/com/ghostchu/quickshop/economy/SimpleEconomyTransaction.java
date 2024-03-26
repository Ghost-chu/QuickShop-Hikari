package com.ghostchu.quickshop.economy;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.economy.Benefit;
import com.ghostchu.quickshop.api.economy.EconomyCore;
import com.ghostchu.quickshop.api.economy.EconomyTransaction;
import com.ghostchu.quickshop.api.economy.operation.DepositEconomyOperation;
import com.ghostchu.quickshop.api.economy.operation.WithdrawEconomyOperation;
import com.ghostchu.quickshop.api.event.EconomyTransactionEvent;
import com.ghostchu.quickshop.api.obj.QUser;
import com.ghostchu.quickshop.api.operation.Operation;
import com.ghostchu.quickshop.common.util.CalculateUtil;
import com.ghostchu.quickshop.common.util.JsonUtil;
import com.ghostchu.quickshop.util.logger.Log;
import com.ghostchu.quickshop.util.performance.PerfMonitor;
import lombok.Builder;
import lombok.ToString;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

/**
 * Transaction
 * A secure way to transfer the money between players.
 * Support rollback :)
 */
@ToString
public class SimpleEconomyTransaction implements EconomyTransaction {
    @JsonUtil.Hidden
    private final QuickShop plugin = QuickShop.getInstance();
    private final Deque<Operation> processingStack = new LinkedList<>();
    @Nullable
    private QUser from;
    @Nullable
    private QUser to;
    private double amount;
    @NotNull
    @JsonUtil.Hidden
    private EconomyCore core;
    private double amountAfterTax;
    private double tax;
    @Nullable
    private QUser taxer;
    private boolean allowLoan;
    private World world;
    @Nullable
    private String currency;
    @Nullable
    private String lastError = null;

    private Benefit benefit;


    /**
     * Create a transaction
     *
     * @param from        The account that money from, but null will be ignored.
     * @param to          The account that money to, but null will be ignored.
     * @param core        economy core
     * @param allowLoan   allow loan?
     * @param amount      the amount of money
     * @param taxAccount  tax account
     * @param taxModifier tax modifier
     */

    @Builder
    public SimpleEconomyTransaction(@Nullable QUser from, @Nullable QUser to, double amount, double taxModifier, @Nullable QUser taxAccount, EconomyCore core, Boolean allowLoan, @NotNull World world, @Nullable String currency, boolean neverFail, @Nullable Benefit benefit) {
        this.from = from;
        this.to = to;
        this.core = core == null ? QuickShop.getInstance().getEconomy() : core;
        this.amount = amount;
        this.taxer = taxAccount;
        this.allowLoan = Objects.requireNonNullElseGet(allowLoan, () -> plugin.getConfig().getBoolean("shop.allow-economy-loan", false));
        this.world = world;
        this.currency = currency;
        if (this.currency == null) {
            this.currency = plugin.getCurrency();
        }
        this.benefit = benefit;
        if (this.benefit == null) {
            this.benefit = new SimpleBenefit();
        }
        if (Double.doubleToLongBits(taxModifier) != Double.doubleToLongBits(0.0d)) { //Calc total money and apply tax
            this.amountAfterTax = CalculateUtil.multiply(CalculateUtil.subtract(1, taxModifier), amount);
        } else {
            this.amountAfterTax = amount;
        }
        this.tax = CalculateUtil.subtract(amount, amountAfterTax); //Calc total tax
        if (from == null && to == null) {
            lastError = "From and To cannot be null in same time.";
            throw new IllegalArgumentException("From and To cannot be null in same time.");
        }
        new EconomyTransactionEvent(this).callEvent();
    }

    /**
     * Checks this transaction can be finished
     *
     * @return The transaction can be finished (had enough money)
     */
    @Override
    public boolean checkBalance() {
        return from == null || (core.getBalance(from, world, currency) >= amount) || allowLoan;
    }

    @Override
    public @Nullable QUser getFrom() {
        return from;
    }

    @Override
    public void setFrom(@Nullable QUser from) {
        this.from = from;
    }

    /**
     * Commit the transaction with callback
     *
     * @param callback The result callback
     * @return The transaction success.
     */
    @Override
    public boolean commit(@NotNull TransactionCallback callback) {
        Log.transaction("Transaction begin: Regular Commit --> " + from + " => " + to + "; Amount: " + amount + " Total(after tax): " + amountAfterTax + " Tax: " + tax + ", EconomyCore: " + core.getName());
        if (!callback.onCommit(this)) {
            this.lastError = "Plugin cancelled this transaction.";
            return false;
        }
        if (!checkBalance()) {
            this.lastError = "From hadn't enough money";
            callback.onFailed(this);
            return false;
        }
        if (from != null && !this.executeOperation(new WithdrawEconomyOperation(from, amount, world, currency, core))) {
            this.lastError = "Failed to withdraw " + amount + " from player " + from + " account. LastError: " + core.getLastError();
            callback.onFailed(this);
            return false;
        }
        if (benefit.isEmpty()) {
            if (to != null && !this.executeOperation(new DepositEconomyOperation(to, amountAfterTax, world, currency, core))) {
                this.lastError = "Failed to deposit " + amountAfterTax + " to player " + to + " account. LastError: " + core.getLastError();
                callback.onFailed(this);
                return false;
            }
            callback.onSuccess(this);
        } else {
            // Process benefit deposit
            Log.transaction("Benefit processing per-player...");
            double payout = 0d;
            for (Map.Entry<QUser, Double> entry : benefit.getRegistry().entrySet()) {
                payout += entry.getValue();
                Log.transaction("Benefit for " + entry.getKey() + ", value: " + entry.getValue() + ". Payout = " + payout);
                if (!this.executeOperation(new DepositEconomyOperation(entry.getKey(), amountAfterTax * entry.getValue(), world, currency, core))) {
                    this.lastError = "Failed to deposit " + amountAfterTax * entry.getValue() + " to player " + to + " account. LastError: " + core.getLastError();
                    callback.onFailed(this);
                    return false;
                }
            }
            double ownerCanGet = CalculateUtil.multiply(amountAfterTax, (1 - payout));
            Log.transaction("Benefit for owner remaining: " + ownerCanGet);
            if (ownerCanGet > 0) {
                if (to != null && !this.executeOperation(new DepositEconomyOperation(to, ownerCanGet, world, currency, core))) {
                    this.lastError = "Failed to deposit " + ownerCanGet + " to player " + to + " account. LastError: " + core.getLastError();
                    callback.onFailed(this);
                    return false;
                }
            }
            callback.onSuccess(this);
        }
        if (tax > 0 && taxer != null && !this.executeOperation(new DepositEconomyOperation(taxer, tax, world, currency, core))) {
            this.lastError = "Failed to deposit tax account: " + tax + ". LastError: " + core.getLastError();
            callback.onTaxFailed(this);
            //Tax never should failed.
        }
        return true;
    }

    @Override
    public @Nullable QUser getTo() {
        return to;
    }

    @Override
    public void setTo(@Nullable QUser to) {
        this.to = to;
    }

    @Override
    public double getAmount() {
        return amount;
    }

    @Override
    public void setAmount(double amount) {
        this.amount = amount;
    }

    @Override
    public double getAmountAfterTax() {
        return amountAfterTax;
    }

    @Override
    public void setAmountAfterTax(double amountAfterTax) {
        this.amountAfterTax = amountAfterTax;
    }

    @Override
    public @NotNull EconomyCore getCore() {
        return core;
    }

    @Override
    public void setCore(@NotNull EconomyCore core) {
        this.core = core;
    }

    @Override
    public @NotNull Deque<Operation> getProcessingStack() {
        return processingStack;
    }

    @Override
    public @Nullable String getCurrency() {
        return currency;
    }

    @Override
    public void setCurrency(@Nullable String currency) {
        this.currency = currency;
    }

    @Override
    public @Nullable QUser getTaxer() {
        return taxer;
    }

    @Override
    public void setTaxer(@Nullable QUser taxer) {
        this.taxer = taxer;
    }

    @Override
    @NotNull
    public World getWorld() {
        return world;
    }

    @Override
    public void setWorld(@NotNull World world) {
        this.world = world;
    }

    @Nullable
    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public void setLastError(@NotNull String lastError) {
        this.lastError = lastError;
    }

    @Override
    public void setAllowLoan(boolean allowLoan) {
        this.allowLoan = allowLoan;
    }

    /**
     * Commit the transaction by the Fail-Safe way
     * Automatic rollback when commit failed
     *
     * @return The transaction success.
     */
    @Override
    public boolean failSafeCommit() {
        Log.transaction("Transaction begin: FailSafe Commit --> " + from + " => " + to + "; Amount: " + amount + ", EconomyCore: " + core.getName());
        boolean result = commit();
        if (!result) {
            Log.transaction(Level.WARNING, "Fail-safe commit failed, starting rollback: " + lastError);
            rollback(true);
        }
        return result;
    }

    /**
     * Commit the transaction
     *
     * @return The transaction success.
     */
    @Override
    public boolean commit() {
        try (PerfMonitor ignored = new PerfMonitor("Economy Transaction - Commit")) {
            return this.commit(new SimpleTransactionCallback() {
                @Override
                public void onSuccess(@NotNull SimpleEconomyTransaction economyTransaction) {
                }
            });
        }
    }

    /**
     * Getting the tax in this transaction
     *
     * @return The tax in this transaction
     */
    @Override
    public double getTax() {
        return tax;
    }

    @Override
    public void setTax(double tax) {
        this.tax = tax;
    }

    /**
     * Rolling back the transaction
     *
     * @param continueWhenFailed Continue when some parts of the rollback fails.
     * @return A list contains all steps executed. If "continueWhenFailed" is false, it only contains all success steps before hit the error. Else all.
     */
    @SuppressWarnings("UnusedReturnValue")
    @NotNull
    @Override
    public List<Operation> rollback(boolean continueWhenFailed) {
        try (PerfMonitor ignored = new PerfMonitor("Economy Transaction - Rollback")) {
            List<Operation> operations = new ArrayList<>();
            while (!processingStack.isEmpty()) {
                Operation operation = processingStack.pop();
                if (!operation.isCommitted()) {
                    continue;
                }
                if (operation.isRollback()) {
                    continue;
                }
                try {
                    boolean result = operation.rollback();
                    if (!result) {
                        if (continueWhenFailed) {
                            operations.add(operation);
                            continue;
                        } else {
                            break;
                        }
                    }
                    operations.add(operation);
                } catch (Exception exception) {
                    if (continueWhenFailed) {
                        operations.add(operation);
                        plugin.logger().warn("Failed to rollback transaction: {}; Operation: {}; Transaction: {}; Skipping...", core.getLastError(), operation, this);
                    } else {
                        plugin.logger().warn("Failed to rollback transaction: {}; Operation: {}; Transaction: {}", core.getLastError(), operation, this);
                        break;
                    }
                }
            }
            return operations;
        }
    }

    private boolean executeOperation(@NotNull Operation operation) {
        if (operation.isCommitted()) {
            throw new IllegalStateException("Operation already committed");
        }
        if (operation.isRollback()) {
            throw new IllegalStateException("Operation already rolled back, you must create another new operation.");
        }
        try {
            boolean result = operation.commit();
            if (!result) {
                return false;
            }
            processingStack.push(operation);
            return true;
        } catch (Exception exception) {
            plugin.logger().warn("Failed to execute operation: {}; Operation: {}; Transaction: {}", core.getLastError(), operation, this);
            this.lastError = "Failed to execute operation: " + core.getLastError() + "; Operation: " + operation;
            return false;
        }
    }

    public interface SimpleTransactionCallback extends TransactionCallback {
        /**
         * Calling while Transaction commit
         *
         * @param economyTransaction Transaction
         * @return Does commit event has been cancelled
         */
        default boolean onCommit(@NotNull SimpleEconomyTransaction economyTransaction) {
            return true;
        }

        /**
         * Calling while Transaction commit failed
         * Use EconomyTransaction#getLastError() to getting reason
         * Use EconomyTransaction#getSteps() to getting the fail step
         *
         * @param economyTransaction Transaction
         */
        default void onFailed(@NotNull SimpleEconomyTransaction economyTransaction) {
            Log.transaction(Level.WARNING, "Transaction failed: " + economyTransaction.getLastError() + ", transaction: " + economyTransaction);
        }

        /**
         * Calling while Transaction commit successfully
         *
         * @param economyTransaction Transaction
         */
        default void onSuccess(@NotNull SimpleEconomyTransaction economyTransaction) {
            Log.transaction("Transaction succeed: " + economyTransaction);
        }

        /**
         * Calling while Tax processing failed
         * Use EconomyTransaction#getLastError() to getting reason
         * Use EconomyTransaction#getSteps() to getting the fail step
         *
         * @param economyTransaction Transaction
         */
        default void onTaxFailed(@NotNull SimpleEconomyTransaction economyTransaction) {
            Log.transaction(Level.WARNING, "Tax Transaction failed: " + economyTransaction.getLastError() + ", transaction: " + economyTransaction);
        }

    }


}
