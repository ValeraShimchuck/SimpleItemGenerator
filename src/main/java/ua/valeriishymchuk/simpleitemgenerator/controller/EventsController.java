package ua.valeriishymchuk.simpleitemgenerator.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import ua.valeriishymchuk.simpleitemgenerator.common.message.KyoriHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.tick.TickerTime;
import ua.valeriishymchuk.simpleitemgenerator.dto.ItemUsageResultDTO;
import ua.valeriishymchuk.simpleitemgenerator.service.IItemService;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class EventsController implements Listener {

    IItemService itemService;
    TickerTime tickerTime;
    Map<Player, Long> lastDropTick = new WeakHashMap<>();
    Map<Player, Long> lastPlayerClickTick = new WeakHashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    private void onUsage(PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY) return;
        long currentTick = tickerTime.getTick();
        Long lastDropTick = this.lastDropTick.get(event.getPlayer());
        Long lastPlayerClickTick = this.lastPlayerClickTick.get(event.getPlayer());
        if (lastDropTick != null && currentTick == lastDropTick) return;
        if (lastPlayerClickTick != null && currentTick == lastPlayerClickTick) return;
        ItemUsageResultDTO result = itemService.useItem(
                event.getPlayer(),
                event.getAction(),
                event.getItem(),
                event.getClickedBlock()
        );
        handleResult(result, event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onWorkbench(PrepareItemCraftEvent event) {
        boolean shouldCancel = !Arrays.stream(event.getInventory().getMatrix())
                .filter(Objects::nonNull)
                .allMatch(itemService::canBeUsedInCraft);
        if (shouldCancel) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        lastDropTick.put(player, tickerTime.getTick());
        handleResult(itemService.dropItem(player, event.getItemDrop().getItemStack()), player, event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    private void onInteractAt(PlayerInteractAtEntityEvent event) {
        long currentTick = tickerTime.getTick();
        Long lastPlayerClickTickValue = this.lastPlayerClickTick.get(event.getPlayer());
        if (lastPlayerClickTickValue != null && currentTick == lastPlayerClickTickValue) return;
        lastPlayerClickTick.put(event.getPlayer(), tickerTime.getTick());
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
