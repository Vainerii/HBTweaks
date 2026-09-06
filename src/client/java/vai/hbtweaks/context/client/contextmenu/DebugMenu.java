package vai.hbtweaks.context.client.contextmenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.authlib.properties.Property;
import com.mojang.serialization.JsonOps;
import fr.herobrine.network.effects.ServerboundRequestEffectsPacket;
import fr.herobrine.network.mods.ServerboundHerobrineTweaksHandshakePacket;
import fr.herobrine.network.speech.ServerboundStartTypingPacket;
import fr.herobrine.network.speech.ServerboundStopTypingPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.permission.v1.PermissionContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import vai.hbtweaks.context.HBTweaksContext;
import vai.hbtweaks.context.client.HitboxDebug;
import vai.hbtweaks.context.client.Util;
import vai.hbtweaks.context.client.effects.EffectsBank;
import vai.hbtweaks.context.client.keyboard.WritersBank;
import vai.hbtweaks.context.client.listeners.ContextMenuTrigger;
import vai.hbtweaks.context.client.mouse.MouseTracker;
import vai.hbtweaks.context.client.network.Packets;
import vai.hbtweaks.context.client.script.ScriptRunner;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DebugMenu {

    private static final int LABEL_MAX = 40;
    private static final int HOVERED_MAX = 10;
    private static final double DUMP_RANGE = 3.0;
    private static final Gson DUMP_GSON = new GsonBuilder().setPrettyPrinting().create();

    private DebugMenu() {}

    private static void row(ContextMenu menu, String key, String value) {
        String v = value == null ? "null" : value;
        String shown = v.length() > LABEL_MAX ? v.substring(0, LABEL_MAX) + "… (" + v.length() + ")" : v;
        menu.addCopyItem(Component.literal(key + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(shown).withStyle(ChatFormatting.WHITE)), v);
    }

    private static void row(ContextMenu menu, String key, boolean value) {
        menu.addCopyItem(Component.literal(key + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(value))
                        .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED)),
                String.valueOf(value));
    }

    private static String pos(double x, double y, double z) {
        return "%.2f %.2f %.2f".formatted(x, y, z);
    }

    // ---------------------------------------------------------------- other player

    public static ContextMenu forPlayer(Player player) {
        Minecraft mc = Minecraft.getInstance();
        ContextMenu menu = new ContextMenu(0, 0, player);
        PlayerInfo pi = mc.player == null ? null : mc.player.connection.getPlayerInfo(player.getUUID());

        menu.addSubmenuItem("Identity", identity(player, pi));
        menu.addSubmenuItem("Pos", position(player));
        menu.addSubmenuItem("Data", state(player, pi));
        menu.addSubmenuItem("Banks", banks(player));
        menu.addSubmenuItem("Hovered", hovered(player));
        //menu.addSubmenuItem("Permissions", permissions(player));
        if (pi != null)
            menu.addSubmenuItem("Profile", profile(player, pi));
        return menu;
    }

    private static ContextMenu permissions(Player player) {
        ContextMenu m = new ContextMenu(0, 0, player);
        PermissionContext ctx = player.getPermissionContext();
        boolean any = false;
        for (PermissionContext.Key<?> k : ctx.keys()) {
            any = true;
            row(m, k.id().toString(), String.valueOf(ctx.checkPermission(k.id())));
        }
        if (!any)
            m.addInfoItem(Component.literal("[AUCUNE]").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return m;
    }

    private static ContextMenu identity(Player player, PlayerInfo pi) {
        ContextMenu m = new ContextMenu(0, 0, player);
        row(m, "UUID", player.getStringUUID());
        row(m, "MC name", Util.getMCName(player));
        if (pi != null && pi.getTabListDisplayName() != null)
            row(m, "RP name", pi.getTabListDisplayName().getString());
        row(m, "Fake name", Util.getFakeName(player));
        return m;
    }

    private static ContextMenu position(Player player) {
        ContextMenu m = new ContextMenu(0, 0, player);
        fillPosition(m, player);
        return m;
    }

    private static void fillPosition(ContextMenu m, Entity player) {
        Minecraft mc = Minecraft.getInstance();
        row(m, "Exact", pos(player.getX(), player.getY(), player.getZ()));
        row(m, "Distance", "%.3f".formatted(mc.player.position().distanceTo(player.position())));
    }

    private static ContextMenu state(Player player, PlayerInfo pi) {
        Minecraft mc = Minecraft.getInstance();
        ContextMenu m = new ContextMenu(0, 0, player);
        row(m, "Type", player.getClass().getSimpleName());
        row(m, "isReal", Util.isReal(player));
        row(m, "isNoGravity", player.isNoGravity());
        row(m, "isInvisible", player.isInvisible());
        row(m, "isPickable", player.isPickable());
        row(m, "Loaded", mc.level != null && mc.level.getPlayerByUUID(player.getUUID()) != null);
        row(m, "HP", "%.1f / %.1f".formatted(player.getHealth(), player.getMaxHealth()));
        if (pi != null) {
            row(m, "Gamemode", pi.getGameMode().getName());
            row(m, "Ping", pi.getLatency() + " ms");
            PlayerTeam team = pi.getTeam();
            row(m, "Team", team == null ? null : team.getName());
        }
        Entity vehicle = player.getVehicle();
        row(m, "Vehicle", vehicle == null ? null : vehicle.getClass().getSimpleName() + " #" + vehicle.getId());
        return m;
    }

    private static ContextMenu banks(Player player) {
        UUID uuid = player.getUUID();
        ContextMenu m = new ContextMenu(0, 0, player);
        row(m, "isWriting", WritersBank.isWriting(uuid));
        row(m, "alreadyWrote", WritersBank.alreadyWrote(uuid));
        long since = WritersBank.sinceLastWrite(uuid);
        row(m, "Last packet", since < 0 ? "never" : since + " ms / " + WritersBank.timeout() + " ms");
        row(m, "Has effects", EffectsBank.has(uuid));
        row(m, "Effects", String.valueOf(EffectsBank.get(uuid).size()));
        for (MobEffectInstance eff : EffectsBank.get(uuid))
            row(m, "  " + eff.getEffect().value().getDisplayName().getString(),
                    " " + eff.getAmplifier() + " / " + eff.getDuration() + "t");
        return m;
    }

    private static ContextMenu profile(Player player, PlayerInfo pi) {
        ContextMenu m = new ContextMenu(0, 0, player);
        for (Map.Entry<String, Property> e : pi.getProfile().properties().entries())
            row(m, e.getKey(), e.getValue().value());
        return m;
    }

    // ---------------------------------------------------------------- self

    public static ContextMenu forSelf(Player self) {
        ContextMenu menu = new ContextMenu(0, 0, self);
        menu.addCheckboxItem(new ContextMenu.CheckboxItem(
                Component.literal("Hitbox des entités invisibles (F3+B)")) {
            @Override public boolean isChecked() {
                return HitboxDebug.show;
            }
            @Override protected void checked() {
                HitboxDebug.show = true;
            }
            @Override protected void unchecked() {
                HitboxDebug.show = false;
            }
        });
        menu.addCheckboxItem(new ContextMenu.CheckboxItem(
                Component.literal("Hitbox des entités visibles (F3+B)")) {
            @Override public boolean isChecked() {
                return HitboxDebug.showVisible;
            }
            @Override protected void checked() {
                HitboxDebug.showVisible = true;
            }
            @Override protected void unchecked() {
                HitboxDebug.showVisible = false;
            }
        });
        menu.addCheckboxItem(new ContextMenu.CheckboxItem(
                Component.literal("Hitbox à travers les blocs (F3+B)")) {
            @Override public boolean isChecked() {
                return HitboxDebug.throughWalls;
            }
            @Override protected void checked() {
                HitboxDebug.throughWalls = true;
            }
            @Override protected void unchecked() {
                HitboxDebug.throughWalls = false;
            }
        });
        menu.addSubmenuItem("Myself", forPlayer(self));
        menu.addSubmenuItem("Network", network(self));
        menu.addSubmenuItem("Runtime", runtime(self));
        menu.addSubmenuItem("Hovered", hovered(self));
        menu.addSubmenuItem("Players", players(self));
        menu.addSubmenuItem("Actions", actions(self));
        return menu;
    }

    private static ContextMenu network(Player self) {
        ContextMenu m = new ContextMenu(0, 0, self);
        row(m, "handshake", ClientPlayNetworking.canSend(
                Packets.type(ServerboundHerobrineTweaksHandshakePacket.PACKET_INFO)));
        row(m, "start typing", ClientPlayNetworking.canSend(
                Packets.type(ServerboundStartTypingPacket.PACKET_INFO)));
        row(m, "stop typing", ClientPlayNetworking.canSend(
                Packets.type(ServerboundStopTypingPacket.PACKET_INFO)));
        row(m, "request effects", ClientPlayNetworking.canSend(
                Packets.type(ServerboundRequestEffectsPacket.PACKET_INFO)));
        return m;
    }

    private static ContextMenu runtime(Player self) {
        ContextMenu m = new ContextMenu(0, 0, self);
        row(m, "Writers", WritersBank.writingCount() + " / vus " + WritersBank.seenCount());
        row(m, "EffectsBank", String.valueOf(EffectsBank.size()));
        return m;
    }

    /** Entities under cursor, nearest first */
    private static ContextMenu hovered(Player self) {
        ContextMenu m = new ContextMenu(0, 0, self);
        List<Entity> list = MouseTracker.getHoveredEntities();
        row(m, "Count", String.valueOf(list.size()));
        if (list.isEmpty()) {
            m.addInfoItem(Component.literal("[NONE]").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return m;
        }
        int shown = Math.min(list.size(), HOVERED_MAX);
        for (Entity e : list.subList(0, shown))
            m.addSubmenuItem(Component.literal(e.getName().getString()), entityEntry(self, e));
        if (list.size() > shown)
            m.addInfoItem(Component.literal("+ " + (list.size() - shown) + " more")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return m;
    }

    private static ContextMenu entityEntry(Player owner, Entity e) {
        ContextMenu m = new ContextMenu(0, 0, owner);
        row(m, "Type", EntityType.getKey(e.getType()).toString());
        row(m, "Id", String.valueOf(e.getId()));
        row(m, "UUID", e.getStringUUID());
        row(m, "Name", e.getName().getString());
        fillPosition(m, e);
        row(m, "Rotation", "yaw %.1f / pitch %.1f".formatted(e.getYRot(), e.getXRot()));
        AABB bb = e.getBoundingBox();
        row(m, "Size X", "%.2f".formatted(bb.getXsize()));
        row(m, "Size Y", "%.2f".formatted(bb.getYsize()));
        row(m, "Size Z", "%.2f".formatted(bb.getZsize()));
        row(m, "isInvisible", e.isInvisible());
        row(m, "isPickable", e.isPickable());
        row(m, "isNoGravity", e.isNoGravity());
        row(m, "isSilent", e.isSilent());
        row(m, "isInvulnerable", e.isInvulnerable());
        row(m, "onGround", e.onGround());
        Entity vehicle = e.getVehicle();
        row(m, "Vehicle", vehicle == null ? null : vehicle.getClass().getSimpleName() + " #" + vehicle.getId());
        row(m, "Passengers", String.valueOf(e.getPassengers().size()));
        m.addActionItem(Component.literal("Print NBT in logs").withStyle(ChatFormatting.AQUA), () -> printNbt(e));
        return m;
    }

    private static void printNbt(Entity e) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(e.problemPath(), HBTweaksContext.LOGGER)) {
            TagValueOutput output = TagValueOutput.createWithContext(reporter, e.registryAccess());
            e.saveWithoutId(output);
            HBTweaksContext.LOGGER.info("NBT {} #{} : {}", EntityType.getKey(e.getType()), e.getId(), output.buildResult());
        } catch (Exception ex) {
            HBTweaksContext.LOGGER.error("Failed to find NBT", ex);
        }
        Minecraft.getInstance().gui.getChat().addClientSystemMessage(Component.literal("NBT printed in logs"));
    }

    private static ContextMenu players(Player self) {
        Minecraft mc = Minecraft.getInstance();
        ContextMenu m = new ContextMenu(0, 0, self);
        if (mc.level == null) {
            m.addInfoItem(Component.literal("[NONE]").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return m;
        }
        int count = 0;
        for (Player p : mc.level.players()) {
            if (!Util.isReal(p)) continue;
            count++;
            Component rp = Util.getRpName(p);
            String mcName = Util.getMCName(p);
            Component label = rp != null && !rp.getString().isEmpty()
                    ? rp.copy()
                    : Component.literal(mcName == null ? p.getName().getString() : mcName);
            m.addSubmenuItem(label, playerEntry(p));
        }
        if (count == 0)
            m.addInfoItem(Component.literal("[NONE]").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        return m;
    }

    private static ContextMenu playerEntry(Player player) {
        ContextMenu m = new ContextMenu(0, 0, player);
        Component rp = Util.getRpName(player);
        row(m, "RP name", rp == null ? null : rp.getString());
        row(m, "MC name", Util.getMCName(player));
        fillPosition(m, player);
        return m;
    }

    private static ContextMenu actions(Player self) {
        ContextMenu m = new ContextMenu(0, 0, self);
        m.addActionItem(Component.literal("Empty queue"), ScriptRunner::clearQueue);
        m.addActionItem(Component.literal("Empty WritersBank"), WritersBank::clear);
        m.addActionItem(Component.literal("Empty EffectsBank"), EffectsBank::clear);
        m.addActionItem(Component.literal("Reload YAML"), DebugMenu::reloadMenus);
        m.addActionItem(Component.literal("Dump closest entities"), () -> dumpClosestEntities(self));
        return m;
    }

    private static void dumpClosestEntities(Player self) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        String stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").format(LocalDateTime.now());
        Path dir = FabricLoader.getInstance().getGameDir().resolve("entity_dump").resolve(stamp + "_dump");

        int written = 0;
        try {
            Files.createDirectories(dir);
            AABB box = AABB.ofSize(self.position(), DUMP_RANGE * 2, DUMP_RANGE * 2, DUMP_RANGE * 2);
            for (Entity e : mc.level.getEntities(self, box, en -> !(en instanceof Player))) {
                if (e.position().distanceTo(self.position()) > DUMP_RANGE) continue;
                if (dumpEntity(e, dir)) written++;
            }
        } catch (IOException ex) {
            HBTweaksContext.LOGGER.error("Entity dump failed", ex);
            return;
        }

        mc.gui.getChat().addClientSystemMessage(Component.literal(
                written + " entities dumped into " + dir).withStyle(ChatFormatting.GREEN));
    }

    /** ProblemReporter.ScopedCollector is for broken entities */
    private static boolean dumpEntity(Entity e, Path dir) {
        try (ProblemReporter.ScopedCollector reporter =
                     new ProblemReporter.ScopedCollector(e.problemPath(), HBTweaksContext.LOGGER)) {
            TagValueOutput out = TagValueOutput.createWithContext(reporter, e.registryAccess());
            e.saveWithoutId(out);
            JsonElement json = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, out.buildResult());
            Path file = dir.resolve(e.getClass().getSimpleName() + "_" + e.getId() + ".json");
            Files.writeString(file, DUMP_GSON.toJson(json));
            return true;
        } catch (Exception ex) {
            HBTweaksContext.LOGGER.error("Failed to dump entity {}", e.getId(), ex);
            return false;
        }
    }

    private static void reloadMenus() {
        ContextMenuTrigger.onFileEdited(ContextMenuTrigger.CUSTOM_MENU,
                CustomContextMenuLoader.readYaml(ContextMenuTrigger.CUSTOM_MENU));
        ContextMenuTrigger.onFileEdited(ContextMenuTrigger.CUSTOM_MENU_SELF,
                CustomContextMenuLoader.readYaml(ContextMenuTrigger.CUSTOM_MENU_SELF));
        Minecraft.getInstance().gui.getChat().addClientSystemMessage(
                Component.literal("Reloaded"));
    }

}
