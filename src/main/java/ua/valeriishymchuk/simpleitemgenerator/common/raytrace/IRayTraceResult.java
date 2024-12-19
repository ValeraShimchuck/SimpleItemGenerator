package ua.valeriishymchuk.simpleitemgenerator.common.raytrace;

public interface IRayTraceResult {
    IRayTraceResult MISS = new RayTraceResult(null);

    default boolean isMiss() {
        return this == MISS;
    }

    default boolean hitEntity() {
        return this instanceof RayTraceEntityResult;
    }

    default boolean hitBlock() {
        return this instanceof RayTraceBlockResult;
    }

}
