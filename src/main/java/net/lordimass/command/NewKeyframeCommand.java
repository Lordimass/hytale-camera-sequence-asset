package net.lordimass.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.EasingType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.AssetArgumentType;
import com.hypixel.hytale.server.core.command.system.arguments.types.EnumArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.singleplayer.SingleplayerModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.lordimass.assets.CameraKeyframe;
import net.lordimass.assets.CameraKeyframe.Keyframe;
import net.lordimass.assets.CameraSequenceAsset;
import net.lordimass.assets.DepthOfFieldSettingsAsset;
import net.lordimass.utils.TransformUtils;

import javax.annotation.Nonnull;
import java.awt.*;
import java.nio.file.Path;
import java.util.Map;

import static net.lordimass.utils.TransformUtils.toDegrees;

public class NewKeyframeCommand extends AbstractPlayerCommand {
    final RequiredArg<CameraSequenceAsset> sequenceArg;
    final OptionalArg<String> titleArg;
    final OptionalArg<String> notesArg;
    final OptionalArg<Float> durationArg;
    final OptionalArg<EasingType> easingArg;
    final OptionalArg<Float> fovArg;
    final OptionalArg<DepthOfFieldSettingsAsset> depthOfFieldArg;
    final OptionalArg<Integer> keyFrameArg;


    public NewKeyframeCommand() {
        super("keyframe", "Append a new frame to a camera sequence at your current position.");

        this.sequenceArg = withRequiredArg("sequence", "The ID of the camera sequence asset to append the frame to",
                new AssetArgumentType<>("CameraAsset", CameraSequenceAsset.class,
                        "Camera Asset")
        );
        this.titleArg = withOptionalArg(
            "title", "A human readable title to give the keyframe.",
            ArgTypes.STRING
        );
        this.notesArg = withOptionalArg(
            "notes", "Human readable notes to attack to the keyframe.",
            ArgTypes.STRING
        );
        this.durationArg = withOptionalArg(
            "duration", "The length of time this keyframe should take, in seconds.",
            ArgTypes.FLOAT
        );
        this.easingArg = withOptionalArg(
            "easing", "The type of camera easing that should be used.",
            new EnumArgumentType<>("EasingType", EasingType.class)
        );
        this.fovArg = withOptionalArg(
            "fov", "The FOV the camera should have moved to by the end of this keyframe.",
            ArgTypes.FLOAT
        );
        this.depthOfFieldArg = withOptionalArg("depthoffield", "Depth of field preset effect.",
            new AssetArgumentType<>("DepthOfFieldAsset", DepthOfFieldSettingsAsset.class,
                "Depth of field asset")
        );
        this.keyFrameArg = withOptionalArg("Keyframe", "Keyframe to overwrite", ArgTypes.INTEGER);
    }

    @Override
    protected void execute(@Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

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

        var title = titleArg.get(context);
        var notes = notesArg.get(context);
        var duration = durationArg.get(context);
        var easing = easingArg.get(context);
        var fov = fovArg.get(context);
        var dof = depthOfFieldArg.get(context);

        var transform = TransformUtils.getEyeTransform(ref, store);
        var frame = keyFrameArg.get(context);
        var newKeyframe = new Keyframe(transform, title, notes, duration, easing, fov, dof);
        if (frame != null) {
            CameraKeyframe[] frames = seq.getCameraKeyframes();
            if (frame > frames.length || frame <= 0) {
                context.sendMessage(Message.raw("Frame " + frame + " out of range for sequence of length " + frames.length));
                return;
            }
            frames[frame-1] = newKeyframe;
        } else {
            seq.addKeyframe(newKeyframe);
        }

        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            try {
                seqStore.writeAssetToDisk(pack, Map.of(Path.of(seqName + ".json"), seq),
                        SingleplayerModule.isOwner(playerRef));
                context.sendMessage(
                        Message.translation("server.command.keyframe.success")
                                .param("keyframe", seq.getCameraKeyframes().length)
                                .param("assetName", seqName).color(Color.GREEN)
                );
            } catch (Exception exception) {
                LOGGER.atSevere().withCause(exception).log("Failed to save effect preset '%s'", seqName);
                context.sendMessage(
                        Message.translation("server.command.keyframe.fail")
                                .param("keyframe", frame != null ? frame : seq.getCameraKeyframes().length)
                                .param("assetName", seqName)
                                .param("reason", exception.getMessage()).color(Color.RED)
                );
            }
        });
    }
}
