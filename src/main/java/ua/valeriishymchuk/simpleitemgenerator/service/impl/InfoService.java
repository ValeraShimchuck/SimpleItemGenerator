package ua.valeriishymchuk.simpleitemgenerator.service.impl;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SemanticVersion;
import ua.valeriishymchuk.simpleitemgenerator.entity.ConfigEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.LangEntity;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.IUpdateRepository;
import ua.valeriishymchuk.simpleitemgenerator.service.IInfoService;

import java.util.concurrent.CompletableFuture;

@FieldDefaults(level = AccessLevel.PRIVATE ,makeFinal = true)
@RequiredArgsConstructor
public class InfoService implements IInfoService {

    IUpdateRepository updateRepository;
    IConfigRepository configRepository;
    SemanticVersion currentVersion;


    private ConfigEntity getConfig() {
        return configRepository.getConfig();
    }

    private LangEntity getLang() {
        return configRepository.getLang();
    }

    @Override
    public Option<Component> getMessage(Player player) {
        if (!player.isOp() || !getConfig().isSendWelcomeMessage()) return Option.none();
        return Option.some(getLang().getAdminWelcome().replaceText("%version%", currentVersion.toFullString()).bake());
    }

    @Override
    public CompletableFuture<Option<Component>> getNewUpdateMessage(Player player) {
        if (!player.isOp() || !getConfig().isCheckForUpdates()) return CompletableFuture.completedFuture(Option.none());
        return updateRepository.getLatestPluginVersion().thenApply(version -> {
            if (currentVersion.isAtLeast(version)) return Option.none();
            return Option.some(getLang().getNewUpdateVersion()
                            .replaceText("%new_version%", version.toFullString())
                            .replaceText("%current_version%", currentVersion.toFullString())
                    .bake());
        });
    }

    @Override
    public Component getUsage() {
        return getLang().getSigUsage()
                .replaceText("%version%", currentVersion.toFullString())
                .bake();
    }
}
