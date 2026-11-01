package fr.anger.echoes.fireforge.client;

import fr.anger.echoes.fireforge.witherset.AshDoubleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Le skin du joueur dont un double est issu : celui de la liste des joueurs s'il est en ligne, sinon chargé comme celui
 * d'une tête de joueur (une fois), et le skin par défaut en attendant.
 */
final class DoubleSkins {

    private DoubleSkins() {}

    private static final Map<UUID, PlayerSkin> LOADED = new ConcurrentHashMap<>();
    private static final Set<UUID> LOADING = ConcurrentHashMap.newKeySet();

    static PlayerSkin of(AshDoubleEntity entity) {
        UUID owner = entity.owner();
        if (owner == null) return DefaultPlayerSkin.get(entity.getUUID());
        Minecraft mc = Minecraft.getInstance();
        PlayerInfo online = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(owner);
        if (online != null) return online.getSkin();
        PlayerSkin skin = LOADED.get(owner);
        if (skin != null) return skin;
        if (LOADING.add(owner)) {
            SkullBlockEntity.fetchGameProfile(owner)
                    .thenCompose(profile -> profile.map(p -> mc.getSkinManager().getOrLoad(p))
                            .orElseGet(() -> CompletableFuture.completedFuture(DefaultPlayerSkin.get(owner))))
                    .thenAccept(loaded -> LOADED.put(owner, loaded));
        }
        return DefaultPlayerSkin.get(owner);
    }
}
