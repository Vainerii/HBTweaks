package vai.hbtweaks.context.client.config;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public class HBConfig {

    public enum HoverLocation { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, MOUSE }

    public enum BoxPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT }

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

    @SerialEntry public HoverLocation hoverLocation = HoverLocation.MOUSE;
    @SerialEntry public BoxPosition boxPosition = BoxPosition.TOP_LEFT;
    @SerialEntry public boolean hidePlusBox = false;
}
