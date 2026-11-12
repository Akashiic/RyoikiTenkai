# Estudo técnico: como o Jujutsu Shenanigans faz as expansões de domínio (Roblox / Luau)

Complemento do [`ESTUDO.md`](ESTUDO.md), que cobre o que o jogador vê e as regras. Este documento cobre **como o jogo
provavelmente funciona por dentro**: o motor (Roblox, Luau), a dinâmica do mapa e da destruição, onde ficam as
arenas dos domínios, o teleporte, a rede entre cliente e servidor, e o choque.

> **Aviso importante.** O código do Jujutsu Shenanigans (JJS) é **fechado**. Ninguém de fora tem os scripts Luau do
> jogo, e este estudo **não contém nem tenta obter** o código deles. O que está aqui vem de:
> * fatos observáveis: wiki, changelog oficial do jogo, vídeos, cenas de desenvolvimento;
> * deduções a partir desses fatos;
> * práticas do Roblox: documentação oficial e o DevForum.
>
> Cada afirmação tem um nível de evidência:
>
> | Marca | Significa |
> |---|---|
> | **[F]** | Fato: está na wiki, no changelog, na documentação ou aparece em vídeo |
> | **[D]** | Dedução forte a partir de fatos |
> | **[P]** | Prática comum no Roblox: o JJS *provavelmente* faz assim, mas não há prova |
>
> O código Luau da seção 7 é **nosso**, uma reconstrução ilustrativa da arquitetura, não o código do JJS.

Escrito em 2026-11-12. O changelog citado vai até a versão 1.83.2 (RBLXGUIDE, que leu a wiki do JJS em 24/10/2026).

## 1. O jogo e o motor

* **Quem fez:** o desenvolvedor **Tze**, pelo grupo *Tze's Shenanigans*. O jogo saiu em **17/04/2022** **[F]**.
* **Como começou:** o vídeo de história do jogo mostra o Tze testando tudo num **baseplate cinza** com botões de depuração
  (criar bonecos, zerar recargas, encher o medidor de despertar). Depois veio a cidade **[F]**.
* **Motor:** Roblox, com scripts em **Luau** (o Lua tipado do Roblox) **[F]**. O Roblox é cliente-servidor:
  * o servidor é a autoridade: vida, dano, estados, posições validadas;
  * cada cliente tem o seu `LocalScript`, a sua câmera, a sua interface e os seus efeitos visuais;
  * os dois conversam por `RemoteEvent` / `RemoteFunction` (e `UnreliableRemoteEvent` para coisas efêmeras) **[F docs]**.
* **Gênero:** "battlegrounds" (como o The Strongest Battlegrounds): arena aberta, todos contra todos, kits de golpes por
  personagem, ultimate ("despertar") que libera um moveset novo **[F]**.

## 2. Dinâmica do mapa

### 2.1 Mapas

* **Primeiros mapas** **[F, vídeo de história]**:
  * o baseplate de testes;
  * uma cidade-protótipo (ruas, casas coloridas, árvores, bancos, hospital);
  * uma **cidade numa ilha flutuante**, com borda em volta.
* **Mapas grandes e pequenos que entram numa roleta** **[F, changelog]**:
  * Shibuya Station (refeita na 1.39);
  * Shipping Yard e Roadside (1.42);
  * Construction Site (1.45);
  * Shibuya Crossing e Zenin HQ (1.50).
* **Interativos no mapa** **[F]**: palco com microfone, piano, outdoors personalizáveis, painel do trem, portais e
  portas de eventos.
* **Temas sazonais:** Natal (neve, fortes de neve), portais de eventos. O mapa recebe objetos temporários a cada
  atualização **[F]**.

### 2.2 Destruição

A destruição é uma parte central do jogo.

* **Voxels.** Prédios e chão quebram em **blocos retangulares com física**. O HUD de desenvolvimento do Tze mostra um
  contador de **"779 Voxels"** a 63 FPS **[F, vídeo de história, 05:45]**:
  * o Dismantle corta **fatias limpas** nos prédios;
  * o Reversal Red derruba prédios bloco a bloco;
  * o Lapse Blue e os avanços abrem crateras no chão **[F]**.
