package net.sabio.wandsofcombat.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.sabio.wandsofcombat.Wandsofcombat;
import org.jspecify.annotations.NonNull;

public record AquaSyncPacket(int aquaHp, boolean unlocked, boolean returning) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AquaSyncPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "aqua_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AquaSyncPacket> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, AquaSyncPacket::aquaHp, ByteBufCodecs.BOOL, AquaSyncPacket::unlocked, ByteBufCodecs.BOOL, AquaSyncPacket::returning, AquaSyncPacket::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
    }
}
