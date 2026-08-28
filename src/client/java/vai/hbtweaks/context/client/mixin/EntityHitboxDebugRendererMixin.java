package vai.hbtweaks.context.client.mixin;

import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vai.hbtweaks.context.client.HitboxDebug;

@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {

    @Redirect(method = "emitGizmos", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInvisible()Z"))
    private boolean hbtweaks$showInvisibleHitboxes(Entity entity) {
        return !HitboxDebug.show && entity.isInvisible();
    }

    private static GizmoProperties hbtweaks$onTop(GizmoProperties properties) {
        return HitboxDebug.throughWalls ? properties.setAlwaysOnTop() : properties;
    }

    @Redirect(method = "showHitboxes", at = @At(value = "INVOKE", target = "Lnet/minecraft/gizmos/Gizmos;cuboid(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/gizmos/GizmoStyle;)Lnet/minecraft/gizmos/GizmoProperties;"))
    private GizmoProperties hbtweaks$cuboidOnTop(AABB aabb, GizmoStyle style) {
        return hbtweaks$onTop(Gizmos.cuboid(aabb, style));
    }

    @Redirect(method = "showHitboxes", at = @At(value = "INVOKE", target = "Lnet/minecraft/gizmos/Gizmos;point(Lnet/minecraft/world/phys/Vec3;IF)Lnet/minecraft/gizmos/GizmoProperties;"))
    private GizmoProperties hbtweaks$pointOnTop(Vec3 position, int argb, float size) {
        return hbtweaks$onTop(Gizmos.point(position, argb, size));
    }

    @Redirect(method = "showHitboxes", at = @At(value = "INVOKE", target = "Lnet/minecraft/gizmos/Gizmos;arrow(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;I)Lnet/minecraft/gizmos/GizmoProperties;"))
    private GizmoProperties hbtweaks$arrowOnTop(Vec3 start, Vec3 end, int argb) {
        return hbtweaks$onTop(Gizmos.arrow(start, end, argb));
    }
}
