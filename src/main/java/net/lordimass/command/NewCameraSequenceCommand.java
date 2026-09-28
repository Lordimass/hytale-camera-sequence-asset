package net.lordimass.command;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.builtin.buildertools.BuilderToolsUserData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.DefaultArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.singleplayer.SingleplayerModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.lordimass.assets.CameraKeyframe;
import net.lordimass.assets.CameraSequenceAsset;
import net.lordimass.utils.PackUtils;
import net.lordimass.utils.TransformUtils;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.nio.file.Path;
import java.util.Map;

public class NewCameraSequenceCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> nameArg = withRequiredArg(
        "name", "The name of the new camera sequence", ArgTypes.STRING
    );
    private final DefaultArg<String> packArg = withDefaultArg(
        "pack", "server.commands.prefab.save.pack.desc", ArgTypes.STRING, "",
        "server.commands.prefab.save.pack.desc"
    );
    private final FlagArg overwriteFlag = withFlagArg(
        "overwrite", "server.commands.prefab.save.overwrite.desc"
    );
    private final FlagArg hideUiFlag = withFlagArg(
        "hideui", "Hide the player's UI while this sequence is playing"
    );
    private final OptionalArg<Float> fovArg = withOptionalArg(
        "basefov", "The base FOV to use for this sequence.", ArgTypes.FLOAT
    );

    public NewCameraSequenceCommand() {
        super("sequence", "Create a new camera sequence");
        
    }

    @Override
    protected void execute(
        @NonNull CommandContext context,
        @NonNull Store<EntityStore> store,
        @NonNull Ref<EntityStore> ref,
        @NonNull PlayerRef playerRef,
        @NonNull World world
    ) {
        final var playerComponent = store.getComponent(ref, Player.getComponentType());
        assert playerComponent != null;

        var seqStore = CameraSequenceAsset.getAssetStore();
        final String packName = packArg.get(context);
        final AssetPack pack = PackUtils.resolveTargetPack(
            packName != null ? packName : "", playerComponent, context
        );
        if (pack == null) return;
        BuilderToolsUserData.get(playerComponent).setLastSavePack(pack.getName());


        final String seqName = nameArg.get(context).trim();
        if (seqName.isBlank()) {
            context.sendMessage(Message.translation("server.builderTools.prefabSave.nameRequired"));
            return;
        }
        if (seqName.contains("..")) {
            context.sendMessage(Message.translation("server.builderTools.attemptedToSaveOutsidePrefabsDir"));
            return;
        }

        final boolean overwrite = overwriteFlag.get(context);

        if (CameraSequenceAsset.getAssetMap().getAsset(seqName) != null && !overwrite) {
            playerRef.sendMessage(
                Message.raw("Sequence " + seqName + " already exists!" +
                    "Supply the --overwrite flag to overwrite it with a fresh asset")
                    .color(Color.RED)
            );
            return;
        }

        // Account for player eye height and head rotation in initial keyframe
        var transform = TransformUtils.getEyeTransform(playerRef, store);

        CameraSequenceAsset seq = new CameraSequenceAsset(
            seqName,
            new CameraKeyframe.Keyframe(transform, null, null, null, null, null, null),
            fovArg.get(context),
            hideUiFlag.get(context)
        );

        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            try {
                seqStore.writeAssetToDisk(
                    pack,
                    Map.of(Path.of(seq.getId() + ".json"), seq),
                    SingleplayerModule.isOwner(playerRef)
                );
                playerRef.sendMessage(
                    Message.translation("server.command.camerasequence.new.success")
                        .param("sequenceName", seq.getId())
                        .param("packName", pack.getName()).color(Color.GREEN)
                );
            } catch (Exception e) {
                playerRef.sendMessage(
                    Message.translation("server.command.camerasequence.new.fail")
                        .param("sequenceName", seq.getId())
                        .param("packName", pack.getName())
                        .param("reason", e.getMessage()).color(Color.RED)
                );
            }
        });


    }
}
