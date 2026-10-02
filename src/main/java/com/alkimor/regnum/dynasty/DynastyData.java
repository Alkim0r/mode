package com.alkimor.regnum.dynasty;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Династия игрока: супруг(а), дети, поколение, «снимок» навыков на случай продолжения за наследника. */
public class DynastyData {
    public record Child(UUID id, String name, long bornDay, String role) {
        public static final Codec<Child> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("id").forGetter(Child::id),
                Codec.STRING.fieldOf("name").forGetter(Child::name),
                Codec.LONG.fieldOf("born").forGetter(Child::bornDay),
                Codec.STRING.optionalFieldOf("role", "").forGetter(Child::role)
        ).apply(i, Child::new));

        public Child withRole(String r) {
            return new Child(id, name, bornDay, r);
        }
    }

    public static final Codec<DynastyData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("generation", 1).forGetter(d -> d.generation),
            Codec.STRING.optionalFieldOf("spouse", "").forGetter(d -> d.spouseId == null ? "" : d.spouseId.toString()),
            Codec.STRING.optionalFieldOf("spouseName", "").forGetter(d -> d.spouseName),
            Codec.LONG.optionalFieldOf("lastBirth", -1L).forGetter(d -> d.lastBirthDay),
            Child.CODEC.listOf().optionalFieldOf("children", List.of()).forGetter(d -> d.children),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("snapshot", Map.of()).forGetter(d -> d.xpSnapshot),
            Codec.LONG.optionalFieldOf("snapshotTime", 0L).forGetter(d -> d.snapshotTime)
    ).apply(i, DynastyData::fromSaved));

    public int generation = 1;
    public UUID spouseId;
    public String spouseName = "";
    public long lastBirthDay = -1;
    public final List<Child> children = new ArrayList<>();
    /** Опыт навыков до смерти — восстанавливается, если продолжить за наследника. */
    public final Map<String, Integer> xpSnapshot = new HashMap<>();
    public long snapshotTime = 0;

    private static DynastyData fromSaved(Integer gen, String spouse, String spouseName, Long lastBirth, List<Child> kids,
                                         Map<String, Integer> snap, Long snapTime) {
        DynastyData d = new DynastyData();
        d.generation = gen;
        d.spouseId = spouse.isEmpty() ? null : UUID.fromString(spouse);
        d.spouseName = spouseName;
        d.lastBirthDay = lastBirth;
        d.children.addAll(kids);
        d.xpSnapshot.putAll(snap);
        d.snapshotTime = snapTime;
        return d;
    }

    public boolean married() {
        return spouseId != null;
    }

    public static boolean adult(Child c, long today) {
        return today - c.bornDay() >= Dynasty.ADULT_DAYS;
    }

    public Optional<Child> firstAdult(long today) {
        return children.stream().filter(c -> adult(c, today)).findFirst();
    }
}
