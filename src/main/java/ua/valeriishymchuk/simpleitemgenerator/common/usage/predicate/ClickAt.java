package ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate;

import ua.valeriishymchuk.simpleitemgenerator.common.usage.Predicate;

public enum ClickAt {
    AIR,
    PLAYER,
    ENTITY,
    BLOCK;

    public Predicate asType() {
        return new Predicate(null, this, null, null, null, null, null);
    }
}
