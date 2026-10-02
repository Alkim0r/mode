"""Проверки структуры и ассетов моделей шахтных существ Regnum."""
from pathlib import Path
import sys
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(Path(__file__).resolve().parent))
import bosses
from preview import world_transforms

JAVA = ROOT / "src/main/java/com/alkimor/regnum/client/model"
TEXTURES = ROOT / "src/main/resources/assets/regnum/textures/entity"

expected_legs = {f"leg_{row}_{side}" for row in ("front", "mid_front", "mid_back", "rear") for side in ("l", "r")}
models = [bosses.crawler(), bosses.crawler_queen()]
for model in models:
    model.pack()
    parts = {part.name: part for part in model.parts}
    assert expected_legs <= parts.keys(), f"{model.name}: missing one or more articulated legs"
    for part in model.parts:
        assert part.parent is None or part.parent in parts, f"{model.name}: missing parent {part.parent} for {part.name}"
    transforms = world_transforms(model)
    for leg in expected_legs:
        foot = f"{leg}_foot"
        assert parts[f"{leg}_shin"].parent == leg and parts[foot].parent == f"{leg}_shin"
        assert abs(transforms[foot][1, 3] - 24) < 0.001, f"{model.name}: {foot} does not meet the ground"
    # Keep the shared family usable in crowds and on small GPUs.
    cube_count = sum(len(part.cubes) for part in model.parts)
    assert cube_count <= 100, f"{model.name}: geometry budget exceeded ({cube_count} cubes)"
    assert model.tw * model.th <= 32768, f"{model.name}: texture budget exceeded"
    head_front = min(cube.origin[2] for cube in parts["head"].cubes)
    for side in ("l", "r"):
        eye = parts[f"eye_{side}"]
        assert eye.pivot[2] + min(cube.origin[2] for cube in eye.cubes) < head_front, f"{model.name}: eyes buried inside the head"

    java_file = JAVA / f"{model.java_class}.java"
    assert java_file.is_file(), f"missing generated model {java_file.name}"
    assert java_file.read_text(encoding="utf-8") == model.java("com.alkimor.regnum.client.model")

    texture = TEXTURES / f"{model.name}.png"
    glow_texture = TEXTURES / f"{model.name}_glow.png"
    expected_texture, expected_glow = model.paint()
    with Image.open(texture) as image:
        assert image.size == (model.tw, model.th) and image.getbbox(), f"invalid texture {texture.name}"
        assert image.convert("RGBA").tobytes() == expected_texture.tobytes(), f"stale generated texture {texture.name}"
    assert expected_glow.getbbox(), f"{model.name}: emissive eyes are missing"
    with Image.open(glow_texture) as image:
        assert image.size == (model.tw, model.th) and image.getbbox(), f"invalid emissive mask {glow_texture.name}"
        assert image.convert("RGBA").tobytes() == expected_glow.tobytes(), f"stale emissive mask {glow_texture.name}"

queen = {part.name: part for part in models[1].parts}
assert "phase_two_fissures" in queen and "phase_three_core" in queen, "queen phase visuals are missing"
index = (JAVA / "ModelIndex.java").read_text(encoding="utf-8")
assert "CrawlerGeometry.LAYER" in index and "CrawlerQueenGeometry.LAYER" in index, "mine geometry layers are not registered"
print("OK: both creatures have 8 grounded articulated legs, bounded geometry/atlases, current textures, glow masks and phase details")
