package net.lordimass.assets;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.modules.camera.CameraKeyframeBuilder;
import com.hypixel.hytale.server.core.modules.camera.CameraSequenceBuilder;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import static net.lordimass.utils.UiUtils.hideUI;
import static net.lordimass.utils.UiUtils.showUI;

public class CameraSequenceAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, CameraSequenceAsset>> {
    public static final String ASSET_PATH = "CameraSequence/CameraSequence";

    private static AssetStore<String, CameraSequenceAsset, DefaultAssetMap<String, CameraSequenceAsset>> assetStore;

    @Nonnull public static final AssetBuilderCodec<String, @NotNull CameraSequenceAsset> CODEC =
        AssetBuilderCodec.builder(
            CameraSequenceAsset.class,
            CameraSequenceAsset::new,
            Codec.STRING,
            (asset, id) -> asset.id = id,
            CameraSequenceAsset::getId,
            (asset, data) -> asset.data = data,
            asset -> asset.data
        )
            .append(new KeyedCodec<>("Keyframes", new ArrayCodec<>(CameraKeyframe.CODEC, CameraKeyframe[]::new)),
                (asset, keyframes) -> asset.cameraKeyframes = keyframes,
                CameraSequenceAsset::getCameraKeyframes
            )
            .addValidator(Validators.nonNull())
            .addValidator(Validators.nonEmptyArray())
            .add()
            .append(new KeyedCodec<>("BaseFov", Codec.FLOAT),
                (asset, baseFov) -> asset.baseFov = baseFov,
                CameraSequenceAsset::getBaseFov
            )
            .add()
            .append(new KeyedCodec<>("HideUi", Codec.BOOLEAN),
                (asset, hideUI) -> asset.hideUI = hideUI,
                CameraSequenceAsset::isHideUI
            )
            .documentation("Whether the rest of the UI should be hidden while the effect is playing.")
            .add()
            .build();

    private AssetExtraInfo.Data data;
    @Getter private String id;
    @Getter private CameraKeyframe[] cameraKeyframes = new CameraKeyframe[0];
    @Getter private Float baseFov;
    @Getter private boolean hideUI;

    public CameraSequenceAsset() {}

    public CameraSequenceAsset(
        @Nonnull String id,
        @Nonnull CameraKeyframe initialKeyframe,
        @Nullable Float baseFov,
        boolean hideUI
    ) {
        this.id = id;
        this.baseFov = baseFov != null ? baseFov : 60;
        this.cameraKeyframes = new CameraKeyframe[]{initialKeyframe};
        this.hideUI = hideUI;
    }

    public static AssetStore<String, CameraSequenceAsset, DefaultAssetMap<String, CameraSequenceAsset>> getAssetStore() {
        if (assetStore == null) assetStore = AssetRegistry.getAssetStore(CameraSequenceAsset.class);
        return assetStore;
    }

    public static DefaultAssetMap<String, CameraSequenceAsset> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    // Ok technically kinda goes against the whole idea of immutability but then again, the asset was made the only way to play animations T-T
    public void setCameraKeyframes(CameraKeyframe[] cameraKeyframes) {
        this.cameraKeyframes = cameraKeyframes == null ? new CameraKeyframe[0] : cameraKeyframes;
    }

    public static void register(JavaPlugin host) {
        host.getAssetRegistry().register(
            HytaleAssetStore.builder(CameraSequenceAsset.class, new DefaultAssetMap<>())
                .setPath(ASSET_PATH)
                .setCodec(CODEC)
                .setKeyFunction(CameraSequenceAsset::getId)
                .build()
        );

        CameraKeyframe.CODEC.register(
            "Keyframe",
            CameraKeyframe.Keyframe.class,
            CameraKeyframe.Keyframe.CODEC);

        CameraKeyframe.CODEC.register(
            "KeyframeLookingAt",
            CameraKeyframe.KeyframeLookingAt.class,
            CameraKeyframe.KeyframeLookingAt.CODEC);
    }

