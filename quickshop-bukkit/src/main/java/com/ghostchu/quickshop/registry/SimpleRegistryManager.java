package com.ghostchu.quickshop.registry;

import com.ghostchu.quickshop.api.registry.BuiltInRegistry;
import com.ghostchu.quickshop.api.registry.Registry;
import com.ghostchu.quickshop.api.registry.RegistryManager;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class SimpleRegistryManager implements RegistryManager {
    private final Map<String, Registry> REGISTRY_LIST = new ConcurrentSkipListMap<>();

    @Override
    public Registry getRegistry(BuiltInRegistry registry) {
        return getRegistry(registry.getName());
    }

    @Override
    public Registry getRegistry(String registryName) {
        return REGISTRY_LIST.get(registryName);
    }

    @Override
    public Map<String, Registry> getRegistryList() {
        return ImmutableMap.copyOf(REGISTRY_LIST);
    }

    @Override
    public void registerRegistry(String namespacedName, Registry registry) {
        if (REGISTRY_LIST.get(namespacedName) != null) {
            throw new IllegalArgumentException("Registry " + namespacedName + " already registered in RegistryManager!");
        }
        this.REGISTRY_LIST.put(namespacedName, registry);
    }

    @Override
    public void unregisterRegistry(String namespacedName) {
        this.REGISTRY_LIST.remove(namespacedName);
    }

    @Override
    @NotNull
    public Map<String, Registry> getRegistries() {
        return ImmutableMap.copyOf(REGISTRY_LIST);
    }
}
