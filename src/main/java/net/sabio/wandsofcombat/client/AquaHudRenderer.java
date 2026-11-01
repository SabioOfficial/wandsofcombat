package net.sabio.wandsofcombat.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.sabio.wandsofcombat.Wandsofcombat;
import net.sabio.wandsofcombat.aqua.AquaHearts;

import java.util.Arrays;

@Environment(EnvType.CLIENT)
public final class AquaHudRenderer {
    private static final int ICON_SIZE = 9;
    private static final int ICON_SPACING = 8;
    private static final int PULSE_INTERVAL_TICKS = 5;
    public static final int AQUA_SLOTS = AquaHearts.MAX_AQUA_HP / 2;
    private static final int NO_Y = Integer.MIN_VALUE;
    public static final int[] SLOT_Y = {NO_Y, NO_Y, NO_Y};

    private static final Identifier TEX_FULL = tex("aqua_heart_full");
    private static final Identifier TEX_HALF = tex("aqua_heart_half");
    private static final Identifier TEX_EMPTY = tex("aqua_heart_empty");
    private static final Identifier TEX_FULL_CHARGED = tex("aqua_heart_full_charged");
    private static final Identifier TEX_HALF_CHARGED = tex("aqua_heart_half_charged");
    private static final Identifier TEX_EMPTY_CHARGED = tex("aqua_heart_empty_charged");

    private AquaHudRenderer() {}

    public static int aquaSlots() {
        return AquaClientState.getAquaHp() / 2;
    }

    private static Identifier tex(String name) {
        return Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "textures/gui/sprites/hud/" + name + ".png");
    }

    public static void initialize() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HEALTH_BAR,
                Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "aqua_hearts"),
                AquaHudRenderer::render
        );
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        Player player = client.player;
        if (player == null || !AquaHearts.isHoldingWand(player)) {
            return;
        }

        boolean charged = AquaClientState.isUnlocked() && (player.tickCount / PULSE_INTERVAL_TICKS) % 2 == 0;
        int aquaHp = AquaClientState.getAquaHp();
        int shown = Math.min(aquaHp, Mth.ceil(player.getHealth()));

        int left = client.getWindow().getGuiScaledWidth() / 2 - 91;
        int y = client.getWindow().getGuiScaledHeight() - 39;

        for (int i = 0; i < AQUA_SLOTS; i++) {
            int fill = Math.clamp(shown - i * 2, 0, 2);
            Identifier texture = switch (fill) {
                case 2 -> charged ? TEX_FULL_CHARGED : TEX_FULL;
                case 1 -> charged ? TEX_HALF_CHARGED : TEX_HALF;
                default -> charged ? TEX_EMPTY_CHARGED : TEX_EMPTY;
            };
            int slotY = SLOT_Y[i] != NO_Y ? SLOT_Y[i] : y;
            SLOT_Y[i] = NO_Y;
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, left + i * ICON_SPACING, slotY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
        Arrays.fill(SLOT_Y, NO_Y);
    }
}