* **Peças marcadas como "Breakable".** No Free Build, cada peça tem a opção `Breakable`, além de `CanCollide` e
  `CastShadow` **[F, vídeo 16:50]**. **[D]** Isso indica que o sistema de destruição age só sobre peças marcadas como
  quebráveis, e não sobre o mapa inteiro.
* **Custo no servidor, otimizado várias vezes** **[F, changelog]**:
  * "desempenho do servidor durante a destruição muito melhorado" (1.03);
  * otimizações em 1.16, 1.28, 1.52;
  * "lag spikes quando peças somem" (1.08), o que indica que os escombros **somem depois de um tempo**;
  * "o sistema de destruição foi reescrito do zero" (1.83), e o antigo **voltou** na 1.83.1.
* **Opções no cliente** **[F]**: "Destruction Smoke" (1.30) e **"Destruction Interpolation"** (1.37).
  * **[D]** Ter uma opção de interpolação no cliente indica que **o servidor simula ou decide a quebra e os clientes
    suavizam o movimento dos escombros**, em vez de cada cliente simular a própria física.
* **Ligação com os domínios** **[F, wiki]**:
  * golpes que destroem terreno ferem a borda de um domínio por fora, com um estrondo;
  * no Skill Builder, qualquer hitbox com "Debree" maior que 0 fere domínios.
  * **[D]** A borda do domínio no mapa é tratada **pelo mesmo sistema de destruição**: ela tem "vida" e recebe dano
    de hitboxes com o atributo de quebrar.

## 3. A expansão de domínio por dentro

### 3.1 O fato central: a arena fica longe do mapa

A wiki diz **[F]**:

> Cada modelo de domínio surge num ponto único, **longe do mapa**, cada um a uma **distância fixa do anterior**. Os
> jogadores dentro são **teleportados para lá, relativo à posição do conjurador**.

Outras evidências que confirmam:

| Evidência | Fonte | O que mostra |
|---|---|---|
| "Vozes de jogadores dentro de um domínio podem ser ouvidas de fora" (1.37); "dá para ouvir melhor os jogadores dentro de domínios" (1.46) | Changelog **[F]** | O chat de voz do Roblox é **espacial** (por proximidade). Se quem está no domínio sumiu da voz de quem ficou perto, é porque foi **fisicamente para longe**. O Tze precisou corrigir isso à mão **[D]** |
| "Domínios ficando de lado" (bug, 1.04) | Changelog **[F]** | A arena é posicionada com rotação (`CFrame`), provavelmente seguindo a orientação do conjurador. Um erro nessa conta deixava a arena torta **[D]** |
| No Free Build dá para pôr um ponto de renascimento **dentro** de um domínio, renascer lá e decorar o interior | Wiki **[F]** | O interior é **geometria real no `Workspace`**, não só um efeito de tela |
| O vídeo de história mostra o jogador saindo da cidade e indo para o vagão branco e a plataforma do metrô do domínio do Hakari | Vídeo, 12:21 **[F]** | É teleporte para outro lugar, não troca de céu |
| O domínio pode ser atingido **por fora**, na borda | Wiki **[F]** | Existe uma **segunda peça**: o domo que fica **no mapa**, no lugar da conjuração |

**[D] A estrutura, então, é dupla:**

```
    MAPA (cidade)                                 LONGE DO MAPA (área das arenas)
   ┌──────────────────────────┐                 ┌──────────────────────────────────────┐
   │      .-''''-.            │   teleporte     │  [arena 1]    [arena 2]    [arena 3]  │
   │    /  DOMO    \  ◄── recebe dano de fora   │   vazio        santuário   metrô      │
   │   |  (borda)   |         │  ──────────────►│   ◄─ distância fixa ─►                │
   │    \          /          │   (posição      │                                       │
   │      '-....-'            │    relativa ao  │  quem foi pego está aqui               │
   │  quem ficou fora vê isto │    conjurador)  │  (efeito garantido, trilha, céu)      │
   └──────────────────────────┘                 └──────────────────────────────────────┘
```

