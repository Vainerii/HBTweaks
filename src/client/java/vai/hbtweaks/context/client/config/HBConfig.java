package vai.hbtweaks.context.client.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class HBConfig {

    public enum HoverLocation { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, MOUSE, DISABLED }

    public enum BoxPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, DISABLED }

    public enum MenuStyle { NORMAL, MINIMAL }

    public enum LayAssistMode { OFF, SNEAK_TOGGLE, SNEAK_HOLD }

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

    @SerialEntry public boolean contextMenus = true;
    @SerialEntry public HoverLocation hoverLocation = HoverLocation.MOUSE;
    @SerialEntry public BoxPosition boxPosition = BoxPosition.TOP_LEFT;
    @SerialEntry public boolean hidePlusBox = false;
    @SerialEntry public MenuStyle menuStyle = MenuStyle.NORMAL;
    @SerialEntry public boolean shareTyping = true;
    @SerialEntry public LayAssistMode layAssist = LayAssistMode.SNEAK_TOGGLE;
    @SerialEntry public List<String> hiddenMenus = new ArrayList<>();
    //@SerialEntry public boolean showMyName = true;
}
