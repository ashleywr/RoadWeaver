package net.shiroha233.roadweaver.core.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Locale;

/**
 * Optional, persisted map presentation for one RoadWeaver endpoint.
 *
 * <p>The physical {@link StructureInfo#structureId()} remains the identity
 * used by planning and persistence. This small presentation record is purely
 * semantic: integrations can distinguish a gym, a starter settlement, a
 * landmark, or a cave without pretending they are different structures.</p>
 */
public record MapLocationIcon(Kind kind, String marker, int accentColor, String label) {
    public static final int DEFAULT_ACCENT_COLOR = 0xFF5FAE63;

    public MapLocationIcon {
        kind = kind == null ? Kind.DEFAULT : kind;
        marker = marker == null ? "" : marker;
        label = label == null ? "" : label;
    }

    public static final Codec<MapLocationIcon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Kind.CODEC.optionalFieldOf("kind", Kind.DEFAULT).forGetter(MapLocationIcon::kind),
            Codec.STRING.optionalFieldOf("marker", "").forGetter(MapLocationIcon::marker),
            Codec.INT.optionalFieldOf("accent_color", DEFAULT_ACCENT_COLOR).forGetter(MapLocationIcon::accentColor),
            Codec.STRING.optionalFieldOf("label", "").forGetter(MapLocationIcon::label)
    ).apply(instance, MapLocationIcon::new));

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(kind.ordinal());
        buffer.writeUtf(marker, 32);
        buffer.writeInt(accentColor);
        buffer.writeUtf(label, 160);
    }

    public static MapLocationIcon read(FriendlyByteBuf buffer) {
        int ordinal = buffer.readVarInt();
        Kind[] kinds = Kind.values();
        Kind kind = ordinal >= 0 && ordinal < kinds.length ? kinds[ordinal] : Kind.DEFAULT;
        return new MapLocationIcon(kind, buffer.readUtf(32), buffer.readInt(), buffer.readUtf(160));
    }

    public enum Kind {
        DEFAULT,
        VILLAGE,
        TOWN,
        GYM,
        ELITE_FOUR,
        STARTER,
        OUTPOST,
        LANDMARK,
        CAVE;

        public static final Codec<Kind> CODEC = Codec.STRING.xmap(Kind::fromSerializedName, Kind::serializedName);

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Kind fromSerializedName(String value) {
            if (value != null) {
                for (Kind kind : values()) {
                    if (kind.serializedName().equals(value.toLowerCase(Locale.ROOT))) return kind;
                }
            }
            return DEFAULT;
        }
    }
}
