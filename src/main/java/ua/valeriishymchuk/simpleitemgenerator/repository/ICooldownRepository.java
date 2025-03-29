package ua.valeriishymchuk.simpleitemgenerator.repository;

import io.vavr.control.Option;
import ua.valeriishymchuk.simpleitemgenerator.common.cooldown.CooldownType;

import java.util.UUID;

public interface ICooldownRepository {

    void save();
    void reload();
    void updateGlobal(String itemKey, int hashCode, boolean isFreeze);
    void updatePerPlayer(String itemKey, UUID player, int hashCode, boolean isFreeze);
    Option<Long> lastUsageGlobal(String itemKey, int hashCode, boolean isFreeze);
    Option<Long> lastUsagePerPlayer(String itemKey, UUID player, int hashCode, boolean isFreeze);

}