    /**
     * Build the camera sequence builder object with all the keyframes.
     * WARNING: If <code>HideUI</code> is enabled, this will not take effect here.
     */
    public CameraSequenceBuilder buildSequenceBuilder(@Nullable PlayerRef playerRef, @Nullable Consumer<PlayerRef> onComplete) {
        CameraSequenceBuilder seqBuilder = new CameraSequenceBuilder()
            .baseFov(baseFov)
            .onComplete((playerRef1) -> {
                if (onComplete != null) onComplete.accept(playerRef1);
                showUI(playerRef1);
            });

        // Add keyframes
        for (CameraKeyframe keyframe : cameraKeyframes) {
            Vector3d position = keyframe.isRelativeToPlayer() && playerRef != null
                ? new Vector3d(playerRef.getTransform().getPosition()).add(keyframe.getPosition())
                : keyframe.getPosition();
            CameraKeyframeBuilder keyframeBuilder = new CameraKeyframeBuilder(
                keyframe.getDurationSeconds(),
                keyframe.getEasing()
            );

            if (keyframe.getFov() != null) keyframeBuilder.fov(keyframe.getFov());
            if (keyframe.getDepthOfFieldSettingsAsset() != null) keyframeBuilder.depthOfField(keyframe.getDepthOfFieldSettingsAsset().getDepthOfFieldSettings());
            if (keyframe instanceof CameraKeyframe.Keyframe) {
                Rotation3f look = keyframe.isRelativeToPlayer() && playerRef != null
                    ? new Rotation3f(playerRef.getHeadRotation()).add(((CameraKeyframe.Keyframe) keyframe).getLookRadians())
                    : ((CameraKeyframe.Keyframe) keyframe).getLookRadians();
                look.x = ((CameraKeyframe.Keyframe) keyframe).getLookRadians().x; // Pitch should never be relative.
                look.z = ((CameraKeyframe.Keyframe) keyframe).getLookRadians().z; // Roll should never be relative.
                float yaw = playerRef != null ? playerRef.getHeadRotation().yaw() : 0;
                position = keyframe.isRelativeToPlayer() && playerRef != null
                    ? new Vector3d(playerRef.getTransform().getPosition()).add(new Vector3d(
                    -keyframe.getPosition().x*Math.sin(yaw) + keyframe.getPosition().z*Math.cos(yaw),
                    keyframe.getPosition().y,
                    -keyframe.getPosition().x*Math.cos(yaw) - keyframe.getPosition().z*Math.sin(yaw)
                )) : keyframe.getPosition();
                seqBuilder = seqBuilder.keyframe(keyframeBuilder
                    .position(position)
                    .look(look)
                );
            } else if (keyframe instanceof CameraKeyframe.KeyframeLookingAt) {
                Vector3d lookAtPoint = keyframe.isRelativeToPlayer() && playerRef != null
                    ? playerRef.getTransform().getPosition().add(((CameraKeyframe.KeyframeLookingAt) keyframe).getLookAtPoint())
                    : ((CameraKeyframe.KeyframeLookingAt) keyframe).getLookAtPoint();
                seqBuilder = seqBuilder.keyframe(keyframeBuilder
                    .position(position)
                    .lookAt(lookAtPoint)
                );
            }
        }
        // Add flags to return camera control to player afterwards.
        return seqBuilder.addFlag((byte)1).addFlag((byte)2).addFlag((byte)4);
    }

    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(CameraSequenceAsset::getAssetStore));


    /**
     * Build the camera sequence builder object with all the keyframes.
     * WARNING: If <code>HideUI</code> is enabled, this will not take effect here.
     */
    public CameraSequenceBuilder buildSequenceBuilder(@Nullable PlayerRef playerRef) {
        return buildSequenceBuilder(playerRef, null);
    }

    public void play(@Nonnull PlayerRef playerRef, @Nullable Consumer<PlayerRef> onComplete) {
        if (hideUI) hideUI(playerRef);
        buildSequenceBuilder(playerRef, onComplete).sendTo(playerRef);
    }

    public void play(@Nonnull PlayerRef playerRef) {
        play(playerRef, null);
    }

    public void addKeyframe(CameraKeyframe keyframe) {
        CameraKeyframe[] updated = Arrays.copyOf(cameraKeyframes, cameraKeyframes.length + 1);
        updated[cameraKeyframes.length] = keyframe;
        cameraKeyframes = updated;
    }

    public void removeKeyframe(int keyframe) {
        cameraKeyframes[keyframe] = null;
        var listFrames = new ArrayList(Arrays.stream(cameraKeyframes).toList());
        listFrames.remove(keyframe);
        this.cameraKeyframes = (CameraKeyframe[]) listFrames.toArray(new CameraKeyframe[cameraKeyframes.length-1]);
    }

    public CameraSequenceAsset clone() {
        var seq = new CameraSequenceAsset();
        seq.baseFov = baseFov;
        seq.cameraKeyframes = Arrays.copyOf(cameraKeyframes, cameraKeyframes.length);
        seq.data = data;
        seq.hideUI = hideUI;
        seq.id = id;        
        return seq;
    }
}
