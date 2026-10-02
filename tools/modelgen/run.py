"""Запуск: python3 tools/modelgen/run.py (из корня проекта)."""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import bosses  # noqa
import units  # noqa

TEX = "src/main/resources/assets/regnum/textures/entity"
JAVA = "src/main/java/com/alkimor/regnum/client/model"
PKG = "com.alkimor.regnum.client.model"


def emit(model, variants=None):
    model.pack()
    source = model.java(PKG)  # Validate before opening any generated Java file for writing.
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(JAVA, exist_ok=True)
    if variants:
        for vname, fn in variants.items():
            img, glow = model.paint(fn)
            img.save(f"{TEX}/{vname}.png")
            if glow.getbbox():
                glow.save(f"{TEX}/{vname}_glow.png")
    else:
        img, glow = model.paint()
        img.save(f"{TEX}/{model.name}.png")
        if glow.getbbox():
            glow.save(f"{TEX}/{model.name}_glow.png")
    with open(f"{JAVA}/{model.java_class}.java", "w", encoding="utf-8") as f:
        f.write(source)


def index(models):
    """ModelIndex.java: регистрация слоёв, таблицы моделей войск, декор для облегчённого режима."""
    L = []
    L.append(f"package {PKG};")
    L.append("")
    L.append("import net.minecraft.client.model.geom.ModelLayerLocation;")
    L.append("import net.minecraft.client.model.geom.ModelPart;")
    L.append("import net.neoforged.neoforge.client.event.EntityRenderersEvent;")
    L.append("")
    L.append("import java.util.HashMap;")
    L.append("import java.util.List;")
    L.append("import java.util.Map;")
    L.append("import java.util.function.Function;")
    L.append("")
    L.append("/** Сгенерировано tools/modelgen/run.py — не редактировать вручную. */")
    L.append("public final class ModelIndex {")
    L.append("    private ModelIndex() {}")
    L.append("")
    L.append("    /** [культура][тип модели]; typeIndex связывает имя SoldierType с таблицей. */")
    L.append("    public static final ModelLayerLocation[][] UNITS = {")
    for c in units.CULTURES:
        row = ", ".join(f"{units.model_class(c, k)}.LAYER" for k in units.TYPES)
        L.append(f"            {{{row}}},")
    L.append("    };")
    L.append("    public static final String[] CULTURE_IDS = {" + ", ".join(f'"{c}"' for c in units.CULTURES) + "};")
    L.append("    public static final String[] TYPE_IDS = {" + ", ".join(f'"{k}"' for k in units.TYPES) + "};")
    L.append("    public static int typeIndex(String typeName) {")
    L.append("        for (int i = 0; i < TYPE_IDS.length; i++) if (TYPE_IDS[i].equalsIgnoreCase(typeName)) return i;")
    L.append("        return 1; // Unknown types remain visible as a swordsman.")
    L.append("    }")
    L.append("    /** Разбойники: головорез, лучник, атаман. */")
    L.append("    public static final ModelLayerLocation[] BANDITS = {BanditThugModel.LAYER, BanditArcherModel.LAYER, BanditCaptainModel.LAYER};")
    L.append("    public static final String[] BANDIT_IDS = {\"thug\", \"archer\", \"captain\"};")
    L.append("")
    L.append("    private static final Map<ModelLayerLocation, Function<ModelPart, List<ModelPart>>> DECOR = new HashMap<>();")
    L.append("")
    L.append("    static {")
    for m in models:
        L.append(f"        DECOR.put({m.java_class}.LAYER, {m.java_class}::decor);")
    L.append("    }")
    L.append("")
    L.append("    public static List<ModelPart> decor(ModelLayerLocation layer, ModelPart root) {")
    L.append("        Function<ModelPart, List<ModelPart>> f = DECOR.get(layer);")
    L.append("        return f == null ? List.of() : f.apply(root);")
    L.append("    }")
    L.append("")
    L.append("    public static void register(EntityRenderersEvent.RegisterLayerDefinitions e) {")
    for m in models:
        L.append(f"        e.registerLayerDefinition({m.java_class}.LAYER, {m.java_class}::create);")
    L.append("    }")
    L.append("}")
    with open(f"{JAVA}/ModelIndex.java", "w", encoding="utf-8") as f:
        f.write("\n".join(L) + "\n")


if __name__ == "__main__":
    os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
    models = []
    for fn in bosses.ALL:
        m = fn()
        emit(m)
        models.append(m)
    for model, variants in units.ALL():
        emit(model, variants)
        models.append(model)
    index(models)
    print("models ok:", len(models))
