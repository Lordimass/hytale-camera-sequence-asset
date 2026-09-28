package net.lordimass.command;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class NewCommand extends AbstractCommandCollection {
    public NewCommand() {
        super("new", "Create a new sequence or keyframe");

        addSubCommand(new NewCameraSequenceCommand());
        addSubCommand(new NewKeyframeCommand());
    }
}
