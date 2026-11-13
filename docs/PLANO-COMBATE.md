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
| B2 | **Vazio: a rajada linear.** Feixes de luz que correm numa direção só, como na abertura do domínio; na abertura e como assinatura dele |
| B3 | **No choque, os dois misturados:** cortes entrando no chão do Vazio e rajadas atravessando o do Santuário, e o que acontece onde um encontra o outro |

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