* **O domo no mapa** é o que quem ficou fora vê: uma esfera preta (ou branca) destrutível, que segura o espaço e é o
  alvo da quebra por fora.
* **A arena longe** é onde a luta acontece: cenário completo, céu e iluminação próprios, efeito garantido.
* **"Cada um a uma distância fixa do anterior"** **[D]** indica um sistema de **vagas**. Cada domínio ativo recebe a
  próxima vaga livre (vaga N fica a N × distância de um ponto-base), então dois domínios simultâneos não se encostam.

### 3.2 Passo a passo provável de uma expansão

| # | Etapa | Lado | Evidência |
|---|---|---|---|
| 1 | O jogador aperta o golpe 4 no despertar. O cliente manda a intenção ao servidor | Cliente → servidor | **[P]** Padrão de todo battlegrounds (seção 5) |
| 2 | **Validação:** está no ar? dentro ou perto de outro domínio? em recarga? no despertar certo? | Servidor | **[F]** Regras da wiki: não no ar (1.05), não dentro nem perto de outro domínio |
| 3 | **Preparo de 1,25 s:** animação do sinal de mão, aura, decalque no chão. Um acerto ou atordoamento cancela | Servidor guarda o estado; clientes animam | **[F]** Wiki. **[D]** O cancelamento por acerto é checado no servidor |
| 4 | **Aviso ("pop-up")** para quem está a até ~37,5 studs: faixa de mangá com o **rosto do avatar Roblox do jogador** | Cliente (`ScreenGui`) | **[F]** "Os pop-ups de domínio agora usam o rosto do Roblox do jogador" (1.27). **[P]** `Players:GetUserThumbnailAsync` |
| 5 | **Captura:** o servidor lista quem está no raio no fim do preparo | Servidor | **[F]** "Só entra quem é pego no preparo" |
| 6 | **Domo no mapa** criado no lugar, com "vida" para a quebra por fora | Servidor (peça replicada) | **[F]** Quebra por fora com estrondo |
| 7 | **Arena:** o servidor reserva uma vaga e põe (ou já tem) o modelo da arena lá, girado conforme o conjurador | Servidor | **[F]** Wiki. **[D]** O bug de "domínio de lado" |
| 8 | **Esconder o teleporte:** flash ou tela cobrindo enquanto o lugar novo carrega | Cliente | **[F]** Bug "domínios cegando a tela por um instante" (1.61). **[P]** DevForum: prender a área com `RequestStreamAroundAsync` / pré-carregar |
| 9 | **Teleporte** de todos os capturados mantendo a posição relativa (`arena.CFrame * centro:ToObjectSpace(jogador.CFrame)`) | Servidor | **[F]** "Relativo à posição do conjurador" |
| 10 | **Ambiente:** céu, cor, névoa e trilha do domínio para quem está dentro | Cliente | **[F]** Docs: efeitos de pós-processamento na `Camera` aparecem só para aquele jogador. **[P]** Troca do `Sky` / `Lighting` no cliente |
| 11 | **Efeito garantido** em laço até acabar o tempo | Servidor | **[F]** Números da wiki. **[D]** Malevolent Shrine: 109 cortes em 18 s, cerca de **1 corte a cada 0,165 s** |
| 12 | **Fim:** estilhaço, todos voltam ao mapa (provavelmente para onde estavam ou relativo ao domo), o domo some, a recarga começa | Servidor + clientes | **[F]** Visual nos vídeos. **[P]** O retorno |

### 3.3 Detalhes que o changelog revela

* **Parkour desligado dentro dos domínios** (1.75) **[F]**: a arena tem paredes e chão próprios, e o jogo impede escalar
  e correr pelas paredes para não sair do espaço.
* **"Transições mais suaves ao invadir um domínio"** (1.13) **[F]**: invadir também é um teleporte para a arena.
* **"Não dá para trocar de lugar com jogadores durante a expansão"** (bug, 1.43) **[F]**: o Switcher trocava de posição
  com alguém e escapava ou entrava. **[D]** O teleporte da captura disputava com outros teleportes.
