package net.lordimass.utils;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class TransformUtils {
    private TransformUtils() {}

    public static Rotation3f toDegrees(Rotation3f rot) {
        rot.setPitch((float) Math.toDegrees(rot.pitch()));
        rot.setYaw((float) Math.toDegrees(rot.yaw()));
        rot.setRoll((float) Math.toDegrees(rot.roll()));
        return rot;
    }

    public static Transform getEyeTransform(PlayerRef playerRef, ComponentAccessor<EntityStore> componentAccessor) {
        Ref<EntityStore> ref = playerRef.getReference();
        assert ref != null;
        var transform = playerRef.getTransform().clone();
        var headTransform = componentAccessor.getComponent(ref, HeadRotation.getComponentType());
        assert headTransform != null;
        var model = componentAccessor.getComponent(ref, ModelComponent.getComponentType());
        assert model != null;
        transform.getPosition().add(0, model.getModel().getEyeHeight(), 0);
        transform.setRotation(toDegrees(headTransform.getRotation()));
        return transform;
    }
}
