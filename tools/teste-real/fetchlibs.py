import json, os, sys, urllib.request, glob
client, out = sys.argv[1], sys.argv[2]
names = []
for f in ('version.json', '1.7.10.json'):
    d = json.load(open(os.path.join(client, 'bin', f)))
    for l in d['libraries']:
        n = l['name']
        if 'natives' in l and 'rules' in l: pass
        if n.startswith('net.minecraftforge:forge'): continue
        if 'platform' in n or n.startswith('tv.twitch'): continue
        names.append((n, l.get('url')))
cp = []
for n, url in names:
    g, a, v = n.split(':')
    fn = f'{a}-{v}.jar'
    dst = os.path.join(out, fn)
    if not os.path.exists(dst):
        hits = glob.glob(os.path.expanduser(f'~/.gradle/caches/modules-2/files-2.1/{g}/{a}/{v}/*/{fn}'))
        if hits:
            import shutil; shutil.copy(hits[0], dst)
        else:
            path = f"{g.replace('.', '/')}/{a}/{v}/{fn}"
            ok = False
            for base in ('https://libraries.minecraft.net/', 'https://maven.minecraftforge.net/', 'https://repo1.maven.org/maven2/'):
                try:
                    urllib.request.urlretrieve(base + path, dst); ok = True; break
                except Exception as e:
                    if os.path.exists(dst): os.remove(dst)
            if not ok: print('MISSING', n); continue
    cp.append(dst)
open(os.path.join(out, 'classpath.txt'), 'w').write(':'.join(cp))
print(len(cp), 'libraries')
