#!/usr/bin/env python3
"""Converte os seus áudios para os sons do RyoikiTenkai: corta, ajusta o volume, põe em mono ou estéreo conforme o
som, grava em .ogg (Vorbis, 44,1 kHz) com o nome certo, na pasta que o mod lê.

Precisa do ffmpeg no PATH. Para origens que são links, precisa também do yt-dlp (pip install yt-dlp).

Uso:
    python converter.py --lista                      mostra os nomes dos sons e o que cada um é
    python converter.py sons.txt                     converte tudo o que está em sons.txt
    python converter.py sons.txt --pasta "C:/.../.minecraft"
    python converter.py sons.txt --saida "meu-pack/assets/ryoikitenkai/sounds"

sons.txt tem uma linha por som, com os campos separados por "|":

    nome | origem | início | duração

    vazio_ambiente    | C:/audios/tema.mp3     | 0:42 | 30
    santuario_voz     | https://...            | 3.5  | 2.2
    saida             | vidro.wav

"origem" é um arquivo do seu PC ou um link que o yt-dlp saiba baixar. "início" e "duração" são opcionais, em
segundos ou m:ss. Linhas vazias e as que começam com # são ignoradas.

Por padrão grava em <pasta>/config/ryoikitenkai/sons/, que o mod lê ao abrir o jogo (e no F3+T). O mod não traz som
nenhum: o que não tiver arquivo fica em silêncio. Com --saida grava em outra pasta.
"""
import argparse
import os
import shutil
import subprocess
import sys
import tempfile

# nome: (canais, laço, descrição). Mono é obrigatório para o que toca de um lugar do mundo: o jogo não atenua estéreo.
SONS = {
    "vazio_trilha": (2, False, "a trilha da expansão inteira do Vazio: do sinal de mão ao fim da quebra (16,45 s)"),
    "santuario_trilha": (2, False, "a trilha da expansão inteira do Santuário (20,45 s)"),
    "clash_trilha": (2, False, "a trilha do choque de domínios, no lugar da trilha do domínio (até 15 s)"),
    "golpe": (1, False, "um golpe do combo no choque, no conjurador (bem curto: até 4 por segundo)"),
    "impacto": (1, False, "um golpe que acerta, em quem leva (bem curto)"),
    "sinal": (1, False, "o sinal de mão carregando, no conjurador (1,25 s)"),
    "faixa_corte": (2, False, "a batida em cada corte de câmera da faixa (curto)"),
    "saida": (2, False, "o domínio se quebrando, para quem está dentro"),
    "domo": (1, True, "o zumbido do domo, para quem está fora (laço)"),
    "vazio_voz": (2, False, "a fala do conjurador ao expandir o Vazio, desde o início do sinal de mão"),
    "vazio_entrada": (1, False, "o Vazio se fechando"),
    "vazio_ambiente": (2, True, "o que se ouve dentro do Vazio (laço; 14 s de domínio)"),
    "santuario_voz": (2, False, "a fala do conjurador ao expandir o Santuário, desde o início do sinal de mão"),
    "santuario_entrada": (1, False, "o Santuário se fechando"),
    "santuario_ambiente": (2, True, "o que se ouve dentro do Santuário (laço; 18 s de domínio)"),
    "vazio_rajada": (2, False, "uma rajada de feixes do Vazio (a cada 3,2 s, menos de 1 s)"),
    "santuario_corte": (2, False, "um corte do Santuário (toca a cada 8 ticks, bem curto)"),
}


def segundos(texto):
    texto = texto.strip()
    if not texto:
        return None
    total = 0.0
    for parte in texto.split(":"):
        total = total * 60 + float(parte.replace(",", "."))
    return total


