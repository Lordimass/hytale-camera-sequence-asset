package net.lordimass.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import net.lordimass.assets.CameraSequenceAsset;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.nio.file.Path;

public class RemoveSequenceCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> nameArg = withRequiredArg(
        "name", "The name of the new camera sequence", ArgTypes.STRING
    );

    public RemoveSequenceCommand() {
        super("sequence", "Permanently delete an existing camera sequence");
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

        final String seqName = nameArg.get(context).trim();
        if (seqName.contains("..")) {
            context.sendMessage(Message.translation("server.builderTools.attemptedToSaveOutsidePrefabsDir"));
            return;
        }

        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            try {
                seqStore.removeAssetWithPath(Path.of(seqName + ".json"));
                context.sendMessage(
                    Message.translation("server.command.camerasequence.remove.sequence.fail")
                        .param("sequenceName", seqName).color(Color.GREEN)
                );
            } catch (Exception e) {
                context.sendMessage(
                    Message.translation("server.command.camerasequence.remove.sequence.fail")
                        .param("sequenceName", seqName)
                        .param("reason", e.getMessage()).color(Color.RED)
                );
            }

        });

    }
}
