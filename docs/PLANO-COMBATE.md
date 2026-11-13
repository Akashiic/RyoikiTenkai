# Plano: o combate e os efeitos do Choque de Domínios

Pedido do dono em 2026-11-13: sem pressa, com imaginação, como um dev sênior faria; mecânica agradável e fluida
junto com animação e efeitos agradáveis; **uma peça por vez**, cada uma medida e mostrada antes da seguinte. Este
documento é a ordem das peças. O estado de cada uma está em [`VISUAL.md`](VISUAL.md) e no
[`HANDOFF.md`](HANDOFF.md).

## Princípios

* **Só os dois combatentes mudam.** As regras de combate valem para os dois conjuradores, e só durante o choque.
  Quem mais estiver no domínio fica como no Minecraft: anda, leva dano e recuo normais.
* **O servidor decide, o cliente sente.** O acerto, o placar e o movimento são do servidor; o cliente trata o clique
  antes do jogo e desenha. Nada que o cliente possa fingir vale ponto.
* **Medir antes de mostrar.** Lógica pura com teste; visual conferido no visor de quatro vistas ou em capturas do
  jogo, de mais de um ângulo; o que não foi visto no jogo fica como "a verificar".
* **Nenhuma cópia.** Os efeitos e os golpes são desenho próprio, com a ideia do choque do Jujutsu Shenanigans.

## Fase A: regras do combate (feita em 2026-11-13)

| | Peça | Estado |
|---|---|---|
| A1 | O clique dos combatentes é só o golpe do nosso sistema: sem ataque, recuo ou tranco de dano do Minecraft | feito |
| A2 | Os dois correm o tempo todo, em qualquer direção | feito |
| A3 | Os combatentes não quebram bloco: clicar é golpear | feito |
| A4 | Os outros jogadores no domínio continuam normais | feito (já era assim; conferido) |

## Fase B: os efeitos de assinatura de cada domínio

| | Peça |
|---|---|
| B1 | **Santuário: o corte.** Feito em 2026-11-13: arcos de lâmina em rajadas cruzadas, com crescente, fio, estilhaços e cicatriz no chão (`VISUAL.md`). O dono decidiu manter o arremesso do quarto golpe |
| B1b | **Santuário: riscos no ar.** Feito em 2026-11-13, a pedido do dono: marcas pretas e curtas de lâmina por toda parte, que aparecem e somem (`VISUAL.md`) |
| B2 | **Vazio: a rajada e o congelamento.** Feito em 2026-11-13. O dono esclareceu os dois modos do Vazio: "iniciando" (a rajada de feixes que irradia do astrolábio, 2,5 s) e "completo" (o Vazio parado, com todos os pegos congelados até o fim). Ver `VISUAL.md` |
| B3 | **No choque, os dois misturados.** Feito em 2026-11-13: cortes e riscos entram no chão do Vazio e os feixes atravessam o do Santuário, cada um rareando por 14 blocos; o feixe chega cortado ao Santuário e a cicatriz do corte esfria no Vazio; a rajada do Vazio não para enquanto ele é contestado (`VISUAL.md`). Filmado dos dois lados, em primeira e em terceira pessoa |

## Fase M: o modo Feiticeiro (ideia do dono em 2026-11-13)

O dono propôs organizar tudo num **modo**: o Minecraft normal de um lado (inventário, blocos, interação) e, com
**Ctrl+R**, o modo Feiticeiro do outro: o inventário some, a luta do nosso sistema fica ligada, e no rodapé entram
**quatro habilidades, a última sendo a Expansão de Domínio**. Ctrl+R de novo volta ao normal, com o inventário
intacto mesmo que a pessoa tenha morrido no modo. As peças vêm antes dos golpes da fase C, porque é o modo que diz
quando um clique é golpe.

| | Peça | Estado |
|---|---|---|
| M1 | **O modo em si:** Ctrl+R, a barra some, a mão fica vazia, nada do inventário pode ser usado, movido, largado ou pego, sem quebrar bloco nem ataque do Minecraft, e morrer no modo não perde nada. O servidor decide | feito em 2026-11-13 (`VISUAL.md`, "Modo Feiticeiro") |
| M2 | **O rodapé:** a barra de vida própria (sem fome nem experiência, pedido do dono) e as quatro casas de habilidade com ícone, tecla (1 a 4) e recarga | feito em 2026-11-13 (`VISUAL.md`, "O rodapé") |
| M3 | **A quarta casa conjura o domínio:** a tecla 4, no modo, expande o domínio da técnica do jogador, com **60 s de recarga** (decisão do dono) contados do sinal de mão. Responder ao domínio de outro pela tecla 4 abre o choque. O comando `/ryoiki expandir` continua, para a staff, sem recarga | feito em 2026-11-13 (`VISUAL.md`, "A tecla 4") |

