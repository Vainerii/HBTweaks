package vai.hbtweaks.context.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/**
 * Developer-only client command <code>/hbtdebug set &lt;true|false&gt;</code>, used to toggle
 * the permission override at runtime. Only registered when the debug file exists.
 *
 * @see DebugPerm
 */
public final class DebugCommand {

    private DebugCommand() {}

    /** Registers the command on the client command dispatcher. */
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) ->
                dispatcher.register(literal("hbtdebug")
                        .then(literal("set")
                                .then(argument("value", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean value = BoolArgumentType.getBool(ctx, "value");
                                            DebugPerm.set(value);
                                            ctx.getSource().sendFeedback(
                                                    Component.literal("hasPerm = " + value));
                                            return 1;
                                        })))));
    }
}
