package net.lordimass.utils;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.builtin.buildertools.BuilderToolsUserData;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.entity.entities.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PackUtils {

    private static final Message MESSAGE_PACK_NOT_FOUND = Message.translation("server.commands.editprefab.save.pack.notFound");
    private static final Message MESSAGE_PACK_IMMUTABLE = Message.translation("server.commands.editprefab.save.pack.immutable");
    private static final Message MESSAGE_PACK_NO_PACK = Message.translation("server.command.camerasequence.new.packRequired");
    private PackUtils() {}

    @Nullable
    public static AssetPack resolveTargetPack(
        @Nonnull String explicitPackName, @Nonnull Player playerComponent, @Nonnull CommandContext context
    ) {
        AssetModule assetModule = AssetModule.get();
        if (!explicitPackName.isEmpty()) {
            AssetPack pack = assetModule.getAssetPack(explicitPackName);
            if (pack == null) {
                context.sendMessage(MESSAGE_PACK_NOT_FOUND.param("name", explicitPackName));
                return null;
            } else if (pack.isImmutable()) {
                context.sendMessage(MESSAGE_PACK_IMMUTABLE.param("name", explicitPackName));
                return null;
            } else {
                return pack;
            }
        } else {
            String lastPack = BuilderToolsUserData.get(playerComponent).getLastSavePack();
            if (lastPack != null) {
                AssetPack pack = assetModule.getAssetPack(lastPack);
                if (pack != null && !pack.isImmutable()) {
                    return pack;
                }
            }

            AssetPack basePack = assetModule.getBaseAssetPack();
            if (!basePack.isImmutable()) {
                return basePack;
            }

            context.sendMessage(MESSAGE_PACK_NO_PACK);
            return null;
        }
    }
}
