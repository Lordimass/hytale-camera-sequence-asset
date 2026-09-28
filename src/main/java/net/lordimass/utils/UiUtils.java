package net.lordimass.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.UUID;

public final class UiUtils {
    private UiUtils() {}

    public static void hideUI(PlayerRef playerRef) {
        // Hide UI if enabled
        UUID worldUUID = playerRef.getWorldUuid();
        if (worldUUID == null) return;
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) return;
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) return;
            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            if (player == null) return;
            player.getHudManager().setVisibleHudComponents(playerRef);
        });
    }

    public static void showUI(PlayerRef playerRef) {
        // Hide UI if enabled
        UUID worldUUID = playerRef.getWorldUuid();
        if (worldUUID == null) return;
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) return;
        world.execute(() -> {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null) return;
            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            if (player == null) return;
            player.getHudManager().resetVisibleHudComponents(playerRef);
        });
    }
}
