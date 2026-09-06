package vai.hbtweaks.context.client.notes;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import vai.hbtweaks.context.client.contextmenu.ContextMenu;

import java.util.List;

public final class NotesMenu {

    private NotesMenu() {}

    public static ContextMenu forPlayer(Player player) {
        String key = NotesBank.keyOf(player);
        ContextMenu menu = new ContextMenu(0, 0, player);

        ContextMenu.NoteBlock block = new ContextMenu.NoteBlock() {
            @Override
            public List<String> pages() {
                return NotesBank.get(key);
            }

            @Override
            public void edit() {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(new NotesEditScreen(mc.screen, key, NotesBank.get(key)));
            }
        };

        menu.addNoteBar(block);
        menu.withNoteBlock(block);
        return menu;
    }
}
