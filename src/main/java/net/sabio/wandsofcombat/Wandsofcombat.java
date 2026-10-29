package net.sabio.wandsofcombat;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.ItemComponentTooltipProviderRegistry;
import net.sabio.wandsofcombat.effect.ModEffects;
import net.sabio.wandsofcombat.entity.ModEntities;
import net.sabio.wandsofcombat.item.*;
import net.sabio.wandsofcombat.mana.ManaManager;
import net.sabio.wandsofcombat.network.ManaSyncPacket;
import net.sabio.wandsofcombat.potion.ModBrewingRecipes;
import net.sabio.wandsofcombat.potion.ModPotions;

import java.util.logging.Logger;

public class Wandsofcombat implements ModInitializer {
    public static final String MOD_ID = "wandsofcombat";
    public static final Logger LOGGER = Logger.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        ItemComponentTooltipProviderRegistry.addFirst(WandTooltip.TYPE);
        MagnetPullManager.initialize();
        ElectricWandLightningHandler.initialize();
        ElectricWandPassiveHandler.initialize();
        LightningStrikeHandler.initialize();
        PhantomModeHandler.initialize();
        ModEntities.init();
        PhantomWandStalkHandler.initialize();
        PhantomWandComboHandler.initialize();
        PullAttackScheduler.initialize();
        MagmaWandHandler.initialize();
        IceWandComboHandler.initialize();
        ManaSyncPacket.initialize();
        ManaManager.initialize();
        ModEffects.initialize();
        ModPotions.initialize();
        ModBrewingRecipes.initialize();
    }
}