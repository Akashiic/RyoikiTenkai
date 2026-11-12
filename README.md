# RyoikiTenkai

**Expansão de Domínio** e **Choque de Domínios** para o servidor Minecraft 1.7.10 da Rede Akashic, inspirados em
Jujutsu Kaisen e no Jujutsu Shenanigans. É um mod Forge para os **dois lados**: o servidor conduz os domínios e as
arenas, o cliente desenha.

O estado do projeto, as decisões e o que falta estão em [`docs/HANDOFF.md`](docs/HANDOFF.md).

## O que já existe

* `/ryoiki expandir <vazio|santuario> [jogador]` (operadores): o sinal de mão de 1,25 s, e quem está no raio vai para a
  arena, cada um na sua posição em relação ao conjurador. No fim (14 s no Vazio Infinito, 18 s no Santuário Malevolente)
  todos voltam para onde estavam.
* `/ryoiki encerrar` e `/ryoiki preparar` (constrói as arenas do mundo antes do primeiro uso).
* Quem fica de fora vê o domo (branco ou preto) no lugar da conjuração.
* O visual dos dois domínios (Vazio Estelar e Santuário Carmesim), todo desenhado por código: faixa de entrada com o
  rosto da skin do conjurador, selo no chão, céu com reflexo, estruturas, cortes, tremor e saída em estilhaços. Ver
  [`docs/VISUAL.md`](docs/VISUAL.md).

## Como funciona, em uma frase

A arena fica longe do mapa e é feita de um bloco invisível; o cliente recebe os chunks dela durante o sinal de mão,
não tem malha nenhuma para montar e desenha o domínio por cima. Os detalhes estão em
[`docs/TRANSPORTE.md`](docs/TRANSPORTE.md).

## Build

```bash
./gradlew spotlessApply build
```

Gradle no padrão GTNH (RetroFuturaGradle), bytecode Java 8 (Jabel). O jar sai em `build/libs/` e vai em `mods/` no
servidor e no cliente.

## Teste automático do teleporte

```bash
echo warm > run/client/ryoiki-harness
xvfb-run -a -s "-screen 0 1280x720x24 +extension RANDR" ./gradlew runClient -Pharness
```

O cliente abre um mundo, conjura os dois domínios, grava quadro a quadro o que acontece em cada teleporte
(`run/client/ryoiki-harness.txt`), salva capturas (`run/client/screenshots/ryoiki-*.png`) e fecha. Com `cold` no
arquivo, conjura sem preparar as arenas antes. Sem placa de vídeo: `LIBGL_ALWAYS_SOFTWARE=1`, e o LWJGL 2 precisa do
`xrandr` (`apt-get install -y x11-xserver-utils`).

O mesmo harness entra num servidor de verdade (`server <host> <porta>` no arquivo) e obedece a um arquivo de comandos.
Foi assim que o mod foi testado no Crucible da rede com o modpack TEOF 5.4:
[`tools/teste-real/LEIA-ME.md`](tools/teste-real/LEIA-ME.md).

## No servidor

* O jar vai em `mods/` do servidor e de todos os clientes.
* As arenas ficam em x = 200.000, z = 200.000 de cada mundo (`config/ryoikitenkai.cfg`). **Num mundo com borda
  (WorldBorder), elas precisam ficar dentro dela**; se o teleporte for bloqueado, o domínio é cancelado com um aviso e
  ninguém sai do lugar.
* Opcional, para as arenas ficarem sempre carregadas, em `config/forgeChunkLoading.cfg`:

  ```
  ryoikitenkai {
      I:maximumChunksPerTicket=8
      I:maximumTicketCount=4
  }
  ```