**Feito também em 2026-11-13, a pedido do dono:** parar, correr e pular no modo, por um modelo da passada
(`VISUAL.md`, "Parar, correr e pular no modo"). Próximo passo possível: deslocamento de osso no Futura3D, para o
quadril subir e descer (aterrissagem com joelhos dobrados).

### As habilidades: aprovadas pelo dono em 2026-11-13

O agente propôs e o dono aprovou as seis. **Depois, em 2026-11-13, o dono mandou seguir a wiki do jogo de
referência para as habilidades 1, 2 e 3 do Vazio** (o orbe azul controlável, o vermelho, e a terceira da mesma
lista), o que mudou as três do Vazio; as do Santuário continuam as da proposta. São desenho próprio, com a ideia de cada técnica. **Feitas: a 1 de cada
técnica (Atração e Talho) e a 2 do Vazio (Repulsão)**, em 2026-11-13 (`VISUAL.md`). Faltam a 3 do Vazio e a 2 e
a 3 do Santuário.

| | Vazio Estelar: domina o espaço | Santuário Carmesim: corta |
|---|---|---|
| 1 | **Atração.** Um orbe que segue a câmera por 3,5 s, prende quem está perto, carrega e arremessa; no chão varre à frente, no ar suspende quem conjura e vai para qualquer lado (refeita em 2026-11-13 pela dinâmica que o dono mostrou). Recarga 12 s | **Talho.** Uma lâmina que não se vê voa em linha reta por até 12 blocos, corta o primeiro que encontrar e deixa a cicatriz no chão. O golpe de longe. Recarga 5 s |
| 2 | **Repulsão.** Feita em 2026-11-13: carrega 1,2 s e solta um orbe vermelho que voa reto por 24 blocos, atravessa e arremessa; dano cai com a distância; no ar, suspende quem conjura e mira para qualquer lado. Recarga 10 s | **Retalhar.** De perto (3 blocos), uma rajada de cortes cruzados no alvo à frente: quanto mais perto, mais cortes acertam (de 3 a 6). O golpe de perto. Recarga 9 s |
| 3 | **A fazer, pela wiki que o dono indicou:** a união do azul e do vermelho, uma massa roxa que se forma por uns 3 s e avança apagando o que estiver no caminho; no ar, suspende e mira para qualquer lado; recarga longa. (Substitui o "Passo do Vazio" da proposta do agente) | **Rede de Cortes.** Marca um quadrado de 5 blocos no chão à frente; 1 s depois a área é retalhada por 2 s. Quem ficar apanha, quem está fora pensa antes de entrar. Para negar terreno. Recarga 14 s |
| 4 | **Expansão de Domínio: Vazio Estelar** | **Expansão de Domínio: Santuário Carmesim** |

O Vazio é controle (puxar, afastar, reposicionar) e o Santuário é agressão (longe, perto, área), para os dois
jogarem diferente. Em 2026-11-13 o dono decidiu que **o modo ignora WorldGuard e qualquer proteção** e que só
entra nele quem tem a **permissão** (`ryoikitenkai.modo`). Continua valendo, por recomendação do agente, que só é
atingido quem também está no modo, e que o dano é pequeno e fixo (no config).

Decisões que o dono ainda não tomou (perguntadas em 2026-11-13): quem pode ser golpeado fora do choque (o agente
recomenda só quem também está no modo, respeitando PvP e proteção de terreno); se os golpes dão dano fora do choque
(recomenda dano pequeno e fixo); como o jogador ganha a técnica (Vazio ou Santuário); quais são as habilidades 1 a 3.
Enquanto isso, **no modo o jogador não bate em ninguém fora de um choque**: o ataque do Minecraft está desligado e
os golpes do nosso sistema só existem no choque.

## Fase C: os golpes, um por vez

Kits diferentes por domínio, com pernas (proposta em `VISUAL.md`; o dono ainda não disse se troca algum golpe).

| | Peça |
|---|---|
| C1 | Vazio, golpe 2: jab de esquerda |
| C2 | Vazio, golpe 3: chute lateral alto |
| C3 | Vazio, golpe 4: chute giratório (finalizador) |
| C4 | Cair no chão e levantar |
| C5 a C8 | Santuário: gancho ao corpo, cruzado de cima para baixo, joelhada, chute de calcanhar que derruba |
| C9 | A reação a cada tipo de golpe (no corpo, de cima, de baixo), além da que existe |

## Fase D: o que faz o combate "encaixar"

| | Peça |
|---|---|
| D1 | Regras de troca: levar golpe interrompe a cadeia? Há defesa ou esquiva? (decidir com o dono) |
| D2 | A pausa do impacto (os dois congelam por 1 ou 2 quadros no acerto) e a câmera no finalizador |
| D3 | Sons por golpe e por domínio (arquivos do dono) |
| D4 | Ver tudo com jogadores de verdade e ajustar ritmo, alcance e força |

## Feito antes deste plano

A guarda e o golpe 1 (direto de direita), a reação a levar um golpe, a cadeia de quatro cliques, a frente viva entre
os dois domínios. Ver `VISUAL.md`.
