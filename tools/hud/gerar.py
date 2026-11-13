#!/usr/bin/env python3
"""Gera a textura do rodapé do modo Feiticeiro: assets/ryoikitenkai/textures/gui/modo.png.

Desenho próprio, feito por código (numpy + Pillow): rode de novo depois de mexer aqui. A folha tem 512x256 texels,
com DOIS texels por pixel da interface do jogo, para o traço fino aguentar a escala 2 sem borrar. As posições de cada
peça estão em PECAS, e o jogo as lê nas mesmas coordenadas (client/ModeHud.java): mudou aqui, mude lá.

Quase tudo é branco ou cinza com transparência, e o jogo tinge com a cor da técnica. Só a base das molduras tem cor
própria (laca escura).
"""
import os
import sys

import numpy as np
from PIL import Image

W, H = 512, 256

# nome: (x, y, largura, altura) em texels
PECAS = {
    "barra_base": (0, 0, 392, 40),
    "barra_friso": (0, 40, 392, 40),
    "barra_cheia": (0, 80, 392, 40),
    "barra_vidro": (0, 120, 392, 40),
    "casa_base": (0, 160, 40, 40),
    "casa_friso": (40, 160, 40, 40),
    "final_base": (80, 160, 52, 52),
    "final_friso": (132, 160, 52, 52),
    "final_brilho": (184, 160, 52, 52),
    "escudo": (236, 160, 20, 20),
    "icones": (0, 216, 256, 32),  # oito de 32x32: vazio 1 a 4, santuário 1 a 4
}


def grade(w, h):
    y, x = np.mgrid[0:h, 0:w].astype(np.float64)
    return x + 0.5, y + 0.5


def cobre(d):
    """Cobertura de um pixel a uma distância com sinal d da borda (negativa: dentro)."""
    return np.clip(0.5 - d, 0, 1)


def hexagono(x, y, cx, cy, meia_l, meia_a, k=1.0):
    """Um retângulo comprido de pontas em bico: distância com sinal."""
    ax, ay = np.abs(x - cx), np.abs(y - cy)
    return np.maximum(ay - meia_a, (ax + ay * k - meia_l) / np.sqrt(1 + k * k))


def losango(x, y, cx, cy, r):
    return (np.abs(x - cx) + np.abs(y - cy) - r) / np.sqrt(2)


def oitavado(x, y, cx, cy, meia, canto):
    """Quadrado de cantos cortados."""
    ax, ay = np.abs(x - cx), np.abs(y - cy)
    return np.maximum(np.maximum(ax, ay) - meia, (ax + ay - (2 * meia - canto)) / np.sqrt(2))


def circulo(x, y, cx, cy, r):
    return np.hypot(x - cx, y - cy) - r


def segmento(x, y, x0, y0, x1, y1, r):
    px, py, dx, dy = x - x0, y - y0, x1 - x0, y1 - y0
    t = np.clip((px * dx + py * dy) / (dx * dx + dy * dy), 0, 1)
    return np.hypot(px - t * dx, py - t * dy) - r


def ruido(w, h, sx, sy, seed):
    """Ruído suave: uma grade de valores esticada (sx, sy texels por célula)."""
    rng = np.random.default_rng(seed)
    gw, gh = int(np.ceil(w / sx)) + 3, int(np.ceil(h / sy)) + 3
    g = rng.random((gh, gw))
    small = Image.fromarray((g * 255).astype(np.uint8))
    big = small.resize((int(gw * sx), int(gh * sy)), Image.BICUBIC)
    return np.asarray(big, dtype=np.float64)[:h, :w] / 255


def rgba(w, h):
    return np.zeros((h, w, 4), dtype=np.float64)


