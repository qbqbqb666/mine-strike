package dev.minestrike.skins.mixin;

import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameOverlay;
import com.phasetranscrystal.blockoffensive.entity.CompositionC4Entity;
import com.phasetranscrystal.fpsmatch.core.map.MapTeams;
import dev.minestrike.skins.BombHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;
import java.util.UUID;

public final class BombHudMixin {
    @Mixin(value = CompositionC4Entity.class, remap = false)
    public static abstract class Fuse {
        @Redirect(method = "setFuse", at = @At(value = "INVOKE", target = "Lcom/phasetranscrystal/fpsmatch/core/map/MapTeams;getSpecPlayers()Ljava/util/List;"))
        private List<UUID> minestrike$timerRecipients(MapTeams teams) {
            return teams.getJoinedPlayersWithSpec();
        }
    }

    @Mixin(value = CSGameOverlay.class, remap = false)
    public static abstract class Overlay {
        @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;m_280056_(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"))
        private int minestrike$bombIcon(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean shadow) {
            if (BombHud.visible(text, CSClientData.isStart, CSClientData.isWaitingWinner,
                    CSClientData.time, CSClientData.bombFuse, CSClientData.bombTotalFuse)) {
                BombHud.draw(graphics, CSClientData.bombFuse, CSClientData.bombTotalFuse, Minecraft.m_91087_().m_91296_());
                return 0;
            }
            return graphics.m_280056_(font, text, x, y, color, shadow);
        }
    }

    private BombHudMixin() {}
}
