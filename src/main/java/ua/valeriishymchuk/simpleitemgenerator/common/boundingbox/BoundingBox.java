package ua.valeriishymchuk.simpleitemgenerator.common.boundingbox;

import io.vavr.Tuple;
import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.joml.Vector2d;
import org.joml.Vector3d;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.RayTraceHelper;
import ua.valeriishymchuk.simpleitemgenerator.common.wrapper.BlockFaceWrapper;

import java.text.NumberFormat;
import java.util.Map;
import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BoundingBox {

    Vector3d min;
    Vector3d max;

    public BoundingBox(Vector3d min, Vector3d max) {
        this.min = new Vector3d(
                Math.min(min.x(), max.x()),
                Math.min(min.y(), max.y()),
                Math.min(min.z(), max.z())
        );
        this.max = new Vector3d(
                Math.max(min.x(), max.x()),
                Math.max(min.y(), max.y()),
                Math.max(min.z(), max.z())
        );
    }

    public BoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this(new Vector3d(minX, minY, minZ), new Vector3d(maxX, maxY, maxZ));
    }

    public Option<IntersectResult> intersects(Vector3d lineStart, Vector3d lineEnd) {
        Map<BlockFaceWrapper, BoxPlane> planes = getBlockPlanes();
        return HashMap.ofAll(planes)
                .map((face, boxPlane) -> Tuple.of(face, boxPlane.intersects(lineStart, lineEnd).getOrNull()))
                .filterValues(Objects::nonNull)
                .minBy(t -> lineStart.distance(t._2()))
                .map(t -> new IntersectResult(t._2(), t._1()));
        //return Option.ofOptional(planes.values().stream()
        //        .map(boxPlane -> boxPlane.intersects(lineStart, lineEnd))
        //        .filter(Option::isDefined)
        //        .map(Option::get)
        //        .min(Comparator.comparingDouble(lineStart::distance)));
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    @Getter
    public static class IntersectResult {
        Vector3d point;
        BlockFaceWrapper face;
    }

    private Map<BlockFaceWrapper, BoxPlane> getBlockPlanes() {
        Vector3d faceVector = new Vector3d(0, -1, 0);
        Vector3d originVector = new Vector3d(min.x(), min.y(), max.z());
        BoxPlane down = new BoxPlane(
                faceVector,
                originVector,
                RayTraceHelper.getPointFromPlane(originVector, faceVector, originVector),
                RayTraceHelper.getPointFromPlane(originVector, faceVector, new Vector3d(max.x(), min.y(), min.z()))
        );

        faceVector = new Vector3d(0, 1, 0);
        originVector = new Vector3d(min.x(), max.y(), min.z());
        BoxPlane up = new BoxPlane(
                faceVector,
                originVector,
                RayTraceHelper.getPointFromPlane(originVector, faceVector, originVector),
                RayTraceHelper.getPointFromPlane(originVector, faceVector, max)
        );

        faceVector = new Vector3d(0, 0, -1);
        originVector = min;
        BoxPlane north = new BoxPlane(
                faceVector,
                originVector,
                RayTraceHelper.getPointFromPlane(originVector, faceVector, originVector),
                RayTraceHelper.getPointFromPlane(originVector, faceVector, new Vector3d(max.x(), max.y(), min.z()))
        );

        faceVector = new Vector3d(1, 0, 0);
        originVector = new Vector3d(max.x(), min.y(), min.z());
        BoxPlane east = new BoxPlane(
                faceVector,
                originVector,
                RayTraceHelper.getPointFromPlane(originVector, faceVector, originVector),
                RayTraceHelper.getPointFromPlane(originVector, faceVector, max)
        );

        faceVector = new Vector3d(-1, 0, 0);
        originVector = new Vector3d(min.x(), min.y(), max.z());
        BoxPlane west = new BoxPlane(
                faceVector,
                originVector,
                RayTraceHelper.getPointFromPlane(originVector, faceVector, originVector),
                RayTraceHelper.getPointFromPlane(originVector, faceVector, new Vector3d(min.x(), max.y(), min.z()))
        );

        faceVector = new Vector3d(0, 0, 1);
        originVector = new Vector3d(max.x(), min.y(), max.z());
        BoxPlane south = new BoxPlane(
                faceVector,
                originVector,
                RayTraceHelper.getPointFromPlane(originVector, faceVector, originVector),
                RayTraceHelper.getPointFromPlane(originVector, faceVector, new Vector3d(min.x(), max.y(), max.z()))
        );

        return HashMap.of(
                BlockFaceWrapper.DOWN, down,
                BlockFaceWrapper.UP, up,
                BlockFaceWrapper.NORTH, north,
                BlockFaceWrapper.EAST, east,
                BlockFaceWrapper.WEST, west,
                BlockFaceWrapper.SOUTH, south
        ).toJavaMap();


    }

    @Override
    public String toString() {
        return "BoundingBox{" +
                "min=" + min.toString(NumberFormat.getNumberInstance()) +
                ", max=" + max.toString(NumberFormat.getNumberInstance()) +
                '}';
    }

    @FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
    @RequiredArgsConstructor
    private static class BoxPlane {
        Vector3d normal;
        Vector3d origin;
        Vector2d start;
        Vector2d end;

        private boolean within(Vector2d point) {
            return point.x >= start.x && point.x <= end.x && point.y >= start.y && point.y <= end.y;
        }

        private boolean within(Vector3d point) {
            return within(RayTraceHelper.getPointFromPlane(origin, normal, point));
        }

        private Option<Vector3d> intersects(Vector3d lineStart, Vector3d lineEnd) {
            double distance = lineStart.distance(lineEnd);
            return RayTraceHelper.findLineAndPlaneIntersection(lineStart, lineEnd, origin, normal)
                    .filter(this::within)
                    .filter(point -> lineStart.distance(point) <= distance && lineEnd.distance(point) <= distance);
        }

    }
}
