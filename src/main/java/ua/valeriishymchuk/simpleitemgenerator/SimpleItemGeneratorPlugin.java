package ua.valeriishymchuk.simpleitemgenerator;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.exceptions.ArgumentParseException;
import cloud.commandframework.exceptions.InvalidSyntaxException;
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.SneakyThrows;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.yaml.NodeStyle;
import ua.valeriishymchuk.simpleitemgenerator.common.commands.CommandException;
import ua.valeriishymchuk.simpleitemgenerator.common.config.ConfigLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.config.builder.ConfigLoaderConfigurationBuilder;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.metrics.MetricsHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.scheduler.BukkitTaskScheduler;
import ua.valeriishymchuk.simpleitemgenerator.common.tick.TickerTime;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SemanticVersion;
import ua.valeriishymchuk.simpleitemgenerator.controller.CommandsController;
import ua.valeriishymchuk.simpleitemgenerator.controller.EventsController;
import ua.valeriishymchuk.simpleitemgenerator.controller.PacketsController;
import ua.valeriishymchuk.simpleitemgenerator.controller.TickController;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.IUpdateRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.impl.ConfigRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.impl.UpdateRepository;
import ua.valeriishymchuk.simpleitemgenerator.service.IInfoService;
import ua.valeriishymchuk.simpleitemgenerator.service.IItemService;
import ua.valeriishymchuk.simpleitemgenerator.service.impl.InfoService;
import ua.valeriishymchuk.simpleitemgenerator.service.impl.ItemService;

import java.util.function.Function;
import java.util.logging.Level;

public final class SimpleItemGeneratorPlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder
                .build(this, new PacketEventsSettings()
                        .checkForUpdates(false)));
        PacketEvents.getAPI().load();
    }

    public IConfigRepository configRepository;

    @Override
    public void onEnable() {
        if (!checkFor1132()) {
            return;
        }
        ConfigLoader configLoader = yamlLoader();
        CommandManager<CommandSender> commandManager = setupCommandManager();
        BukkitTaskScheduler taskScheduler = new BukkitTaskScheduler(this);
        configRepository = new ConfigRepository(configLoader, getLogger());
        if (!configRepository.reload()) {
            getLogger().severe("Failed to load config. Shutting down...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        PacketEvents.getAPI().init();
        IUpdateRepository updateRepository = new UpdateRepository();
        SemanticVersion currentVersion = SemanticVersion.parse(getDescription().getVersion());
        IInfoService infoService = new InfoService(updateRepository, configRepository, currentVersion);
        IItemService itemService = new ItemService(configRepository);
        new CommandsController(itemService, infoService).setupCommands(commandManager);
        TickerTime tickerTime = new TickerTime(taskScheduler);
        tickerTime.start();
        Bukkit.getPluginManager().registerEvents(new EventsController(itemService, infoService,tickerTime, taskScheduler), this);
        new TickController(itemService, taskScheduler).start();
        //PacketEvents.getAPI().getEventManager().registerListener(new PacketsController(), PacketListenerPriority.MONITOR);
        MetricsHelper.init(this);
    }

    private boolean checkFor1132() {
        if (SemanticVersion.CURRENT_MINECRAFT.isAtLeast(1, 13) && !SemanticVersion.CURRENT_MINECRAFT.isAtLeast(1, 14)) {
            getLogger().severe("SimpleItemGenerator is not supported for 1.13-1.13.2. Shutting down...");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }

    @Override
    public void onDisable() {
        MetricsHelper.shutdown();
        PacketEvents.getAPI().terminate();
    }

    private ConfigLoader yamlLoader() {
        return new ConfigLoader(
                getDataFolder(),
                ".yml",
                ConfigLoaderConfigurationBuilder.yaml()
                        .peekBuilder(b -> b.indent(2).nodeStyle(NodeStyle.BLOCK))
                        .build()
        );
    }

    @SneakyThrows
    private CommandManager<CommandSender> setupCommandManager() {
        CommandManager<CommandSender> manager = new BukkitCommandManager<>(
                this,
                CommandExecutionCoordinator.simpleCoordinator(),
                Function.identity(),
                Function.identity()
        );
        new MinecraftExceptionHandler<CommandSender>()
                .withDefaultHandlers()
                .withHandler(
                        MinecraftExceptionHandler.ExceptionType.NO_PERMISSION,
                        (sender, exception) -> configRepository.getLang().getNoPermission()
                                .replaceText("%permission%", ((NoPermissionException) exception).getMissingPermission()).bake()
                )
                .withHandler(
                        MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX,
                        (sender, exception) -> configRepository.getLang().getInvalidCommandSyntax()
                                .replaceText("%usage%", ((InvalidSyntaxException) exception).getCorrectSyntax()).bake()
                )
                .withHandler(MinecraftExceptionHandler.ExceptionType.ARGUMENT_PARSING, (sender, exception) -> {
                    ArgumentParseException argumentParseException = (ArgumentParseException) exception;
                    if (argumentParseException.getCause() instanceof CommandException) {
                        return ((CommandException) argumentParseException.getCause()).getErrorMessage();
                    }
                    if (argumentParseException.getCause() instanceof IntegerArgument.IntegerParseException) {
                        IntegerArgument.IntegerParseException integerParseException = (IntegerArgument.IntegerParseException) argumentParseException.getCause();
                        return configRepository.getLang().getInvalidIntegerError()
                                .replaceText("%number%", integerParseException.getInput())
                                .replaceText("%min%", integerParseException.getMin())
                                .replaceText("%max%", integerParseException.getMax())
                                .bake();
                    }
                    getLogger().log(Level.SEVERE, "An unknown argument error occurred", argumentParseException.getCause());
                    return configRepository.getLang().getUnknownArgumentError().replaceText("%error%", argumentParseException.getCause()).bake();
                })
                .apply(manager, s -> new Audience() {

                    @Override
                    public void sendMessage(final @NotNull Identity source, final @NotNull Component message, final @NotNull MessageType type) {
                        KyoriHelper.sendMessage(s, message);
                    }
                });
        return manager;
    }

}
