package vai.hbtweaks.context.client.mouse;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Event fired by MouseTracker when a mouse button is released while the cursor is free.
 */
public interface MouseTrackerEntityClickUpCallback {

    Event<MouseTrackerEntityClickUpCallback> EVENT = EventFactory.createArrayBacked(MouseTrackerEntityClickUpCallback.class,
            (listeners) -> (entities, click, screenType) -> {
                for (MouseTrackerEntityClickUpCallback listener : listeners)
                    listener.onClickUp(entities, click, screenType);
            });

    /**
     * @param entities the entities under the cursor, nearest first, possibly empty
     * @param click the released button
     * @param screenType the screen open at release time
     */
    void onClickUp(List<Entity> entities, ClickType click, ScreenType screenType);
}
