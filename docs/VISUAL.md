# Visual dos domínios

O que o jogador vê, do sinal de mão ao fim do domínio. Tudo é **desenhado por código** no cliente (`client/`): não há
modelo, textura nem som no mod (os sons são arquivos do dono, ver "Som"). A arena é feita de blocos invisíveis, então o cliente desenha o domínio inteiro.

> **Sobre a referência.** O dono pediu o visual "igual ao do Jujutsu Shenanigans". O agente não reproduz a arte, os
> personagens nem as cenas de um jogo ou de um anime existentes (inclusive o santuário de bocas e chifres da
> referência que ele mandou); o que foi feito é um **visual próprio** com a mesma estrutura dramática (entrada com
> gesto, domo, interior com um santuário central, cortes, estilhaço). Os dois domínios se chamam, na tela, **Vazio
> Estelar** e **Santuário Carmesim** (os nomes estão em `assets/ryoikitenkai/lang/`, é só trocar).

![Vazio Estelar](img/visual-vazio.png)

![Santuário Carmesim](img/visual-santuario.png)

## A sequência

| Momento | O que aparece | Onde |
|---|---|---|
| Sinal de mão (25 ticks) | Uma **faixa inclinada** cruza a tela de quem está perto (o conjurador e quem está a até raio + 12 blocos). Nela, o **boneco do conjurador com a skin dele faz o gesto**: ergue os braços do repouso até o sinal do domínio (uma mão diante do rosto no vazio, as duas erguidas e unidas diante da boca, espelhadas, no santuário), com um clarão no instante em que o sinal se completa. Ao lado, o nome do domínio e o do jogador, e riscos de velocidade. Entra em 5 ticks e sai nos 4 últimos | `CutIn` |
| | **A câmera sobre o boneco** (`CutIn#shot`): nos dois domínios, **três planos com cortes secos** aos 6 e aos 12 ticks, com um clarão a cada corte. O boneco está no lugar desde o primeiro quadro e a faixa entra por trás dele. **Vazio:** 1) de costas e de baixo, o corpo inteiro, o braço começando a subir; 2) close na mão que sobe, de lado e inclinado; 3) close frontal do rosto com a mão diante dele, com um tranco quando o sinal se completa. Fundo: braços de luz girando e estrelas em riscos. **Santuário:** 1) de cima e de lado, a cabeça baixa; 2) do chão, olhando para cima, as mãos se unindo em primeiro plano; 3) de frente e **reto** (sem giro nem inclinação lateral), com a câmera acima olhando para baixo: os olhos por cima das mãos unidas. Fundo: uma lua, anéis sobre água vermelha, cortes de luz e brasas. A câmera mira a cabeça ou a mão **do próprio modelo** (medidas no mesmo quadro, numa passada que não desenha nada), e é ela que gira, não o modelo. Com a skin comum (sem o Futura3D) os ângulos são os mesmos, mas a mira é sempre a cabeça | `CutIn` |
| | Um **selo** no chão sob o conjurador (anéis e doze raios que giram) e uma onda que corre até onde o domo vai ficar | `DomainRenderer#seal` |
| | O **domo** cresce do conjurador, devagar e depois de uma vez | `DomainRenderer` |
| Entrada | Flash branco (cobre o teleporte) | `DomainRenderer#onOverlay` |
| Interior | Céu, reflexo, chão e o que há na arena, conforme o domínio (abaixo) | `DomainSky`, `DomainLook` |
| Para quem ficou de fora | O domo (branco ou preto) com uma linha de luz onde encosta no chão | `DomainRenderer` |
| Saída | A tela fica inteira na cor do domínio, **racha** e os **estilhaços** voam, com a cidade atrás. Um tranco na câmera | `Shatter`, `fx/ShardField` |

## Vazio Estelar (`VoidLook`)

* **Céu:** espaço profundo, 1.100 estrelas que piscam (algumas com brilho em cruz), cinco nebulosas.
* **Astrolábio:** ao norte, um núcleo escuro num aro branco com quatro anéis girando, cada um no seu plano. Abre nos
  primeiros 45 ticks.
* **Chão:** vidro negro. O céu inteiro é desenhado de novo de cabeça para baixo, mais fraco, e o chão é translúcido
  por cima: o reflexo é de verdade. Anéis de luz correm do centro; uma linha marca a borda do domínio.
