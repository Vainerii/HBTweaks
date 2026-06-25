package vai.hbtweaks.context.client.mouse;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;

import java.util.List;

public interface MouseTrackerEntityClickUpCallback {

    Event<MouseTrackerEntityClickUpCallback> EVENT = EventFactory.createArrayBacked(MouseTrackerEntityClickUpCallback.class,
            (listeners) -> (entities, click, screenType) -> {
                for (MouseTrackerEntityClickUpCallback listener : listeners)
                    listener.onClickUp(entities, click, screenType);
            });

    void onClickUp(List<Entity> entities, ClickType click, ScreenType screenType);
}
