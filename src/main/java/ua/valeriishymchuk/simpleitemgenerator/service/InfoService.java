package ua.valeriishymchuk.simpleitemgenerator.service;

import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SemanticVersion;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SigFeatureTag;
import ua.valeriishymchuk.simpleitemgenerator.entity.MainConfigEntity;
import ua.valeriishymchuk.simpleitemgenerator.entity.LangEntity;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.IUpdateRepository;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@FieldDefaults(level = AccessLevel.PRIVATE ,makeFinal = true)
@RequiredArgsConstructor
public class InfoService {

    IUpdateRepository updateRepository;
    IConfigRepository configRepository;
    SemanticVersion currentVersion;

    @NonFinal
    long lastTimeWasUsed = 0;


    private MainConfigEntity getConfig() {
        return configRepository.getConfig();
    }

    private LangEntity getLang() {
        return configRepository.getLang();
    }

    public void updatePluginActivity() {
        lastTimeWasUsed = System.currentTimeMillis();
    }

    public boolean isUsedActively() {
        return System.currentTimeMillis() - lastTimeWasUsed < 1000 * 60 * 60 * 24;
    }

    public boolean isDebug() {
        return getConfig().isDebug();
    }
    public boolean isDebugTick() {
        return getConfig().isTickDebug();
    }
    public Set<SigFeatureTag> getFeatures() {
        return getConfig().getFeatures();
    }

    public Option<Component> getMessage(Player player) {
        if (!player.isOp() || !getConfig().isSendWelcomeMessage()) return Option.none();
        return Option.some(getLang().getAdminWelcome().replaceText("%version%", currentVersion.toFullString()).bake());
    }

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

    public Component getUsage() {
        return getLang().getSigUsage()
                .replaceText("%version%", currentVersion.toFullString())
                .bake();
    }
}