def ler(caminho):
    itens = []
    with open(caminho, encoding="utf-8-sig") as f:
        for numero, linha in enumerate(f, 1):
            linha = linha.strip()
            if not linha or linha.startswith("#"):
                continue
            campos = [c.strip() for c in linha.split("|")]
            if len(campos) < 2 or not campos[1]:
                sys.exit(f"{caminho}, linha {numero}: falta a origem (nome | origem | início | duração)")
            nome = campos[0].lower()
            if nome not in SONS:
                sys.exit(f"{caminho}, linha {numero}: '{nome}' não é um som do mod. Veja --lista.")
            try:
                inicio = segundos(campos[2]) if len(campos) > 2 else None
                duracao = segundos(campos[3]) if len(campos) > 3 else None
            except ValueError:
                sys.exit(f"{caminho}, linha {numero}: início e duração são segundos ou m:ss")
            itens.append((nome, campos[1], inicio, duracao))
    return itens


def baixar(link, pasta):
    if shutil.which("yt-dlp") is None:
        sys.exit("Para links é preciso o yt-dlp: pip install yt-dlp")
    modelo = os.path.join(pasta, "%(id)s.%(ext)s")
    subprocess.run(["yt-dlp", "--no-playlist", "-f", "bestaudio/best", "-o", modelo, link], check=True)
    arquivos = sorted(os.listdir(pasta), key=lambda n: os.path.getmtime(os.path.join(pasta, n)))
    if not arquivos:
        sys.exit(f"O yt-dlp não baixou nada de {link}")
    return os.path.join(pasta, arquivos[-1])


def converter(nome, origem, inicio, duracao, destino, volume):
    canais, laco, _ = SONS[nome]
    comando = ["ffmpeg", "-y", "-loglevel", "error"]
    if inicio:
        comando += ["-ss", str(inicio)]
    comando += ["-i", origem]
    if duracao:
        comando += ["-t", str(duracao)]
    filtros = []
    if volume:
        filtros.append("loudnorm=I=-16:TP=-1.5:LRA=11")
    if not laco:
        # Um corte seco estala; os laços ficam como vieram, para a emenda não aparecer.
        filtros.append("afade=t=in:d=0.01")
        if duracao:
            filtros.append(f"afade=t=out:st={max(0.0, duracao - 0.08)}:d=0.08")
    if filtros:
        comando += ["-af", ",".join(filtros)]
    saida = os.path.join(destino, nome + ".ogg")
    comando += ["-vn", "-map_metadata", "-1", "-ac", str(canais), "-ar", "44100", "-c:a", "libvorbis", "-q:a", "5",
                saida]
    subprocess.run(comando, check=True)
    return saida


def main():
    p = argparse.ArgumentParser(description="Converte áudios para os sons do RyoikiTenkai.")
    p.add_argument("arquivo", nargs="?", help="a lista: nome | origem | início | duração")
    p.add_argument("--pasta", default=".", help="a pasta do jogo (onde fica a config/); padrão: a pasta atual")
    p.add_argument("--saida", help="grava nesta pasta em vez de <pasta>/config/ryoikitenkai/sons")
    p.add_argument("--sem-volume", action="store_true", help="não normaliza o volume")
    p.add_argument("--lista", action="store_true", help="mostra os sons do mod e sai")
    a = p.parse_args()
    if a.lista or not a.arquivo:
        for nome, (canais, laco, texto) in SONS.items():
            print(f"{nome:20} {'mono   ' if canais == 1 else 'estéreo'} {texto}")
        return
    if shutil.which("ffmpeg") is None:
        sys.exit("ffmpeg não encontrado no PATH")
    destino = a.saida or os.path.join(a.pasta, "config", "ryoikitenkai", "sons")
    os.makedirs(destino, exist_ok=True)
    itens = ler(a.arquivo)
    with tempfile.TemporaryDirectory() as tmp:
        for i, (nome, origem, inicio, duracao) in enumerate(itens):
            if "://" in origem:
                pasta = os.path.join(tmp, str(i))
                os.makedirs(pasta)
                origem = baixar(origem, pasta)
            elif not os.path.isfile(origem):
                sys.exit(f"{nome}: não achei o arquivo {origem}")
            saida = converter(nome, origem, inicio, duracao, destino, not a.sem_volume)
            print(f"{nome}: {saida}")
    faltam = [n for n in SONS if not os.path.isfile(os.path.join(destino, n + ".ogg"))]
    if faltam:
        print("Sem arquivo (ficam em silêncio):", ", ".join(faltam))


if __name__ == "__main__":
    main()
