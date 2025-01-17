package ua.valeriishymchuk.simpleitemgenerator.tester;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerSetCompression;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.google.common.base.Preconditions;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import ua.valeriishymchuk.simpleitemgenerator.SimpleItemGeneratorPlugin;
import ua.valeriishymchuk.simpleitemgenerator.tester.annotation.Test;
import ua.valeriishymchuk.simpleitemgenerator.tester.client.MinecraftTestClient;
import ua.valeriishymchuk.simpleitemgenerator.tester.stream.WrappedPrintStream;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static com.google.common.base.Preconditions.checkArgument;

public class SIGTesterPlugin extends JavaPlugin {

    private static boolean isFinished = false;

    //public SIGTesterPlugin() {
    //    System.setErr(new WrappedPrintStream(System.err, obj -> {
    //        System.out.println("Got error: " + obj);
    //    }));
    //}


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
            runTests();
            testClient();
        } catch (Throwable e) {
            e.printStackTrace();
            fail();
            return;
        }
        //Bukkit.getScheduler().runTask(this, SIGTesterPlugin::success);
        //Bukkit.getScheduler().runTaskLater(this, SIGTesterPlugin::success, 20 * 20);
    }

    private void testClient() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListener() {
            @Override
            public void onPacketReceive(PacketReceiveEvent event) {
                System.out.println("Got a packet from client: " + event.getPacketType() + " " + event.getPacketId());
            }
        }, PacketListenerPriority.MONITOR);
        Location playerPos = new Location(Bukkit.getWorld("world"), 0, 0, 0);
        MinecraftTestClient testClient = new MinecraftTestClient(
                new UserProfile(UUID.randomUUID(), "test1"),
                event -> {
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
    }

    private void runTests() {
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
        Bukkit.shutdown();
    }
}
