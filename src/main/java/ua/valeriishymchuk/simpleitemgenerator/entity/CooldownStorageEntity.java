package ua.valeriishymchuk.simpleitemgenerator.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.bukkit.entity.Player;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@ConfigSerializable
@FieldDefaults(level = AccessLevel.PUBLIC)
@Getter
public class CooldownStorageEntity {

    Map<String, Map<Integer, ItemCooldownInfo>> cooldowns = new LinkedHashMap<>();
    Map<String, Map<Integer, ItemCooldownInfo>> freeze = new LinkedHashMap<>();

    @Getter
    @ConfigSerializable
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @AllArgsConstructor
    public static class ItemCooldownInfo {

        @Setter
        Long cooldownSince;
        Map<UUID, Long> playerCooldown;

        public ItemCooldownInfo() {
            this(null, new HashMap<>());
        }


    }

}