* **"Malevolent Shrine não te congela mais"** (1.00.1) **[F]**: o efeito garantido é um estado aplicado aos capturados,
  ajustável por domínio.
* **"Domínios podem ser abertos perto dos spawns"** (1.15) **[F]**: antes havia uma zona proibida perto dos pontos de
  renascimento.
* **Condição "Is In Domain" no Skill Builder** (1.67) **[F]**: o servidor guarda "este jogador está num domínio" como
  um **estado consultável** pelos golpes.
* **Votação no Deadly Sentencing** (1.68) **[F]**: domínios podem ter **interface e regras próprias**. A arena é um
  "minijogo" plugado no sistema geral.

### 3.4 Malevolent Shrine e Infinite Void dentro desse esquema

* **Infinite Void** **[F]**: 14 s, sem agir. **[D]** Implementado como um **estado** aplicado aos capturados (do mesmo
  tipo dos estados do Skill Builder: `NoM1`, travas, atordoamento), que bloqueia ações novas mas **não cancela** as que
  já começaram (a wiki confirma isso).
* **Malevolent Shrine** **[F]**: 2 HP por corte, 0,5 bloqueando, bloqueio em 360°. **[D]** Um laço no servidor aplica
  dano a cada ~0,165 s a todos os capturados, conferindo o estado "bloqueando". Os cortes visuais são gerados **no
  cliente**, em sincronia aproximada.
* **Incomplete Shrine** (sem borda) **[F]**: alcance infinito, gera escombros no mapa (1.46: "cria menos escombros").
  **[D]** Esse **não teleporta**: acontece **no próprio mapa**, usando o sistema de destruição. Por isso ele não choca e
  quebra os outros domínios.

## 4. O choque de domínios por dentro

* **Desde o começo** **[F, vídeo de história, 07:43]**: o primeiro "Double Domain" já mostrava Gojo e Sukuna expandindo
  juntos, a faixa dividida e a ida para um ambiente combinado.
* **Detecção** **[F]**: domínios conjurados ao mesmo tempo ou numa janela curta. **[D]** Como só dá para conjurar fora de
  domínios, a "janela" é o **preparo**: quem começa a conjurar perto de alguém que ainda está no preparo entra no grupo.
* **Arena do choque** **[F]**: "domínios fundidos", borda sempre preta, cenários piscando um sobre o outro. **[D]** Uma
  arena de choque (ou a do primeiro) onde os **clientes alternam rapidamente** os cenários dos participantes (céu e
  modelos). É um efeito puramente visual: a lógica não muda por cenário.
* **Regras atuais** **[F]**:
  * efeitos garantidos e habilidades desligados;
  * M1 e esquiva trocados por versões passivas que não atordoam;
  * barra de **+6,25 % por acerto** no conjurador rival;
  * **15 s**;
  * empate quebra todos;
  * a trilha de choque substitui as outras.
  * **[D]** Implementar isso é: um estado "em choque" nos participantes (que troca o moveset, como o Skill Builder faz
    com estados), um contador por participante no servidor, e uma interface replicada (contador e barras).
* **Resolução** **[F]**: flash, estilhaço do perdedor. **[D]** O servidor troca a arena do grupo pela do vencedor (ou
  mantém e só troca o cenário nos clientes) e liga o efeito garantido do vencedor.

## 5. Rede e arquitetura de golpes (o padrão dos battlegrounds)

O que a comunidade de desenvolvedores de jogos "estilo TSB/JJS" descreve no DevForum **[P]**:

* **Cliente → servidor → todos os clientes.**
  1. O cliente manda a tecla apertada.
  2. O servidor valida estado e recarga.
  3. O servidor avisa todos os clientes para tocarem o golpe.
* **Cada cliente toca a animação** e dispara efeitos e sons nos marcadores da animação
  (`GetMarkerReachedSignal`). Assim o golpe parece fluido mesmo quando o atacante tem ping alto.
