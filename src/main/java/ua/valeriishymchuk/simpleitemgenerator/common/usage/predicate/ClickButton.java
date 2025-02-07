package ua.valeriishymchuk.simpleitemgenerator.common.usage.predicate;

import ua.valeriishymchuk.simpleitemgenerator.common.usage.Predicate;

public enum ClickButton {
    RIGHT,
    LEFT,
    DROP;

    public Predicate asType() {
        return new Predicate(this, null, null, null, null);
    }

}