* **No ar:** 110 pontos de luz subindo devagar.

## Santuário Carmesim (`ShrineLook`)

* **Céu:** preto, com o horizonte em brasa e uma **lua de sangue** baixa (refletida na água).
* **Chão:** água vermelha, com ondas que saem do centro.
* **O santuário:** no **meio da arena**, atrás do conjurador (que cai 13 blocos à frente da porta, de costas para
  ela). Um salão quadrado de 10 blocos de lado sobre uma base em degraus, com a porta acesa por dentro, três telhados
  de beirais virados para cima e uma agulha com três anéis de luz; uma corda grossa com tiras de papel na frente,
  um estandarte de cada lado e lanternas de pedra. A lua fica atrás dele.
  * **É sólido.** No sinal de mão o servidor ergue o salão em blocos invisíveis (`arena/ShrineCore`, paredes e teto,
    girado para onde o conjurador olhava) e o desfaz quando o domínio acaba. Ninguém cai dentro dele.
  * É um desenho próprio. O dono pediu o santuário da referência (bocas na porta, chifres, caveiras) "100%": isso o
    agente não reproduz.
* **Em volta:** doze **portais** em anel, fora da parede, cada um com duas lanternas que tremulam;
  **torres** negras no horizonte; névoa vermelha rasteira em faixas; brasas subindo.
* **Cortes:** a cada 4 ticks um risco branco cruza a arena num instante e deixa um rastro vermelho que some
  (`fx/SlashField`). A posição de cada corte sai do número do domínio e do tempo, então **todos os clientes veem os
  mesmos cortes sem nenhuma mensagem**. Cada corte dá um tranco na câmera. Por enquanto é só visual: não causa dano.

## Choque de Domínios (`ClashHud`, `ClientDomain#shown`)

![Choque](img/choque.png)

* **Sinal do rival:** quem responde a um domínio ganha a própria faixa e a própria voz, por cima do que estiver na
  tela (o sinal dele pode terminar já dentro da arena).
* **Durante:** os dois cenários **se alternam a cada 10 ticks** (meio segundo; mais rápido que isso vira um
  pisca-pisca de tela inteira) (céu, chão, estruturas e névoa), com um tremor a cada
  troca. Numa arena que não abriu como Santuário, o Santuário é desenhado recuado, com o meio da arena diante da
  porta. Para quem está fora, **o domo fica preto**.
* **Na tela, no topo:** "CHOQUE DE DOMÍNIOS", o **contador** grande de 15 a 0 (vermelho e pulsando nos últimos 5 s) e,
  dos lados, a **barra de cada conjurador** com nome e porcentagem, na cor do domínio dele, enchendo em direção ao
  meio. Fica no topo porque embaixo estão a vida, a armadura e a barra de itens.
* **Resolução:** clarão na cor do vencedor e tremor; o domínio do vencedor recomeça. No empate, a quebra de sempre.

### Os quatro golpes (`ComboChain`, `ClientDomain.Blow`)

![Os quatro golpes](img/golpes.png)

No choque, cada clique dos dois conjuradores é um golpe de uma **cadeia de quatro**: direto de direita, cruzado de
esquerda, gancho subindo e, para fechar, um chute frontal. Em cima, de lado; embaixo, por trás.

* **Ritmo:** um golpe a cada 6 ticks; um clique até 4 ticks adiantado fica guardado e sai na hora certa; 18 ticks
  sem golpear e a cadeia volta ao primeiro; depois do quarto, 14 ticks de recuperação.
* **Acerto:** não precisa mirar no boneco. Vale o rival a até 3,6 blocos e dentro de 60° da direção do olhar (ou
  colado). Cada acerto enche 6,25 % da barra.
* **Movimento:** cada golpe dá um passo à frente (maior no quarto), e o quarto **arremessa** o rival para trás.
* **Animação:** o modelo do Futura3D faz a pose de cada golpe (2 ticks para bater, segura, solta), e a pose de um
  golpe se mistura na do seguinte, sem voltar ao repouso entre eles. Os quatro golpes posam os mesmos ossos
  (`UpBody`, braços, antebraços, pernas). Com a skin comum só há o balanço de braço do jogo.
* **Impacto:** um clarão no peito de quem leva, na cor do domínio de quem bate (maior no quarto), o tranco de dano e
  um tremor de câmera para os dois.
