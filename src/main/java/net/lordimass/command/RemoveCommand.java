package net.lordimass.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import org.jspecify.annotations.NonNull;

public class RemoveCommand extends AbstractCommandCollection {
    public RemoveCommand() {
        super("remove", "Remove an existing sequence or keyframe");

        addSubCommand(new RemoveSequenceCommand());
        addSubCommand(new RemoveKeyframeCommand());
    }
}
