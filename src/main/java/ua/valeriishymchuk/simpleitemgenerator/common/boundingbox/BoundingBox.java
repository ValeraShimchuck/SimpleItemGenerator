package ua.valeriishymchuk.simpleitemgenerator.common.boundingbox;

import io.vavr.collection.HashMap;
import io.vavr.control.Option;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.bukkit.block.BlockFace;
import org.joml.Vector2d;
import org.joml.Vector3d;
import ua.valeriishymchuk.simpleitemgenerator.common.raytrace.RayTraceHelper;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Map;

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

    public Option<Vector3d> intersects(Vector3d lineStart, Vector3d lineEnd) {
        Map<BlockFace, BoxPlane> planes = getBlockPlanes();
        return Option.ofOptional(planes.values().stream()
                .map(boxPlane -> boxPlane.intersects(lineStart, lineEnd))
                .filter(Option::isDefined)
                .map(Option::get)
                .min(Comparator.comparingDouble(lineStart::distance)));
    }

    private Map<BlockFace, BoxPlane> getBlockPlanes() {
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
                BlockFace.DOWN, down,
                BlockFace.UP, up,
                BlockFace.NORTH, north,
                BlockFace.EAST, east,
                BlockFace.WEST, west,
                BlockFace.SOUTH, south
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
