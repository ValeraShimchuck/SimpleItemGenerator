package ua.valeriishymchuk.simpleitemgenerator.common.raytrace;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Option;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.joml.Math;
import org.joml.Matrix3d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import ua.valeriishymchuk.simpleitemgenerator.common.boundingbox.BoundingBox;
import ua.valeriishymchuk.simpleitemgenerator.common.reflection.ReflectedRepresentations;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

public class RayTraceHelper {

    public static final double EPSILON = 1.0E-7;

    //private static Option<RayTraceBlockResult> fastClip(Vector from, Vector to, World world) {
    //    // I copied it from 1.21 nms, so I have no idea what is going on there
//
    //    final double adjX = EPSILON * (from.getX() - to.getX());
    //    final double adjY = EPSILON * (from.getY() - to.getY());
    //    final double adjZ = EPSILON * (from.getZ() - to.getZ());
//
    //    if (adjX == 0.0 && adjY == 0.0 && adjZ == 0.0) {
    //        return Option.none();
    //    }
//
    //    final double toXAdj = to.getX() - adjX;
    //    final double toYAdj = to.getY() - adjY;
    //    final double toZAdj = to.getZ() - adjZ;
    //    final double fromXAdj = from.getX() + adjX;
    //    final double fromYAdj = from.getY() + adjY;
    //    final double fromZAdj = from.getZ() + adjZ;
//
    //    int currX = floor(fromXAdj);
    //    int currY = floor(fromYAdj);
    //    int currZ = floor(fromZAdj);
//
    //    final Vector currPos = new Vector();
//
    //    final double diffX = toXAdj - fromXAdj;
    //    final double diffY = toYAdj - fromYAdj;
    //    final double diffZ = toZAdj - fromZAdj;
//
    //    final double dxDouble = Math.signum(diffX);
    //    final double dyDouble = Math.signum(diffY);
    //    final double dzDouble = Math.signum(diffZ);
//
    //    final int dx = (int)dxDouble;
    //    final int dy = (int)dyDouble;
    //    final int dz = (int)dzDouble;
//
    //    final double normalizedDiffX = diffX == 0.0 ? Double.MAX_VALUE : dxDouble / diffX;
    //    final double normalizedDiffY = diffY == 0.0 ? Double.MAX_VALUE : dyDouble / diffY;
    //    final double normalizedDiffZ = diffZ == 0.0 ? Double.MAX_VALUE : dzDouble / diffZ;
//
    //    double normalizedCurrX = normalizedDiffX * (diffX > 0.0 ? (1.0 - frac(fromXAdj)) : frac(fromXAdj));
    //    double normalizedCurrY = normalizedDiffY * (diffY > 0.0 ? (1.0 - frac(fromYAdj)) : frac(fromYAdj));
    //    double normalizedCurrZ = normalizedDiffZ * (diffZ > 0.0 ? (1.0 - frac(fromZAdj)) : frac(fromZAdj));
//
    //    Chunk lastChunk = null;
//
    //    int lastChunkX = Integer.MIN_VALUE;
    //    int lastChunkY = Integer.MIN_VALUE;
    //    int lastChunkZ = Integer.MIN_VALUE;
//
    //    int minHeight = ReflectedRepresentations.World.getMinHeight(world);
    //    int maxHeight = world.getMaxHeight();
//
    //    for (;;) {
    //        currPos.setX(currX).setY(currY).setZ(currZ);
//
    //        final int newChunkX = currX >> 4;
    //        final int newChunkY = currY >> 4;
    //        final int newChunkZ = currZ >> 4;
//
    //        final int chunkDiff = ((newChunkX ^ lastChunkX) | (newChunkZ ^ lastChunkZ));
    //        final int chunkYDiff = newChunkY ^ lastChunkY;
//
    //        if ((chunkDiff | chunkYDiff) != 0) {
    //            if (chunkDiff != 0) {
    //                lastChunk = world.getChunkAt(newChunkX, newChunkZ);
    //            }
    //            lastChunkX = newChunkX;
    //            lastChunkY = newChunkY;
    //            lastChunkZ = newChunkZ;
    //        }
    //        if (currY <= maxHeight && currY >= minHeight) {
    //            Block block = world.getChunkAt(newChunkX, newChunkZ).getBlock(currX & 15, currY, currZ & 15);
//
    //        }
//
//
    //    }
//
//
    //}

