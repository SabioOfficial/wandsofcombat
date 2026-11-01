package net.sabio.wandsofcombat.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.sabio.wandsofcombat.aqua.AquaHearts;
import net.sabio.wandsofcombat.network.AquaSyncPacket;

@Environment(EnvType.CLIENT)
public final class AquaClientState {
    private static int aquaHp = AquaHearts.MAX_AQUA_HP;
    private static boolean unlocked = false;
    private static boolean returning = false;

    private AquaClientState() {}

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(AquaSyncPacket.TYPE, (payload, context) -> context.client().execute(() -> update(payload.aquaHp(), payload.unlocked(), payload.returning())));
    }

    public static void update(int newAquaHp, boolean newUnlocked, boolean newReturning) {
        AquaHearts.setClientView(newAquaHp, newUnlocked);
        aquaHp = newAquaHp;
        unlocked = newUnlocked;
        returning = newReturning;
    }

    public static int getAquaHp() {
        return aquaHp;
    }

    public static boolean isUnlocked() {
        return unlocked;
    }

    public static boolean isReturning() {
        return returning;
    }
}
