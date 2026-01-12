package com.github.nalamodikk.commands;

import com.github.nalamodikk.display.DisplayEntityManager;
import com.github.nalamodikk.display.impl.MagicCircleDisplayEntity;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 測試指令：生成魔法陣
 */
public class TestDisplayCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("test_display")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("magic_circle")
                        .executes(context -> {
                            Vec3 pos = context.getSource().getPosition();
                            MagicCircleDisplayEntity entity = (MagicCircleDisplayEntity) DisplayEntityManager
                                    .create("magic_circle", UUID.randomUUID());

                            if (entity != null) {
                                entity.setPos(pos.add(0, 1, 0)); // 在玩家上方1格
                                entity.setRadius(2.0f);
                                entity.setRotationSpeed(2.0f);
                                entity.setColor(0x00FFFF); // 青色

                                context.getSource().sendSuccess(
                                        () -> Component.literal("已生成魔法陣於 " + pos),
                                        true);
                                return 1;
                            }

                            context.getSource().sendFailure(Component.literal("生成失敗！"));
                            return 0;
                        })
                        .then(Commands.argument("radius", FloatArgumentType.floatArg(0.5f, 10.0f))
                                .executes(context -> {
                                    Vec3 pos = context.getSource().getPosition();
                                    float radius = FloatArgumentType.getFloat(context, "radius");

                                    MagicCircleDisplayEntity entity = (MagicCircleDisplayEntity) DisplayEntityManager
                                            .create("magic_circle", UUID.randomUUID());

                                    if (entity != null) {
                                        entity.setPos(pos.add(0, 1, 0));
                                        entity.setRadius(radius);
                                        entity.setRotationSpeed(2.0f);

                                        context.getSource().sendSuccess(
                                                () -> Component.literal("已生成半徑 " + radius + " 的魔法陣"),
                                                true);
                                        return 1;
                                    }

                                    return 0;
                                }))));
    }
}