* **Som (opcional):** `golpe.ogg` e `impacto.ogg` em `config/ryoikitenkai/sons/`.

Verificado com dois clientes no servidor de teste: rival vencendo por tempo e por barra cheia, anfitrião vencendo por
barra cheia, empate, resposta durante o sinal de mão e depois de o domínio abrir. Os quatro golpes foram vistos com o modelo de teste. **A verificar:** a sensação do combo com jogadores de verdade (ritmo,
alcance, força do arremesso), as poses em outros modelos, a alternância de
cenários vista ao vivo (as capturas pegam um quadro só), quem assiste de fora perto do domo, mais de um jogador pego
além dos dois conjuradores, e o choque em sobrevivência com armas e habilidades de outros mods.

## Som (`DomainSounds`, `SoundCues`, `OwnerSounds`)

**O mod não traz som nenhum** (decisão do dono: só a trilha dele). Cada som é um `<nome>.ogg` em
`config/ryoikitenkai/sons/` do cliente; `OwnerSounds` é um resource pack sobre essa pasta que também escreve o
`sounds.json`, com um evento para cada arquivo presente. O que não tem arquivo não toca e não gera aviso no log.

* **A trilha da expansão:** `vazio_trilha` e `santuario_trilha` tocam uma vez, em estéreo, do sinal de mão ao fim da
  quebra (16,45 s e 20,45 s), para o conjurador e quem vê a faixa. Some em meio segundo se o domínio acabar antes ou
  se o jogador não for pego.
* **A voz:** `vazio_voz` e `santuario_voz` tocam uma vez, em estéreo, no primeiro tick do sinal de mão, por cima da
  trilha, para os mesmos jogadores.
* **O choque:** `clash_trilha`. Quando o choque começa (`DomainWire.CLASH`), a trilha do domínio some em 0,5 s e a
  do choque começa, para quem está no domínio e quem está a até raio + 12 blocos do domo; no fim do choque
  (`CLASH_END`, o fim do domínio, ou o jogador de fora se afastar) ela some em 0,5 s.
* **A música do jogo** (`MusicHush`): enquanto uma trilha do dono toca, a música que estava tocando é parada e
  nenhuma começa (categoria "música"). Depois o jogo volta a tocar no tempo dele, uma faixa nova.
* **Por momento, opcionais:** `sinal` (no conjurador, mono), `faixa_corte`, `<domínio>_entrada` (mono; do domo para
  quem está fora), `<domínio>_ambiente` (laço), `santuario_corte`, `saida`, `domo` (laço, mono).

Os nomes, os tempos e o conversor (`tools/sons/converter.py`) estão em
[`tools/sons/LEIA-ME.md`](../tools/sons/LEIA-ME.md).

Verificado no cliente de teste, com o OpenAL gravando num arquivo e um tom puro por som: sem arquivos, o mod não toca
nada e o log fica limpo; com arquivos, voz e trilha começam juntas no sinal de mão, os fades do choque duram 0,5 s na
entrada e na saída, e a música do jogo para quando a trilha começa. **A verificar:** ouvir num PC de verdade, com dois
jogadores (quem está fora, perto do domo, num choque) e no modpack completo.

## A arte do dono na faixa

A faixa tem um espaço, à direita, para uma imagem ou uma animação curta de cada domínio. **O mod não traz nenhuma**:
quem monta o modpack põe o arquivo na pasta de configuração **do cliente**. Há dois jeitos:

```
config/ryoikitenkai/faixa/vazio.gif              um GIF animado, tocado na velocidade dele
config/ryoikitenkai/faixa/santuario.gif

config/ryoikitenkai/faixa/vazio/00.png, 01.png...    ou uma pasta de quadros PNG, na ordem dos nomes
config/ryoikitenkai/faixa/vazio/fps.txt              (opcional; um número, 12 se faltar)
```

Toca em laço enquanto a faixa está na tela (até 96 quadros, até 1.024 pixels de lado). Se houver o GIF e a pasta, vale
o GIF. Sem nenhum dos dois, a faixa fica como antes. O mod lê na primeira vez que a faixa aparece, então trocar o
arquivo pede reabrir o jogo. Só GIF e PNG: WebP não é lido.

O que vai ali é escolha e responsabilidade de quem monta o modpack. O dono pediu duas cenas do anime e insistiu; o
agente não as embutiu no mod nem as usou nas amostras (as capturas mostram uma animação de teste).

