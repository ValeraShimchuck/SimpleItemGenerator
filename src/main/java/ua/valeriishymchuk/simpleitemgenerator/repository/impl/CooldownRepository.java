package ua.valeriishymchuk.simpleitemgenerator.repository.impl;

import io.vavr.control.Option;
import io.vavr.control.Validation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;
import org.bukkit.Bukkit;
import ua.valeriishymchuk.simpleitemgenerator.common.config.ConfigLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.config.error.ConfigurationError;
import ua.valeriishymchuk.simpleitemgenerator.common.error.ErrorVisitor;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.entity.CooldownStorageEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class CooldownRepository {

    ConfigLoader configLoader;
    @NonFinal
    CooldownStorageEntity storage;
    ErrorVisitor errorVisitor;

    public void save() {
        configLoader.save(CooldownStorageEntity.class, "cooldowns", storage);
    }

    public boolean reload() {
        final Validation<ConfigurationError, CooldownStorageEntity> storageLoadResult;
        storageLoadResult = configLoader.loadOrSave(CooldownStorageEntity.class, "cooldowns");
        if (storageLoadResult.isInvalid()) {
            KyoriHelper.sendMessage(
                    Bukkit.getConsoleSender(),
                    "<red>[SimpleItemGenerator] Found error in cooldowns.yml:</red>"
            );
            errorVisitor.visitError(storageLoadResult.getError().asConfigException());
            return false;
        }
        storage = storageLoadResult.get();
        return true;
    }

    public void updateGlobal(String itemKey, int hashCode, boolean isFreeze) {
        final Map<String, Map<Integer, CooldownStorageEntity.ItemCooldownInfo>> storage = isFreeze ? this.storage.freeze : this.storage.cooldowns;
        CooldownStorageEntity.ItemCooldownInfo info = storage.computeIfAbsent(itemKey, k -> new HashMap<>())
                .computeIfAbsent(hashCode, k -> new CooldownStorageEntity.ItemCooldownInfo());
        info.setCooldownSince(System.currentTimeMillis());
    }

    public void updatePerPlayer(String itemKey, UUID player, int hashCode, boolean isFreeze) {
        final Map<String, Map<Integer, CooldownStorageEntity.ItemCooldownInfo>> storage = isFreeze ? this.storage.freeze : this.storage.cooldowns;
        CooldownStorageEntity.ItemCooldownInfo info = storage
                .computeIfAbsent(itemKey, k -> new HashMap<>())
                .computeIfAbsent(hashCode, k -> new CooldownStorageEntity.ItemCooldownInfo());
        info.getPlayerCooldown().put(player, System.currentTimeMillis());
    }

    public Option<Long> lastUsageGlobal(String itemKey, int hashCode, boolean isFreeze) {
        final Map<String, Map<Integer, CooldownStorageEntity.ItemCooldownInfo>> storage = isFreeze ? this.storage.freeze : this.storage.cooldowns;
        final Map<Integer, CooldownStorageEntity.ItemCooldownInfo> values = storage.get(itemKey);
        if (values == null) return Option.none();
        final CooldownStorageEntity.ItemCooldownInfo cooldowns = values.get(hashCode);
        if (cooldowns == null) return Option.none();
        return Option.of(cooldowns.getCooldownSince());
    }

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
