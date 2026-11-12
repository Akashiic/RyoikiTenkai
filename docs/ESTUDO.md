# Estudo: Expansão de Domínio e Choque de Domínios (referência: Jujutsu Shenanigans)

Estudo de referência para um recurso de **Expansão de Domínio** no servidor 1.7.10 (com o Kunftig), a partir do
jogo de Roblox **Jujutsu Shenanigans** (JJS). Cobre os dois domínios pedidos, o do Gojo (**Infinite Void**, do
personagem *Honored One*) e o do Sukuna (**Malevolent Shrine**, do *Vessel* desperto como *King of Curses*), e o
**choque de domínios** (dois jogadores expandindo ao mesmo tempo). No fim, a tradução para o nosso ambiente.

> Escrito em 2026-11-12. Os números do JJS vêm da wiki de fãs do jogo (via RBLXGUIDE, que a leu em 24/10/2026) e
> mudam a cada atualização. O que só foi visto em vídeo está marcado como tal; o que tem fontes em conflito está em
> **Divergências**.

## 0. Fontes e confiabilidade

| Fonte | O que deu | Confiança |
|---|---|---|
| RBLXGUIDE: lista de domínios, guia de domínios, páginas do Vessel e do Honored One (dados da wiki do JJS em 24/10/2026) | Regras gerais, números de cada domínio, regras do choque | Boa (secundária, com divergências apontadas pelo próprio site) |
| Roonby (02/04/2026, atualização do choque) | Choque só com M1, barra que enche por acerto, mais de 2 domínios | Média |
| Critical Hits (22/08/2025) | Invasão de domínio, formas de entrar | Média |
| Vídeos analisados com o vidIQ (4) | Sequência visual, tempos, HUD | Boa para o visual, fraca para números |
| DevForum do Roblox | Como o efeito costuma ser montado no Roblox | Média |
| Wiki do JJS (Fandom) | Bloqueada (HTTP 402) para leitura automática | — |

Vídeos analisados:

* *Every Domain Expansion in Jujutsu Shenanigans (2026)*, Tirepin, 0:00 a 1:13 (Infinite Void, 0.2 Domain, Malevolent Shrine).
* *DOMAIN CLASH IN JJS!*, JustVision (o vídeo de referência).
* *Domain clash in #jujutsushenanigans*, Markeyroblox, 28/04/2026 (choque de 5 domínios).
* *This Domain Clash in Jujutsu Shenanigans made me feel like Sukuna!*, ilenvy, 25/02/2026.
* *Domain clash in Jujutsu Shenanigans (real)*, cesarsalad, 15/04/2024 (versão antiga do choque).

## 1. Regras gerais dos domínios no JJS

### Conjuração

* **Preparo de 1,25 s** com o sinal de mão (1,5 s em dois domínios de outros personagens). Ser acertado ou atordoado no
  preparo **cancela** o domínio. Alguns domínios dão imunidade total no preparo.
* **Proibido conjurar:** dentro de outro domínio, perto da borda de um, ou no ar.
* **Alcance:** quem está no raio no momento do preparo recebe o aviso e entra. O raio é "em geral" **37,5 studs**.
* **Recarga:** 120 s nos dois domínios estudados. Ambos são o golpe 4 do moveset desperto, e o despertar dura 60 s.

### Espaço

* **Arena separada.** Cada modelo de domínio fica num ponto fixo **longe do mapa**, um a uma distância fixa do outro.
  Quem está dentro é **teleportado** para lá, mantendo a posição relativa ao conjurador. A cidade não é alterada.
* **Domo destrutível** com borda **branca** (Infinite Void e um outro) ou **preta** (os demais). No choque, a borda é
  sempre preta.

### Entrada, saída e quebra

* **Entrar:** normalmente, só quem é pego no preparo. Existem golpes e truques para forçar a entrada (Critical Hits).
* **Quebrar:** por dentro, não dá. É preciso matar o conjurador ou usar um contra específico. Por fora, golpes que
  destroem terreno ferem a borda, com um estrondo audível.
* **Efeito garantido:** a maioria aplica o efeito a todos os que estão dentro durante todo o tempo ativo.
* **Antidomínio:** quatro habilidades de outros personagens anulam ou furam domínios. Domínios sem borda de verdade são
  imunes a elas.

### Áudio

* Voz em japonês dizendo "Domain Expansion" (*Ryōiki Tenkai*) e depois a trilha do domínio até ele quebrar. No choque,
  o tema de choque substitui tudo.

