package com.alkimor.regnum.client.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Сгенерировано tools/modelgen/run.py — не редактировать вручную. */
public final class ModelIndex {
    private ModelIndex() {}

    /** [культура][тип модели]; typeIndex связывает имя SoldierType с таблицей. */
    public static final ModelLayerLocation[][] UNITS = {
            {UnitNorthMilitiaModel.LAYER, UnitNorthSwordsmanModel.LAYER, UnitNorthArcherModel.LAYER, UnitNorthKnightModel.LAYER, UnitNorthShieldmanModel.LAYER, UnitNorthSpearmanModel.LAYER, UnitNorthGreatswordModel.LAYER, UnitNorthLightCavModel.LAYER, UnitNorthHeavyCavModel.LAYER, UnitNorthHorseArcherModel.LAYER, UnitNorthCrossbowModel.LAYER, UnitNorthMusketeerModel.LAYER, UnitNorthBombardierModel.LAYER, UnitNorthScoutModel.LAYER},
            {UnitEmpireMilitiaModel.LAYER, UnitEmpireSwordsmanModel.LAYER, UnitEmpireArcherModel.LAYER, UnitEmpireKnightModel.LAYER, UnitEmpireShieldmanModel.LAYER, UnitEmpireSpearmanModel.LAYER, UnitEmpireGreatswordModel.LAYER, UnitEmpireLightCavModel.LAYER, UnitEmpireHeavyCavModel.LAYER, UnitEmpireHorseArcherModel.LAYER, UnitEmpireCrossbowModel.LAYER, UnitEmpireMusketeerModel.LAYER, UnitEmpireBombardierModel.LAYER, UnitEmpireScoutModel.LAYER},
            {UnitWestMilitiaModel.LAYER, UnitWestSwordsmanModel.LAYER, UnitWestArcherModel.LAYER, UnitWestKnightModel.LAYER, UnitWestShieldmanModel.LAYER, UnitWestSpearmanModel.LAYER, UnitWestGreatswordModel.LAYER, UnitWestLightCavModel.LAYER, UnitWestHeavyCavModel.LAYER, UnitWestHorseArcherModel.LAYER, UnitWestCrossbowModel.LAYER, UnitWestMusketeerModel.LAYER, UnitWestBombardierModel.LAYER, UnitWestScoutModel.LAYER},
            {UnitSteppeMilitiaModel.LAYER, UnitSteppeSwordsmanModel.LAYER, UnitSteppeArcherModel.LAYER, UnitSteppeKnightModel.LAYER, UnitSteppeShieldmanModel.LAYER, UnitSteppeSpearmanModel.LAYER, UnitSteppeGreatswordModel.LAYER, UnitSteppeLightCavModel.LAYER, UnitSteppeHeavyCavModel.LAYER, UnitSteppeHorseArcherModel.LAYER, UnitSteppeCrossbowModel.LAYER, UnitSteppeMusketeerModel.LAYER, UnitSteppeBombardierModel.LAYER, UnitSteppeScoutModel.LAYER},
            {UnitSultanateMilitiaModel.LAYER, UnitSultanateSwordsmanModel.LAYER, UnitSultanateArcherModel.LAYER, UnitSultanateKnightModel.LAYER, UnitSultanateShieldmanModel.LAYER, UnitSultanateSpearmanModel.LAYER, UnitSultanateGreatswordModel.LAYER, UnitSultanateLightCavModel.LAYER, UnitSultanateHeavyCavModel.LAYER, UnitSultanateHorseArcherModel.LAYER, UnitSultanateCrossbowModel.LAYER, UnitSultanateMusketeerModel.LAYER, UnitSultanateBombardierModel.LAYER, UnitSultanateScoutModel.LAYER},
            {UnitClansMilitiaModel.LAYER, UnitClansSwordsmanModel.LAYER, UnitClansArcherModel.LAYER, UnitClansKnightModel.LAYER, UnitClansShieldmanModel.LAYER, UnitClansSpearmanModel.LAYER, UnitClansGreatswordModel.LAYER, UnitClansLightCavModel.LAYER, UnitClansHeavyCavModel.LAYER, UnitClansHorseArcherModel.LAYER, UnitClansCrossbowModel.LAYER, UnitClansMusketeerModel.LAYER, UnitClansBombardierModel.LAYER, UnitClansScoutModel.LAYER},
    };
    public static final String[] CULTURE_IDS = {"north", "empire", "west", "steppe", "sultanate", "clans"};
    public static final String[] TYPE_IDS = {"militia", "swordsman", "archer", "knight", "shieldman", "spearman", "greatsword", "light_cav", "heavy_cav", "horse_archer", "crossbow", "musketeer", "bombardier", "scout"};
    public static int typeIndex(String typeName) {
        for (int i = 0; i < TYPE_IDS.length; i++) if (TYPE_IDS[i].equalsIgnoreCase(typeName)) return i;
        return 1; // Unknown types remain visible as a swordsman.
    }
    /** Разбойники: головорез, лучник, атаман. */
    public static final ModelLayerLocation[] BANDITS = {BanditThugModel.LAYER, BanditArcherModel.LAYER, BanditCaptainModel.LAYER};
    public static final String[] BANDIT_IDS = {"thug", "archer", "captain"};

