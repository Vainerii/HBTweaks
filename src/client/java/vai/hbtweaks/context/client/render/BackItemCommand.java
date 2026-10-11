// DELETEME

// package vai.hbtweaks.context.client.render;
// 
// import com.mojang.brigadier.arguments.FloatArgumentType;
// import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
// import net.minecraft.network.chat.Component;
// 
// import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
// import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;
//
// public final class BackItemCommand {
// 
//     private BackItemCommand() {}
// 
//     public static void register() {
//         ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) ->
//                 dispatcher.register(literal("hbback")
//                         .executes(ctx -> {
//                             BackItems.enabled = !BackItems.enabled;
//                             ctx.getSource().sendFeedback(Component.literal(
//                                     BackItems.enabled ? "Back placement ON" : "Vanilla placement"));
//                             return 1;
//                         })
//                         .then(literal("freeze")
//                                 .executes(ctx -> {
//                                     BackItems.freezeArms = !BackItems.freezeArms;
//                                     ctx.getSource().sendFeedback(Component.literal(
//                                             BackItems.freezeArms ? "Arms frozen" : "Arms animating"));
//                                     return 1;
//                                 }))
//                         .then(literal("show")
//                                 .executes(ctx -> {
//                                     ctx.getSource().sendFeedback(Component.literal(show()));
//                                     return 1;
//                                 }))
//                         .then(literal("rot")
//                                 .then(argument("pitch", FloatArgumentType.floatArg())
//                                         .then(argument("yaw", FloatArgumentType.floatArg())
//                                                 .then(argument("roll", FloatArgumentType.floatArg())
//                                                         .executes(ctx -> {
//                                                             BackItems.pitchDegrees = FloatArgumentType.getFloat(ctx, "pitch");
//                                                             BackItems.yawDegrees = FloatArgumentType.getFloat(ctx, "yaw");
//                                                             BackItems.rollDegrees = FloatArgumentType.getFloat(ctx, "roll");
//                                                             ctx.getSource().sendFeedback(Component.literal(show()));
//                                                             return 1;
//                                                         })))))
//                         .then(literal("pos")
//                                 .then(argument("x", FloatArgumentType.floatArg())
//                                         .then(argument("y", FloatArgumentType.floatArg())
//                                                 .then(argument("z", FloatArgumentType.floatArg())
//                                                         .executes(ctx -> {
//                                                             BackItems.offsetX = FloatArgumentType.getFloat(ctx, "x");
//                                                             BackItems.offsetY = FloatArgumentType.getFloat(ctx, "y");
//                                                             BackItems.offsetZ = FloatArgumentType.getFloat(ctx, "z");
//                                                             ctx.getSource().sendFeedback(Component.literal(show()));
//                                                             return 1;
//                                                         })))))));
//     }
// 
//     private static String show() {
//         return "back=" + BackItems.enabled
//                 + " pos=" + BackItems.offsetX + " " + BackItems.offsetY + " " + BackItems.offsetZ
//                 + " freeze=" + BackItems.freezeArms + " rot=" + BackItems.pitchDegrees + " " + BackItems.yawDegrees + " " + BackItems.rollDegrees;
//     }
// }
