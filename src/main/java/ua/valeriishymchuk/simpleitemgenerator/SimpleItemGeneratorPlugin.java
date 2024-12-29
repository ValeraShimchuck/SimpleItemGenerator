package ua.valeriishymchuk.simpleitemgenerator;

import cloud.commandframework.CommandManager;
import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.SneakyThrows;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.yaml.NodeStyle;
import ua.valeriishymchuk.simpleitemgenerator.common.config.ConfigLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.config.builder.ConfigLoaderConfigurationBuilder;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.metrics.MetricsHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.scheduler.BukkitTaskScheduler;
import ua.valeriishymchuk.simpleitemgenerator.common.tick.TickerTime;
import ua.valeriishymchuk.simpleitemgenerator.controller.CommandsController;
import ua.valeriishymchuk.simpleitemgenerator.controller.EventsController;
import ua.valeriishymchuk.simpleitemgenerator.controller.TickController;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.impl.ConfigRepository;
import ua.valeriishymchuk.simpleitemgenerator.service.IItemService;
import ua.valeriishymchuk.simpleitemgenerator.service.impl.ItemService;

import java.util.function.Function;

public final class SimpleItemGeneratorPlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();

    }

    @Override
    public void onEnable() {
        //PacketEvents.getAPI().init(); // we don't actually need any packet-related stuff, only some utilities
        ConfigLoader configLoader = yamlLoader();
        CommandManager<CommandSender> commandManager = setupCommandManager();
        BukkitTaskScheduler taskScheduler = new BukkitTaskScheduler(this);
        IConfigRepository configRepository = new ConfigRepository(configLoader);
        if (!configRepository.reload()) {
            getLogger().severe("Failed to load config. Shutting down...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        IItemService itemService = new ItemService(configRepository);
        new CommandsController(itemService).setupCommands(commandManager);
        TickerTime tickerTime = new TickerTime(taskScheduler);
        tickerTime.start();
        Bukkit.getPluginManager().registerEvents(new EventsController(itemService, tickerTime), this);
        new TickController(itemService, taskScheduler).start();
        MetricsHelper.init(this);
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
                        (sender, exception) -> Component.text("You don't have permission to use this command! "  + exception.getMessage())
                )
                .withHandler(
                        MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX,
                        (sender, exception) -> Component.text("Invalid syntax! " + exception.getMessage())
                )
                .apply(manager, s -> new Audience() {
                    @Override
                    public void sendMessage(@NotNull Component message) {
                        KyoriHelper.sendMessage(s, message);
                    }
                });
        return manager;
    }

}
