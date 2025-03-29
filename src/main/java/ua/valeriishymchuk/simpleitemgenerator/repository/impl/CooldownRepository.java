package ua.valeriishymchuk.simpleitemgenerator.repository.impl;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;
import ua.valeriishymchuk.simpleitemgenerator.common.config.ConfigLoader;
import ua.valeriishymchuk.simpleitemgenerator.entity.CooldownStorageEntity;
import ua.valeriishymchuk.simpleitemgenerator.repository.ICooldownRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class CooldownRepository implements ICooldownRepository {

    ConfigLoader configLoader;
    @NonFinal
    CooldownStorageEntity storage;

    @Override
    public void save() {
        configLoader.save(CooldownStorageEntity.class, "cooldowns", storage);
    }

    @Override
    public void reload() {
        storage = configLoader.loadOrSave(CooldownStorageEntity.class, "cooldowns");
    }

    @Override
    public void updateGlobal(String itemKey, int hashCode, boolean isFreeze) {
        final Map<String, Map<Integer, CooldownStorageEntity.ItemCooldownInfo>> storage = isFreeze ? this.storage.freeze : this.storage.cooldowns;
        CooldownStorageEntity.ItemCooldownInfo info = storage.computeIfAbsent(itemKey, k -> new HashMap<>())
                .computeIfAbsent(hashCode, k -> new CooldownStorageEntity.ItemCooldownInfo());
        info.setCooldownSince(System.currentTimeMillis());
    }

    @Override
    public void updatePerPlayer(String itemKey, UUID player, int hashCode, boolean isFreeze) {
        final Map<String, Map<Integer, CooldownStorageEntity.ItemCooldownInfo>> storage = isFreeze ? this.storage.freeze : this.storage.cooldowns;
        CooldownStorageEntity.ItemCooldownInfo info = storage
                .computeIfAbsent(itemKey, k -> new HashMap<>())
                .computeIfAbsent(hashCode, k -> new CooldownStorageEntity.ItemCooldownInfo());
        info.getPlayerCooldown().put(player, System.currentTimeMillis());
    }

    @Override
    public Option<Long> lastUsageGlobal(String itemKey, int hashCode, boolean isFreeze) {
        final Map<String, Map<Integer, CooldownStorageEntity.ItemCooldownInfo>> storage = isFreeze ? this.storage.freeze : this.storage.cooldowns;
        final Map<Integer, CooldownStorageEntity.ItemCooldownInfo> values = storage.get(itemKey);
        if (values == null) return Option.none();
        final CooldownStorageEntity.ItemCooldownInfo cooldowns = values.get(hashCode);
        if (cooldowns == null) return Option.none();
        return Option.of(cooldowns.getCooldownSince());
    }

    @Override
    public Option<Long> lastUsagePerPlayer(String itemKey, UUID player, int hashCode, boolean isFreeze) {
        final Map<String, Map<Integer, CooldownStorageEntity.ItemCooldownInfo>> storage = isFreeze ? this.storage.freeze : this.storage.cooldowns;
        final Map<Integer, CooldownStorageEntity.ItemCooldownInfo> values = storage.get(itemKey);
        if (values == null) return Option.none();
        final CooldownStorageEntity.ItemCooldownInfo cooldowns = values.get(hashCode);
        if (cooldowns == null) return Option.none();
        final Long playerInfo = cooldowns.getPlayerCooldown().get(player);
        return Option.of(playerInfo);
    }
}
