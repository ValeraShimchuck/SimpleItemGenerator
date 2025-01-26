package ua.valeriishymchuk.simpleitemgenerator.tester;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.exceptions.ArgumentParseException;
import cloud.commandframework.exceptions.InvalidSyntaxException;
import cloud.commandframework.exceptions.NoPermissionException;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerSetCompression;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.google.common.base.Preconditions;
import de.tr7zw.changeme.nbtapi.NBT;
import io.vavr.concurrent.Future;
import lombok.SneakyThrows;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import ua.valeriishymchuk.simpleitemgenerator.SimpleItemGeneratorPlugin;
import ua.valeriishymchuk.simpleitemgenerator.api.SimpleItemGenerator;
import ua.valeriishymchuk.simpleitemgenerator.common.commands.CommandException;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.nbt.NBTConverter;
import ua.valeriishymchuk.simpleitemgenerator.entity.ConfigEntity;
import ua.valeriishymchuk.simpleitemgenerator.tester.annotation.Test;
import ua.valeriishymchuk.simpleitemgenerator.tester.client.MinecraftTestClient;
import ua.valeriishymchuk.simpleitemgenerator.tester.stream.WrappedPrintStream;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static ua.valeriishymchuk.simpleitemgenerator.tester.version.VersionUtils.runSince;
import static ua.valeriishymchuk.simpleitemgenerator.tester.version.VersionUtils.runUpTo;

public class SIGTesterPlugin extends JavaPlugin {

    private static boolean isFinished = false;

    //public SIGTesterPlugin() {
    //    System.setErr(new WrappedPrintStream(System.err, obj -> {
    //        System.out.println("Got error: " + obj);
    //    }));
    //}

    private CompletableFuture<String> proceedFuture = null;

