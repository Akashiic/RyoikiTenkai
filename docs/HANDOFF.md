# Handoff: para o agente que assume o RyoikiTenkai

Olá. Você está assumindo um projeto que até agora só tem **pesquisa e discussão**: nenhuma linha de código foi escrita.
Este documento junta tudo o que foi estudado, discutido e decidido com o dono, para você não começar do zero.

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
2. [`ESTUDO.md`](ESTUDO.md): regras e números do JJS, o visual cena a cena (com tempos), o choque, a escala para
   Minecraft (cerca de 13 blocos de raio) e a tabela de cada elemento contra o que o Kunftig já tem ou precisa.
3. [`ESTUDO-TECNICO.md`](ESTUDO-TECNICO.md): como o JJS provavelmente faz por dentro, com cada afirmação marcada como
   fato, dedução ou prática comum:
   * domo destrutível no mapa e arena longe, com teleporte;
   * vagas de arena;
   * destruição em voxels;
   * rede cliente-servidor;
   * os bugs que o JJS teve e o que eles ensinam.
4. O guia da API do Kunftig: `docs/API.md` no repositório [Akashiic/Kunftig](https://github.com/Akashiic/Kunftig) (e o
   `CLAUDE.md` de lá).

## 3. Decisões do dono

| Tema | Decisão |
|---|---|
| Monetização | Nenhuma. Pode usar nomes, falas e o visual de Jujutsu Kaisen e do JJS como referência livremente |
| Plugin Bukkit | O dono disse que **não quer plugin**. O resto da arquitetura é com você |
| O domo quebra por fora? | **Não.** Nenhum golpe quebra o domo |
| Onde o domínio pode ser usado | **Akashic/Spawn**, como o dono escreveu. Confirme com ele se é um mundo só (`Akashic/Spawn`) ou dois (`Akashic` e `Spawn`). Cada mundo permitido precisa da própria região de arenas, se você seguir o caminho das arenas |
| Domínios ao mesmo tempo | **No máximo 2.** Decida e documente se um choque (2 domínios numa arena só) ocupa uma vaga ou duas |

## 4. O plano que saiu da discussão (proposta, pode mudar)

### 4.1 O problema central: instantâneo, sem tela de carregamento

* No 1.7.10, o **"Downloading terrain" só aparece quando o jogador troca de dimensão** (ou entra no servidor). Um
  teleporte dentro do **mesmo mundo** nunca mostra essa tela.
* O que aparece num teleporte longo dentro do mesmo mundo é um **vazio por alguns instantes**: o servidor manda os
  chunks do lugar novo em lotes pequenos por tick, e o cliente ainda precisa montar a malha 3D.
* O dono quer uma **dinâmica instantânea**.

### 4.2 A proposta: como o JJS (domo no mapa e arena longe), mas entregando os chunks antes

1. **Domo só para quem ficou de fora.** Uma exibição do Kunftig (esfera GeckoLib: preta no Sukuna, branca no Gojo) no
   ponto da conjuração. Não quebra (decisão do dono).
2. **Arena no mesmo mundo, longe do mapa.**
   * Uma região reservada (por exemplo, x = 100.000), uma vaga por domínio.
   * Construída uma vez e **cercada de chunks vazios**, para o resto do raio de visão carregar quase de graça.
   * Chunks mantidos **sempre carregados** no servidor (tickets do ForgeChunkManager, pelo lado servidor do Kunftig).
3. **Pré-envio durante o preparo.** O preparo dura 1,25 s (25 ticks). Nesse tempo o servidor manda os chunks da arena
   para os clientes que vão ser pegos.
   * O cliente 1.7.10 guarda chunks recebidos mesmo longe dele, então no teleporte a arena já está na memória.
   * Uma arena de raio ~16 ocupa de 9 a 16 chunks: poucas dezenas de KB por jogador.
4. **Volta instantânea.**
   * No teleporte, o servidor manda o cliente descartar os chunks da cidade (no 1.7.10, o descarte é um
     `S21PacketChunkData` com bitmask 0 e `groundUp`).
   * **Cliente:** um Mixin ignora esses descartes para os chunks em volta do domo enquanto o domínio durar.
   * **Servidor:** no fim, reenvia esses chunks antes do teleporte de volta.