    private static final Map<ModelLayerLocation, Function<ModelPart, List<ModelPart>>> DECOR = new HashMap<>();

    static {
        DECOR.put(CryptLordModel.LAYER, CryptLordModel::decor);
        DECOR.put(MireMotherModel.LAYER, MireMotherModel::decor);
        DECOR.put(ForgemasterModel.LAYER, ForgemasterModel::decor);
        DECOR.put(ScarabQueenModel.LAYER, ScarabQueenModel::decor);
        DECOR.put(CrawlerGeometry.LAYER, CrawlerGeometry::decor);
        DECOR.put(CrawlerQueenGeometry.LAYER, CrawlerQueenGeometry::decor);
        DECOR.put(UnitNorthMilitiaModel.LAYER, UnitNorthMilitiaModel::decor);
        DECOR.put(UnitNorthSwordsmanModel.LAYER, UnitNorthSwordsmanModel::decor);
        DECOR.put(UnitNorthArcherModel.LAYER, UnitNorthArcherModel::decor);
        DECOR.put(UnitNorthKnightModel.LAYER, UnitNorthKnightModel::decor);
        DECOR.put(UnitNorthShieldmanModel.LAYER, UnitNorthShieldmanModel::decor);
        DECOR.put(UnitNorthSpearmanModel.LAYER, UnitNorthSpearmanModel::decor);
        DECOR.put(UnitNorthGreatswordModel.LAYER, UnitNorthGreatswordModel::decor);
        DECOR.put(UnitNorthLightCavModel.LAYER, UnitNorthLightCavModel::decor);
        DECOR.put(UnitNorthHeavyCavModel.LAYER, UnitNorthHeavyCavModel::decor);
        DECOR.put(UnitNorthHorseArcherModel.LAYER, UnitNorthHorseArcherModel::decor);
        DECOR.put(UnitNorthCrossbowModel.LAYER, UnitNorthCrossbowModel::decor);
        DECOR.put(UnitNorthMusketeerModel.LAYER, UnitNorthMusketeerModel::decor);
        DECOR.put(UnitNorthBombardierModel.LAYER, UnitNorthBombardierModel::decor);
        DECOR.put(UnitNorthScoutModel.LAYER, UnitNorthScoutModel::decor);
        DECOR.put(UnitEmpireMilitiaModel.LAYER, UnitEmpireMilitiaModel::decor);
        DECOR.put(UnitEmpireSwordsmanModel.LAYER, UnitEmpireSwordsmanModel::decor);
        DECOR.put(UnitEmpireArcherModel.LAYER, UnitEmpireArcherModel::decor);
        DECOR.put(UnitEmpireKnightModel.LAYER, UnitEmpireKnightModel::decor);
        DECOR.put(UnitEmpireShieldmanModel.LAYER, UnitEmpireShieldmanModel::decor);
        DECOR.put(UnitEmpireSpearmanModel.LAYER, UnitEmpireSpearmanModel::decor);
        DECOR.put(UnitEmpireGreatswordModel.LAYER, UnitEmpireGreatswordModel::decor);
        DECOR.put(UnitEmpireLightCavModel.LAYER, UnitEmpireLightCavModel::decor);
        DECOR.put(UnitEmpireHeavyCavModel.LAYER, UnitEmpireHeavyCavModel::decor);
        DECOR.put(UnitEmpireHorseArcherModel.LAYER, UnitEmpireHorseArcherModel::decor);
        DECOR.put(UnitEmpireCrossbowModel.LAYER, UnitEmpireCrossbowModel::decor);
        DECOR.put(UnitEmpireMusketeerModel.LAYER, UnitEmpireMusketeerModel::decor);
        DECOR.put(UnitEmpireBombardierModel.LAYER, UnitEmpireBombardierModel::decor);
        DECOR.put(UnitEmpireScoutModel.LAYER, UnitEmpireScoutModel::decor);
        DECOR.put(UnitWestMilitiaModel.LAYER, UnitWestMilitiaModel::decor);
        DECOR.put(UnitWestSwordsmanModel.LAYER, UnitWestSwordsmanModel::decor);
        DECOR.put(UnitWestArcherModel.LAYER, UnitWestArcherModel::decor);
        DECOR.put(UnitWestKnightModel.LAYER, UnitWestKnightModel::decor);
        DECOR.put(UnitWestShieldmanModel.LAYER, UnitWestShieldmanModel::decor);
        DECOR.put(UnitWestSpearmanModel.LAYER, UnitWestSpearmanModel::decor);
        DECOR.put(UnitWestGreatswordModel.LAYER, UnitWestGreatswordModel::decor);
        DECOR.put(UnitWestLightCavModel.LAYER, UnitWestLightCavModel::decor);
        DECOR.put(UnitWestHeavyCavModel.LAYER, UnitWestHeavyCavModel::decor);
        DECOR.put(UnitWestHorseArcherModel.LAYER, UnitWestHorseArcherModel::decor);
        DECOR.put(UnitWestCrossbowModel.LAYER, UnitWestCrossbowModel::decor);
        DECOR.put(UnitWestMusketeerModel.LAYER, UnitWestMusketeerModel::decor);
        DECOR.put(UnitWestBombardierModel.LAYER, UnitWestBombardierModel::decor);
        DECOR.put(UnitWestScoutModel.LAYER, UnitWestScoutModel::decor);
        DECOR.put(UnitSteppeMilitiaModel.LAYER, UnitSteppeMilitiaModel::decor);
        DECOR.put(UnitSteppeSwordsmanModel.LAYER, UnitSteppeSwordsmanModel::decor);
        DECOR.put(UnitSteppeArcherModel.LAYER, UnitSteppeArcherModel::decor);
        DECOR.put(UnitSteppeKnightModel.LAYER, UnitSteppeKnightModel::decor);
        DECOR.put(UnitSteppeShieldmanModel.LAYER, UnitSteppeShieldmanModel::decor);
        DECOR.put(UnitSteppeSpearmanModel.LAYER, UnitSteppeSpearmanModel::decor);
        DECOR.put(UnitSteppeGreatswordModel.LAYER, UnitSteppeGreatswordModel::decor);
        DECOR.put(UnitSteppeLightCavModel.LAYER, UnitSteppeLightCavModel::decor);
        DECOR.put(UnitSteppeHeavyCavModel.LAYER, UnitSteppeHeavyCavModel::decor);
        DECOR.put(UnitSteppeHorseArcherModel.LAYER, UnitSteppeHorseArcherModel::decor);
        DECOR.put(UnitSteppeCrossbowModel.LAYER, UnitSteppeCrossbowModel::decor);
        DECOR.put(UnitSteppeMusketeerModel.LAYER, UnitSteppeMusketeerModel::decor);
        DECOR.put(UnitSteppeBombardierModel.LAYER, UnitSteppeBombardierModel::decor);
        DECOR.put(UnitSteppeScoutModel.LAYER, UnitSteppeScoutModel::decor);
        DECOR.put(UnitSultanateMilitiaModel.LAYER, UnitSultanateMilitiaModel::decor);
        DECOR.put(UnitSultanateSwordsmanModel.LAYER, UnitSultanateSwordsmanModel::decor);
        DECOR.put(UnitSultanateArcherModel.LAYER, UnitSultanateArcherModel::decor);
        DECOR.put(UnitSultanateKnightModel.LAYER, UnitSultanateKnightModel::decor);
        DECOR.put(UnitSultanateShieldmanModel.LAYER, UnitSultanateShieldmanModel::decor);
        DECOR.put(UnitSultanateSpearmanModel.LAYER, UnitSultanateSpearmanModel::decor);
        DECOR.put(UnitSultanateGreatswordModel.LAYER, UnitSultanateGreatswordModel::decor);
        DECOR.put(UnitSultanateLightCavModel.LAYER, UnitSultanateLightCavModel::decor);
        DECOR.put(UnitSultanateHeavyCavModel.LAYER, UnitSultanateHeavyCavModel::decor);
        DECOR.put(UnitSultanateHorseArcherModel.LAYER, UnitSultanateHorseArcherModel::decor);
        DECOR.put(UnitSultanateCrossbowModel.LAYER, UnitSultanateCrossbowModel::decor);
        DECOR.put(UnitSultanateMusketeerModel.LAYER, UnitSultanateMusketeerModel::decor);
        DECOR.put(UnitSultanateBombardierModel.LAYER, UnitSultanateBombardierModel::decor);
        DECOR.put(UnitSultanateScoutModel.LAYER, UnitSultanateScoutModel::decor);
        DECOR.put(UnitClansMilitiaModel.LAYER, UnitClansMilitiaModel::decor);
        DECOR.put(UnitClansSwordsmanModel.LAYER, UnitClansSwordsmanModel::decor);
        DECOR.put(UnitClansArcherModel.LAYER, UnitClansArcherModel::decor);
        DECOR.put(UnitClansKnightModel.LAYER, UnitClansKnightModel::decor);
        DECOR.put(UnitClansShieldmanModel.LAYER, UnitClansShieldmanModel::decor);
        DECOR.put(UnitClansSpearmanModel.LAYER, UnitClansSpearmanModel::decor);
        DECOR.put(UnitClansGreatswordModel.LAYER, UnitClansGreatswordModel::decor);
        DECOR.put(UnitClansLightCavModel.LAYER, UnitClansLightCavModel::decor);
        DECOR.put(UnitClansHeavyCavModel.LAYER, UnitClansHeavyCavModel::decor);
        DECOR.put(UnitClansHorseArcherModel.LAYER, UnitClansHorseArcherModel::decor);
        DECOR.put(UnitClansCrossbowModel.LAYER, UnitClansCrossbowModel::decor);
        DECOR.put(UnitClansMusketeerModel.LAYER, UnitClansMusketeerModel::decor);
        DECOR.put(UnitClansBombardierModel.LAYER, UnitClansBombardierModel::decor);
        DECOR.put(UnitClansScoutModel.LAYER, UnitClansScoutModel::decor);
        DECOR.put(BanditThugModel.LAYER, BanditThugModel::decor);
        DECOR.put(BanditArcherModel.LAYER, BanditArcherModel::decor);
        DECOR.put(BanditCaptainModel.LAYER, BanditCaptainModel::decor);
    }