* **O servidor só manda o identificador do efeito e onde.** A lógica visual mora em `ModuleScript`s no cliente.
  Efeitos pesados (partículas, escombros) **nunca** rodam no servidor.
* **Uma classe por golpe:** um *Service* no servidor (lógica) e um *Controller* no cliente (visual).
* **Hitbox no cliente ou no servidor** é a grande discussão. No cliente é mais fluido mas exige validação no servidor;
  no servidor é mais seguro.
* `UnreliableRemoteEvent` só para coisas efêmeras e frequentes. Nunca para vida ou dano.

**[D] O Skill Builder do JJS revela a arquitetura de dados dos golpes.** Os jogadores montam golpes ligando nós:

* `Hitbox` (com "Debree" e "Link User"), `Projectile`, `Grab`, `Counter`;
* `State` (`Iframe`, `NoM1`, `Direction Lock`...), `Tag` (variáveis);
* `Branch`, `Loop`, `Wait`;
* `Visual` (com `Mesh`), `Particle Emitter`;
* condições como `Is In Domain` e `Has Awk Bar`.

Isso mostra que o motor do jogo é **orientado a dados**: golpes são sequências de nós com tempos, hitboxes, estados e
efeitos ligados por etiquetas, separando lógica de visual. Os domínios provavelmente usam as mesmas peças: estados nos
capturados, hitboxes com o atributo de quebrar, efeitos por etiqueta.

## 6. Problemas conhecidos e como o Roblox resolve

| Problema | No JJS | Como se resolve no Roblox |
|---|---|---|
| Ao teleportar longe, o lugar novo aparece sem renderizar por alguns quadros | **[F]** Bug de tela "cegando" na expansão (1.61). **[D]** O flash esconde a carga | **[F docs / P]** `Player:RequestStreamAroundAsync(posição)` antes do teleporte. Modelos `Persistent` para arenas sempre carregadas. Pré-carregar recursos (`ContentProvider:PreloadAsync`). Ou o cliente põe uma cópia local da arena até a do servidor chegar (DevForum) |
| Voz por proximidade quebra quando metade do grupo vai para longe | **[F]** Corrigido nas 1.37 e 1.46 | Roteamento manual do áudio de voz entre jogadores |
| Teleportes concorrentes (trocas, invasões) | **[F]** Bug do Switcher (1.43) | Travar teleportes de outros golpes durante a captura |
| Arena girada errada | **[F]** "Domínio de lado" (1.04) | Usar só o yaw do conjurador ao montar o `CFrame` da arena |
| Sair da arena pelas paredes | **[F]** Parkour desligado (1.75) | Paredes invisíveis e desligar movimentos especiais no estado "em domínio" |
| Efeitos de muitos jogadores pesando | **[F]** Limites de efeitos no Creator Clash (1.73, 1.83.2); não mostrar efeitos fora de alcance | Limitar quantidade e tamanho de efeitos no cliente e cortar por distância |

## 7. Reconstrução ilustrativa em Luau (nossa, não do JJS)

Esboço de **como uma arquitetura assim pode ser escrita** no Roblox, para entender as peças. Simplificado: sem
tratamento de erro, sem invasão, sem choque.