5. **O flash esconde a remontagem.** Os poucos quadros que o cliente leva para montar a malha 3D ficam escondidos pela
   sequência faixa de mangá → flash → arena. O JJS faz o mesmo: o bug "domínio cegando a tela" (versão 1.61) era o
   flash que esconde o teleporte.

### 4.3 Caminhos considerados e descartados (reavalie se quiser)

| Caminho | Por que foi descartado |
|---|---|
| **Outra dimensão** | A tela de "Downloading terrain" é inevitável. Dá para escondê-la com Mixin, mas o cliente recria o `WorldClient` e descarta todos os chunks: vários quadros pretos |
| **Domínio no próprio lugar**, desenhado só no cliente por cima do terreno | Instantâneo, mas o terreno real continua lá (árvores, casas, desníveis). Esconder blocos só no cliente faz o servidor discordar da colisão, e o jogador é puxado de volta ("moved wrongly") |
| **Teleporte simples sem pré-envio** | Mostra de meio segundo a alguns segundos de vazio. Bom para um primeiro teste, não para o resultado final |

### 4.4 O que o Kunftig precisaria ganhar (API 14, sugestão)

* **Chunks:** pré-envio para jogadores escolhidos, retenção de chunks no cliente durante o domínio, tickets para as
  arenas.
* **Tela e ambiente:**
  * flash de tela inteira com textura e cor;
  * céu por jogador (`IRenderHandler`);
  * neblina (`FogColors` / `FogDensity`);
  * tremor de câmera;
  * pós-processamento (`ShaderGroup`);
  * HUD com movimento (faixas de mangá entrando em diagonal);
  * câmera cinematográfica (entidade de câmera em `renderViewEntity`).
* O que a **API 13 já tem** e serve para os domínios:
  * exibições GeckoLib com escala, animação e movimento (domo, estruturas, torii, esfera com halo);
  * HUD com texto, barra e imagem (contador e barras do choque);
  * teclas próprias com limite e validação (`KunftigInput`);
  * cortes estilo SlashBlade (`KunftigEffects.slash`);
  * partículas coloridas e modernas;
  * trava de voo e `pushVelocity`.

## 5. Fatos do ambiente que você vai precisar

### Servidor e cliente

* Minecraft 1.7.10, Forge 10.13.4.1614, UniMixins.
* Servidor híbrido Thermos/Crucible. O **Crucible de produção roda Java 21**.
* O modpack é o TEOF, com **Angelica** (renderização otimizada: teste cedo tudo que mexe em renderização) e GeckoLib
  Unofficial 1.0.4.

### Kunftig ([Akashiic/Kunftig](https://github.com/Akashiic/Kunftig))

* É o mod próprio da rede, no cliente e no servidor.
* Gradle no padrão GTNH (RetroFuturaGradle). Bytecode Java 8 (Jabel) no `src/`. Spotless obrigatório.
* API atual: **13** (`KunftigApi.API_VERSION`), commit `7d4c7ff` da `main` em 2026-11-12.
* Cada canal novo é registrado pelo cliente só se ele sabe desenhar. Os plugins e o servidor sabem quem precisa de
  alternativa (`drawsDisplays`...).

### Builds nesta nuvem

* O **Maven Central devolveu HTTP 429** (limite de requisições) num build do Kunftig. O repositório da Spigot
  (`hub.spigotmc.org/nexus`) respondeu normal.
* O `:api` do Kunftig foi compilado direto com `javac --release 8` contra a `spigot-api` 1.8.8 quando o Gradle falhou.

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

## 6. Riscos para atacar cedo

1. **O cliente desenha imediatamente os chunks recebidos antes do teleporte?** Pelo código do vanilla deveria, mas a
   Angelica muda a renderização. Faça esse teste antes de construir em cima.
2. **Teleportes concorrentes:** homes, /tpa e golpes durante o domínio precisam ser bloqueados (o JJS teve esse bug com
   o Switcher).
3. **Sair pelas paredes da arena:** o JJS desligou o parkour dentro dos domínios (1.75).
4. **Custo:** várias exibições grandes ao mesmo tempo num choque (domo, estruturas, chão).
5. **Voz:** veja a seção 5.

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
* Marque o que não foi visto no jogo como "a verificar".
* Sempre confira os repositórios do Akashiic (`list_repos`) antes de supor qualquer coisa sobre o ambiente.
* Mudou o plano? Atualize este arquivo para o próximo agente.