def pinta(img, cor, alfa):
    """Compõe uma cor (r, g, b de 0 a 1, ou matrizes) por cima, com a cobertura alfa."""
    a = np.clip(alfa, 0, 1)
    for i in range(3):
        c = cor[i] if not np.isscalar(cor[i]) else np.full(a.shape, cor[i])
        img[..., i] = (c * a + img[..., i] * img[..., 3] * (1 - a)) / np.maximum(a + img[..., 3] * (1 - a), 1e-9)
    img[..., 3] = a + img[..., 3] * (1 - a)


def branco(alfa):
    img = rgba(alfa.shape[1], alfa.shape[0])
    img[..., :3] = 1
    img[..., 3] = np.clip(alfa, 0, 1)
    return img


# A barra de vida: 196x20 pixels da interface. O corpo é um hexágono comprido; à esquerda, um losango maior que ele,
# onde o jogo escreve a vida em número.
BW, BH = 392, 40
CORPO = dict(cx=212, cy=20, meia_l=176, meia_a=13)   # corpo de fora: x de 36 a 388
OCO = dict(cx=212, cy=20, meia_l=170, meia_a=8)      # onde a vida corre
GEMA = dict(cx=22, cy=20, r=19)


def barra_base():
    x, y = grade(BW, BH)
    img = rgba(BW, BH)
    corpo = hexagono(x, y, **CORPO)
    gema = losango(x, y, **GEMA)
    fora = np.minimum(corpo, gema)
    # Sombra por baixo de tudo, para a barra descolar de qualquer fundo.
    pinta(img, (0, 0, 0), 0.45 * cobre(np.minimum(hexagono(x, y - 1.5, **CORPO), losango(x, y - 1.5, **GEMA)) - 1.5))
    # Laca escura: mais clara em cima, com um grão leve.
    grao = ruido(BW, BH, 3, 3, 7) * 0.035
    luz = 0.115 - 0.07 * (y / BH) + grao
    pinta(img, (luz * 1.0, luz * 0.95, luz * 1.25), cobre(fora))
    # O oco: quase preto, com sombra interna em cima (a moldura faz sombra na vida).
    oco = hexagono(x, y, **OCO)
    fundo = 0.03 + 0.03 * np.clip((y - 12) / 16, 0, 1)
    pinta(img, (fundo, fundo, fundo * 1.4), cobre(oco))
    # O miolo do losango: um poço escuro para o número.
    poco = losango(x, y, GEMA["cx"], GEMA["cy"], GEMA["r"] - 5)
    pinta(img, (0.035, 0.03, 0.055), cobre(poco))
    return img


def barra_friso():
    x, y = grade(BW, BH)
    corpo = hexagono(x, y, **CORPO)
    gema = losango(x, y, **GEMA)
    fora = np.minimum(corpo, gema)
    a = np.zeros((BH, BW))
    # Fio de fora, fino e claro; mais forte em cima, onde a luz bate.
    luz = 0.55 + 0.45 * np.clip(1 - y / BH * 1.3, 0, 1)
    a = np.maximum(a, cobre(np.abs(fora + 1.2) - 0.75) * luz)
    # Fio em volta do oco, mais apagado.
    oco = hexagono(x, y, **OCO)
    a = np.maximum(a, cobre(np.abs(oco - 1.6) - 0.55) * 0.45)
    # O losango: fio duplo.
    a = np.maximum(a, cobre(np.abs(gema + 1.2) - 0.8) * luz)
    a = np.maximum(a, cobre(np.abs(gema + 5.2) - 0.5) * 0.55)
    # Na ponta direita, um pequeno losango cheio, como remate.
    a = np.maximum(a, cobre(losango(x, y, 388, 20, 3.2)) * 0.9)
    return branco(a)


