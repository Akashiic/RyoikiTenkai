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
* O visual ainda é um rascunho: céu, chão e domo desenhados por código, e um flash na ida e na volta.

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
