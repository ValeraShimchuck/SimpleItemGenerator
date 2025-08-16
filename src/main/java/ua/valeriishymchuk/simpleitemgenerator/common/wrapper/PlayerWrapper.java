package ua.valeriishymchuk.simpleitemgenerator.common.wrapper;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import ua.valeriishymchuk.simpleitemgenerator.common.annotation.UsesMinecraft;

import java.util.UUID;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
public class PlayerWrapper {

    String username;
    UUID uuid;

    @UsesMinecraft
    public Option<Player> toBukkit() {
        return Option.of(Bukkit.getPlayer(uuid));
    }

    @Contract("null -> null")
    public static PlayerWrapper fromBukkit(Player player) {
        if (player == null) return null;
        return new PlayerWrapper(
                player.getName(),
                player.getUniqueId()
        );
    }
}