## Como é feito

* **O céu entra no lugar do céu do jogo.** Enquanto o jogador está no domínio, o cliente troca o `IRenderHandler` de
  céu do mundo pelo do domínio, e os de nuvem e de chuva por um que não desenha nada (`DomainSky`); ao sair, devolve
  o que havia. O céu é desenhado em volta da câmera, antes de tudo, então fica atrás de tudo e a qualquer distância.
  Com distância de renderização menor que 4 chunks o jogo não desenha céu: aí o mod desenha no fim do quadro, só onde
  não há nada na frente.
* **A neblina** do jogo passa a ter a cor do domínio (`EntityViewRenderEvent.FogColors`).
* **O tremor** vira a entidade da câmera uns graus só durante o desenho de cada quadro e desfaz logo depois
  (`CameraShake`). Nada vai para o servidor.
* **A cena fica atrás do conjurador.** A mensagem de entrada leva para onde o conjurador olhava quando o domínio
  abriu; céu, chão e estruturas são girados por esse ângulo, igual para todos lá dentro.
* **O gesto usa o modelo do jogador.** Onde o 3D Futura Engine desenha os jogadores (o mod de modelos da rede, no
  repositório YesSteveModel), o conjurador faz o sinal com o **próprio modelo**: `FuturaBridge` chama, por reflexão,
  `FuturaModels.setBonePose` (API 6 do Futura3D) a cada quadro, com a rotação de cada osso como num arquivo de
  animação. Isso vale **na faixa** (o jogador é desenhado ali como no inventário, da cintura para cima, enquadrado
  pela cabeça do modelo) **e no mundo** (durante o sinal e nos primeiros 34 ticks do domínio).
  * Ossos posados: `RightArm`, `RightForeArm`, `LeftArm`, `LeftForeArm`, `Head` (nomes padrão dos modelos) e os dedos
    `right_fg`, `right_ffg`, `left_fg`, `left_ffg`, `left_dmz` (os quatro dedos como um bloco em duas juntas, e o
    polegar). Um modelo que não tem um osso simplesmente não o move.
  * Os ângulos foram acertados num modelo de teste que o dono mandou (uma personagem de quimono, CC BY-NC-SA, **fora
    do repositório**) e conferidos no modelo padrão do modpack. Em outros modelos o gesto pode sair diferente.
  * **Dedos um a um não existem** nesses modelos: os quatro dedos são um osso só. O sinal é feito com os braços, as
    mãos e esse bloco.
* **Sem o Futura3D** (ou com um mais antigo que a API 6), a faixa mostra um `ModelBiped` do próprio jogo com a skin
  comum do conjurador, com os braços posados à mão.

![O gesto no modelo do jogador](img/visual-gesto-modelo.png)

* **Um domínio novo** é uma classe que implementa `DomainLook` (céu, chão, o que há na arena e as cores da faixa e
  dos estilhaços) e uma entrada em `DomainType`.

## Verificado e a verificar

* **Verificado** em 2026-11-12: no cliente de desenvolvimento com a Angelica 2.2.45, e no cliente **TEOF 5.4**
  completo conectado ao Crucible da rede (as últimas capturas de cada imagem acima). Sempre com renderização por
  software.
* **A verificar:**
  * com placa de vídeo e com um **shaderpack** ligado (os shaders da Angelica mudam o céu e podem ignorar o céu do
    mod);
  * o **custo em quadros por segundo** num PC modesto (são alguns milhares de triângulos por quadro, sem textura);
  * a faixa de entrada em telas muito largas ou com a escala da interface no máximo;
  * skins em alta resolução (HD);
  * o gesto em outros modelos do servidor (cada modelo tem o seu repouso de braços e os seus ossos de dedos);
  * o que os outros jogadores veem (a faixa, o selo, o domo): as capturas são de um jogador só;
  * o minimapa (JourneyMap) continua mostrando as coordenadas da arena.

## O que falta para o visual

* **Som:** ouvir a trilha do dono num PC de verdade e com dois jogadores; o choque de domínios ainda não tem som.
* **Efeitos de jogo:** os cortes do santuário e a paralisia do vazio são só imagem.
* **O sinal de mão no mundo sem o Futura3D:** com a skin comum o jogador fica parado (o gesto só aparece na faixa).
* **Choque de domínios.**