### Sequência visual comum (vídeos)

1. Faixa diagonal estilo mangá atravessa a tela, com a arte do personagem e "DOMAIN" / "EXPANSION".
2. Um **decalque branco serrilhado de 8 pontas** aparece no chão, sob o conjurador.
3. Flash ou corte de transição, e o interior aparece.
4. No fim, o domínio se **estilhaça em cacos poligonais** (brancos no Gojo, pretos no Sukuna) e a cidade volta.

## 2. Infinite Void (Gojo / Honored One)

### Números

| Item | Valor |
|---|---|
| Acesso | Golpe 4 do despertar *Six Eyes* (G; tira a venda, cura 25 HP, 60 s) |
| Preparo | ~1,25 s |
| Duração | **14 s** |
| Recarga | 120 s |
| Borda | Branca |
| Efeito garantido | Todos menos o conjurador ficam **sem agir por 14 s** ("sobrecarga de informação"). **Não interrompe** o que o alvo já estava fazendo: um golpe em andamento termina |
| Tags | Imbloqueável, acerta quem está caído, interrompível no preparo |
| Choque | Pode chocar |

### Visual (vídeo Tirepin, 0:00 a 0:20)

| Tempo | O que acontece |
|---|---|
| 0:00 a 0:02 | Faixa diagonal com Gojo fazendo o sinal (indicador e médio cruzados), "DOMAIN" à esquerda e "EXPANSION" à direita. O personagem ergue o braço direito com o sinal, e o decalque de 8 pontas aparece no chão |
| 0:02 | Flash para um **vazio branco** com neblina |
| 0:03 a 0:05 | **Túnel de hiperespaço:** linhas de velocidade rosa e magenta saindo do centro, fundo roxo profundo, personagem flutuando |
| 0:06 a 0:17 | **Vazio estável:** céu preto ou azul-escuro com estrelas fracas e poeira cósmica. Atrás e acima do conjurador, uma **esfera preta enorme com halo dourado** (disco de acreção / íris), de 3 a 4 vezes a altura do personagem. Faixas de névoa cinza e branca rente ao meio do quadro, faíscas brancas flutuando devagar na frente. Câmera média e estável |
| 0:18 a 0:20 | Estilhaço em **cacos de vidro brancos** saindo do centro, e a rua volta |

Descrição da wiki: "vazio azul com estrelas e galáxias".

### Variante 0.2 Domain

Despertar alternativo: o especial no meio da sequência do despertar.

* Gojo tira a venda e expande o vazio por **0,2 s**. O efeito garantido tem o **dobro do alcance**, mas metade do tempo.
* Depois vem um avanço em 3 fases (35 + 120 + 65 = 220 de dano). Segurar a cabeça de dois alvos no fim dispara uma cena
  que mata os dois.
* **Não tem borda, não choca** e é imune aos antidomínios.
* Visual: balões de fala de mangá, tudo fica preto com a voz, flash branco com os braços abertos e volta imediata ao
  mapa com um avanço borrado.

## 3. Malevolent Shrine (Sukuna / Vessel em King of Curses)

### Números

| Item | Valor |
|---|---|
| Acesso | Golpe 4 do despertar *King of Curses* (G; cura 45 HP, 60 s, não interrompível). No despertar, o M1 vira corte de 24 studs |
| Preparo | ~1,25 s |
| Duração | **18 s** |
| Recarga | 120 s |
| Borda | Preta |
| Efeito garantido | Cortes constantes em todos os que estão dentro: **2 HP por corte**, **0,5 HP** bloqueando (bloqueio vale em 360°) |
| Total | **218 HP** em 18 s (109 cortes, ~12 HP/s). Bloqueando: 54,5 HP. **Quem bloqueia não morre** enquanto não soltar o bloqueio |
| Choque | Pode chocar |

### Visual (vídeo Tirepin, 0:47 a 1:12; vídeo ilenvy)

