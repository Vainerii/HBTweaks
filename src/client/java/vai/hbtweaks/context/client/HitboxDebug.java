package vai.hbtweaks.context.client;

/**
 * Developer toggles for the hitbox debug view, set from the debug menu and read by
 * EntityHitboxDebugRendererMixin.
 */
public final class HitboxDebug {

    /** Also draws hitboxes of invisible entities. */
    public static boolean show = false;

    /** Draws hitboxes on top of the world geometry, so they stay visible through walls. */
    public static boolean throughWalls = false;

    /** Draws hitboxes of visible entities. When false, only invisible ones are shown. */
    public static boolean showVisible = true;

    private HitboxDebug() {}
}
