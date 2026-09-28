package net.lordimass.command;

import com.hypixel.hytale.builtin.commandmacro.MacroCommandParameter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.EasingType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
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

import net.lordimass.assets.CameraKeyframe.Keyframe;
import net.lordimass.assets.CameraSequenceAsset;
import net.lordimass.assets.DepthOfFieldSettingsAsset;

import java.awt.*;
import java.nio.file.Path;
import java.util.Map;

import javax.annotation.Nonnull;

public class CameraKeyframeCommand extends AbstractPlayerCommand {
    final RequiredArg<CameraSequenceAsset> sequenceArg;
    final OptionalArg<String> titleArg;
    final OptionalArg<String> notesArg;
    final OptionalArg<Float> durationArg;
    final OptionalArg<EasingType> easingArg;
    final OptionalArg<Float> fovArg;
    final OptionalArg<DepthOfFieldSettingsAsset> depthOfFieldArg;


    public CameraKeyframeCommand() {
        super("camerakeyframe", "Append a new frame to a camera sequence at your current position.");
        this.addAliases("keyframe");

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
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef,
            @Nonnull World world) {

        var seq = sequenceArg.get(commandContext);

        if (seq == null) {
            commandContext.sendMessage(
                    Message.raw("Couldn't find camera sequence '" + sequenceArg.get(commandContext) + "'")
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
            commandContext.sendMessage(Message.raw("Pack " + pack.getName() + " is immutable.").color(Color.RED));
            return;
        }

        var title = titleArg.get(commandContext);
        var notes = notesArg.get(commandContext);
        var duration = durationArg.get(commandContext);
        var easing = easingArg.get(commandContext);
        var fov = fovArg.get(commandContext);
        var dof = depthOfFieldArg.get(commandContext);

        var transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        assert transformComponent != null;
        var headTransform = store.getComponent(ref, HeadRotation.getComponentType());
        var eyeHeight = store.getComponent(ref, ModelComponent.getComponentType()).getModel().getEyeHeight(ref, store);
        var transform = transformComponent.getTransform();
        assert headTransform != null;
        transform.setRotation(toDegrees(headTransform.getRotation()));
        transform.getPosition().add(0, eyeHeight, 0);
        seq.addKeyframe(new Keyframe(transform, title, notes, duration, easing, fov, dof));

        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            try {
                seqStore.writeAssetToDisk(pack, Map.of(Path.of(seqName + ".json"), seq),
                        SingleplayerModule.isOwner(playerRef));
                playerRef.sendMessage(
                        Message.translation("server.command.keyframe.success")
                                .param("keyframe", seq.getCameraKeyframes().length)
                                .param("assetName", seqName).color(Color.GREEN)
                );
            } catch (Exception exception) {
                LOGGER.atSevere().withCause(exception).log("Failed to save effect preset '%s'", seqName);
                playerRef.sendMessage(
                        Message.translation("server.command.keyframe.fail")
                                .param("keyframe", seq.getCameraKeyframes().length)
                                .param("assetName", seqName)
                                .param("reason", exception.getMessage()).color(Color.RED)
                );
            }
        });
    }

    private Rotation3f toDegrees(Rotation3f rot) {
        rot.setPitch((float) Math.toDegrees(rot.pitch()));
        rot.setYaw((float) Math.toDegrees(rot.yaw()));
        rot.setRoll((float) Math.toDegrees(rot.roll()));
        return rot;
    }
}