| Tempo | O que acontece |
|---|---|
| 0:47 a 0:50 | Faixa diagonal **carmesim e preta** com o rosto do Sukuna dentro do santuário e "DOMAIN EXPANSION". Sinal de mão *Enmaten* (palmas juntas, dedos entrelaçados no peito), **aura vermelho-sangue pulsando** no corpo e o decalque de 8 pontas no chão |
| 0:50 a 0:51 | Flash em **silhueta invertida preto e branco** do personagem e de um telhado de pagode |
| 0:52 a 1:09 | Céu **preto puro** e chão **carmesim escuro e refletivo, com ondulações** saindo do centro (água de sangue). Atrás do conjurador, um **portal torii vermelho com uma boca humana gigante e realista** (dentes à mostra). Árvores estilizadas carmesim e pretas nas laterais. Névoa e fumaça vermelhas. **Cortes:** fitas de luz brancas e compridas com rastro vermelho, cruzando o centro em vários ângulos diagonais, sem parar. **Tremor de tela forte** a cada ciclo de cortes |
| 1:10 a 1:12 | O santuário some e **cacos de vidro pretos** se espalham. A rua volta, com a aura vermelha ainda no personagem |

Descrição da wiki: "santuário atrás do usuário, num vazio preto com fumaça vermelha".

### Variante Incomplete Shrine

Domínio de outro personagem (*Strongest Of History*, o Sukuna da era Heian).

* **Sem borda**, alcance **infinito**, 20 s, 3 HP por corte.
* Santuário maior, com **9 olhos e uma boca gigante**.
* **Quebra na hora todos os outros domínios no alcance**, sem choque.

### Nota sobre o original

No mangá, o Malevolent Shrine **não tem barreira**: é um domínio aberto que corta tudo num raio. O JJS deu a ele uma
borda preta para caber no sistema de domos e de choque, e guardou o "sem barreira" para o Incomplete Shrine. É um bom
precedente para nós: **a mecânica pode simplificar o original** sem perder a leitura do efeito.

## 4. Choque de Domínios

### Regras atuais (wiki, outubro de 2026)

| Regra | Valor |
|---|---|
| Gatilho | Dois ou mais jogadores conjuram **ao mesmo tempo ou numa janela curta** (a wiki não diz o tamanho da janela) |
| Participantes | Dois ou mais. O vídeo de abril de 2026 mostra **5** |
| Durante o choque | Efeitos garantidos **desligados**, domínios **fundidos**, dreno do despertar pausado, **habilidades desligadas**. M1 e esquiva trocados por versões passivas: **o M1 não atordoa nem derruba** |
| Progresso | Cada participante tem uma barra. **+6,25 % por acerto no conjurador rival** (16 acertos, ou 4 sequências de M1). Quem não conjurou não soma |
| Tempo | **15 s** |
| Vitória | Quem encher a barra primeiro, ou quem tiver mais porcentagem quando o tempo acabar. O vencedor fica com o domínio e os outros são cancelados |
| Empate total | **Todos os domínios quebram** |
| Exceções | O 0.2 Domain (e outro domínio sem borda) não choca. O Incomplete Shrine quebra os outros sem choque |
| Visual | Borda sempre preta. O tema de choque substitui as trilhas |

### Visual (vídeo de referência JustVision; Markeyroblox 2026)

| Fase | Tempo aproximado | O que aparece |
|---|---|---|
| Ativação | 0 a 2 s | Vozes em japonês sobrepostas. **Painéis de mangá divididos**, um por participante, com "DOMAIN EXPANSION". Com mais de 2, a tela vira um mosaico com efeito de **falha (glitch)** duplicando painéis e texto |
| Transição | ~1 s | Tela preta com linhas, ou **respingo de tinta branca** sobre o preto, e uma esfera cósmica que "puxa" os participantes |
| Choque | **15 s** | **Os cenários dos domínios piscam e se alternam um sobre o outro** (sem parede dividindo), com troca rápida de céu e estruturas. Feixes de luz nas cores de cada domínio (ciano e branco contra vermelho) cruzam a arena, com aberração cromática e distorção. **Contador grande e branco com contorno preto, centralizado embaixo, de 15 a 0.** Barra de habilidades escondida, vida e nomes visíveis. Todos se movem e trocam M1 |
| Resolução | ~1,5 s | **Flash branco total** (ou vermelho, quando o Sukuna vence) com **respingos e estilhaços de tinta preta**, tremor de tela, e o domínio perdedor se estilhaça |
| Domínio vencedor | resto | O interior do vencedor sozinho. A câmera sobe num contra-plongée até o rosto do vencedor. O efeito garantido começa |

### Histórico de versões

* **Abril de 2024** (vídeo cesarsalad): já existia choque, mas **sem barra e sem contador**. Os dois brigam no vazio e o
  vencedor sai da luta.
* **Abril de 2026** (Roonby e vídeos de 2026): choque como **minijogo**, com contador de 15 s, barra por acerto, só M1 e
  sem derrubar.

### Divergências

