"""Encode an unedited Minecraft frame sequence as a lossless animated WebP."""
from pathlib import Path
from PIL import Image
import json
import argparse

ROOT = Path(__file__).resolve().parents[1]
shots = ROOT.parent / 'regnum-codex-build/run-visual/screenshots'
parser = argparse.ArgumentParser()
parser.add_argument('--pattern', default='queen_motion_*.png')
parser.add_argument('--minimum-frames', type=int, default=150)
args = parser.parse_args()
frames = sorted(shots.glob(args.pattern))
if not frames:
    raise SystemExit('No live Minecraft frames found')
prefix = max(p.name.rsplit('_', 1)[0] for p in frames)
frames = [p for p in frames if p.name.startswith(prefix + '_')]
if len(frames) < args.minimum_frames:
    raise SystemExit('Capture still starting; do not encode a partial clip')
images = [Image.open(p).convert('RGB') for p in frames]
if min(images[0].size) < 64 or any(im.size != images[0].size for im in images):
    raise SystemExit('Invalid/minimized framebuffer or changing dimensions; capture is not visual QA')
out = ROOT / 'coord/previews'
out.mkdir(exist_ok=True)
target = out / (prefix + '.webp')
images[0].save(target, format='WEBP', save_all=True, append_images=images[1:],
               lossless=True, duration=100, loop=0, method=4)
with Image.open(target) as verified:
    if verified.n_frames != len(frames):
        raise ValueError('Encoded frame count differs from capture')
    if verified.size != images[0].size:
        raise ValueError('Encoded size differs from capture')
metadata = dict(output=str(target), source='Opt-in Minecraft battle showcase framebuffer',
    frame_count=len(frames), playback_ms_per_frame=100, pixels_edited=False,
    size=images[0].size, bytes=target.stat().st_size, source_frames=[str(p) for p in frames])
(out / (prefix + '.json')).write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
for img in images:
    img.close()
print(json.dumps({k: v for k, v in metadata.items() if k != 'source_frames'}, ensure_ascii=False))
