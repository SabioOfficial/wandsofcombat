package net.sabio.wandsofcombat.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import net.sabio.wandsofcombat.aqua.AquaHearts;
import net.sabio.wandsofcombat.client.AquaHudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Hud.class)
public abstract class HeartRenderMixin {
    @Inject(
            method = "extractHeart(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Hud$HeartType;IIZZZ)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void wandsofcombat$replaceAquaHeart(GuiGraphicsExtractor graphics, @Coerce Object type, int xo, int yo, boolean isHardcore, boolean blinks, boolean half, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || !AquaHearts.isHoldingWand(player)) return;

        int left = client.getWindow().getGuiScaledWidth() / 2 - 91;
        int baseY = client.getWindow().getGuiScaledHeight() - 39;
        int slot = (xo - left) / 8;

        boolean firstRow = yo > baseY - 5;
        boolean aquaSlot = slot >= 0 && slot < AquaHudRenderer.aquaSlots();
        if (firstRow && aquaSlot) {
            AquaHudRenderer.SLOT_Y[slot] = yo;
            ci.cancel();
        }
    }

    @ModifyVariable(
            method = "extractHeart(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Hud$HeartType;IIZZZ)V",
            at = @At("HEAD"),
            argsOnly = true,
            name = "xo")
    private int wandsofcombat$shiftPastLostAquaHearts(int xo) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || !AquaHearts.isHoldingWand(player)) return xo;

        int left = client.getWindow().getGuiScaledWidth() / 2 - 91;
        int present = AquaHudRenderer.aquaSlots();
        int slot = (xo - left) / 8;
        return slot >= present ? xo + (AquaHudRenderer.AQUA_SLOTS - present) * 8 : xo;
    }
}
