package vai.hbtweaks.context.client.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * User configuration, handled by YACL and saved to
 * <code>config/hb-tweaks-context/config.json</code>. Every public field annotated with
 * SerialEntry is persisted.
 *
 * @see ModMenuIntegration
 */
public class HBConfig {

    /** Where the name box of the player under the mouse cursor is drawn. */
    public enum HoverLocation { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, MOUSE }

    /** Where the info box of the player at the crosshair is drawn. */
    public enum BoxPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT }

    /** Visual style of the context menu. */
    public enum MenuStyle { NORMAL, MINIMAL }

    /** YACL handler, used to load, save and read the default values. */
    public static final ConfigClassHandler<HBConfig> HANDLER =
            ConfigClassHandler.createBuilder(HBConfig.class)
                    .id(Identifier.fromNamespaceAndPath("hb-tweaks-context", "config"))
                    .serializer(handler -> GsonConfigSerializerBuilder.create(handler)
                            .setPath(FabricLoader.getInstance().getConfigDir()
                                    .resolve("hb-tweaks-context").resolve("config.json"))
                            .build())
                    .build();

    public static HBConfig get() {
        return HANDLER.instance();
    }

    /** Enables the right-click context menus. */
    @SerialEntry public boolean contextMenus = true;
    @SerialEntry public HoverLocation hoverLocation = HoverLocation.MOUSE;
    @SerialEntry public BoxPosition boxPosition = BoxPosition.TOP_LEFT;
    /** Hides the "+" box of the context menu, the toggle that enters edit mode. */
    @SerialEntry public boolean hidePlusBox = false;
    @SerialEntry public MenuStyle menuStyle = MenuStyle.NORMAL;
    /**
     * Sends the local "is writing" status to other players. Toggled from the Options submenu
     * of the context menu, not from the config screen.
     */
    @SerialEntry public boolean shareTyping = true;
    /** Labels of the root context menu rows the user chose to hide. */
    @SerialEntry public List<String> hiddenMenus = new ArrayList<>();
    //@SerialEntry public boolean showMyName = true;
}
