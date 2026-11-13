# Sons do RyoikiTenkai

O mod traz um som para cada momento do domínio, feito por síntese. Qualquer um deles pode ser trocado por um arquivo
seu, sem recompilar: basta um `.ogg` com o nome certo em `config/ryoikitenkai/sons/` **do cliente** (vai no modpack).
O jogo lê a pasta ao abrir e no F3+T.

## Os nomes

```
python converter.py --lista
```

| Nome | O que é | Canais |
|---|---|---|
| `sinal` | o sinal de mão carregando (1,25 s) | mono |
| `faixa_corte` | a batida em cada corte de câmera da faixa | estéreo |
| `saida` | o domínio se quebrando | estéreo |
| `domo` | o zumbido do domo para quem está fora (laço) | mono |
| `vazio_voz`, `santuario_voz` | a fala do conjurador (no mod é silêncio) | mono |
| `vazio_entrada`, `santuario_entrada` | o domínio se fechando | mono |
| `vazio_ambiente`, `santuario_ambiente` | o que se ouve dentro (laço; 14 s e 18 s de domínio) | estéreo |
| `santuario_corte` | um corte (toca a cada 0,4 s: use algo bem curto) | estéreo |

Mono é obrigatório no que toca de um lugar do mundo: o jogo não diminui um som estéreo com a distância.

## Converter os seus áudios

Precisa do [ffmpeg](https://ffmpeg.org/) no PATH; para origens que são links, também do `yt-dlp`
(`pip install yt-dlp`).

1. Copie `sons-exemplo.txt` para `sons.txt` e preencha: `nome | origem | início | duração`. A origem é um arquivo do
   seu PC ou um link; início e duração são opcionais (segundos ou `m:ss`).
2. Rode, apontando para a pasta do jogo:

   ```
   python converter.py sons.txt --pasta "C:/Users/voce/AppData/Roaming/.technic/modpacks/teof"
   ```

O script corta, normaliza o volume (`--sem-volume` desliga), põe cada som em mono ou estéreo, grava em Vorbis
44,1 kHz e salva como `<pasta>/config/ryoikitenkai/sons/<nome>.ogg`. Com `--saida <pasta>` grava em outro lugar (por
exemplo `assets/ryoikitenkai/sounds` de um resource pack). No fim ele lista os sons que continuam com o do mod.

## Refazer os sons do mod

```
python3 tools/sons/gerar.py
```

Escreve em `src/main/resources/assets/ryoikitenkai/sounds/` (numpy + ffmpeg).
