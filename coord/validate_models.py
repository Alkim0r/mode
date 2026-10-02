"""Validate generated model coverage and create offline contact sheets for X2."""
from pathlib import Path
import re
import sys
import json

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools/modelgen"))
import units
from preview import sheet
from PIL import Image

source = (ROOT / "src/main/java/com/alkimor/regnum/kingdom/SoldierType.java").read_text(encoding="utf-8")
enum_names = re.findall(r'^    ([A-Z_]+)\("', source, flags=re.M)
assert set(name.lower() for name in enum_names) == set(units.TYPES), "Missing SoldierType model"
tex_root = ROOT / "src/main/resources/assets/regnum/textures/entity"
java_root = ROOT / "src/main/java/com/alkimor/regnum/client/model"
metrics = []
for culture in units.CULTURES:
    entries = []
    for kind in units.TYPES:
        model = units.unit_model(culture, kind)
        model.pack()
        generated = model.java("com.alkimor.regnum.client.model")
        assert (java_root / (model.java_class + ".java")).read_text(encoding="utf-8") == generated
        names = {part.name for part in model.parts}
        assert {"head", "body", "left_arm", "right_arm", "left_leg", "right_leg"} <= names
        for part in model.parts:
            assert part.parent is None or part.parent in names
        if kind in ("greatsword", "horse_archer", "crossbow", "musketeer", "bombardier"):
            assert "shield" not in names
        if kind == "musketeer": assert "musket" in names
        if kind == "bombardier": assert "hand_bombard" in names
        for variant in range(3):
            with Image.open(tex_root / f"{model.name}_{variant}.png") as image:
                assert image.size == (model.tw, model.th)
                assert image.getbbox() is not None
                image.load()
        fn = next(iter(units.skin_variants(model.name).values()))
        texture, glow = model.paint(fn)
        cubes = sum(len(part.cubes) for part in model.parts)
        metrics.append(dict(culture=culture, type=kind, parts=len(model.parts), cubes=cubes,
                            triangles=cubes * 12, texture=[model.tw, model.th]))
        entries.append((kind, model, texture, glow if glow.getbbox() else None, -30))
    sheet(entries, str(ROOT / f"coord/x2_{culture}.png"), cols=4, size=(220, 320), scale=7)
(ROOT / "coord/x2-model-validation.json").write_text(json.dumps(metrics, indent=2), encoding="utf-8")
print(f"OK: {len(metrics)} cultural unit models, {len(metrics) * 3} textures, six contact sheets")
print(f"Max geometry: {max(row['triangles'] for row in metrics)} triangles per unit before item/horse layers")
