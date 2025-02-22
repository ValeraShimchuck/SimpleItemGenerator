package ua.valeriishymchuk.simpleitemgenerator.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import ua.valeriishymchuk.simpleitemgenerator.common.component.RawComponent;

import java.util.Arrays;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Getter
@ConfigSerializable
public class LangEntity {

    private static final String SIG_PREPENDER = "<b><gold>SIG</gold></b><dark_gray>>></dark_gray> ";

    private static RawComponent component(String... text) {
        return new RawComponent(Arrays.stream(text).map(s -> SIG_PREPENDER + "<aqua>" + s + "</aqua>").toArray(String[]::new));
    }

    private static RawComponent error(String... text) {
        return new RawComponent(Arrays.stream(text).map(s -> SIG_PREPENDER + "<red>" + s + "</red>").toArray(String[]::new));
    }

    RawComponent giveItemSuccessfully = component("Custom item <dark_green>%key%</dark_green> was successfully given to <dark_green>%player%</dark_green>.");
    RawComponent invalidCommandSyntax = error("Invalid command syntax. Command usage: <white>%usage%</white>.");
    RawComponent noPermission = error("You don't have %permission% permission to use this command.");
    RawComponent invalidPlayer = error("Player <white>%player%</white> wasn't found.");
    RawComponent unknownArgumentError = error(
            "Unknown argument error: <white>%error%</white>!",
            "Please report this error there:",
            "<white><click:open_url:'https://github.com/ValeraShimchuck/SimpleItemGenerator/issues'>https://github.com/ValeraShimchuck/SimpleItemGenerator/issues</click></white>"

    );
    RawComponent invalidIntegerError = error("Invalid integer <white>%number%</white>! The number should be in between <white>%min%</white> and <white>%max%</white>.");
    RawComponent slotNotExist = error("Slot <white>%slot%</white> doesn't exist.");
    RawComponent sigUsage = component(
            "<gold>SimpleItemGenerator</gold> info:",
            "Plugin's version <white>%version%</white>.",
            "To reload the configs use <white><click:suggest_command:'/sig reload'>/sig reload</click></white>.",
            "To give an item use <white><click:suggest_command:'/sig give'>/sig give <key> [player]</click></white>.",
            "If you've encountered an error, or have a question",
            "then <white><click:open_url:'https://discord.gg/ksXEuxCqdC'><hover:show_text:'<green>Click to join our discord server</green>'><b>join</b></hover></click></white> our discord server!"
    );
    RawComponent itemDoesntExist = error("Custom item <white>%key%</white> doesn't exist.");
    RawComponent reloadSuccessfully = component("Configs were successfully reloaded.");
    RawComponent reloadUnsuccessfully = error("Configs weren't reloaded. Check console.");
    RawComponent invalidItem = error("This custom item is invalid. Report this case to the server admins. Item key <white>%key%</white>.");
    RawComponent senderNotPlayer = error("Use <white>player</white> argument to use this command. Or try execute it as a player");
    RawComponent notEnoughItemsSender = error("Player <white>%player%</white> doesn't have <white>%amount%</white> of item <white>%item%</white> in their inventory.");
    RawComponent notEnoughItemsReceiver = error("You don't have <white>%amount%</white> of item <white>[%item%</white><white>]</white>.");
    RawComponent successfullyWithdrewSender = component("Successfully withdrew <white>%amount%</white> of item <white>%item%</white> from <white>%player%</white>.");
    RawComponent successfullyWithdrewReceiver = component("Successfully withdrew <white>%amount%</white> of item <white>[%item%</white><white>]</white> from your inventory.");
    RawComponent creativeDrop = error(
            "You may experience weird behavior when dropping a",
            "custom item in creative mode.",
            "If you want to test a plain experience, please switch off",
            "from creative mode.",
            "Or disable event cancellation of the item in config.yml",
            "like this:",
            "items:",
            "  %key%:",
            "    item: ... # your item appearance",
            "    usage:",
            "    - predicate: [button] drop",
            "      cancel: false"
    );
    RawComponent adminWelcome = component(
            "You are running <gold><b>SimpleItemGenerator</b></gold> version <white>%version%</white>.",
            "If you have any issues or questions",
            "you can join our discord server:",
            "<white><u><click:open_url:'https://discord.gg/ksXEuxCqdC'>https://discord.gg/ksXEuxCqdC</click></u><white/>"
    );

    RawComponent newUpdateVersion = component(
            "There is a new version available out there: <white>%new_version%</white>.",
            "Your current version is <white>%current_version%</white>.",
            "Download options(they are clickable):",
            "<yellow><click:open_url:'https://www.spigotmc.org/resources/simpleitemgenerator-1-8-1-21-4-free.121339/'><hover:show_text:'Click to open'>Spigot</hover></click></yellow>",
            "<green><click:open_url:'https://modrinth.com/plugin/simpleitemgenerator'><hover:show_text:'Click to open'>Modrith</hover></click></green>",
            "<gold><click:open_url:'https://www.curseforge.com/minecraft/bukkit-plugins/simpleitemgenerator'><hover:show_text:'Click to open'>Curse forge</hover></click></gold>",
            "<aqua><click:open_url:'https://hangar.papermc.io/ValeraShimchuck/SimpleItemGenerator'><hover:show_text:'Click to open'>Hangar</hover></click></aqua>",
            "<blue><click:open_url:'https://builtbybit.com/resources/simpleitemgenerator.57158/'><hover:show_text:'Click to open'>BuiltByBit</hover></click></blue>"
    );


}
