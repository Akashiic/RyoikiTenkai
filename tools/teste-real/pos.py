import gzip, struct, sys
def read(path):
    d = gzip.open(path).read(); p = [0]
    def u(fmt):
        n = struct.calcsize(fmt); v = struct.unpack_from(fmt, d, p[0]); p[0] += n; return v[0]
    def s():
        n = u('>H'); v = d[p[0]:p[0]+n].decode('utf-8', 'replace'); p[0] += n; return v
    def payload(t):
        if t == 1: return u('>b')
        if t == 2: return u('>h')
        if t == 3: return u('>i')
        if t == 4: return u('>q')
        if t == 5: return u('>f')
        if t == 6: return u('>d')
        if t == 7:
            n = u('>i'); p[0] += n; return None
        if t == 8: return s()
        if t == 9:
            et = u('>b'); n = u('>i'); return [payload(et) for _ in range(n)]
        if t == 10:
            out = {}
            while True:
                tt = u('>b')
                if tt == 0: return out
                k = s(); out[k] = payload(tt)
        if t == 11:
            n = u('>i'); p[0] += 4*n; return None
        raise ValueError(t)
    t = u('>b'); s(); return payload(t)
n = read(sys.argv[1])
print('Pos', [round(x, 2) for x in n['Pos']], 'Dimension', n.get('Dimension'), 'RyoikiReturn', n.get('ForgeData', {}).get('PlayerPersisted', {}).get('RyoikiReturn'))
