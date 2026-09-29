package net.lordimass.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.AssetArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.singleplayer.SingleplayerModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.lordimass.assets.CameraSequenceAsset;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.nio.file.Path;
import java.util.Map;

public class RemoveKeyframeCommand extends AbstractPlayerCommand {
        private final RequiredArg<CameraSequenceAsset> sequenceArg = withRequiredArg(
            "sequence", "The ID of the camera sequence asset to append the frame to",
            new AssetArgumentType<>("CameraAsset", CameraSequenceAsset.class, "Camera Asset")
        );

    private final RequiredArg<Integer> keyFrameArg = withRequiredArg(
        "Keyframe", "Keyframe to remove", ArgTypes.INTEGER
    );

    public RemoveKeyframeCommand() {
        super("keyframe", "Permanently delete a keyframe from an existing camera sequence");
    }

    @Override
    protected void execute(
        @NonNull CommandContext context,
        @NonNull Store<EntityStore> store,
        @NonNull Ref<EntityStore> ref,
        @NonNull PlayerRef playerRef,
        @NonNull World world
    ) {
        var seq = sequenceArg.get(context);

        if (seq == null) {
            context.sendMessage(
                Message.raw("Couldn't find camera sequence '" + sequenceArg.get(context) + "'")
                    .color(Color.RED));
            return;
        }

        // get the store
        var seqStore = CameraSequenceAsset.getAssetStore();
        var seqName = seq.getId();
        var existingPackName = seqStore.getAssetMap().getAssetPack(seqName);
        assert existingPackName != null;
        var pack = AssetModule.get().getAssetPack(existingPackName);
        if (pack.isImmutable()) {
            context.sendMessage(Message.raw("Pack " + pack.getName() + " is immutable.").color(Color.RED));
            return;
        }

        var frame = keyFrameArg.get(context);
        var frames = seq.getCameraKeyframes();
        if (frame >= frames.length || frame < 0) {
            context.sendMessage(Message.raw("Frame " + frame + " out of range for sequence of length " + frames.length));
            return;
        }
        seq.removeKeyframe(frame);

        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            try {
                seqStore.writeAssetToDisk(pack, Map.of(Path.of(seqName + ".json"), seq),
                    SingleplayerModule.isOwner(playerRef));
                context.sendMessage(
                    Message.translation("server.command.camerasequence.remove.keyframe.success")
                        .param("keyframe", frame)
                        .param("sequenceName", seqName).color(Color.GREEN)
                );
            } catch (Exception exception) {
                LOGGER.atSevere().withCause(exception).log("Failed to save effect preset '%s'", seqName);
                context.sendMessage(
                    Message.translation("server.command.camerasequence.remove.keyframe.fail")
                        .param("keyframe", frame)
                        .param("sequenceName", seqName)
                        .param("reason", exception.getMessage()).color(Color.RED)
                );
            }
        });
    }
}
