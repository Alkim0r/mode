"""Audit frame references in bundled texture packs; optionally repair stale metadata.

Static replacement textures sometimes retain metadata for a multi-frame original.
Only out-of-range frame entries are removed; the image and valid timing are kept.
Run with --fix to repair, then without --fix as a release resource check.
"""
import argparse
import json
from pathlib import Path

from PIL import Image


def audit(root: Path, fix: bool = False) -> int:
    errors = 0
    checked = 0
    for meta in sorted(root.rglob("*.png.mcmeta")):
        texture = meta.with_suffix("")
        if not texture.exists():
            print(f"ERROR orphan metadata: {meta.relative_to(root)}")
            errors += 1
            continue
        data = json.loads(meta.read_text(encoding="utf-8-sig"))
        anim = data.get("animation")
        if anim is None:
            continue
        checked += 1
        with Image.open(texture) as img:
            width, height = img.size
        # Minecraft: both dimensions absent -> square min(image dimensions);
        # only one present -> the other defaults to its full image dimension.
        default = min(width, height)
        fw = anim.get("width", width if "height" in anim else default)
        fh = anim.get("height", height if "width" in anim else default)
        if fw <= 0 or fh <= 0 or width % fw or height % fh:
            print(f"ERROR frame dimensions: {meta.relative_to(root)} {width}x{height} / {fw}x{fh}")
            errors += 1
            continue
        count = (width // fw) * (height // fh)
        frames = anim.get("frames", list(range(count)))
        valid = [entry for entry in frames
                 if 0 <= (entry.get("index", -1) if isinstance(entry, dict) else entry) < count]
        if len(valid) != len(frames):
            if fix:
                anim["frames"] = valid or [0]
                meta.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
                print(f"FIX {meta.relative_to(root)}: {len(frames)} -> {len(anim['frames'])} entries ({count} image frames)")
            else:
                print(f"ERROR frame index: {meta.relative_to(root)} ({count} image frames)")
                errors += 1
    print(f"Pack animation audit: {checked} animations, {errors} errors")
    return errors


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--fix", action="store_true")
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1] / "src/main/resources/resourcepacks")
    args = parser.parse_args()
    raise SystemExit(1 if audit(args.root, args.fix) else 0)
