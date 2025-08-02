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
import com.github.retrooper.packetevents.settings.PacketEventsSettings;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import me.arcaniax.hdb.api.DatabaseLoadEvent;
import ua.valeriishymchuk.libs.net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.yaml.NodeStyle;
import ua.valeriishymchuk.simpleitemgenerator.api.SimpleItemGenerator;
import ua.valeriishymchuk.simpleitemgenerator.api.event.SimpleItemGeneratorLoadEvent;
import ua.valeriishymchuk.simpleitemgenerator.common.commands.CommandException;
import ua.valeriishymchuk.simpleitemgenerator.common.config.ConfigLoader;
import ua.valeriishymchuk.simpleitemgenerator.common.config.builder.ConfigLoaderConfigurationBuilder;
import ua.valeriishymchuk.simpleitemgenerator.common.config.serializer.nbt.v2.CompoundBinaryTagTypeSerializer;
import ua.valeriishymchuk.simpleitemgenerator.common.error.ErrorVisitor;
import ua.valeriishymchuk.simpleitemgenerator.common.item.NBTCustomItem;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.metrics.MetricsHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.scheduler.BukkitTaskScheduler;
import ua.valeriishymchuk.simpleitemgenerator.common.support.HeadDatabaseSupport;
import ua.valeriishymchuk.simpleitemgenerator.common.tick.TickTimer;
import ua.valeriishymchuk.simpleitemgenerator.common.version.SemanticVersion;
import ua.valeriishymchuk.simpleitemgenerator.controller.CommandsController;
import ua.valeriishymchuk.simpleitemgenerator.controller.EventsController;
import ua.valeriishymchuk.simpleitemgenerator.controller.TickController;
import ua.valeriishymchuk.simpleitemgenerator.repository.IConfigRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.IUpdateRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.impl.ConfigRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.impl.CooldownRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.impl.ItemRepository;
import ua.valeriishymchuk.simpleitemgenerator.repository.impl.UpdateRepository;
import ua.valeriishymchuk.simpleitemgenerator.service.InfoService;
import ua.valeriishymchuk.simpleitemgenerator.service.ItemService;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
public final class SimpleItemGeneratorPlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder
                .build(this, new PacketEventsSettings()
                        .checkForUpdates(false)));
        PacketEvents.getAPI().load();
    }

    public IConfigRepository configRepository;
    CommandManager<CommandSender> commandManager;
    BukkitTaskScheduler taskScheduler = new BukkitTaskScheduler(this);
    ConfigLoader configLoader;
    ConfigLoader itemsConfigLoader;
    ItemService itemService;
    InfoService infoService;
    public ItemRepository itemRepository;
    CooldownRepository cooldownRepository = null;
    boolean isHDBLoaded = false;

    @Override
    public void onEnable() {
        if (!isHDBLoaded) {
            PacketEvents.getAPI().init();
            if (!checkFor1132()) {
                return;
            }
            commandManager = setupCommandManager();
            configLoader = configLoader();
            File itemsFolder = new File(getDataFolder(), "items");
            itemsFolder.mkdirs();
            itemsConfigLoader = configLoader(itemsFolder);
            ErrorVisitor errorVisitor = new ErrorVisitor(getLogger());
            configRepository = new ConfigRepository(configLoader, errorVisitor);
            itemRepository = new ItemRepository(configRepository, itemsConfigLoader, errorVisitor);
            IUpdateRepository updateRepository = new UpdateRepository();
            cooldownRepository = new CooldownRepository(cooldownLoader(), errorVisitor);
            SemanticVersion currentVersion = SemanticVersion.parse(getDescription().getVersion());
            infoService = new InfoService(updateRepository, configRepository, currentVersion);
            itemService = new ItemService(
                    configRepository,
                    itemRepository,
                    cooldownRepository,
                    infoService
            );
            new CommandsController(itemService, infoService).setupCommands(commandManager);
        }
        if (!isHDBLoaded && HeadDatabaseSupport.isPluginEnabled()) {
            getLogger().info("Hooked into HeadDatabase. Waiting for database...");
            Bukkit.getPluginManager().registerEvents(new Listener() {
                @EventHandler
                private void onDatabaseLoad(DatabaseLoadEvent event) {
                    getLogger().info("HeadDatabase is loaded fully. Enabling plugin...");
                    HeadDatabaseSupport.init();
                    isHDBLoaded = true;
                    onEnable();
                }
            }, this);
            return;
        }
        taskScheduler.runTask(() -> {
            boolean shouldCreateExample = !configRepository.doesMainConfigExist() &&
                    itemRepository.hasFolder() &&
                    itemRepository.getItemKeys().isEmpty();
            if (shouldCreateExample) {
                getLogger().info("Can't find items folder, creating one...");
                itemRepository.createExample();
            }
            Stream<Boolean> repositoryLoadStatus = Stream.of(
                    cooldownRepository.reload(),
                    configRepository.reload(),
                    itemRepository.reloadItems()
            );
            if (!repositoryLoadStatus.allMatch(b -> b)) {
                getLogger().severe("Failed to load config. Shutting down...");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
            TickTimer tickerTime = new TickTimer(taskScheduler);
            tickerTime.start();
            Bukkit.getPluginManager().registerEvents(new EventsController(itemService, infoService,tickerTime, taskScheduler), this);
            new TickController(itemService, infoService,taskScheduler, tickerTime).start();
            new API();
            Bukkit.getPluginManager().callEvent(new SimpleItemGeneratorLoadEvent());
            MetricsHelper.init(this);
            MetricsHelper.initTotalItemsChart(() -> itemRepository.getItemKeys().size());
            MetricsHelper.initPluginActivityChart(() -> infoService.isUsedActively());
        });
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
        if (cooldownRepository != null) {
            cooldownRepository.save();
        }
        MetricsHelper.shutdown();
        PacketEvents.getAPI().terminate();
    }

    private ConfigLoader configLoader() {
        return configLoader(getDataFolder());
    }

    private ConfigLoader configLoader(File folder) {
        return new ConfigLoader(
                folder,
                ".yml",
                ConfigLoaderConfigurationBuilder.yaml()
                        .peekBuilder(b -> b.indent(2).nodeStyle(NodeStyle.BLOCK))
                        .defaultOptions(opts -> opts.serializers(b ->
                                b.register(CompoundBinaryTag.class, new CompoundBinaryTagTypeSerializer()))
                        )
                        .build()
        );
    }

    private ConfigLoader cooldownLoader() {
        return new ConfigLoader(
                new File(getDataFolder(), "cooldowns"),
                ".yml",
                ConfigLoaderConfigurationBuilder.yaml()
                        .peekBuilder(b -> b.indent(2).nodeStyle(NodeStyle.BLOCK))
                        .defaultOptions(opts -> opts.serializers(b ->
                                b.register(CompoundBinaryTag.class, new CompoundBinaryTagTypeSerializer()))
                        )
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
                                .replaceText("%permission%", ((NoPermissionException) exception)
                                        .getMissingPermission())
                                .bake()
                )
                .withHandler(
                        MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX,
                        (sender, exception) -> configRepository.getLang().getInvalidCommandSyntax()
                                .replaceText("%usage%", ((InvalidSyntaxException) exception).getCorrectSyntax())
                                .bake()
                )
                .withHandler(MinecraftExceptionHandler.ExceptionType.ARGUMENT_PARSING, (sender, exception) -> {
                    ArgumentParseException argumentParseException = (ArgumentParseException) exception;
                    if (argumentParseException.getCause() instanceof CommandException) {
                        return KyoriHelper.convert(((CommandException) argumentParseException.getCause())
                                .getErrorMessage());
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
                    return configRepository.getLang()
                            .getUnknownArgumentError()
                            .replaceText("%error%", argumentParseException.getCause())
                            .bake();
                })
                .apply(manager, s -> s);
        return manager;
    }

    private class API extends SimpleItemGenerator {

        @Override
        public Optional<ItemStack> bakeItem(String key, @Nullable Player player) {
            Objects.requireNonNull(key);
            infoService.updatePluginActivity();
            return itemRepository.bakeItem(key, player).toJavaOptional();
        }

        @Override
        public boolean hasKey(String key) {
            Objects.requireNonNull(key);
            infoService.updatePluginActivity();
            return itemRepository.getItemKeys().contains(key);
        }

        @Override
        public Optional<String> getCustomItemKey(ItemStack item) {
            Objects.requireNonNull(item);
            infoService.updatePluginActivity();
            return NBTCustomItem.getCustomItemId(item).toJavaOptional();
        }

        @Override
        public boolean updateItem(ItemStack item, @Nullable Player player) {
            Objects.requireNonNull(item);
            if (!isCustomItem(item)) return false;
            infoService.updatePluginActivity();
            itemRepository.updateItem(item, player);
            return true;
        }
    }

}