def barra_cheia():
    """A vida: clara, com veios que correm ao comprido, um fio de luz perto do topo e a base mais escura."""
    x, y = grade(BW, BH)
    oco = hexagono(x, y, **OCO)
    v = (y - (OCO["cy"] - OCO["meia_a"])) / (2 * OCO["meia_a"])  # 0 em cima, 1 embaixo
    veios = ruido(BW, BH, 26, 2.2, 11) * 0.6 + ruido(BW, BH, 9, 1.6, 12) * 0.4
    luz = 0.66 + 0.26 * veios
    luz += 0.46 * np.exp(-((v - 0.25) / 0.12) ** 2)   # o fio de luz
    luz -= 0.36 * np.clip((v - 0.58) / 0.42, 0, 1)   # a base
    luz -= 0.20 * np.clip((0.10 - v) / 0.10, 0, 1)   # a beirada de cima
    luz = np.clip(luz, 0, 1)
    img = rgba(BW, BH)
    img[..., 0] = img[..., 1] = img[..., 2] = luz
    img[..., 3] = cobre(oco + 0.6)
    return img


def barra_vidro():
    """Por cima da vida: os entalhes que a dividem em dez, e um reflexo de vidro na metade de cima."""
    x, y = grade(BW, BH)
    oco = hexagono(x, y, **OCO)
    dentro = cobre(oco + 0.6)
    img = rgba(BW, BH)
    # Reflexo: branco, some para baixo.
    v = (y - (OCO["cy"] - OCO["meia_a"])) / (2 * OCO["meia_a"])
    reflexo = 0.16 * np.clip(1 - v / 0.45, 0, 1) * dentro
    img[..., :3] = 1
    img[..., 3] = reflexo
    # Entalhes: riscos escuros e inclinados, a cada décimo do oco.
    x0, x1 = OCO["cx"] - OCO["meia_l"], OCO["cx"] + OCO["meia_l"]
    for i in range(1, 10):
        cx = x0 + (x1 - x0) * i / 10
        risco = cobre(np.abs((x - cx) + (y - OCO["cy"]) * 0.35) - 0.6) * dentro
        pinta(img, (0.02, 0.02, 0.04), risco * 0.62)
    return img


def casa(lado, canto, dupla):
    x, y = grade(lado, lado)
    c = lado / 2
    fora = oitavado(x, y, c, c, c - 2, canto)
    base = rgba(lado, lado)
    pinta(base, (0, 0, 0), 0.45 * cobre(oitavado(x, y - 1.5, c, c, c - 2, canto) - 1.5))
    grao = ruido(lado, lado, 3, 3, 21 + lado) * 0.035
    luz = 0.115 - 0.07 * (y / lado) + grao
    pinta(base, (luz, luz * 0.95, luz * 1.25), cobre(fora))
    oco = oitavado(x, y, c, c, c - 6, canto * 0.72)
    fundo = 0.03 + 0.035 * np.clip((y - 8) / (lado - 8), 0, 1)
    pinta(base, (fundo, fundo, fundo * 1.4), cobre(oco))
    luzf = 0.55 + 0.45 * np.clip(1 - y / lado * 1.3, 0, 1)
    a = cobre(np.abs(fora + 1.2) - 0.75) * luzf
    a = np.maximum(a, cobre(np.abs(oco - 1.4) - 0.5) * 0.45)
    if dupla:
        # A casa do domínio: um segundo fio e um losango em cada canto cortado.
        a = np.maximum(a, cobre(np.abs(fora + 3.6) - 0.5) * 0.6)
        for sx in (-1, 1):
            for sy in (-1, 1):
                a = np.maximum(a, cobre(losango(x, y, c + sx * (c - 5.5), c + sy * (c - 5.5), 2.6)) * 0.95)
    return base, branco(a)


def final_brilho(lado=52):
    """O halo da casa do domínio quando está pronta: um anel macio, somado por cima."""
    x, y = grade(lado, lado)
    c = lado / 2
    d = oitavado(x, y, c, c, c - 6, 10 * 0.72)
    a = np.exp(-(np.maximum(-d, 0) / 5.0) ** 1.4) * cobre(d) * 0.9
    return branco(a)


