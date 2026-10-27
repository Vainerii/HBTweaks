package vai.hbtweaks.context.client;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public final class LyingBody {

    private static final double LENGTH = 1.8;
    private static final double HALF_WIDTH = 0.3;
    private static final double HALF_THICKNESS = 0.25;

    private LyingBody() {}

    public static boolean isLying(Entity e) {
        return e instanceof Player p && p.getPose() == Pose.SLEEPING && p.getBedOrientation() == null;
    }

    public static @Nullable Player owner(Player body) {
        for (Player p : body.level().players()) {
            if (isBodyOf(body, p) && Util.isReal(p))
                return p;
        }
        return null;
    }

    public static boolean isBodyOf(Player body, Player player) {
        return body != player && isLying(body) && player.position().distanceToSqr(body.position()) < 0.01;
    }

    public static Optional<Vec3> clip(Player body, Vec3 from, Vec3 to) {
        Frame f = new Frame(body);
        return localBox(body).clip(f.toLocal(from), f.toLocal(to)).map(f::toWorld);
    }

    public static Vec3[] corners(Player body) {
        Frame f = new Frame(body);
        AABB box = localBox(body);
        Vec3[] corners = new Vec3[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = f.toWorld(new Vec3(
                    (i & 1) == 0 ? box.minX : box.maxX,
                    (i & 2) == 0 ? box.minY : box.maxY,
                    (i & 4) == 0 ? box.minZ : box.maxZ));
        }
        return corners;
    }

    private static AABB localBox(Player body) {
        double s = body.getScale();
        return new AABB(0, -HALF_THICKNESS * s, -HALF_WIDTH * s, LENGTH * s, HALF_THICKNESS * s, HALF_WIDTH * s);
    }

    private record Frame(Vec3 origin, double hx, double hz) {

        Frame(Player body) {
            this(body.position(), -Math.cos(Math.toRadians(body.yBodyRot)), Math.sin(Math.toRadians(body.yBodyRot)));
        }

        Vec3 toLocal(Vec3 v) {
            double dx = v.x - this.origin.x;
            double dz = v.z - this.origin.z;
            return new Vec3(dx * this.hx + dz * this.hz, v.y - this.origin.y, dx * this.hz - dz * this.hx);
        }

        Vec3 toWorld(Vec3 l) {
            return new Vec3(
                    this.origin.x + l.x * this.hx + l.z * this.hz,
                    this.origin.y + l.y,
                    this.origin.z + l.x * this.hz - l.z * this.hx);
        }
    }
}