* **Como a barra enche.** A wiki e o Roonby dizem que é **por acerto no rival**. Um vídeo (ilenvy, fevereiro de 2026)
  mostra uma **barra de cabo de guerra** (amarelo de um lado, vermelho do outro) com **teclas que mudam (A, D)** para
  apertar. Pode ser uma versão anterior a abril de 2026 ou outro modo; **a verificar**. Para nós, as duas ideias servem
  e dá para escolher (ver seção 5.4).
* **A coluna "Time" da wiki** bate com a duração em 8 de 10 domínios, mas não é definida. Os 14 s e 18 s aqui batem com
  as páginas dos personagens.
* **O raio** "37,5 studs" vale "em geral". O vídeo de referência sugeriu entre 25 e 40 studs.

## 5. Tradução para o nosso servidor (Minecraft 1.7.10, com o Kunftig)

### 5.1 Escala

* Um avatar do Roblox tem cerca de **5 studs** de altura e o jogador do Minecraft tem **1,8 bloco**.
* Pela altura do personagem, 37,5 studs equivalem a 7,5 alturas, ou seja, **cerca de 13 blocos de raio**.
* Sugestão: **raio de 12 a 16 blocos, configurável por domínio**.
* Tempos e números (preparo de 25 ticks, 14 s, 18 s, 15 s de choque, 16 acertos) podem ser usados como estão e
  ajustados depois.

### 5.2 Arena separada ou no próprio lugar

O JJS teleporta para uma arena longe do mapa. Para nós, as opções são:

| Opção | Prós | Contras |
|---|---|---|
| **A. No próprio lugar, desenhado no cliente** (domo-exibição + céu, neblina e cor trocados para quem está dentro, estruturas como exibições) | Sem teleporte, sem carregar chunks e sem mexer no mundo. O servidor só guarda o estado | O terreno de dentro continua lá (pode ser bom). Precisa da API 14 para céu, neblina e cor |
| **B. Teleporte para uma região-arena no mesmo mundo**, com chunks mantidos carregados | Arena feita à mão, controle total do chão e dos props. É o que o JJS faz | A troca de chunks pode mostrar o mundo sem renderizar por alguns quadros (o DevForum relata o mesmo problema no Roblox). Exige devolver todos com segurança |
| **C. Teleporte para outro mundo** (um mundo só de domínios) | Céu e iluminação do mundo inteiro já são do domínio | No 1.7.10, a troca de dimensão mostra a tela de "Downloading terrain", o que quebra o efeito. Não recomendado |

**Recomendação: A**, com o chão "trocado" por uma exibição plana (o espelho de sangue do Sukuna, o vazio do Gojo)
quando o domínio pedir. Deixar **B** como evolução, se quisermos arenas feitas à mão.

> O [estudo técnico](ESTUDO-TECNICO.md) confirma que o JJS usa a opção **B**: um domo destrutível no mapa e a arena
> longe, com teleporte. Ele também lista os problemas que isso trouxe (seção 6) e o que isso ensina para nós (seção 8).

### 5.3 Cada elemento visto e onde ele mora

| Elemento | Kunftig hoje (API 13) | Precisa da API 14 |
|---|---|---|
| Faixa diagonal de mangá e "DOMAIN EXPANSION" | `KunftigHud`, imagem e texto, mas parado | Elementos de HUD **com movimento** (entrar em diagonal, tremer, glitch), painéis por participante |
| Decalque de 8 pontas no chão | Exibição plana (`KunftigDisplays`) com estado de "surgir" | — |
| Aura no conjurador | `KunftigParticles` (cores, partículas modernas) | — |
| Flash de transição / estilhaço / tinta | — | **Flash de tela inteira** com textura e cor, e cacos na tela |
| Domo e borda (branca ou preta) | Exibição em esfera, faces para dentro, escala até 64 | — |
| Céu do vazio, estrelas, preto puro | — | **Céu por jogador** (`IRenderHandler`) |
| Névoa vermelha, névoa cinza | Partículas (aproximação) | **Neblina** (`FogColors` / `FogDensity`) |
| Esfera preta com halo dourado, torii com boca, árvores | Exibições GeckoLib, com partes que brilham (`luz`) | — |
| Túnel de hiperespaço (Gojo) | Exibição de cilindro animado | — |
| Chão de sangue ondulando | Exibição plana animada (textura que ondula) | — |
| Cortes brancos e vermelhos cruzando o espaço | **`KunftigEffects.slash`** (arco que gira e some em 10 ticks) | — |
| Tremor de tela | — | **Tremor de câmera** |
| Cenários piscando no choque, aberração cromática | Trocar estados e variantes das exibições rapidamente | **Pós-processamento** (`ShaderGroup`) e alternância de céu |
| Contador de 15 a 0, barras do choque | `KunftigHud` (texto e barra) | — |
| Câmera subindo até o rosto do vencedor | — | **Câmera cinematográfica** (entidade de câmera em `renderViewEntity`) |
| Voz e trilha (a trilha de choque sobrepondo tudo) | Sons do resource pack, tocados pelo servidor | (opcional) parar a música do cliente durante o domínio |
| Congelar quem está no Gojo | Servidor: lentidão e cancelamento de ações. `KunftigInput` desligado para o alvo | — |
| Cortes do Sukuna (dano por tick, bloqueio reduz) | Servidor, dano a cada ~3 ticks, bloqueio pela espada levantada no 1.7.10 | — |