    public static IRayTraceResult rayTrace(LivingEntity entity, Set<Material> transparent, int distance, int blockDistance) {
        RayTraceBlockResult rayTraceBlockResult = getFirstBlockOnTheLine(entity, transparent, blockDistance).getOrNull();
        RayTraceEntityResult rayTraceEntityResult = getFirstEntityOnTheLine(entity, distance).getOrNull();
        if (rayTraceBlockResult == null) return Option.<IRayTraceResult>of(rayTraceEntityResult).getOrElse(IRayTraceResult.MISS);
        if (rayTraceEntityResult == null) return rayTraceBlockResult;
        Location entityIntersection = rayTraceEntityResult.getHitLocation();
        Location blockIntersection = rayTraceBlockResult.getHitLocation();
        Location casterLoc = entity.getEyeLocation();
        if (entityIntersection.distanceSquared(casterLoc) < blockIntersection.distanceSquared(casterLoc)) {
            return rayTraceEntityResult;
        }
        return rayTraceBlockResult;
    }

    private static Option<RayTraceBlockResult> getFirstBlockOnTheLine(LivingEntity livingEntity, Set<Material> transparent, int distance) {
        Block block = livingEntity.getLineOfSight(transparent, distance).stream().filter(b -> !transparent.contains(b.getType())).findFirst().orElse(null);
        if (block == null) return Option.none();
        Location startLoc = livingEntity.getEyeLocation();
        Vector3d startLine = new Vector3d(startLoc.getX(), startLoc.getY(), startLoc.getZ());
        Vector lookVectorBukkit = livingEntity.getLocation().getDirection();
        Vector3d lookVector = new Vector3d(lookVectorBukkit.getX(), lookVectorBukkit.getY(), lookVectorBukkit.getZ());
        Vector3d endLine = startLine.add(lookVector.mul(distance), new Vector3d());
        BoundingBox blocksBox = new BoundingBox(
                block.getX(),
                block.getY(),
                block.getZ(),
                block.getX() + 1,
                block.getY() + 1,
                block.getZ() + 1
        );
        return blocksBox.intersects(startLine, endLine)
                .map(point -> new RayTraceBlockResult(block, new Location(livingEntity.getWorld(), point.x(), point.y(), point.z())));
    }

    private static Option<RayTraceEntityResult> getFirstEntityOnTheLine(LivingEntity livingEntity, int distance) {
        Location startLoc = livingEntity.getEyeLocation();
        Vector3d startLine = new Vector3d(startLoc.getX(), startLoc.getY(), startLoc.getZ());
        Vector lookVectorBukkit = livingEntity.getLocation().getDirection();
        Vector3d lookVector = new Vector3d(lookVectorBukkit.getX(), lookVectorBukkit.getY(), lookVectorBukkit.getZ());
        Vector3d endLine = startLine.add(lookVector.mul(distance), new Vector3d());
        Optional<RayTraceEntityResult> opt = livingEntity.getNearbyEntities(distance, distance, distance)
                .stream()
                .map(entity -> {
                    BoundingBox boundingBox = ReflectedRepresentations.Entity.getEntitiesBoundingBox(entity);
                    return Tuple.of(boundingBox.intersects(startLine, endLine).getOrNull(), entity);
                })
                .filter(tuple -> tuple._1() != null)
                .map(tuple -> new RayTraceEntityResult(
                        tuple._2(),
                        new Location(livingEntity.getWorld(), tuple._1().x(), tuple._1().y(), tuple._1().z())
                ))
                .min(Comparator.comparingDouble(res -> livingEntity.getEyeLocation().distance(res.hitLocation)));
        return Option.ofOptional(opt);
    }

