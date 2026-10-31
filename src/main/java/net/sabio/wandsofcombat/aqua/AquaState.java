package net.sabio.wandsofcombat.aqua;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.sabio.wandsofcombat.Wandsofcombat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class AquaState extends SavedData {
    private final Map<UUID, Integer> aquaPoints;

    private AquaState(Map<UUID, Integer> aquaPoints) {
        this.aquaPoints = new HashMap<>(aquaPoints);
    }

    public AquaState() {
        this.aquaPoints = new HashMap<>();
    }

    private static final Codec<AquaState> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(
            stringIntegerMap -> {
                Map<UUID, Integer> result = new HashMap<>();
                stringIntegerMap.forEach((key, value) -> {
                    try {
                        result.put(UUID.fromString(key), value);
                    } catch (IllegalArgumentException ignored) {}
                });
                return new AquaState(result);
            },
            aquaState -> {
                Map<String, Integer> result = new HashMap<>();
                aquaState.aquaPoints.forEach((key, value) -> result.put(key.toString(), value));
                return result;
            }
    );

    public static final SavedDataType<AquaState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Wandsofcombat.MOD_ID, "wandsofcombat_aqua"),
            AquaState::new,
            CODEC,
            null
    );

    public static AquaState get(MinecraftServer server) {
        return Objects.requireNonNull(server.getLevel(ServerLevel.OVERWORLD)).getDataStorage().computeIfAbsent(TYPE);
    }

    public int getPoints(UUID player) {
        return aquaPoints.getOrDefault(player, AquaHearts.MAX_AQUA_HP);
    }

    public void setPoints(UUID player, int points) {
        aquaPoints.put(player, Math.clamp(points, 0, AquaHearts.MAX_AQUA_HP));
        setDirty();
    }
}