def escudo():
    """Um escudo pequeno: o contorno, com a ponta para baixo, e um miolo fraco."""
    x, y = grade(20, 20)
    ax = np.abs(x - 10)
    # Lados retos até o meio e depois fechando em ponta.
    lado = ax - np.where(y < 10, 6.6, 6.6 - (y - 10) * 0.86)
    forma = np.maximum(np.maximum(lado, 2.6 - y), y - 17.6)
    a = cobre(np.abs(forma + 1.0) - 0.85) * 0.95
    a = np.maximum(a, cobre(forma + 2.6) * 0.32)
    return branco(a)


# Os ícones: 32x32, brancos. Cada um é a ideia da habilidade num traço só.
def icone(desenho):
    x, y = grade(32, 32)
    return branco(np.clip(desenho(x, y), 0, 1))


def seta(x, y, px, py, dx, dy, tam, r=1.1):
    """Uma ponta de seta em (px, py) apontando para (dx, dy)."""
    n = np.hypot(dx, dy)
    dx, dy = dx / n, dy / n
    ox, oy = -dy, dx
    a = segmento(x, y, px, py, px - dx * tam + ox * tam, py - dy * tam + oy * tam, r)
    b = segmento(x, y, px, py, px - dx * tam - ox * tam, py - dy * tam - oy * tam, r)
    return np.minimum(a, b)


def i_atrair(x, y):
    # Um ponto que puxa: quatro setas para dentro em volta de um núcleo.
    a = cobre(circulo(x, y, 16, 16, 3.4))
    a = np.maximum(a, cobre(np.abs(circulo(x, y, 16, 16, 6.6)) - 0.7) * 0.55)
    for dx, dy in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        px, py = 16 + dx * 7.6, 16 + dy * 7.6
        a = np.maximum(a, cobre(seta(x, y, px, py, -dx, -dy, 4.2)))
        a = np.maximum(a, cobre(segmento(x, y, px, py, 16 + dx * 12.6, 16 + dy * 12.6, 1.1)))
    return a


def i_repelir(x, y):
    # Um núcleo que estoura: raios para fora.
    a = cobre(circulo(x, y, 16, 16, 4.6))
    for k in range(8):
        ang = np.pi / 8 + k * np.pi / 4
        c, s = np.cos(ang), np.sin(ang)
        lon = 13.6 if k % 2 == 0 else 11.0
        a = np.maximum(a, cobre(segmento(x, y, 16 + c * 7.4, 16 + s * 7.4, 16 + c * lon, 16 + s * lon, 1.15)))
    return a


def i_passo(x, y):
    # Um passo que não atravessa o espaço: a figura aqui, e os rastros dela atrás.
    a = cobre(losango(x, y, 21.5, 16, 6.6))
    a = np.maximum(a, cobre(np.abs(losango(x, y, 14.5, 16, 6.0)) - 0.75) * 0.75)
    a = np.maximum(a, cobre(np.abs(losango(x, y, 8.2, 16, 5.2)) - 0.65) * 0.42)
    return a


def i_vazio(x, y):
    # O domínio do Vazio: o astrolábio. Um aro, duas órbitas cruzadas e o ponto escuro no meio.
    a = cobre(np.abs(circulo(x, y, 16, 16, 12.2)) - 0.9)
    for ang in (0.62, -0.62):
        c, s = np.cos(ang), np.sin(ang)
        u = (x - 16) * c + (y - 16) * s
        v = -(x - 16) * s + (y - 16) * c
        elipse = (np.hypot(u / 12.0, v / 4.6) - 1) * 4.6
        a = np.maximum(a, cobre(np.abs(elipse) - 0.7) * 0.85)
    a = np.maximum(a, cobre(circulo(x, y, 16, 16, 2.6)))
    return a


def crescente(x, y, cx, cy, r, grossura, ang, abertura):
    """Um arco que afina nas pontas, como um corte de lâmina."""
    u = (x - cx) * np.cos(ang) + (y - cy) * np.sin(ang)
    v = -(x - cx) * np.sin(ang) + (y - cy) * np.cos(ang)
    t = np.arctan2(v, u)
    f = np.clip(1 - (np.abs(t) / abertura) ** 2, 0, 1)
    return np.abs(np.hypot(u, v) - r) - grossura * np.sqrt(f) + (1 - np.sign(f)) * 4