### 5.4 Choque: proposta de regras para nós

Usamos as regras atuais do JJS como base, adaptadas ao combate do 1.7.10:

* **Gatilho:** um segundo domínio conjurado **durante o preparo do primeiro** (janela de 25 ticks) e com os
  conjuradores **dentro do raio um do outro**. Fora disso vale a regra "não se conjura dentro de um domínio".
* **Durante o choque:** efeitos garantidos e habilidades desligados (o servidor já controla habilidades e o Overkill tem
  os ganchos). Só ataque básico, **sem empurrão**.
* **Progresso:** **+6,25 % por acerto no conjurador rival** (16 acertos). Opcional: a variante de **cabo de guerra**, com
  a tecla Especial do `KunftigInput` apertada várias vezes, que já tem limite de 20 por segundo e validação no
  servidor. Dá para deixar as duas e escolher por configuração.
* **15 s.** Vence quem encher a barra ou quem tiver mais no fim. **Empate quebra todos.** Mais de 2 participantes
  funciona do mesmo jeito.
* **Visual:**
  * borda preta;
  * alternância entre os cenários dos participantes a cada 2 a 4 ticks, com tremor;
  * contador grande no centro de baixo;
  * flash na cor do vencedor ao resolver, o perdedor se estilhaçando, e o efeito garantido do vencedor começando.

## 6. A verificar

* Como a barra do choque enche na versão atual do JJS (acerto ou cabo de guerra com teclas).
* O tamanho exato da janela de choque no JJS.
* O raio real no jogo (a wiki diz "em geral 37,5 studs").
* A compatibilidade do céu próprio e do pós-processamento com a **Angelica** e os shaderpacks do TEOF. É o primeiro
  teste técnico a fazer antes da API 14.
* O custo de várias exibições grandes ao mesmo tempo (domo, estruturas, chão) com GeckoLib, num choque de 3 ou mais
  domínios.

## Links

* RBLXGUIDE, lista de domínios: https://rblxguide.com/games/jujutsu-shenanigans/domain-expansions
* RBLXGUIDE, guia de domínios: https://rblxguide.com/games/jujutsu-shenanigans/updates/domain-expansion-guide
* RBLXGUIDE, Vessel: https://rblxguide.com/games/jujutsu-shenanigans/characters/vessel
* RBLXGUIDE, Honored One: https://rblxguide.com/games/jujutsu-shenanigans/characters/honored-one
* Roonby, atualização do choque: https://roonby.com/2026/04/02/domain-clashing-ryu-guide-everything-latest-jujutsu-shenanigans-update/
* Critical Hits, invasão de domínio: https://criticalhits.com.br/dicas/roblox-jujutsu-shenanigans-como-funciona-a-invasao-de-dominio/
* Sportskeeda, Vessel: https://sportskeeda.com/roblox-news/jujutsu-shenanigans-a-beginner-s-guide-vessel
* DevForum, domínio tipo Infinite Void: https://devforum.roblox.com/t/how-to-make-a-domain-like-infinite-void-in-jujutsu-kaisen/2768513
* DevForum, efeito de domínio: https://devforum.roblox.com/t/how-to-make-this-domain-expansion-effect/1719648
* Vídeos: https://www.youtube.com/watch?v=SGez4ZSv5ys · https://www.youtube.com/shorts/ClrUcOQ3EFM ·
  https://www.youtube.com/shorts/JY0v37Vd70w · https://www.youtube.com/shorts/LOjigtLhaXw ·
  https://www.youtube.com/shorts/UNR7zG7EHGM
