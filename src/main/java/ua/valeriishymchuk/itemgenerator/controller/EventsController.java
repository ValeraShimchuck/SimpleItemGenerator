package ua.valeriishymchuk.itemgenerator.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import ua.valeriishymchuk.itemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.itemgenerator.dto.ItemUsageResultDTO;
import ua.valeriishymchuk.itemgenerator.service.IItemService;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class EventsController implements Listener {

    IItemService itemService;

    @EventHandler(priority = EventPriority.HIGH)
    private void onUsage(PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY) return;
        ItemUsageResultDTO result = itemService.useItem(event.getPlayer(), event.getAction(), event.getItem());
        handleResult(result, event.getPlayer(), event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onInteractAt(PlayerInteractAtEntityEvent event) {
        ItemUsageResultDTO result = itemService.useItemAt(
                event.getPlayer(),
                true,
                event.getRightClicked(),
                event.getPlayer().getItemInHand()
        );
        handleResult(result, event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        boolean isCancelled = event.isCancelled();
        ItemUsageResultDTO result = itemService.useItemAt(player, false, event.getEntity(), player.getItemInHand());
        handleResult(result, player, event);
        if (isCancelled) {
            event.setCancelled(true);
        }
    }

    private void handleResult(ItemUsageResultDTO result, Player player, Cancellable event) {
        event.setCancelled(result.isShouldCancel());
        result.getCommands().forEach(commands -> {
            CommandSender sender = commands.isExecuteAsConsole() ? Bukkit.getConsoleSender() : player;
            Bukkit.dispatchCommand(sender, commands.getCommand());
        });
        result.getMessage().peek(message -> KyoriHelper.sendMessage(player, message));
    }

}
