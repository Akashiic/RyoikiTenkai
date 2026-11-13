# Handoff: para o agente que assume o RyoikiTenkai

Olá. Você está assumindo um projeto que já tem o **esqueleto do mod e o teleporte para a arena funcionando e medido**
(seção 0). O visual ainda é um rascunho. Este documento junta o que foi estudado, discutido, decidido e construído, para
você não começar do zero.

## 0. Estado em 2026-11-12

O que existe no código (um mod Forge 1.7.10 para os dois lados, `com.akashic.ryoiki`):

| Peça | Onde | Estado |
|---|---|---|
| Arena longe do mapa, feita de um bloco invisível (`ryoikitenkai:veu`), 4 chunks por arena, terreno em volta esvaziado | `arena/`, `block/BlockVeil` | Feito, testado no harness |
| Sessão de domínio: sinal de mão (25 ticks), captura por raio, teleporte com a posição relativa ao conjurador, duração, volta ao ponto de origem | `domain/DomainManager` | Feito, testado no harness com 1 jogador |
| Pré-envio dos chunks da arena no sinal de mão; o cliente guarda os chunks da cidade durante o domínio | `domain/DomainManager#cast`, `net/ChunkKeep`, `mixin/` | Feito, medido (ver [`TRANSPORTE.md`](TRANSPORTE.md)) |
| Segurança: logout, parada e queda do servidor, outro teleporte no meio, teleporte bloqueado por plugin | `domain/DomainManager` | Feito, **testado no Crucible** |
| Segurança: morte e pérola do ender dentro do domínio | `domain/DomainManager` | Feito, **a verificar** no jogo |
| **Visual dos dois domínios**, desenhado por código: faixa de entrada com o rosto da skin, selo, domo, céu com reflexo, astrolábio, portais, lanternas, lua, névoa, cortes, tremor, saída em estilhaços | `client/`, `fx/` | Feito, visto no cliente TEOF 5.4 ([`VISUAL.md`](VISUAL.md)) |
| `/ryoiki expandir <vazio\|santuario> [jogador]`, `/ryoiki encerrar`, `/ryoiki preparar` (operadores) | `command/` | Feito |
| Harness: o cliente conjura sozinho, grava cada quadro dos teleportes e tira capturas | `dev/TransportHarness` | Feito (`./gradlew runClient -Pharness`, com `-Pangelica` para rodar com a Angelica). Também entra num servidor de verdade e obedece a um arquivo de comandos: [`tools/teste-real/`](../tools/teste-real/LEIA-ME.md) |

Depende de uma versão do **3D Futura Engine com a API 6** (`setBonePose`, `setScreenDraw`), que está no branch
`ccr-21bf04a1-vn1lyk` do repositório YesSteveModel e ainda não foi mesclada: sem ela o mod funciona, mas o gesto usa a
skin comum.

O que **não** existe ainda: sons, efeitos garantidos (congelar no Vazio, dano dos cortes no Santuário), o sinal de mão
no modelo do jogador, choque de domínios, tecla ou item para conjurar, recarga.