    public static List<ModelPart> decor(ModelLayerLocation layer, ModelPart root) {
        Function<ModelPart, List<ModelPart>> f = DECOR.get(layer);
        return f == null ? List.of() : f.apply(root);
    }

    public static void register(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(CryptLordModel.LAYER, CryptLordModel::create);
        e.registerLayerDefinition(MireMotherModel.LAYER, MireMotherModel::create);
        e.registerLayerDefinition(ForgemasterModel.LAYER, ForgemasterModel::create);
        e.registerLayerDefinition(ScarabQueenModel.LAYER, ScarabQueenModel::create);
        e.registerLayerDefinition(CrawlerGeometry.LAYER, CrawlerGeometry::create);
        e.registerLayerDefinition(CrawlerQueenGeometry.LAYER, CrawlerQueenGeometry::create);
        e.registerLayerDefinition(UnitNorthMilitiaModel.LAYER, UnitNorthMilitiaModel::create);
        e.registerLayerDefinition(UnitNorthSwordsmanModel.LAYER, UnitNorthSwordsmanModel::create);
        e.registerLayerDefinition(UnitNorthArcherModel.LAYER, UnitNorthArcherModel::create);
        e.registerLayerDefinition(UnitNorthKnightModel.LAYER, UnitNorthKnightModel::create);
        e.registerLayerDefinition(UnitNorthShieldmanModel.LAYER, UnitNorthShieldmanModel::create);
        e.registerLayerDefinition(UnitNorthSpearmanModel.LAYER, UnitNorthSpearmanModel::create);
        e.registerLayerDefinition(UnitNorthGreatswordModel.LAYER, UnitNorthGreatswordModel::create);
        e.registerLayerDefinition(UnitNorthLightCavModel.LAYER, UnitNorthLightCavModel::create);
        e.registerLayerDefinition(UnitNorthHeavyCavModel.LAYER, UnitNorthHeavyCavModel::create);
        e.registerLayerDefinition(UnitNorthHorseArcherModel.LAYER, UnitNorthHorseArcherModel::create);
        e.registerLayerDefinition(UnitNorthCrossbowModel.LAYER, UnitNorthCrossbowModel::create);
        e.registerLayerDefinition(UnitNorthMusketeerModel.LAYER, UnitNorthMusketeerModel::create);
        e.registerLayerDefinition(UnitNorthBombardierModel.LAYER, UnitNorthBombardierModel::create);
        e.registerLayerDefinition(UnitNorthScoutModel.LAYER, UnitNorthScoutModel::create);
        e.registerLayerDefinition(UnitEmpireMilitiaModel.LAYER, UnitEmpireMilitiaModel::create);
        e.registerLayerDefinition(UnitEmpireSwordsmanModel.LAYER, UnitEmpireSwordsmanModel::create);
        e.registerLayerDefinition(UnitEmpireArcherModel.LAYER, UnitEmpireArcherModel::create);
        e.registerLayerDefinition(UnitEmpireKnightModel.LAYER, UnitEmpireKnightModel::create);
        e.registerLayerDefinition(UnitEmpireShieldmanModel.LAYER, UnitEmpireShieldmanModel::create);
        e.registerLayerDefinition(UnitEmpireSpearmanModel.LAYER, UnitEmpireSpearmanModel::create);
        e.registerLayerDefinition(UnitEmpireGreatswordModel.LAYER, UnitEmpireGreatswordModel::create);
        e.registerLayerDefinition(UnitEmpireLightCavModel.LAYER, UnitEmpireLightCavModel::create);
        e.registerLayerDefinition(UnitEmpireHeavyCavModel.LAYER, UnitEmpireHeavyCavModel::create);
        e.registerLayerDefinition(UnitEmpireHorseArcherModel.LAYER, UnitEmpireHorseArcherModel::create);
        e.registerLayerDefinition(UnitEmpireCrossbowModel.LAYER, UnitEmpireCrossbowModel::create);
        e.registerLayerDefinition(UnitEmpireMusketeerModel.LAYER, UnitEmpireMusketeerModel::create);
        e.registerLayerDefinition(UnitEmpireBombardierModel.LAYER, UnitEmpireBombardierModel::create);
        e.registerLayerDefinition(UnitEmpireScoutModel.LAYER, UnitEmpireScoutModel::create);
        e.registerLayerDefinition(UnitWestMilitiaModel.LAYER, UnitWestMilitiaModel::create);
        e.registerLayerDefinition(UnitWestSwordsmanModel.LAYER, UnitWestSwordsmanModel::create);
        e.registerLayerDefinition(UnitWestArcherModel.LAYER, UnitWestArcherModel::create);
        e.registerLayerDefinition(UnitWestKnightModel.LAYER, UnitWestKnightModel::create);
        e.registerLayerDefinition(UnitWestShieldmanModel.LAYER, UnitWestShieldmanModel::create);
        e.registerLayerDefinition(UnitWestSpearmanModel.LAYER, UnitWestSpearmanModel::create);
        e.registerLayerDefinition(UnitWestGreatswordModel.LAYER, UnitWestGreatswordModel::create);
        e.registerLayerDefinition(UnitWestLightCavModel.LAYER, UnitWestLightCavModel::create);
        e.registerLayerDefinition(UnitWestHeavyCavModel.LAYER, UnitWestHeavyCavModel::create);
        e.registerLayerDefinition(UnitWestHorseArcherModel.LAYER, UnitWestHorseArcherModel::create);
        e.registerLayerDefinition(UnitWestCrossbowModel.LAYER, UnitWestCrossbowModel::create);
        e.registerLayerDefinition(UnitWestMusketeerModel.LAYER, UnitWestMusketeerModel::create);
        e.registerLayerDefinition(UnitWestBombardierModel.LAYER, UnitWestBombardierModel::create);
        e.registerLayerDefinition(UnitWestScoutModel.LAYER, UnitWestScoutModel::create);
        e.registerLayerDefinition(UnitSteppeMilitiaModel.LAYER, UnitSteppeMilitiaModel::create);
        e.registerLayerDefinition(UnitSteppeSwordsmanModel.LAYER, UnitSteppeSwordsmanModel::create);
        e.registerLayerDefinition(UnitSteppeArcherModel.LAYER, UnitSteppeArcherModel::create);
        e.registerLayerDefinition(UnitSteppeKnightModel.LAYER, UnitSteppeKnightModel::create);
        e.registerLayerDefinition(UnitSteppeShieldmanModel.LAYER, UnitSteppeShieldmanModel::create);
        e.registerLayerDefinition(UnitSteppeSpearmanModel.LAYER, UnitSteppeSpearmanModel::create);
        e.registerLayerDefinition(UnitSteppeGreatswordModel.LAYER, UnitSteppeGreatswordModel::create);
        e.registerLayerDefinition(UnitSteppeLightCavModel.LAYER, UnitSteppeLightCavModel::create);
        e.registerLayerDefinition(UnitSteppeHeavyCavModel.LAYER, UnitSteppeHeavyCavModel::create);
        e.registerLayerDefinition(UnitSteppeHorseArcherModel.LAYER, UnitSteppeHorseArcherModel::create);
        e.registerLayerDefinition(UnitSteppeCrossbowModel.LAYER, UnitSteppeCrossbowModel::create);
        e.registerLayerDefinition(UnitSteppeMusketeerModel.LAYER, UnitSteppeMusketeerModel::create);
        e.registerLayerDefinition(UnitSteppeBombardierModel.LAYER, UnitSteppeBombardierModel::create);
        e.registerLayerDefinition(UnitSteppeScoutModel.LAYER, UnitSteppeScoutModel::create);
        e.registerLayerDefinition(UnitSultanateMilitiaModel.LAYER, UnitSultanateMilitiaModel::create);
        e.registerLayerDefinition(UnitSultanateSwordsmanModel.LAYER, UnitSultanateSwordsmanModel::create);
        e.registerLayerDefinition(UnitSultanateArcherModel.LAYER, UnitSultanateArcherModel::create);
        e.registerLayerDefinition(UnitSultanateKnightModel.LAYER, UnitSultanateKnightModel::create);
        e.registerLayerDefinition(UnitSultanateShieldmanModel.LAYER, UnitSultanateShieldmanModel::create);
        e.registerLayerDefinition(UnitSultanateSpearmanModel.LAYER, UnitSultanateSpearmanModel::create);
        e.registerLayerDefinition(UnitSultanateGreatswordModel.LAYER, UnitSultanateGreatswordModel::create);
        e.registerLayerDefinition(UnitSultanateLightCavModel.LAYER, UnitSultanateLightCavModel::create);
        e.registerLayerDefinition(UnitSultanateHeavyCavModel.LAYER, UnitSultanateHeavyCavModel::create);
        e.registerLayerDefinition(UnitSultanateHorseArcherModel.LAYER, UnitSultanateHorseArcherModel::create);
        e.registerLayerDefinition(UnitSultanateCrossbowModel.LAYER, UnitSultanateCrossbowModel::create);
        e.registerLayerDefinition(UnitSultanateMusketeerModel.LAYER, UnitSultanateMusketeerModel::create);
        e.registerLayerDefinition(UnitSultanateBombardierModel.LAYER, UnitSultanateBombardierModel::create);
        e.registerLayerDefinition(UnitSultanateScoutModel.LAYER, UnitSultanateScoutModel::create);
        e.registerLayerDefinition(UnitClansMilitiaModel.LAYER, UnitClansMilitiaModel::create);
        e.registerLayerDefinition(UnitClansSwordsmanModel.LAYER, UnitClansSwordsmanModel::create);
        e.registerLayerDefinition(UnitClansArcherModel.LAYER, UnitClansArcherModel::create);
        e.registerLayerDefinition(UnitClansKnightModel.LAYER, UnitClansKnightModel::create);
        e.registerLayerDefinition(UnitClansShieldmanModel.LAYER, UnitClansShieldmanModel::create);
        e.registerLayerDefinition(UnitClansSpearmanModel.LAYER, UnitClansSpearmanModel::create);
        e.registerLayerDefinition(UnitClansGreatswordModel.LAYER, UnitClansGreatswordModel::create);
        e.registerLayerDefinition(UnitClansLightCavModel.LAYER, UnitClansLightCavModel::create);
        e.registerLayerDefinition(UnitClansHeavyCavModel.LAYER, UnitClansHeavyCavModel::create);
        e.registerLayerDefinition(UnitClansHorseArcherModel.LAYER, UnitClansHorseArcherModel::create);
        e.registerLayerDefinition(UnitClansCrossbowModel.LAYER, UnitClansCrossbowModel::create);
        e.registerLayerDefinition(UnitClansMusketeerModel.LAYER, UnitClansMusketeerModel::create);
        e.registerLayerDefinition(UnitClansBombardierModel.LAYER, UnitClansBombardierModel::create);
        e.registerLayerDefinition(UnitClansScoutModel.LAYER, UnitClansScoutModel::create);
        e.registerLayerDefinition(BanditThugModel.LAYER, BanditThugModel::create);
        e.registerLayerDefinition(BanditArcherModel.LAYER, BanditArcherModel::create);
        e.registerLayerDefinition(BanditCaptainModel.LAYER, BanditCaptainModel::create);
    }
}
