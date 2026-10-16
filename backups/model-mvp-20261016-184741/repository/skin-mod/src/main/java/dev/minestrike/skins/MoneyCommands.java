package dev.minestrike.skins;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.phasetranscrystal.fpsmatch.core.shop.ShopData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID)
public final class MoneyCommands {
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.m_82127_("minestrike")
                .then(Commands.m_82127_("money").requires(source -> source.m_6761_(2))
                        .then(Commands.m_82127_("add")
                                .then(Commands.m_82129_("players", EntityArgument.m_91470_())
                                        .then(Commands.m_82129_("amount", IntegerArgumentType.integer(1))
                                                .executes(MoneyCommands::execute))))));
    }

    public static int addMoney(ShopData<?> data, int amount) {
        int before = data.getMoney();
        data.addMoney(Math.min(amount, 16000));
        return data.getMoney() - before;
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int amount = IntegerArgumentType.getInteger(context, "amount"), changed = 0;
        for (var player : EntityArgument.m_91477_(context, "players")) {
            var map = MineStrikeSkins.map(player);
            var shop = map == null ? null : map.getShop(player).orElse(null);
            var data = shop == null ? null : shop.playersData.get(player.m_20148_());
            if (map == null || !map.isStart || MapBaseline.busy(map) || data == null) {
                context.getSource().m_81352_(Component.m_237113_(player.m_6302_() + " 未在可用的 CS 比赛商店中，未增加资金。"));
                continue;
            }
            int added = addMoney(data, amount), balance = data.getMoney();
            shop.syncShopMoneyData(player);
            context.getSource().m_288197_(() -> Component.m_237113_(player.m_6302_() + " 比赛资金 +" + added
                    + "，当前 " + balance + "（上限 16000）。"), true);
            changed++;
        }
        return changed;
    }

    private MoneyCommands() {}
}
