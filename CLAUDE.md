# RyoikiTenkai: contexto para quem trabalha neste repositório

**Expansão de Domínio** e **Choque de Domínios** (inspirados em Jujutsu Kaisen e no jogo de Roblox Jujutsu
Shenanigans) para o servidor Minecraft **1.7.10** da rede. Mecânica sobretudo visual: quase inútil no combate, mas boa
para o servidor (clipes, eventos, engajamento).

O estudo de referência está em [`docs/ESTUDO.md`](docs/ESTUDO.md): regras e visual do Infinite Void e do Malevolent
Shrine, as regras do choque, a escala e o que cada elemento precisa no Kunftig. Leia antes de mexer na mecânica.

## Sobre o dono e o ambiente

* **Sempre confira os repositórios do Akashiic** (`list_repos`) antes de responder sobre "o nosso servidor", "o nosso
  ambiente" ou "o nosso projeto". Não presuma a plataforma: a resposta quase sempre está num repositório.
* Responda em **português do Brasil**.
* **Servidor:** Minecraft 1.7.10, Forge 10.13.4.1614 + UniMixins, híbrido **Thermos/Crucible** com plugins Bukkit. O
  Crucible de produção roda **Java 21**. O modpack é o TEOF (com **Angelica**, GeckoLib Unofficial 1.0.4).
* **[Kunftig](https://github.com/Akashiic/Kunftig)** é o mod (motor) próprio da rede, no cliente e no servidor, com a
  `kunftig-api` para plugins: exibições GeckoLib no mundo, HUD, câmera, teclas, lâminas e efeitos, partículas. Guia:
  `docs/API.md` no Kunftig. Recurso novo de cliente vai para o Kunftig como uma **nova versão da API**; a lógica de
  jogo fica nos plugins.
* Projetos irmãos que servem de modelo: [AkashicFishing](https://github.com/Akashiic/AkashicFishing) (o mesmo arranjo
  `core` + `plugin` + `libs/`), Overkill (combate, usa as teclas, o HUD e os efeitos do Kunftig).

## Divisão do trabalho

| Onde | O quê |
|---|---|
| **Este repositório** | O plugin Bukkit: ativação, captura, efeito garantido, choque, configuração dos domínios, comandos. O resource pack dos domínios (modelos GeckoLib, texturas, sons) |
| **Kunftig, API 14** (a fazer) | Efeitos de tela genéricos: céu por jogador, neblina, flash de tela inteira, tremor, pós-processamento, câmera cinematográfica, HUD com movimento |

Até a API 14 existir, a "fase 0" usa só o que a API 13 já tem (exibições, HUD, teclas, efeitos, partículas). Veja a
seção 5.3 do estudo.

## Regras

* `kunftig-api` é sempre **`compileOnly`**, fixado em `libs/` (ver `libs/README.md`). O Kunftig é um mod em `mods/`, não
  entra no `depend` do `plugin.yml`. Isole as chamadas ao Kunftig em classes que só carregam depois de conferir
  `KunftigApi.apiVersion()`.
* API Bukkit: compilar contra a spigot-api 1.8.8, e o portão `checkLegacyBukkitApi` confere contra a 1.7.10 do
  Crucible. Nada de API mais nova.
* Toda lógica de Bukkit roda na thread principal.
* Regras de jogo puras (tempos, estados, choque) ficam no `:core`, sem Minecraft, com testes.
* **Arte própria.** Nada de modelos, texturas, sons ou trilhas do Jujutsu Shenanigans. Nomes e falas do anime só em
  domínios de exemplo; o servidor pode dar nomes e visuais próprios.
* O que não foi visto no jogo fica marcado como "a verificar" na documentação.
* Commits e PRs só quando pedidos. Não usar force-push. Um PR mesclado não é reaproveitado.
