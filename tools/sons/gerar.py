#!/usr/bin/env python3
"""Gera os sons de reserva do mod (os que vão no jar), por síntese. Precisa de numpy e do ffmpeg.

    python3 tools/sons/gerar.py

Escreve em src/main/resources/assets/ryoikitenkai/sounds/. Os nomes são os de SoundCues.java e do sounds.json.
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "assets",
                   "ryoikitenkai", "sounds")
rng = np.random.default_rng(1614)


def t(seconds):
    return np.arange(int(SR * seconds)) / SR


def noise(seconds, low, high):
    """Ruído numa faixa de frequências. Sai periódico (bom para laços), por ser feito no espectro."""
    n = int(SR * seconds)
    freqs = np.fft.rfftfreq(n, 1 / SR)
    spec = rng.normal(size=freqs.size) + 1j * rng.normal(size=freqs.size)
    spec[(freqs < low) | (freqs > high)] = 0
    out = np.fft.irfft(spec, n)
    return out / (np.abs(out).max() + 1e-9)


def fade(x, a=0.005, r=0.02):
    n = x.shape[-1]
    env = np.ones(n)
    na, nr = int(SR * a), int(SR * r)
    env[:na] = np.linspace(0, 1, na)
    env[n - nr:] = np.linspace(1, 0, nr)
    return x * env


def sweep(seconds, f0, f1, power=1.0):
    x = t(seconds)
    f = f0 + (f1 - f0) * (x / seconds) ** power
    return np.sin(2 * np.pi * np.cumsum(f) / SR)


def stereo(left, right):
    return np.stack([left, right])


def write(name, data, peak=0.89):
    data = np.atleast_2d(data)
    data = data / (np.abs(data).max() + 1e-9) * peak
    pcm = (data.T * 32767).astype("<i2")
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        path = tmp.name
    with wave.open(path, "wb") as w:
        w.setnchannels(data.shape[0])
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    out = os.path.join(OUT, name + ".ogg")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", path, "-map_metadata", "-1", "-bitexact",
                    "-c:a", "libvorbis", "-q:a", "3", out], check=True)
    os.remove(path)
    print(name, data.shape[0], "canal(is)", round(data.shape[1] / SR, 2), "s", os.path.getsize(out), "bytes")


def sinal():
    d = 1.25
    x = t(d)
    rise = (x / d) ** 2
    tone = sweep(d, 70, 420, 1.6) + 0.5 * sweep(d, 140, 840, 1.6) + 0.25 * sweep(d, 211, 1270, 1.6)
    air = noise(d, 1500, 9000) * rise ** 2
    return fade((tone * 0.7 + air * 0.6) * (0.15 + rise), 0.02, 0.03)


def faixa_corte():
    d = 0.3
    x = t(d)
    hit = noise(d, 400, 12000) * np.exp(-x * 38)
    thump = np.sin(2 * np.pi * 85 * x) * np.exp(-x * 16)
    body = hit * 0.8 + thump
    return stereo(fade(body), fade(np.roll(body, 40)))


def entrada(low, bell):
    d = 2.6
    x = t(d)
    boom = sweep(d, low * 2.2, low, 0.25) * np.exp(-x * 2.2)
    crack = noise(d, 200, 9000) * np.exp(-x * 14)
    ring = sum(np.sin(2 * np.pi * f * x + i) * np.exp(-x * (1.6 + i * 0.5)) / (i + 1) for i, f in enumerate(bell))
    return fade(boom * 1.2 + crack * 0.7 + ring * 0.35, 0.002, 0.2)


def vazio_ambiente():
    d = 8.0
    x = t(d)

    def side(detune):
        drone = sum(np.sin(2 * np.pi * (f + detune) * x) * a
                    for f, a in ((55, 1), (82.5, 0.5), (110.125, 0.35), (165, 0.18)))
        swell = 0.75 + 0.25 * np.sin(2 * np.pi * x / d + detune * 20)
        air = noise(d, 2500, 9000) * (0.5 + 0.5 * np.sin(2 * np.pi * 2 * x / d + detune * 9))
        glass = sum(np.sin(2 * np.pi * f * x) * (0.5 + 0.5 * np.sin(2 * np.pi * k * x / d + f)) ** 6
                    for f, k in ((1320, 1), (1760.125, 2), (2217.5, 3)))
        return drone * swell + air * 0.12 + glass * 0.05

    return stereo(side(0), side(0.125))


def santuario_ambiente():
    d = 8.0
    x = t(d)

    def side(seed):
        drone = sum(np.sin(2 * np.pi * f * x) * a for f, a in ((41.25, 1), (43.5, 0.7), (82.5, 0.3), (123.75, 0.15)))
        wind = noise(d, 150, 1400) * (0.45 + 0.4 * np.sin(2 * np.pi * x / d + seed)
                                      + 0.15 * np.sin(2 * np.pi * 3 * x / d + seed * 2))
        embers = np.zeros_like(x)
        for at in rng.uniform(0.1, d - 0.2, 70):
            i = int(at * SR)
            n = int(SR * 0.012)
            embers[i:i + n] += rng.normal(size=n) * np.exp(-np.arange(n) / (SR * 0.002)) * rng.uniform(0.2, 1)
        return drone * 0.9 + wind * 0.55 + embers * 0.25

    return stereo(side(0.0), side(1.3))


def santuario_corte():
    d = 0.38
    x = t(d)
    n = int(SR * d)
    swish = np.zeros(n)
    white = rng.normal(size=n)
    # Ruído por um filtro que desce: a lâmina passando.
    centre = 9000 * np.exp(-x * 9) + 700
    phase = np.cumsum(centre) / SR
    swish = white * 0.4 + np.sin(2 * np.pi * phase) * white * 0.6
    swish *= np.exp(-((x - 0.07) / 0.06) ** 2)
    ring = np.sin(2 * np.pi * 3100 * x) * np.exp(-x * 22) * 0.25
    body = swish + ring
    return stereo(fade(body), fade(np.roll(body, 90) * 0.9))


def saida():
    d = 1.9
    x = t(d)

    def side():
        burst = noise(d, 1500, 14000) * np.exp(-x * 9)
        pings = np.zeros_like(x)
        for _ in range(60):
            at = rng.exponential(0.25)
            if at > d - 0.3:
                continue
            f = rng.uniform(2200, 9000)
            i = int(at * SR)
            y = x[:x.size - i]
            pings[i:] += np.sin(2 * np.pi * f * y) * np.exp(-y * rng.uniform(18, 60)) * rng.uniform(0.2, 1)
        low = np.sin(2 * np.pi * 60 * x) * np.exp(-x * 6)
        return burst * 0.9 + pings * 0.45 + low * 0.8

    return stereo(fade(side(), 0.002, 0.2), fade(side(), 0.002, 0.2))


def domo():
    d = 4.0
    x = t(d)
    hum = sum(np.sin(2 * np.pi * f * x) * a for f, a in ((60, 1), (120, 0.5), (180.25, 0.3), (240, 0.12)))
    return hum * (0.8 + 0.2 * np.sin(2 * np.pi * 2 * x / d)) + noise(d, 300, 1200) * 0.08


def main():
    if subprocess.run(["ffmpeg", "-version"], capture_output=True).returncode != 0:
        sys.exit("ffmpeg não encontrado")
    os.makedirs(OUT, exist_ok=True)
    write("sinal", sinal())
    write("faixa_corte", faixa_corte())
    write("vazio_entrada", entrada(38, (523, 784, 1175, 1568, 2349)))
    write("santuario_entrada", entrada(48, (311, 466, 733, 1244, 1741)))
    write("vazio_ambiente", vazio_ambiente(), 0.5)
    write("santuario_ambiente", santuario_ambiente(), 0.5)
    write("santuario_corte", santuario_corte(), 0.7)
    write("saida", saida())
    write("domo", domo(), 0.6)
    # A voz do conjurador não tem reserva: fica em silêncio até o dono pôr a dele.
    for dom in ("vazio", "santuario"):
        write(dom + "_voz", np.zeros(int(SR * 0.1)) + 1e-12, 0.0)


if __name__ == "__main__":
    main()