    // Used to convert a 2d point to a 3d point on a plane defined by a normal
    private static Matrix3d getMinecraftPlaneMatrix(Vector3d planeNormal) {
        Vector3d upVector = new Vector3d(0, 1, 0);
        planeNormal = planeNormal.normalize(new Vector3d());
        double dot = planeNormal.dot(upVector);
        if (Math.abs(dot) == 1) {
            return new Matrix3d(
                    new Vector3d(1, 0, 0),
                    planeNormal,
                    new Vector3d(0, 0, dot)
            );
        }
        Vector3d nupvort = upVector.cross(planeNormal, new Vector3d()).normalize();
        Vector3d yland = planeNormal.cross(nupvort, new Vector3d()).normalize();
        Vector3d xland = planeNormal.cross(yland, new Vector3d()).normalize();
        return new Matrix3d(
                xland,
                planeNormal,
                yland
        );
    }

    public static Option<Vector3d> findLineAndPlaneIntersection(
            Vector3d la, Vector3d lb, // points of a line
            Vector3d origin, Vector3d normal // definition of a plane
    ) {
        Vector3d pointOnPlaneA = getPointOfPlane(origin, normal, new Vector2d(1, 0));
        Vector3d pointOnPlaneB = getPointOfPlane(origin, normal, new Vector2d(0, 1));
        return findLineAndPlaneIntersection(la, lb, origin, pointOnPlaneA, pointOnPlaneB);

    }


    //private static Vector3d getPointOfPlane(Vector3d point, Vector3d normal, Vector2d pointOnPlane) {
    //    Vector3d pointOnPlane3d = new Vector3d(pointOnPlane.x, pointOnPlane.y, 0);
    //    Vector3d cross = normal.cross(pointOnPlane3d, new Vector3d());
    //    return cross.cross(normal, new Vector3d()).add(point);
    //}

    public static Vector3d getPointOfPlane(Vector3d point, Vector3d normal, Vector2d pointOnPlane) {
        Vector3d pointOnPlane3d = new Vector3d(pointOnPlane.x, 0, pointOnPlane.y);
        Matrix3d matrix3d = getMinecraftPlaneMatrix(normal);
        return matrix3d.transform(pointOnPlane3d).add(point);
    }

    public static Vector2d getPointFromPlane(Vector3d planeOrigin, Vector3d planeNormal, Vector3d pointOnPlane) {
        Matrix3d inverseMatrix3d = getMinecraftPlaneMatrix(planeNormal).invert();
        Vector3d transformedPoint = pointOnPlane.sub(planeOrigin, new Vector3d()).mul(inverseMatrix3d);
        return new Vector2d(transformedPoint.x, transformedPoint.z);
    }


    // finding an intersection within a plane in 3d space. If you ever wondered how it's done, there it is.
    // https://en.wikipedia.org/wiki/Line%E2%80%93plane_intersection Parametric form section
    public static Option<Vector3d> findLineAndPlaneIntersection(
            Vector3d la, Vector3d lb, // points of a line
            Vector3d p0, Vector3d p1, Vector3d p2 // points of a plane
    ) {
        Vector3d lab = lb.sub(la, new Vector3d());
        Vector3d p01, p02;
        p01 = p1.sub(p0, new Vector3d());
        p02 = p2.sub(p0, new Vector3d());
        double denominator = new Matrix3d(
                lab.mul(-1, new Vector3d()),
                p01,
                p02
        ).determinant();
        if (denominator == 0) return Option.none();
        double t = p01.cross(p02, new Vector3d()).dot(la.sub(p0, new Vector3d())) /
                lab.mul(-1, new Vector3d()).dot(p01.cross(p02, new Vector3d()));
        return Option.some(la.add(lab.mul(t, new Vector3d()), new Vector3d()));
    }

    private static int floor(double value) {
        int i = (int) value;
        return value < (double) i ? i - 1 : i;
    }

    private static long lfloor(double value) {
        long l = (long) value;
        return value < (double) l ? l - 1L : l;
    }

    private static double frac(double value) {
        return value - (double) lfloor(value);
    }


}
