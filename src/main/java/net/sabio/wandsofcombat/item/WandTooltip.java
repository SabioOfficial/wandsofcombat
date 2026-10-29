package net.sabio.wandsofcombat.item;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.sabio.wandsofcombat.Wandsofcombat;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.util.function.Consumer;

public record WandTooltip(Wand wand) implements TooltipProvider {
    public enum Wand implements StringRepresentable {
        MAGNET("magnet"),
        ICE("ice"),
        ELECTRIC("electric"),
        PHANTOM("phantom"),
        MAGMA("magma");

        public static final Codec<Wand> CODEC = StringRepresentable.fromEnum(Wand::values);
        private final String id;

        Wand(String id) {
            this.id = id;
        }

        @Override
        public @NonNull String getSerializedName() {
            return id;
        }
    }

    public static final DataComponentType<WandTooltip> TYPE = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "wand_tooltip"),
            DataComponentType.<WandTooltip>builder()
                    .persistent(Wand.CODEC.xmap(WandTooltip::new, WandTooltip::wand))
                    .networkSynchronized(ByteBufCodecs.VAR_INT.map(i -> new WandTooltip(Wand.values()[i]), t -> t.wand().ordinal()))
                    .build()
    );

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> tooltip, TooltipFlag flag, DataComponentGetter components) {
        switch (wand) {
            case MAGNET -> MagnetWandItem.addTooltip(tooltip);
            case ICE -> IceWandItem.addTooltip(tooltip);
            case ELECTRIC -> ElectricWandItem.addTooltip(tooltip);
            case PHANTOM -> PhantomWandItem.addTooltip(tooltip);
            case MAGMA -> MagmaWandItem.addTooltip(tooltip);
        }
    }
}
