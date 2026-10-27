package vai.hbtweaks.context.client.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import vai.hbtweaks.context.HBTweaksContext;

/**
 * Vanilla throws (and disconnects) if the server removes a player from a team the client
 * doesn't have them in. The desired end state already holds, so skip instead of crashing.
 */
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerTeamMixin {

    @Redirect(method = "handleSetPlayerTeamPacket", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/scores/Scoreboard;removePlayerFromTeam(Ljava/lang/String;Lnet/minecraft/world/scores/PlayerTeam;)V"))
    private void hbtweaks$safeRemoveFromTeam(Scoreboard scoreboard, String player, PlayerTeam team) {
        if (scoreboard.getPlayersTeam(player) != team) {
            HBTweaksContext.LOGGER.warn("Ignored removal of {} from team {}: not in it client-side", player, team.getName());
            Minecraft.getInstance().gui.getChat().addClientSystemMessage(Component.literal(
                    "Désync équipe ignorée : " + player + " / " + team.getName()).withStyle(ChatFormatting.GRAY));
            return;
        }
        scoreboard.removePlayerFromTeam(player, team);
    }
}
