import re, subprocess, os, sys
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
shots = os.path.join(root, 'run-tour', 'screenshots')
log = open(os.path.join(root, 'run-tour', 'logs', 'latest.log'), encoding='utf-8', errors='replace').read()
caps = [(int(m.group(1)), m.group(2).strip()) for m in re.finditer(r'\[TOUR\] CAPTION (\d+) (.+)', log)]
n = len([f for f in os.listdir(shots) if f.startswith('tour_') and f.endswith('.png')])
print('frames', n, 'captions', len(caps))
font = '/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
tdir = os.path.join(root, 'coord', 'video', 'cap'); os.makedirs(tdir, exist_ok=True)
vf = []
for i, (start, text) in enumerate(caps):
    end = caps[i + 1][0] if i + 1 < len(caps) else n
    p = os.path.join(tdir, f'c{i}.txt'); open(p, 'w', encoding='utf-8').write(text)
    vf.append(f"drawtext=fontfile={font}:textfile={p}:fontsize=30:fontcolor=white:box=1:boxcolor=black@0.55:boxborderw=14:x=(w-text_w)/2:y=h-90:enable='between(n,{start},{end - 1})'")
out = os.path.join(root, 'coord', 'video', 'regnum_tour.mp4')
cmd = ['ffmpeg', '-y', '-loglevel', 'error', '-framerate', '10', '-i', os.path.join(shots, 'tour_%05d.png'),
       '-vf', ','.join(vf) if vf else 'null', '-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-crf', '20', out]
subprocess.check_call(cmd)
print(out, os.path.getsize(out))