```lua
-- ServerScriptService/DomainService.lua  (servidor: autoridade)
local Players = game:GetService("Players")
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local ServerStorage = game:GetService("ServerStorage")

local DomainFx = ReplicatedStorage.Remotes.DomainFx      -- RemoteEvent: servidor -> clientes
local ARENA_ORIGIN = Vector3.new(0, 5000, 50000)          -- área das arenas, longe do mapa
local SLOT_SPACING = 2000                                 -- "cada um a uma distância fixa do anterior"
local WINDUP = 1.25

local slots = {}                                          -- vaga -> domínio ativo

local function freeSlot()
    local i = 1
    while slots[i] do i += 1 end
    return i
end

local function charactersWithin(center, radius)
    local list = {}
    for _, player in Players:GetPlayers() do
        local root = player.Character and player.Character:FindFirstChild("HumanoidRootPart")
        if root and (root.Position - center).Magnitude <= radius then
            table.insert(list, player)
        end
    end
    return list
end

local DomainService = {}

function DomainService.Cast(caster, spec)
    local root = caster.Character.HumanoidRootPart
    -- (validações: no ar, dentro de outro domínio, recarga, despertar...)
    local center = root.CFrame
    DomainFx:FireAllClients("Windup", caster, spec.Id, center.Position)

    task.wait(WINDUP)                                     -- (acerto no preparo cancelaria aqui)

    local caught = charactersWithin(center.Position, spec.Radius)
    local slot = freeSlot()
    local yaw = CFrame.Angles(0, select(2, center:ToEulerAnglesYXZ()), 0)   -- só o yaw: nada de "domínio de lado"
    local arenaCFrame = CFrame.new(ARENA_ORIGIN + Vector3.new(slot * SLOT_SPACING, 0, 0)) * yaw

    local arena = ServerStorage.Arenas[spec.Id]:Clone()
    arena:PivotTo(arenaCFrame)
    arena.Parent = workspace
    local dome = ServerStorage.Domes[spec.Id]:Clone()      -- o domo que fica no mapa (alvo da quebra por fora)
    dome:PivotTo(center)
    dome.Parent = workspace
    slots[slot] = { caster = caster, arena = arena, dome = dome, caught = caught }

    for _, player in caught do
        player:RequestStreamAroundAsync(arenaCFrame.Position)          -- carregar antes de chegar
        DomainFx:FireClient(player, "Enter", spec.Id)                  -- flash, céu, trilha
        local hrp = player.Character.HumanoidRootPart
        local offset = center:ToObjectSpace(hrp.CFrame)                -- posição relativa ao conjurador
        hrp.CFrame = arenaCFrame * offset
    end

    -- efeito garantido (servidor), ex.: cortes a cada ~0,165 s
    local deadline = os.clock() + spec.Duration
    while os.clock() < deadline and slots[slot] do
        spec.SureHit(caught, caster)
        task.wait(spec.Tick)
    end
    DomainService.End(slot, center)
end

function DomainService.End(slot, center)
    local domain = slots[slot]
    slots[slot] = nil
    for _, player in domain.caught do
        DomainFx:FireClient(player, "Shatter")
        -- volta ao mapa (aqui: perto do centro do domo)
        player.Character.HumanoidRootPart.CFrame = center * CFrame.new(math.random(-6, 6), 3, math.random(-6, 6))
    end
    domain.arena:Destroy()
    domain.dome:Destroy()
end

return DomainService
```

```lua
-- StarterPlayerScripts/DomainController.lua  (cliente: só visual)
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local Lighting = game:GetService("Lighting")
local DomainFx = ReplicatedStorage.Remotes.DomainFx
local Visuals = require(ReplicatedStorage.DomainVisuals)       -- tabela por domínio: céu, cor, trilha, faixa

DomainFx.OnClientEvent:Connect(function(kind, ...)
    if kind == "Windup" then
        local caster, id, position = ...
        Visuals[id].PlayCutIn(caster)          -- faixa de mangá com o rosto (GetUserThumbnailAsync)
        Visuals[id].PlayHandSign(caster, position)
    elseif kind == "Enter" then
        local id = ...
        Visuals.Flash()                        -- cobre o teleporte
        local cc = Instance.new("ColorCorrectionEffect")
        cc.TintColor = Visuals[id].Tint
        cc.Parent = workspace.CurrentCamera    -- na câmera: só este jogador vê
        Visuals[id].ApplySky(Lighting)         -- troca local do céu (o cliente não replica)
        Visuals[id].PlayTheme()
    elseif kind == "Shatter" then
        Visuals.Shatter()
        Visuals.RestoreLighting(Lighting)
    end
end)
```

## 8. O que isso ensina para o nosso caso (Minecraft 1.7.10)

