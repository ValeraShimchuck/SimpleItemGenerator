package ua.valeriishymchuk.simpleitemgenerator.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is being run on plugin reload.
 * */
public class SimpleItemGeneratorReloadEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
