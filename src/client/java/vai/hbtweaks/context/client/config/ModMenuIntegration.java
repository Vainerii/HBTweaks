package vai.hbtweaks.context.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModMenuIntegration implements ModMenuApi {

    private static final Component title = Component.translatable("hbtweaks.context.config.title");

    private static final Component c_general = Component.translatable("hbtweaks.context.config.category.general");
    private static final Component o_hover = Component.translatable("hbtweaks.context.config.hover_location");
    private static final Component o_hover_tt = Component.translatable("hbtweaks.context.config.hover_location.tooltip");
    private static final Component o_box = Component.translatable("hbtweaks.context.config.box_position");
    private static final Component o_box_tt = Component.translatable("hbtweaks.context.config.box_position.tooltip");
    private static final Component o_plus = Component.translatable("hbtweaks.context.config.hide_plus");
    private static final Component o_plus_tt = Component.translatable("hbtweaks.context.config.hide_plus.tooltip");

    private static final Component v_top_left = Component.translatable("hbtweaks.context.config.pos.top_left");
    private static final Component v_top_right = Component.translatable("hbtweaks.context.config.pos.top_right");
    private static final Component v_bottom_right = Component.translatable("hbtweaks.context.config.pos.bottom_right");
    private static final Component v_mouse = Component.translatable("hbtweaks.context.config.pos.mouse");

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ModMenuIntegration::createConfigScreen;
    }

    public static Screen createConfigScreen(Screen parent) {
        HBConfig cfg = HBConfig.get();
        HBConfig def = HBConfig.HANDLER.defaults();

        ConfigCategory general = ConfigCategory.createBuilder()
                .name(c_general)
                .option(Option.<HBConfig.HoverLocation>createBuilder()
                        .name(o_hover)
                        .description(dev.isxander.yacl3.api.OptionDescription.of(o_hover_tt))
                        .binding(def.hoverLocation, () -> cfg.hoverLocation, v -> cfg.hoverLocation = v)
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(HBConfig.HoverLocation.class)
                                .formatValue(ModMenuIntegration::hoverLabel))
                        .build())
                .option(Option.<HBConfig.BoxPosition>createBuilder()
                        .name(o_box)
                        .description(dev.isxander.yacl3.api.OptionDescription.of(o_box_tt))
                        .binding(def.boxPosition, () -> cfg.boxPosition, v -> cfg.boxPosition = v)
                        .controller(opt -> EnumControllerBuilder.create(opt)
                                .enumClass(HBConfig.BoxPosition.class)
                                .formatValue(ModMenuIntegration::boxLabel))
                        .build())
                .option(Option.<Boolean>createBuilder()
                        .name(o_plus)
                        .description(dev.isxander.yacl3.api.OptionDescription.of(o_plus_tt))
                        .binding(def.hidePlusBox, () -> cfg.hidePlusBox, v -> cfg.hidePlusBox = v)
                        .controller(TickBoxControllerBuilder::create)
                        .build())
                .build();

        YetAnotherConfigLib yacl = YetAnotherConfigLib.createBuilder()
                .title(title)
                .save(HBConfig.HANDLER::save)
                .category(general)
                .build();

        return yacl.generateScreen(parent);
    }

    private static Component hoverLabel(HBConfig.HoverLocation loc) {
        return switch (loc) {
            case TOP_LEFT -> v_top_left;
            case TOP_RIGHT -> v_top_right;
            case BOTTOM_RIGHT -> v_bottom_right;
            case MOUSE -> v_mouse;
        };
    }

    private static Component boxLabel(HBConfig.BoxPosition pos) {
        return switch (pos) {
            case TOP_LEFT -> v_top_left;
            case TOP_RIGHT -> v_top_right;
            case BOTTOM_RIGHT -> v_bottom_right;
        };
    }
}