Em 2026-11-12 isso tudo **rodou no servidor Crucible da rede com o modpack TEOF 5.4** e com dois jogadores: ida e
volta, parede, `/spawn`, expulsão, parada e queda do servidor no meio do domínio. O resultado e o que o servidor real
obrigou a mudar estão no [`TRANSPORTE.md`](TRANSPORTE.md#no-servidor-real). O que continua **a verificar** é o que
depende de placa de vídeo e de plugins que o mundo de teste não tinha.

## Antes de tudo: você tem carta branca

O dono pediu, com estas palavras, que o próximo agente tenha **carta branca**, use **imaginação e criatividade** e tenha
**permissão total para mudar o plano**: decidir um caminho melhor, um jeito melhor, e julgar a melhor forma de
implementar as coisas.

Leve isso a sério:

* **O plano da seção 4 é uma proposta, não uma ordem.** Se você enxergar um caminho mais simples, mais bonito, mais
  rápido ou mais robusto, troque. Explique ao dono por que trocou e siga.
* **Questione os estudos.** Eles foram feitos com pesquisa de fora (o código do Jujutsu Shenanigans é fechado) e têm
  pontos "a verificar". Se algo ali estiver errado ou não fizer sentido no Minecraft, corrija.
* **Invente.** O objetivo não é copiar o JJS, é fazer o momento mais marcante possível no 1.7.10. Se uma ideia que
  não está em lugar nenhum deixar a expansão de domínio mais impressionante, ela é bem-vinda.
* **Escolha onde o código mora** (no Kunftig, num mod novo aqui, nos dois...), a arquitetura, as ferramentas e a ordem
  das etapas.

Os únicos limites são as **decisões do dono** (seção 3), que são preferências dele sobre o resultado, e as regras de
repositório no fim. Se achar que uma decisão do dono está atrapalhando, converse com ele: não ignore.

## 1. O que é o projeto

**Expansão de Domínio** e **Choque de Domínios**, inspirados em Jujutsu Kaisen e no jogo de Roblox **Jujutsu
Shenanigans** (JJS), para o servidor Minecraft **1.7.10** da Rede Akashic.

* É uma mecânica **sobretudo visual**: quase inútil no combate, mas boa para o servidor (clipes, eventos,
  engajamento).
* **Não será monetizado.**
* Os domínios de referência são o **Infinite Void** (Gojo) e o **Malevolent Shrine** (Sukuna), e o **choque**: dois
  jogadores expandindo ao mesmo tempo, os cenários piscando um sobre o outro, um contador de 15 s, vence quem acertar
  mais o conjurador rival.
* Vídeo de referência que o dono mandou: https://www.youtube.com/shorts/ClrUcOQ3EFM

## 2. Leia nesta ordem

1. **Este arquivo.**
2. [`TRANSPORTE.md`](TRANSPORTE.md) e [`VISUAL.md`](VISUAL.md): como a ida e a volta funcionam, o que se vê, e o que foi
   medido e testado.
3. [`ESTUDO.md`](ESTUDO.md): regras e números do JJS, o visual cena a cena (com tempos), o choque, a escala para
   Minecraft (cerca de 13 blocos de raio) e a tabela de cada elemento contra o que o Kunftig já tem ou precisa.
4. [`ESTUDO-TECNICO.md`](ESTUDO-TECNICO.md): como o JJS provavelmente faz por dentro, com cada afirmação marcada como
   fato, dedução ou prática comum:
   * domo destrutível no mapa e arena longe, com teleporte;
   * vagas de arena;
   * destruição em voxels;
   * rede cliente-servidor;
   * os bugs que o JJS teve e o que eles ensinam.
5. O guia da API do Kunftig: `docs/API.md` no repositório [Akashiic/Kunftig](https://github.com/Akashiic/Kunftig) (e o
   `CLAUDE.md` de lá).

## 3. Decisões do dono

| Tema | Decisão |
|---|---|
| Monetização | Nenhuma. Pode usar nomes, falas e o visual de Jujutsu Kaisen e do JJS como referência livremente |
| Plugin Bukkit | O dono disse que **não quer plugin**. O resto da arquitetura é com você |
| O domo quebra por fora? | **Não.** Nenhum golpe quebra o domo |
| Onde o domínio pode ser usado | Por enquanto **em qualquer mundo**: em 2026-11-12 o dono disse "pode testar sem restrições, depois a gente pensa em restringir". A intenção original era **Akashic/Spawn**, que é **um mundo só** (um mundo do Multiverse com esse nome, vazio, do `AkashicVoidGenerator`). Cada dimensão ganha a própria região de arenas na primeira conjuração |
| O visual | Em 2026-11-12 o dono pediu para "reproduzir o visual igual ao do Jujutsu Shenanigans", do Gojo e do Sukuna, e disse para **não restringir mundos por agora**. O agente não copia a arte, os personagens nem as cenas de um jogo ou anime existentes: fez um visual próprio com a mesma estrutura (**Vazio Estelar** e **Santuário Carmesim**) e avisou o dono. Se ele quiser outro rumo, a conversa é com ele |
| Raio | **30 blocos**, pedido em 2026-11-12 (era 13, da escala do estudo) |
| Santuário e GIFs | O dono pediu o santuário da referência "100%" (com as bocas na porta), no meio da arena, e duas cenas do anime na faixa. Feito: o santuário **no meio, quadrado, menor e sólido**, de desenho próprio; e um **espaço na faixa** para quadros que o próprio modpack fornece (`config/ryoikitenkai/faixa/`). Não feito, por limite do agente: copiar o desenho da referência e embutir as cenas |
| Quem testa | O dono prefere que **o agente faça os testes**: em 2026-11-12 ele mandou o modpack do cliente e os arquivos do servidor para isso (seção 5) |
| Plugin ou mod | **Mod**, confirmado pelo dono em 2026-11-12 ("pode ficar à vontade" para a divisão proposta) |
| Modo Feiticeiro | Ideia do dono em 2026-11-13: **Ctrl+R** troca entre o Minecraft normal e um modo em que o inventário some, a luta fica ligada e o rodapé mostra **4 habilidades, a última a Expansão de Domínio**; voltar devolve o inventário "salvo mesmo se tiver sido morto". Ele mandou começar pela peça M1 (o modo em si). Plano em `PLANO-COMBATE.md`, fase M |
| Rodapé do modo | Em 2026-11-13 o dono pediu "uma barra de vida com textura bonita" para o modo e **remover a fome e a barra de experiência**. Feito na peça M2: no modo o jogador conta como alimentado (a fome dele volta ao sair) |
| Habilidades do modo | As seis propostas pelo agente (3 por técnica, em `PLANO-COMBATE.md`) foram **aprovadas** pelo dono em 2026-11-13, que mandou fazer a 1 e, junto, **melhorar a animação de andar, correr e pular no modo** |
| Proteção e permissão do modo | Em 2026-11-13: "o modo feiticeiro deve ignorar o world guard e sistemas de proteção, só vou liberar ele para quem tiver permissão node". Feito: node `ryoikitenkai.modo` para entrar, e as habilidades aplicam vida e movimento direto, sem o dano do jogo |
| Habilidade 1 do Vazio | Em 2026-11-13 o dono pediu que fosse o **orbe azul controlável pela câmera, com dois modos** (mostrou um GIF do jogo de referência). Feito como "Atração": no chão e no ar. O nome e o desenho no mod são próprios; o texto visível está no arquivo de idioma |
| Habilidades 1 a 3 do Vazio | Em 2026-11-13 o dono indicou a página do personagem na wiki do jogo de referência (`jujutsu-shenanigans.fandom.com/wiki/Honored_One`) como o guia: a 1 é o orbe azul controlável (feita), a 2 "é o vermelho" (feita como "Repulsão"), a 3 é a seguinte da mesma lista (a massa roxa). O texto da wiki se lê pela API (`api.php?action=parse&page=Gojo&prop=wikitext`); a página comum devolve 402 |
| Gestos das habilidades | Em 2026-11-13 o dono recusou a primeira Repulsão ("faltando vermelho", grande demais, "feio e pobre a habilidade saindo da barriga") e mandou **ler o YSM do repositório e o modelo que anexou** para o gesto e a posição do corpo serem fiéis. O modelo anexado (CC BY-NC-SA, não vai para o repositório) é o mesmo do cliente de teste. Regra daqui em diante: toda habilidade sai da mão do modelo (`FuturaBridge#hand`) e tem gesto de corpo inteiro conferido no visor de quatro vistas |
| Fase 2 da Repulsão | Pedido do dono em 2026-11-13: segurando R o orbe volta, e quem for atingido na volta leva o golpe negro, "igual tá na wiki". Feito |
| Nível dos efeitos | Em 2026-11-13 o dono recusou o primeiro golpe negro ("PARE DE FAZER TRABALHO PREGUIÇOSO! faça o VFX igual ou superior ao do gif que você viu"). Regra: quando há um GIF de referência, **rever quadro a quadro antes de desenhar** e reproduzir a sequência inteira (quadros de impacto, tela, mundo), com desenho próprio; conferir cada momento com o relógio do efeito parado (`kokusen`, `kscrub` no harness) |
| Pose do modelo | Em 2026-11-13 o dono mandou **sobrescrever a pose passiva do modelo** nas nossas animações (viu a mão na cintura num feitiço) e fazer a animação de corpo do golpe negro. Regra: toda pose nossa é de corpo inteiro, mãos e dedos incluídos, com peso total; conferir no visor de quatro vistas que nada do modelo aparece por baixo |
| Parado no modo | Em 2026-11-13: a animação passiva de ficar parado continua sendo a do próprio modelo; as nossas só entram ao correr, pular, lutar ou conjurar |
| O roxo | Pedido em 2026-11-13: quem morre no azul deixa a esfera parada 5 s; o vermelho nela vira o roxo, fatal para todos no raio, **sem grife** (nada de quebrar bloco). Feito |
| Recarga do domínio | **60 s**, dito pelo dono em 2026-11-13 ao aprovar a peça M3 (a tecla 4 do modo conjura). Config `dominio.recarga` |
| Domínios ao mesmo tempo | **No máximo 2.** Um choque (2 domínios numa arena só) ocupa **uma vaga**: o domínio do rival não ganha arena própria |

## 4. O caminho escolhido (e o que mudou do plano original)

O detalhe técnico e as medições estão em [`TRANSPORTE.md`](TRANSPORTE.md). Em resumo:

### 4.1 O que ficou do plano original

* Domo no mapa para quem ficou de fora, arena longe no mesmo mundo, com teleporte (como o JJS).
* Os chunks da arena vão para o cliente durante o sinal de mão; o cliente guarda os chunks da cidade (um Mixin ignora
  os descartes) e os tem na memória na volta.
* Um flash cobre o corte.

### 4.2 O que mudou, e por quê

| Mudança | Motivo |
|---|---|
| **A arena é feita de um bloco invisível** e todo o visual é desenhado pelo cliente por cima | Sem blocos visíveis, os chunks da arena não têm malha para montar: o risco nº 1 (a Angelica desenhar a arena com atraso) deixa de existir na ida. De quebra, o visual não fica preso a blocos: chão que ondula, céu próprio, tudo é desenho |
| **O terreno em volta das arenas é apagado** (uma vez, em segundo plano) | Sem isso, cada teleporte faria o servidor carregar e mandar centenas de chunks de terreno que ninguém vê |
| **O piso da arena emite luz** | Quem está dentro fica iluminado igual de dia e de noite |
| **O código começou todo neste repositório**, sem depender do Kunftig | O lado servidor do Kunftig só existe como API para plugins Bukkit (`com.akashic.kunftig.bukkit`), e o dono não quer plugin. Um mod sozinho também roda inteiro no ambiente de desenvolvimento (servidor integrado), o que permitiu o teste automático. O que se provar genérico (flash, tremor, céu, pré-envio de chunks) pode subir para o Kunftig depois |
| **Canal próprio** (`ryoiki:dom`, mensagens cruas tratadas na thread principal do cliente, como no Kunftig) | A ordem com os pacotes do jogo é garantida: o "você entrou" chega sempre antes do teleporte |
| **A volta é para o ponto exato de onde cada um saiu** | A posição dentro da arena não tem equivalente seguro no terreno real (paredes, desníveis) |

### 4.3 Caminhos considerados e descartados

| Caminho | Por que foi descartado |
|---|---|
| **Outra dimensão** | A tela de "Downloading terrain" é inevitável. Dá para escondê-la com Mixin, mas o cliente recria o `WorldClient` e descarta todos os chunks: vários quadros pretos |
| **Domínio no próprio lugar**, desenhado só no cliente por cima do terreno | Instantâneo, mas o terreno real continua lá (árvores, casas, desníveis). Esconder blocos só no cliente faz o servidor discordar da colisão |
| **Arena no céu, em cima do lugar da conjuração** | Não troca chunk nenhum, mas exige pôr e tirar milhares de blocos no mundo real a cada conjuração (com religamento de luz), esbarra no limite de altura e deixa lixo no mundo se o servidor cair. Fica como plano B se o teleporte longe der problema no servidor real |
| **Teleporte simples sem pré-envio** | O jogador fica parado até o chunk chegar. Com o pré-envio ele tem chão no primeiro quadro |

### 4.3b Choque de Domínios (como ficou)

Regras da seção 5.4 do `ESTUDO.md`, decididas pelo agente (carta branca) em 2026-11-13:

* **Gatilho:** conjurar **dentro do sinal de mão de outro domínio** (no raio dele) ou **até `choque.janela` ticks
  depois de ser pego** (60 por padrão, no config). Quem responde é o **rival**; o sinal de mão dele dura os mesmos 25
  ticks, com faixa e voz, e continua dentro da arena se o domínio abrir no meio. Um domínio choca uma vez, com um
  rival só (dois participantes).
* **Durante (15 s):** o tempo do domínio fica parado. Nada fere nem empurra os dois conjuradores por fora do
  combo. Cada clique é um golpe de uma **cadeia de quatro** (`ComboChain`: 6 ticks entre golpes, clique adiantado
  guardado, 14 ticks de recuperação depois do quarto). Um golpe acerta se o rival está a até 3,6 blocos e à frente,
  e vale **+6,25 %** (16 acertos = 4 cadeias). Cada golpe dá um passo à frente e o quarto arremessa o rival. O
  servidor vê o golpe pelo balanço do braço (sem mixin e sem pacote do cliente).
* **Fim:** barra cheia vence na hora; no fim do tempo vence a barra maior; **barras iguais quebram tudo** (todos
  voltam). Se um dos dois morre, sai ou desconecta, o outro vence.
* **Depois:** o vencedor fica com a arena e o domínio **dele recomeça do zero** (14 s ou 18 s); o perdedor fica
  dentro, como qualquer um que foi pego.
* **Visual: fusão, não alternância** (pedido do dono em 2026-11-13). A primeira versão, uma linha reta, ele
  recusou como "pobre e medíocre": o choque tem de ter os dois domínios se misturando e se repelindo, sem linha, e
  sem pressa. Refeito como uma frente viva (`fx/Frontier`, `client/Scene`, `client/ClashFront`, ver `VISUAL.md`).
  **O dono espera este nível de acabamento em tudo o que é visual.**
* **O salão no choque** (2026-11-13): o salão do Santuário é sólido até o pé da escada e é erguido na conjuração.
  Se o Santuário entra como rival, o salão dele é erguido no começo do choque, onde é desenhado
  (`ShrineCore#rival`, `ArenaManager#setHall`), e quem estiver ali é posto diante da porta; fica se ele vence e
  sai se perde. O salão do anfitrião sai se ele perde. `ArenaManager#setCore` varre o interior inteiro da arena,
  então um salão deixado por um servidor que caiu no meio de um choque some na conjuração seguinte.
* **Código:** `domain/ClashScore` (regras puras, com teste), `domain/ComboChain` (idem), `fx/BlowPose` (o corpo em cada golpe: só a guarda e o golpe 1 estão
  trabalhados, mais a reação a levar golpe; o resto é provisório. O dono quer **um kit por domínio, com golpes de
  perna**, e animações de **levar golpe e cair**: a ordem das peças e a proposta dos kits estão em `VISUAL.md`,
  "Os golpes do choque"), `DomainManager` (`clashHost`, `answer`,
  `beginClash`, `tickClash`, `blows`, `resolve`), `DomainWire` (`RIVAL`, `CLASH`, `CLASH_SCORE`, `BLOW`, `CLASH_END`),
  `ClientDomain#shown` e `client/ClashHud`.

**Dois defeitos antigos achados no teste com dois jogadores** (valem para qualquer domínio, não só o choque):

* O pacote dos chunks da arena era **um objeto só para todos** os jogadores próximos; escrito em duas conexões ao
  mesmo tempo, chegou corrompido a um cliente ("Bad compressed data format") e o derrubou. Agora cada jogador recebe
  o seu.
* Com dois jogadores teleportados no mesmo tick, o rastreador de entidades do servidor às vezes deixava um **sem ver
  o outro**. Agora, 5 ticks depois de cada ida e de cada volta, cada um é tirado e recolocado entre os que os outros
  veem (`DomainManager#tickResyncs`).

**O plano do combate e dos efeitos do choque está em [`PLANO-COMBATE.md`](PLANO-COMBATE.md)**: fases A (regras,
feita), B (efeitos de assinatura de cada domínio), C (golpes, um por vez) e D (acabamento). Siga a ordem de lá.

### 4.4 Próximos passos sugeridos

1. ~~Ver no servidor real~~ **Feito** (ver `TRANSPORTE.md`). Sobra o que precisa de placa de vídeo: a volta à cidade
   num PC de verdade.
2. ~~O visual de verdade~~ **Feito** numa primeira versão ([`VISUAL.md`](VISUAL.md)). Falta ver com placa de vídeo e
   com shaderpack, e ouvir o dono sobre o resultado.
3. **Efeitos garantidos**: ~~congelar quem está no Vazio~~ **feito** em 2026-11-13 (ver "Os dois modos do Vazio" em
   `VISUAL.md`); falta o dano dos cortes no Santuário (2 por corte, bloqueio reduz).
4. ~~Choque de domínios~~ **Feito** numa primeira versão (seção "Choque de Domínios" abaixo e em `VISUAL.md`). Falta
   ver com jogadores de verdade e com alguém assistindo de fora.
5. **Como se conjura**: pela **tecla 4 do modo Feiticeiro**, com 60 s de recarga (peças M1 a M3 do
   `PLANO-COMBATE.md`, feitas em 2026-11-13). Falta: permissões, mundos permitidos, e como o jogador ganha a técnica
   (por enquanto `/ryoiki tecnica`). O Overkill (katanas) continua sendo um sistema à parte: no modo a mão está vazia.
6. **Som**: o mod não traz som (decisão do dono: nada sintetizado, nenhum áudio no jar). Trilha de cada expansão,
   voz no sinal de mão, trilha do choque (`clash_trilha`) e sons por momento são `.ogg` do dono em
   `config/ryoikitenkai/sons/`; a música do jogo é calada enquanto uma trilha toca (ver "Som" em `docs/VISUAL.md` e
   `tools/sons/`). A trilha do choque é ligada pelo próprio choque. Falta: ouvir num PC de verdade.
7. **Voz** (AkashicVoiceChat): decidir se o domínio isola ou se vale uma ponte (seção 5).

### 4.5 O que ainda pode ir para o Kunftig

A lista original da "API 14" continua valendo como destino do que for genérico: flash de tela, tremor de câmera, céu e
neblina por jogador, HUD com movimento, câmera cinematográfica, pré-envio e retenção de chunks. Só não é mais
pré-requisito.

## 5. Fatos do ambiente que você vai precisar

### Servidor e cliente

* Minecraft 1.7.10, Forge 10.13.4.1614, UniMixins.
* Servidor híbrido Thermos/Crucible. O **Crucible de produção roda Java 21**.
* O modpack é o TEOF, com **Angelica** (renderização otimizada: teste cedo tudo que mexe em renderização) e GeckoLib
  Unofficial 1.0.4.

### O servidor e o modpack de verdade (vistos em 2026-11-12)

O dono mandou os dois pacotes. Não estão em repositório nenhum: se precisar deles de novo, peça os links.

* **Cliente:** `TEOF-5.4.zip`, nas releases de [Akashiic/Technic-Forbidden](https://github.com/Akashiic/Technic-Forbidden)
  (tag `TEOF-5.4`). 286 mods, Java 8, LWJGL 2. Além da Angelica 2.2.43: **Distant Horizons**, **EndlessIDs** (o bloco
  do mod ganhou o ID 11529), JourneyMap, Waila, CoreTweaks, o Kunftig (`kunftig-b192616`) e o Futura3D.
* **Servidor:** Crucible `1.7.10-staging-67b53034`, `view-distance=10`, 63 plugins no pacote (53 ligados no teste). Os que importam aqui:
  * **WorldBorder:** borda de 25.000 blocos no mundo `world`, e só nele;
  * **Multiverse-Core:** os mundos `Akashic/Spawn` e `Akashic/Evento` (vazios), `plotworld` e as dimensões dos mods;
  * WorldGuard, Essentials, GriefPreventionPlus, PerWorldInventory (troca o modo de jogo ao mudar de mundo), ClearLag;
  * `config/forgeChunkLoading.cfg`: **0 tickets** para mods fora da lista.
* O pacote do servidor estava mais velho que o do cliente (14 mods em versões diferentes) e veio sem os mundos. Como
  alinhar e ligar os dois aqui está em [`tools/teste-real/LEIA-ME.md`](../tools/teste-real/LEIA-ME.md).
* **AkashicBellow** (repositório BelowCore) guarda blocos abaixo de Y 0 em cada chunk. Não atrapalhou o envio dos
  chunks da arena.

### Kunftig ([Akashiic/Kunftig](https://github.com/Akashiic/Kunftig))

* É o mod próprio da rede, no cliente e no servidor.
* Gradle no padrão GTNH (RetroFuturaGradle). Bytecode Java 8 (Jabel) no `src/`. Spotless obrigatório.
* API atual: **13** (`KunftigApi.API_VERSION`), commit `7d4c7ff` da `main` em 2026-11-12.
* O lado servidor do Kunftig é a implementação da `kunftig-api` para **plugins Bukkit** (pacote
  `com.akashic.kunftig.bukkit`, instalado só em híbridos). Não há ainda uma entrada para outro mod Forge usar as
  exibições, o HUD ou as teclas pelo servidor.
* Cada canal novo é registrado pelo cliente só se ele sabe desenhar. Os plugins e o servidor sabem quem precisa de
  alternativa (`drawsDisplays`...).

### Builds nesta nuvem

* O **Maven Central devolve HTTP 429** (limite de requisições) quando o Gradle baixa em paralelo. Rodar com
  `--max-workers=1 -Dorg.gradle.parallel=false` e repetir até as dependências ficarem em cache resolve.
* O cliente roda sem tela: `xvfb-run -a -s "-screen 0 1280x720x24 +extension RANDR"` com `LIBGL_ALWAYS_SOFTWARE=1`; o
  LWJGL 2 precisa do `xrandr` (`apt-get update && apt-get install -y x11-xserver-utils`).

### Voz (AkashicVoiceChat, conferido em 2026-11-12, commit `6a7ecad`)

* [Akashiic/AkashicVoiceChat](https://github.com/Akashiic/AkashicVoiceChat) é **voz espacial**.
* O servidor 1.7.10 é dono das posições e faz o **roteamento por proximidade**. O raio padrão é de **48 blocos**
  (`voiceRadius`, de 1 a 512).
* **Consequência:** com a arena longe, quem foi pego conversa entre si, mas some da voz de quem ficou perto do domo, e
  vice-versa. O JJS teve exatamente isso e corrigiu à mão (versões 1.37 e 1.46).
* Decida se isso é aceitável (o domínio "isola", o que até combina) ou se vale uma ponte. Por exemplo, o roteamento da
  voz pode tratar quem está na arena como se estivesse no centro do domo. O projeto de voz tem regras próprias de
  evidência e de homologação (veja o `CLAUDE.md` e o `AGENTS.md` de lá) antes de mexer nele.

### Outros repositórios da rede que servem de referência

* **AkashicFishing:** consumidor principal do `kunftig-api`, com o mesmo arranjo `core` + `plugin` + `libs/`.
* **Overkill:** combate, usa as teclas, o HUD e os efeitos do Kunftig.
* **Expeditions:** navios. Exibições em que se anda por cima (API 12) e ossos girados pelo servidor (API 13).

## 6. Riscos

1. ~~O cliente desenha imediatamente os chunks recebidos antes do teleporte?~~ **Resolvido** de outro jeito: a arena
   não tem o que desenhar. No harness, com e sem Angelica 2.2.45, o jogador tem chão no quadro 0 (ver `TRANSPORTE.md`).
2. **A volta com placa de vídeo de verdade.** O cliente guarda os dados da cidade, mas precisa remontar as malhas. No
   harness (renderização por software) a cidade já aparece inteira por baixo do flash; num PC real, **a verificar**.
3. **Teleportes concorrentes:** quem sai da arena por outro teleporte é tirado do domínio (e a pérola do ender é
   bloqueada). Testado no Crucible com o `/spawn` do Essentials; `/home` e `/tpa` seguem o mesmo caminho.
4. **Plugins que cancelam ou desviam teleportes.** Aconteceu de verdade: o WorldBorder do mundo `world` desviou o
   teleporte para a arena. O mod agora confere a chegada e cancela o domínio devolvendo todos (testado). Num mundo com
   borda, as arenas precisam ficar dentro dela.
5. **Sair pelas paredes da arena:** a casca é de blocos sólidos e inquebráveis, fechada inclusive nas diagonais (teste
   unitário). Golpes de outros mods que atravessam blocos, **a verificar**.
6. **Custo** de várias exibições grandes num choque.
7. **Voz:** veja a seção 5.
8. **CubicChunks:** a rede tem os repositórios `CubicChunks1710` e `Akashic-CubicChunks-CrucibleCompat` (este, vazio em
   2026-11-12). Se o servidor passar a usar CubicChunks, o formato dos pacotes de chunk muda e o pré-envio e a
   retenção precisam ser revistos.

## 7. Ideias soltas (use, descarte ou supere)

* Os chunks da arena podem ser mandados em **ordem do centro para fora**, com prioridade para o que o jogador vê
  primeiro.
* A faixa de mangá do JJS usa o **rosto do avatar** do jogador (1.27). No Minecraft, o rosto da skin do jogador, tirado
  da textura da skin.
* O choque pode alternar a arena entre os dois cenários trocando **variantes e estados das exibições** e o céu, sem
  mover ninguém.
* O Malevolent Shrine faz 109 cortes em 18 s (~1 a cada 0,165 s). Os cortes visuais podem ser gerados no cliente com
  uma semente que o servidor manda, para todo mundo ver os mesmos cortes sem tráfego.

## 8. Regras de repositório

* Responda ao dono em **português do Brasil**.
* Trabalhe em branches e abra PRs. **Não use force-push.** Um PR mesclado não é reaproveitado.
* `./gradlew spotlessApply build` precisa passar. Bytecode Java 8 (Jabel): sintaxe moderna pode, tipos e APIs do
  Java 9+ não.
* Lógica pura (formas, vagas, protocolo) fica sem classes do Minecraft e com teste unitário.
* O harness (`com.akashic.ryoiki.dev`) nunca vai no jar: só entra com `-Pharness`.
* Marque o que não foi visto no jogo como "a verificar".
* Sempre confira os repositórios do Akashiic (`list_repos`) antes de supor qualquer coisa sobre o ambiente.
* Mudou o plano? Atualize este arquivo para o próximo agente.
