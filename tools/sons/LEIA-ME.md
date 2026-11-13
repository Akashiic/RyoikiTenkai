# Sons do RyoikiTenkai

**O mod não traz som nenhum.** Cada som é um `.ogg` que você põe em `config/ryoikitenkai/sons/` **do cliente** (vai no
modpack), com o nome certo; o que não tiver arquivo fica em silêncio. O jogo lê a pasta ao abrir e no F3+T.

## A trilha da expansão

Para ter um som só por domínio, bastam dois arquivos:

| Arquivo | Começa | Dura |
|---|---|---|
| `vazio_trilha.ogg` | no sinal de mão | 16,45 s (1,25 do sinal + 14 dentro + 1,2 da quebra) |
| `santuario_trilha.ogg` | no sinal de mão | 20,45 s (1,25 + 18 + 1,2) |

Toca uma vez, em estéreo, no ouvido do conjurador e de quem está perto dele no sinal de mão (os que veem a faixa).
Quem não foi pego deixa de ouvir quando o domínio fecha. Se o domínio acabar antes, a trilha some em meio segundo; se
o arquivo for mais longo que o domínio, é cortado do mesmo jeito. Segue o volume geral do jogo.

Marcos dentro da trilha, se quiser casar o áudio com a imagem: cortes de câmera da faixa em 0,3 s e 0,6 s; o domínio
fecha em 1,25 s; termina em 15,25 s (Vazio) ou 19,25 s (Santuário), quando começa a quebra.

## Os outros sons (opcionais)

| Nome | O que é | Canais |
|---|---|---|
| `sinal` | o sinal de mão, no conjurador (1,25 s) | mono |
| `vazio_voz`, `santuario_voz` | a fala do conjurador, nele | mono |
| `faixa_corte` | a batida em cada corte de câmera da faixa | estéreo |
| `vazio_entrada`, `santuario_entrada` | o domínio se fechando; quem está fora ouve do domo | mono |
| `vazio_ambiente`, `santuario_ambiente` | laço enquanto se está dentro | estéreo |
| `santuario_corte` | um corte (a cada 0,4 s: use algo bem curto) | estéreo |
| `saida` | o domínio se quebrando | estéreo |
| `domo` | o zumbido do domo para quem está fora (laço) | mono |

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
44,1 kHz e salva como `<pasta>/config/ryoikitenkai/sons/<nome>.ogg`. `python converter.py --lista` mostra os nomes.