    @Override
    public void onLoad() {
        SimpleItemGeneratorPlugin plugin = getPlugin(SimpleItemGeneratorPlugin.class);
        plugin.getLogger().addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel() == Level.SEVERE) {
                    fail();
                }
                if (record.getLevel() == Level.WARNING) {
                    fail();
                }
            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {

            }
        });
    }

    @SneakyThrows
    @Override
    public void onEnable() {
        if (isFinished) return;
        try {
            registerCommands();
            MinecraftTestClient client = testClient();
            runUpTo(ServerVersion.V_1_9, () -> {
                client.handleNext(PacketType.Play.Server.MAP_CHUNK_BULK, event -> {
                    Bukkit.getScheduler().runTaskLater(this, this::runTests, 1);
                });
            });
            runSince(ServerVersion.V_1_9, () -> {
                client.handleNext(PacketType.Play.Server.CHUNK_DATA, event -> {
                    Bukkit.getScheduler().runTaskLater(this, this::runTests, 1);
                });
            });

        } catch (Throwable e) {
            e.printStackTrace();
            fail();
            return;
        }
        //Bukkit.getScheduler().runTask(this, SIGTesterPlugin::success);
        //Bukkit.getScheduler().runTaskLater(this, SIGTesterPlugin::success, 20 * 20);
    }



        // TODO continue
    // 1.8 configuration load fail/success
    // 1.14+ configuration load fail/success
    // 1.21.4+ configuration load fail/success

    // Usage one command test
    // Usage LMB/RMB command test
    // Usage cooldown test
    // Usage freezetime test
    // Usage drop at entity test
    // Usage drop at block test
    // Usage cancel test

    // commands for testing
    /*
    * /sigtest fail
    * /sigtest proceed <name>
    * */
    private void registerCommands() {
        CommandManager<CommandSender> manager = setupCommandManager();
        Supplier<Command.Builder<CommandSender>> builder = () -> manager.commandBuilder("sigtest");
        Consumer<Command.Builder<CommandSender>[]> register = commands -> Arrays.stream(commands).forEach(manager::command);
        register.accept(Arrays.asList(
                builder.get().literal("fail").handler(ctx -> fail()),
                builder.get().literal("proceed").argument(StringArgument.builder("proceed"))
                        .handler(ctx -> {
                            String proceed = ctx.get("proceed");
                            if (proceedFuture == null || proceedFuture.isDone()) {
                                fail();
                                return;
                            }
                            proceedFuture.complete(proceed);
                        })
        ).toArray(new Command.Builder[0]));
    }

    private CompletableFuture<Void> tryProceed(String key) {
        CompletableFuture<String> future = new CompletableFuture<>();
        if (proceedFuture != null && !proceedFuture.isDone()) {
            throw new RuntimeException("Already awaiting");
        }
        proceedFuture = future;
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            if (proceedFuture.isDone()) return;
            proceedFuture.completeExceptionally(new RuntimeException("Command wasn't executed in time"));
        }, 20);
        return future.thenAccept(result -> {
            if (!result.equals(key)) throw new RuntimeException("Got " + result + " instead of " + key);
        });
    }


    private MinecraftTestClient testClient() {
        Location playerPos = new Location(Bukkit.getWorld("world"), 0, 0, 0);
        MinecraftTestClient testClient = new MinecraftTestClient(
                new UserProfile(UUID.randomUUID(), "test1"),
                event -> {
                    if (isFinished) return;
                    //getLogger().info("Got packet from server: " + event.getPacketType());
                    if (event.getPacketType() == PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
                        WrapperPlayServerPlayerPositionAndLook packet = new WrapperPlayServerPlayerPositionAndLook(event);
                        playerPos.setX(packet.getPosition().getX());
                        playerPos.setY(packet.getPosition().getY());
                        playerPos.setZ(packet.getPosition().getZ());
                        playerPos.setYaw(packet.getYaw());
                        playerPos.setPitch(packet.getPitch());
                    }
                }
        );
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (testClient.getChannel() == null || !testClient.getChannel().isActive()) return;
            if (testClient.getConnectionState() != 3) return;
            testClient.write(new WrapperPlayClientPlayerPosition(
                    new Vector3d(
                            playerPos.getX(),
                            playerPos.getY(),
                            playerPos.getZ()
                    ),
                    false
            ));
        }, 0, 20);
        return testClient;
    }

    private void runTests() {
        try {
            Arrays.stream(getClass().getMethods())
                    .filter(m -> m.getAnnotation(Test.class) != null)
                    .forEach(m -> {
                        try {
                            getLogger().info("Running test: " + m.getName());
                            m.invoke(this);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (Throwable e) {
            e.printStackTrace();
            fail();
        }

        success();
    }

    private void forceSetConfig(String key) {
        checkArgument(setConfig(key), "Failed to set " + key);
    }

    private void ensureDefined(String itemKey) {
        checkArgument(getSIG().configRepository.getConfig().getItem(itemKey).isDefined(), "Can't access " + itemKey);
    }

    @Test
    public void testAPI() {
        forceSetConfig("config1");
        String itemKey = "test-item1";
        ensureDefined(itemKey);
        checkArgument(SimpleItemGenerator.get().hasKey(itemKey), "Can't access " + itemKey + " through API");
        ItemStack itemStack = SimpleItemGenerator.get().bakeItem(itemKey, null).orElse(null);
        checkArgument(itemStack != null, "Can't bake " + itemKey + " through API");
        checkArgument(SimpleItemGenerator.get().isCustomItem(itemStack), "Can't get custom item out of " + itemKey + " through API");
        SimpleItemGenerator.get().updateItem(itemStack, null);
    }

    @Test
    public void setConfigs() {
        checkArgument(setConfig("config1"), "Failed to set config1");
        checkArgument(getSIG().configRepository.getConfig().getItem("test-item1").isDefined(), "Can't access test-item1");
        checkArgument(setConfig("config2"), "Failed to set config2");
        checkArgument(getSIG().configRepository.getConfig().getItem("test-item2").isDefined(), "Can't access test-item2");
    }

    @Test
    public void testFail() {
        checkArgument(!setConfig("fail-config"), "fail-config was run successfully");
    }

    @Test
    public void testGeneral() {
        checkArgument(setConfig("1.8/general"), "Failed to set 1.8/general");
        ConfigEntity.CustomItem item = getSIG().configRepository.getConfig().getItem("test-item").getOrNull();
        checkArgument(item != null, "Can't access test-item");
        ItemStack itemStack = getSIG().configRepository.getConfig().bakeItem("test-item", Bukkit.getOnlinePlayers().stream().findFirst()
                .orElseThrow(NullPointerException::new)).getOrElseThrow(() -> new RuntimeException("Can't bake test-item"));
        NBT.get(itemStack, nbt -> {
            Integer value = nbt.getInteger("test");
            checkArgument(value != null && value == 3, "Got " + value + " instead of 3");
            CompoundBinaryTag kyoriNBT = NBTConverter.fromNBTApi(nbt);
            List<String> strings = kyoriNBT.getList("test2").stream()
                    .map(b -> (ListBinaryTag) b)
                    .flatMap(ListBinaryTag::stream)
                    .map(b -> (StringBinaryTag) b)
                    .map(StringBinaryTag::value).collect(Collectors.toList());
            List<String> expected = Arrays.asList("1", "2", "3", "4", "5", "6");
            checkArgument(strings.equals(expected), "Got " + strings + " instead of " + expected);
        });
    }

    @SneakyThrows
    private boolean setConfig(String key) {
        File configToCopy = new File(getSIGDir(), key + ".yml");
        File destination = new File(getSIGDir(), "config.yml");
        Files.copy(configToCopy.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return getSIG().configRepository.reload();
    }

    private File getSIGDir() {
        return getSIG().getDataFolder();
    }

    private SimpleItemGeneratorPlugin getSIG() {
        return getPlugin(SimpleItemGeneratorPlugin.class);
    }


    private static void success() {
        if (finish(0)) System.out.println("Success!");
    }

    private static void fail() {
        if (finish(1)) System.out.println("Fail!");
    }

    @SneakyThrows
    private static boolean finish(int code) {
        if (isFinished) return false;
        isFinished = true;
        File testFile = new File("results.txt");
        try(FileWriter writer = new FileWriter(testFile, false)) {
            writer.append(code + "");
        }
        Bukkit.shutdown();
        return true;
    }

    @Override
    public void onDisable() {
        if (isFinished) return;
        Bukkit.shutdown();
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
                .apply(manager, s -> new Audience() {

                    @Override
                    public void sendMessage(final @NotNull Identity source, final @NotNull Component message, final @NotNull MessageType type) {
                        KyoriHelper.sendMessage(s, message);
                    }
                });
        return manager;
    }

}
