package dev.minestrike.skins;

import net.minecraft.world.item.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.*;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class CombatEditorItem extends Item {
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,MineStrikeSkins.ID);
    public static final RegistryObject<Item> EDITOR=ITEMS.register("combat_editor",CombatEditorItem::new);
    public CombatEditorItem() { super(new Item.Properties().m_41487_(1)); }
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e)->{
            if(e.getTabKey().equals(CreativeModeTabs.f_256869_)) e.accept(EDITOR);
        });
    }
    @Override public InteractionResultHolder<ItemStack> m_7203_(Level level,Player player,InteractionHand hand) {
        if(player instanceof ServerPlayer server) {
            if(CombatStats.authorized(server)) CombatNetwork.open(server);
            else server.m_5661_(Component.m_237113_("需要创造模式及管理员权限"),true);
        }
        return InteractionResultHolder.m_19092_(player.m_21120_(hand),level.f_46443_);
    }
}