def i_talho(x, y):
    # Um corte só, que voa: um crescente.
    return cobre(crescente(x, y, 4.5, 27.5, 21.0, 2.3, -np.pi / 4, 0.62))


def i_retalhar(x, y):
    # Vários cortes cruzados de perto.
    a = cobre(segmento(x, y, 6, 8, 26, 24, 0.2) - 1.25 * np.clip(1 - np.abs((x - 16) / 11) ** 2, 0, 1))
    a = np.maximum(a, cobre(segmento(x, y, 26, 7, 6, 25, 0.2) - 1.25 * np.clip(1 - np.abs((x - 16) / 11) ** 2, 0, 1)))
    a = np.maximum(a, cobre(segmento(x, y, 4.5, 17.5, 27.5, 14.5, 0.2) - 1.0 * np.clip(1 - np.abs((x - 16) / 12) ** 2, 0, 1)) * 0.85)
    return a


def i_rede(x, y):
    # Uma rede de cortes armada no chão.
    a = np.zeros(x.shape)
    for k in (-1, 0, 1):
        a = np.maximum(a, cobre(segmento(x, y, 7 + k * 6.5, 26, 16 + k * 6.5, 6, 0.95)))
        a = np.maximum(a, cobre(segmento(x, y, 5.5, 22 - k * 5.6 - 0.8, 27.5, 18.4 - k * 5.6 - 0.8, 0.95)) * 0.8)
    return a


def i_santuario(x, y):
    # O domínio do Santuário: um portal. Dois postes, uma trave e um telhado de pontas erguidas.
    a = cobre(np.maximum(np.abs(x - 9.5) - 1.5, np.maximum(11 - y, y - 28)))
    a = np.maximum(a, cobre(np.maximum(np.abs(x - 22.5) - 1.5, np.maximum(11 - y, y - 28))))
    a = np.maximum(a, cobre(np.maximum(np.abs(x - 16) - 9.5, np.abs(y - 15.5) - 1.2)))
    telhado = np.abs(y - (9.2 - 0.022 * (x - 16) ** 2)) - 1.7
    a = np.maximum(a, cobre(np.maximum(telhado, np.abs(x - 16) - 13.5)))
    return a


def cola(folha, nome, img):
    x, y, w, h = PECAS[nome]
    assert img.shape[1] == w and img.shape[0] == h, (nome, img.shape)
    folha[y:y + h, x:x + w] = img


def folha():
    f = np.zeros((H, W, 4))
    cola(f, "barra_base", barra_base())
    cola(f, "barra_friso", barra_friso())
    cola(f, "barra_cheia", barra_cheia())
    cola(f, "barra_vidro", barra_vidro())
    base, friso = casa(40, 7, False)
    cola(f, "casa_base", base)
    cola(f, "casa_friso", friso)
    base, friso = casa(52, 10, True)
    cola(f, "final_base", base)
    cola(f, "final_friso", friso)
    cola(f, "final_brilho", final_brilho())
    cola(f, "escudo", escudo())
    x, y, w, h = PECAS["icones"]
    for i, d in enumerate((i_atrair, i_repelir, i_passo, i_vazio, i_talho, i_retalhar, i_rede, i_santuario)):
        f[y:y + 32, x + i * 32:x + i * 32 + 32] = icone(d)
    return f


if __name__ == "__main__":
    aqui = os.path.dirname(os.path.abspath(__file__))
    destino = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        aqui, "..", "..", "src", "main", "resources", "assets", "ryoikitenkai", "textures", "gui", "modo.png")
    Image.fromarray((np.clip(folha(), 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA").save(destino, optimize=True)
    print("escrito:", os.path.normpath(destino))