* **O JJS usa duas peças: o domo no mapa e a arena longe.** No Minecraft o equivalente seria:
  * uma exibição de domo no lugar da conjuração (para quem ficou fora ver e acertar);
  * uma região-arena no mesmo mundo, com chunks mantidos carregados.
  * Essa é a opção **B** do `ESTUDO.md` (seção 5.2), agora com a confirmação de que **é assim que o JJS faz**.
* **Os mesmos problemas que o JJS teve, nós teremos:**
  * mundo sem renderizar logo depois do teleporte (no 1.7.10: chunks carregando), que pede um flash cobrindo e chunks
    mantidos carregados;
  * se o servidor tiver **voz por proximidade** (vale conferir o AkashicVoiceChat), quem foi para a arena some da voz
    de quem ficou;
  * teleportes concorrentes de outros sistemas;
  * jogadores saindo pelas paredes da arena.
* **Vantagem da opção A (desenhar no lugar, pelo cliente):** nenhum desses problemas, ao custo de o terreno de dentro
  continuar lá. O JJS mostra que a opção B dá um resultado mais imersivo e controlado.
* **A separação "servidor decide, cliente desenha"** que os battlegrounds usam é a mesma que o Kunftig já segue.

## 9. A verificar

* Se as arenas do JJS ficam sempre carregadas (modelos persistentes) ou se são clonadas a cada expansão.
* Para onde os jogadores voltam no fim: a posição original ou relativa ao domo.
* Se o JJS usa `StreamingEnabled` (o streaming muda como o teleporte longe se comporta).
* Como a arena do choque é escolhida (a do primeiro conjurador ou uma arena própria de choque).
* Como os escombros voltam ao normal: o changelog mostra que eles somem com o tempo (1.08), mas não como o mapa se
  reconstrói.

## Fontes

* Changelog do JJS (V1.00 a V1.83.2): https://rblxguide.com/games/jujutsu-shenanigans/update-log
* Regras dos domínios (wiki via RBLXGUIDE): https://rblxguide.com/games/jujutsu-shenanigans/updates/domain-expansion-guide ·
  https://rblxguide.com/games/jujutsu-shenanigans/domain-expansions
* Vídeo *The ENTIRE History Of Jujutsu Shenanigans* (cenas de desenvolvimento), analisado com o vidIQ:
  https://www.youtube.com/watch?v=Eh99bFOoDpc
* Skill Builder (nós Projectile, Grab, Counter, State, Tag, Mesh):
  https://itemlevel.net/jujutsu-shenanigans-how-to-use-every-new-skill-builder-block-in-jjs-update/
* Quem fez o jogo: https://deltiasgaming.com/?p=291826 · https://rowatcher.com/games/3508322461/jujutsu-shenanigans
* DevForum, arquitetura TSB/JJS (replicação de efeitos e animações):
  https://devforum.roblox.com/t/best-way-to-replicate-vfxanimations-for-a-battlegrounds-game-tsbjjs-style-architecture/4814813
* DevForum, efeitos no cliente ou no servidor: https://devforum.roblox.com/t/client-vs-server-sided-vfx-help/2908106
* DevForum, atraso de renderização ao teleportar para o domínio:
  https://devforum.roblox.com/t/removing-teleportation-render-lag/3181818
* DevForum, efeitos de domínio (domo, `ShapePartial`, bloom, faixa branca):
  https://devforum.roblox.com/t/how-to-make-a-creating-domain-expansion-effect/2863076 ·
  https://devforum.roblox.com/t/how-to-make-this-domain-expansion-effect/1719648 ·
  https://devforum.roblox.com/t/how-to-make-a-domain-like-infinite-void-in-jujutsu-kaisen/2768513
* Documentação do Roblox, streaming: https://create.roblox.com/docs/workspace/streaming
* Documentação do Roblox, pós-processamento (efeitos na `Camera` só para aquele jogador):
  https://create.roblox.com/docs/environment/post-processing-effects
* Documentação do Roblox, `Player:RequestStreamAroundAsync`:
  https://create.roblox.com/docs/reference/engine/classes/Player#RequestStreamAroundAsync
