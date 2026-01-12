package com.github.nalamodikk.particle.utils;

import com.github.nalamodikk.common.config.ModCommonConfig;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class Math3DUtil {
    private static final Random random = new Random(System.currentTimeMillis());
    private static ExecutorService EXECUTOR;

    private static ExecutorService getExecutor() {
        if (EXECUTOR == null) {
            int threads = 4;
            try {
                if (ModCommonConfig.INSTANCE != null && ModCommonConfig.INSTANCE.calculateThreadCount != null) {
                    threads = ModCommonConfig.INSTANCE.calculateThreadCount.get();
                }
            } catch (Exception e) {
                // Config not loaded (Test env)
            }
            if (threads <= 0) threads = 4;
            EXECUTOR = Executors.newFixedThreadPool(threads, r -> new Thread(r, "Math3DUtil-ThreadPool"));
        }
        return EXECUTOR;
    }

    public static List<RelativeLocation> fillLine(RelativeLocation p1, RelativeLocation p2, double refiner) {
        int actualCount = (int) Math.round(p1.distance(p2) * refiner);
        return getLineLocations(p1, p2, actualCount);
    }

    public static List<RelativeLocation> fillLine(Vec3 p1, Vec3 p2, double refiner) {
        return fillLine(RelativeLocation.of(p1), RelativeLocation.of(p2), refiner);
    }

    public static Vector3f colorOf(int r, int g, int b) {
        return new Vector3f(r / 255f, g / 255f, b / 255f);
    }

    public static List<RelativeLocation> generateDottedLine(RelativeLocation target, int totalCount, int dottedCount, double step) {
        List<RelativeLocation> res = new ArrayList<>();
        double len = target.length();
        if (len <= step) return new ArrayList<>();
        if (step <= 0.0) return getLineLocations(new RelativeLocation(), target, totalCount);
        
        double lineStep = len / dottedCount - step;
        if (lineStep <= 0) return new ArrayList<>();
        
        int perCount = Math.max(1, totalCount / dottedCount);
        RelativeLocation dir = target.normalize();
        RelativeLocation current = dir.multiplyClone(lineStep);
        RelativeLocation pre = new RelativeLocation();
        
        for (int i = 0; i < dottedCount; i++) {
            res.addAll(getLineLocations(pre, current, perCount));
            pre = current.addClone(dir.multiplyClone(step));
            current = pre.addClone(dir.multiplyClone(lineStep));
        }
        return res;
    }

    public static List<RelativeLocation> generateDottedCircle(double r, int totalCount, int dottedCount, double step) {
        List<RelativeLocation> res = new ArrayList<>();
        if (step >= 2 * Math.PI) return new ArrayList<>();
        
        int perArcCount = Math.max(1, totalCount / dottedCount);
        double solidArcLengthStep = 2 * Math.PI / dottedCount - step;
        double angleStep = solidArcLengthStep / perArcCount;
        
        double pre = 0.0;
        double current = solidArcLengthStep;
        
        for (int i = 0; i < dottedCount; i++) {
            for (int j = 0; j < perArcCount; j++) {
                double arcAngle = pre + j * angleStep;
                res.add(new RelativeLocation(
                    Math.cos(arcAngle) * r,
                    0.0,
                    Math.sin(arcAngle) * r
                ));
            }
            pre = current + step;
            current = pre + solidArcLengthStep;
        }
        return res;
    }

    public static List<RelativeLocation> getLightningEffectNodes(RelativeLocation start, RelativeLocation end, int counts) {
        double len = end.distance(start);
        double offsetStep = len / 4;
        return getLightningEffectNodes(start, end, counts, offsetStep);
    }

    public static List<RelativeLocation> getLightningEffectNodes(RelativeLocation start, RelativeLocation end, int counts, double offsetRange) {
        List<RelativeLocation> res = new ArrayList<>();
        res.add(start);
        res.addAll(getLightningNodes(start, end, counts, offsetRange));
        res.add(end);
        return res;
    }

    private static List<RelativeLocation> getLightningNodes(RelativeLocation start, RelativeLocation end, int counts, double offsetRange) {
        return getLightningNodesAttenuation(start, end, counts, offsetRange, 1.0);
    }

    private static List<RelativeLocation> getLightningNodesAttenuation(RelativeLocation start, RelativeLocation end, int counts, double currentOffsetRange, double attenuation) {
        double fixedOffsetRange = Math.max(currentOffsetRange, 0.01);
        if (attenuation < 0.01 || attenuation > 1.0) throw new IllegalArgumentException("Attenuation must be between 0.01 and 1.0");
        
        RelativeLocation mid = start.addClone(end.minus(start).multiply(0.5));
        
        // Random offset
        double x = random.nextDouble() * 2 - 1;
        double y = random.nextDouble() * 2 - 1;
        double z = random.nextDouble() * 2 - 1;
        RelativeLocation randOffset = new RelativeLocation(x, y, z).normalize().multiply(random.nextDouble(-fixedOffsetRange, fixedOffsetRange));
        mid.add(randOffset);

        List<RelativeLocation> res = new ArrayList<>();
        res.add(mid);
        
        if (counts <= 1) return res;
        
        double nextOffsetRange = Math.max(fixedOffsetRange * attenuation, 0.01);
        List<RelativeLocation> left = getLightningNodesAttenuation(start, mid, counts - 1, nextOffsetRange, attenuation);
        List<RelativeLocation> right = getLightningNodesAttenuation(mid, end, counts - 1, nextOffsetRange, attenuation);
        
        List<RelativeLocation> combined = new ArrayList<>(left);
        combined.addAll(res);
        combined.addAll(right);
        return combined;
    }

    public static List<RelativeLocation> connectLineWithNodes(List<RelativeLocation> nodes, int preLineCount) {
        List<RelativeLocation> res = new ArrayList<>();
        for (int i = 0; i < nodes.size() - 1; i++) {
            RelativeLocation current = nodes.get(i);
            RelativeLocation next = nodes.get(i + 1);
            res.addAll(getLineLocations(current, next, preLineCount));
        }
        return res;
    }

    public static List<RelativeLocation> getHalfCircleXZ(double r, int count, double rotate) {
        List<RelativeLocation> res = new ArrayList<>();
        double step = Math.PI / count;
        double radius = 0.0;
        for (int i = 0; i < count; i++) {
            res.add(new RelativeLocation(
                r * Math.cos(radius), 0.0, r * Math.sin(radius)
            ));
            radius += step;
        }
        if (rotate != 0.0) {
            rotateAsAxis(res, RelativeLocation.yAxis(), rotate);
        }
        return res;
    }

    public static List<RelativeLocation> getPolygonInCircleLocations(int n, int edgeCount, double r) {
        if (n < 3) throw new IllegalArgumentException("n must be at least 3");
        if (edgeCount < 1) throw new IllegalArgumentException("edgeCount must be at least 1");

        List<RelativeLocation> vertices = getPolygonInCircleVertices(n, r);
        List<RelativeLocation> result = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            RelativeLocation vi = vertices.get(i);
            RelativeLocation vj = vertices.get(j);

            Vec3 direction = new Vec3(vj.x - vi.x, vj.y - vi.y, vj.z - vi.z);
            double length = direction.length();
            double step = (edgeCount > 1) ? length / (edgeCount - 1) : 0.0;

            List<RelativeLocation> lineLocations = getLineLocations(vi.toVector(), direction, step, edgeCount);
            result.addAll(lineLocations);
        }
        return result;
    }

    public static List<RelativeLocation> getPolygonInCircleVertices(int n, double r) {
        if (n < 3) throw new IllegalArgumentException("n must be at least 3");
        List<RelativeLocation> vertices = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double theta = 2 * Math.PI * i / n;
            vertices.add(new RelativeLocation(r * Math.cos(theta), 0.0, r * Math.sin(theta)));
        }
        return vertices;
    }

    public static List<RelativeLocation> rotatePointsToPoint(List<RelativeLocation> shape, RelativeLocation toPoint, RelativeLocation axis) {
        int threads = 4;
        try {
             if (ModCommonConfig.INSTANCE != null && ModCommonConfig.INSTANCE.calculateThreadCount != null) {
                 threads = ModCommonConfig.INSTANCE.calculateThreadCount.get();
             }
        } catch (Exception e) {}
        if (threads <= 0) threads = 4;
        return rotatePointsToPointAsync(shape, toPoint, axis, threads);
    }

    public static List<RelativeLocation> rotatePointsToPointAsync(List<RelativeLocation> shape, RelativeLocation toPoint, RelativeLocation axis, int threads) {
        List<RelativeLocation> copy = new CopyOnWriteArrayList<>(shape);
        if (axis.cross(toPoint).length() >= -1e-5 && axis.cross(toPoint).length() <= 1e-5 && axis.dot(toPoint) > 0) {
            return shape;
        }
        if (copy.isEmpty()) return shape;
        
        int actualThreads = Math.min(threads, copy.size());
        int taskPreThreadCount = copy.size() / actualThreads;
        int notHandledTaskCount = copy.size() % actualThreads;
        
        RelativeLocation na = axis.normalize();
        double axisYaw = getYawFromLocation(na);
        double axisPitch = getPitchFromLocation(na);
        
        RelativeLocation toa = toPoint.normalize();
        double toYaw = getYawFromLocation(toa);
        double toPitch = getPitchFromLocation(toa);
        
        Quaterniond q = new Quaterniond();
        q.rotateY(axisYaw).rotateLocalX(axisPitch);
        
        Quaterniond toQ = new Quaterniond();
        toQ.rotateY(-toYaw).rotateX(-toPitch);
        
        List<Future<?>> tasks = new ArrayList<>();
        int currentIndex = 0;
        
        for (int t = 0; t < actualThreads; t++) {
            int next = currentIndex + taskPreThreadCount;
            if (notHandledTaskCount > 0) {
                next++;
                notHandledTaskCount--;
            }
            int start = currentIndex;
            int end = next;
            currentIndex = next;
            
            tasks.add(getExecutor().submit(() -> {
                Vector3d vector = new Vector3d();
                for (int i = start; i < end; i++) {
                    RelativeLocation it = copy.get(i);
                    vector.set(it.x, it.y, it.z);
                    vector.rotate(q);
                    vector.rotate(toQ);
                    it.x = vector.x;
                    it.y = vector.y;
                    it.z = vector.z;
                }
            }));
        }
        
        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return shape;
    }

    public static List<RelativeLocation> rotatePointsToPoint(List<RelativeLocation> locList, Vec3 origin, Vec3 toPoint, RelativeLocation axis) {
        if (axis.length() >= -0.00001 && axis.length() <= 0.000001) {
            return locList;
        }
        RelativeLocation relToPoint = RelativeLocation.of(origin, toPoint);
        return rotatePointsToPoint(locList, relToPoint, axis);
    }

    public static List<RelativeLocation> getCircleXZ(double r, int count) {
        List<RelativeLocation> res = new ArrayList<>();
        double step = 2 * Math.PI / count;
        double radius = 0.0;
        for (int i = 0; i < count; i++) {
            res.add(new RelativeLocation(
                r * Math.cos(radius), 0.0, r * Math.sin(radius)
            ));
            radius += step;
        }
        return res;
    }
    
    public static List<RelativeLocation> getLineLocations(RelativeLocation start, RelativeLocation end, int count) {
        return getLineLocations(start.toVector(), end.toVector(), count);
    }
    
    public static List<RelativeLocation> getLineLocations(Vec3 start, Vec3 end, int count) {
        RelativeLocation origin = RelativeLocation.of(start);
        List<RelativeLocation> res = new ArrayList<>();
        res.add(origin);
        res.add(RelativeLocation.of(end));
        
        double step = start.distanceTo(end) / count;
        Vec3 direction = end.subtract(start).normalize().scale(step);
        RelativeLocation relativeDirection = RelativeLocation.of(direction);
        RelativeLocation next = origin;
        
        for (int i = 2; i <= count; i++) {
            RelativeLocation pos = next.addClone(relativeDirection);
            next = pos.clone();
            res.add(next);
        }
        return res;
    }

     public static List<RelativeLocation> getLineLocations(Vec3 origin, Vec3 direction, double step, int count) {
        RelativeLocation originRel = RelativeLocation.of(origin);
        List<RelativeLocation> res = new ArrayList<>();
        res.add(originRel);
        
        RelativeLocation relativeDirection = RelativeLocation.of(direction.normalize().scale(step));
        RelativeLocation next = originRel;
        
        for (int i = 2; i <= count; i++) {
            RelativeLocation pos = next.addClone(relativeDirection);
            next = pos.clone();
            res.add(next);
        }
        return res;
    }

    public static List<RelativeLocation> rotateAsAxis(List<RelativeLocation> locList, RelativeLocation axis, double angle) {
        int threads = 4;
        try {
             if (ModCommonConfig.INSTANCE != null && ModCommonConfig.INSTANCE.calculateThreadCount != null) {
                 threads = ModCommonConfig.INSTANCE.calculateThreadCount.get();
             }
        } catch (Exception e) {}
        if (threads <= 0) threads = 4;
        return rotateAsAxisAsync(locList, axis, angle, threads);
    }

    public static List<RelativeLocation> rotateAsAxisAsync(List<RelativeLocation> shape, RelativeLocation axis, double angle, int threads) {
        List<RelativeLocation> copy = new CopyOnWriteArrayList<>(shape);
        if (copy.isEmpty()) return shape;
        
        int actualThreads = Math.min(threads, copy.size());
        int taskPreThreadCount = copy.size() / actualThreads;
        int notHandledTaskCount = copy.size() % actualThreads;
        
        Quaterniond q = new Quaterniond();
        q.rotateAxis(angle, axis.toVector3d());
        
        List<Future<?>> tasks = new ArrayList<>();
        int currentIndex = 0;
        
        for (int t = 0; t < actualThreads; t++) {
            int next = currentIndex + taskPreThreadCount;
            if (notHandledTaskCount > 0) {
                next++;
                notHandledTaskCount--;
            }
            int start = currentIndex;
            int end = next;
            currentIndex = next;
            
            tasks.add(getExecutor().submit(() -> {
                Vector3d vector = new Vector3d();
                for (int i = start; i < end; i++) {
                    RelativeLocation it = copy.get(i);
                    vector.set(it.x, it.y, it.z);
                    vector.rotate(q);
                    it.x = vector.x;
                    it.y = vector.y;
                    it.z = vector.z;
                }
            }));
        }
        
        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return shape;
    }
    
     public static double getYawFromLocation(Vec3 loc) {
        return Math.atan2(loc.z, loc.x);
    }

    public static double getYawFromLocation(RelativeLocation loc) {
        return Math.atan2(-loc.x, loc.z);
    }

    public static double getPitchFromLocation(RelativeLocation v) {
        return Math.atan2(v.y, Math.sqrt(v.x * v.x + v.z * v.z));
    }

    public static double getPitchFromLocation(Vec3 v) {
        double length = v.length();
        if (length == 0.0) return 0.0;
        return Math.asin(v.y / length);
    }

}
