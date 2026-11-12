# Teste no servidor real

Como o RyoikiTenkai foi testado, em 2026-11-12, com os arquivos do servidor da rede (Crucible) e o cliente do modpack
TEOF 5.4, numa máquina sem tela e sem placa de vídeo. Os scripts desta pasta são os que foram usados; os caminhos
(`/home/user/teof`, o Java 8 baixado pelo Gradle) são os daquela máquina e precisam ser ajustados.

Os dois pacotes vêm do dono (o cliente é a release `TEOF-5.4` de `Akashiic/Technic-Forbidden`; o servidor, um link que
ele manda). **Nada deles entra neste repositório.**

## Servidor

1. Extraia o pacote sem `akashicweb`, `crash-reports`, `stall-reports`, `logs` e `journeymap`.
2. **Desligue o que fala com o mundo de fora** antes de ligar qualquer coisa. O pacote é uma cópia da produção e traz
   credenciais. Foram tirados: DiscordSRV (e o seu fix), LiteBans, PixCraft, PixelVip, pvpstats, StatsAPI, Votifier,
   Updater e o mod `akashicweb`; e, a pedido do dono, CastleOfStone e BungeeGuard. O LuckPerms passou de `mysql` para
   `h2`, e `bungeecord` foi para `false` no `spigot.yml`.
3. **Alinhe os mods com os do cliente** (o servidor só aceita quem tem as mesmas versões). Em 2026-11-12 foi preciso
   trocar AM2.5, ATM-Family, Mirai, Eln, Omni Pipes, adventurebackpack, akashicmixer, AE2, etfuturum, gtnhlib,
   hodgepodge e BaublesExpanded (este fica em `mods/1.7.10/`) pelos jars do cliente, e acrescentar kunftig, futura3d,
   VitaGenesis, AkashicBellow, akashaquestui, AM2PlayGround, Akashic-ElnEndlessIdsFix e AkashicBackpackCompat.
4. `eula=true`, e `server.sh` (Java 21, os `-javaagent` do pacote, `-Dfml.queryResult=confirm` porque a troca de mods
   muda o registro de itens). Os comandos entram pelo `in.fifo`.
5. O pacote não traz os mundos. O `world` é gerado; o `Akashic/Spawn` se cria com
   `mv create Akashic/Spawn normal -g AkashicVoidGenerator`. Para levar alguém para lá: `mv tp <jogador> Akashic/Spawn`
   e `mvconfirm` (o mundo é vazio, o Multiverse pede confirmação).

Sobe em cerca de um minuto e usa perto de 5 GB.

## Cliente

O pacote é no formato Technic: `bin/modpack.jar` (o Forge), `bin/minecraft.jar` e os mods.

1. `fetchlibs.py <pasta do cliente> <pasta de saída>` junta as bibliotecas dos dois JSON de `bin/` (do cache do
   Gradle ou baixando). O LWJGL é trocado pelo 2.9.4 do cache do Gradle, para casar com as bibliotecas nativas que o
   `runClient` deste repositório já extrai em `run/natives/lwjgl2`.
2. Copie o jar do mod **com o harness** (`./gradlew build -Pharness`) para `mods/` do cliente e do servidor.
3. `echo "server 127.0.0.1 25871" > ryoiki-harness` na pasta do cliente, e `client.sh <pasta> <nome>
   -Dryoiki.devHarness=true`. O servidor está em modo offline; dê `op` ao nome antes.

Carrega os 286 mods em 3 a 8 minutos e usa de 5,5 a 7 GB. **Dois clientes completos não cabem em 15 GB** junto com o
servidor. Para dois jogadores foi usada uma versão enxuta: o mesmo Crucible com todos os plugins, mas só com
unimixins, gtnhlib, hodgepodge, lwjgl3ify, falsepatternlib e o RyoikiTenkai no servidor, e unimixins, gtnhlib,
hodgepodge, angelica e o RyoikiTenkai em cada cliente. Sobe em 40 segundos.

## Comandar o teste

O harness entra, espera um arquivo `ryoiki-harness-go` e passa a ler comandos de `ryoiki-harness-cmd` (um por linha;
o arquivo é apagado ao ser lido). O relatório vai para `ryoiki-harness.txt` e as capturas para `screenshots/`.

| Comando | O que faz |
|---|---|
| `chat <texto>` | Manda uma linha de chat ou um comando (`chat /ryoiki preparar`) |
| `cast <vazio\|santuario>` | Conjura e grava a ida, a parede e a volta |
| `view <0\|1\|2>` | Primeira ou terceira pessoa |
| `look <yaw> <pitch>` | Vira o jogador |
| `shot <nome>` | Captura de tela |
| `watch <segundos>` | Uma captura e uma linha de estado a cada segundo e meio (para o jogador que observa) |
| `wait <segundos>`, `where`, `quit` | Espera, escreve a posição e o chat recente, fecha |

Para o que o jogador não faz sozinho, use o console do servidor: `sudo <jogador> spawn`, `kick`, `save-all`, `stop`.
`pos.py <arquivo .dat>` lê do `world/playerdata/` a posição salva de um jogador e a marca de retorno do mod.

## Tropeços que custaram tempo

* **Entrar no servidor por código:** abrir um `GuiConnecting` direto deixa o aperto de mão do FML sem a trava que só
  as entradas do próprio FML criam, e o login morre com "Tempo esgotado". O harness chama
  `FMLClientHandler.connectToRealmsServer` antes (ele só cria a trava).
* **O ponto de spawn de um mundo novo** pode matar o jogador (sufocado ou no vazio). O harness renasce sozinho; ponha
  o jogador em modo criativo e num lugar seguro pelo console antes do `go`.
* **Mods em subpastas** (`mods/1.7.10/`) também contam na comparação de versões.
