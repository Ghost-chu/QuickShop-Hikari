package com.ghostchu.quickshop.economy;

import com.ghostchu.quickshop.QuickShop;
import com.ghostchu.quickshop.api.economy.Benefit;
import com.ghostchu.quickshop.api.obj.QUser;
import com.ghostchu.quickshop.common.util.JsonUtil;
import com.ghostchu.quickshop.common.util.QuickExecutor;
import com.ghostchu.quickshop.obj.QUserImpl;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class SimpleBenefit implements Benefit {
    private final Map<QUser, Double> benefits;

    public SimpleBenefit() {
        this.benefits = new HashMap<>();
    }

    public SimpleBenefit(Map<QUser, Double> benefits) {
        this.benefits = benefits;
    }

    @NotNull
    public static SimpleBenefit deserialize(@Nullable String json) {
        if (StringUtils.isEmpty(json)) {
            return new SimpleBenefit();
        }
        Map<String, Double> map = JsonUtil.regular().fromJson(json, new TypeToken<Map<String, Double>>() {
        }.getType());
        Map<QUser, Double> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, Double> e : map.entrySet()) {
            QUser qUser = QUserImpl.deserialize(QuickShop.getInstance().getPlayerFinder(), e.getKey(), QuickExecutor.getSecondaryProfileIoExecutor());
            parsed.put(qUser, e.getValue());
        }
        return new SimpleBenefit(parsed);
    }

    @Override
    public boolean isEmpty() {
        return benefits.isEmpty();
    }

    @Override
    public double getOverflow(double newAdded) {
        double sum = 0d;
        for (Double value : benefits.values()) {
            sum += value;
        }
        sum += newAdded;
        if (sum <= 1.0d) {
            return 0.0d;
        }
        return sum - 1d;
    }

    @Override
    public void addBenefit(@NotNull QUser player, double benefit) throws BenefitOverflowException, BenefitExistsException {
        double overflow = getOverflow(benefit);
        if (overflow != 0) {
            throw new BenefitOverflowException(overflow);
        }
        if (benefits.containsKey(player)) {
            throw new BenefitExistsException();
        }
        benefits.put(player, benefit);
    }

    @Override
    public void removeBenefit(@NotNull QUser player) {
        benefits.remove(player);
    }

    @Override
    public @NotNull Map<QUser, Double> getRegistry() {
        return new HashMap<>(this.benefits);
    }

    @NotNull
    @Override
    public String serialize() {
        Map<String, Double> map = new LinkedHashMap<>();
        for (Map.Entry<QUser, Double> e : this.benefits.entrySet()) {
            map.put(e.getKey().serialize(), e.getValue());
        }
        return JsonUtil.regular().toJson(map);
    }
}
