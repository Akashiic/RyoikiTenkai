# RyoikiTenkai: contexto para quem trabalha neste repositório

**Expansão de Domínio** e **Choque de Domínios** (inspirados em Jujutsu Kaisen e no jogo de Roblox Jujutsu
Shenanigans) para o servidor Minecraft **1.7.10** da rede. Mecânica sobretudo visual: quase inútil no combate, mas boa
para o servidor (clipes, eventos, engajamento). Não será monetizado.

## Comece por aqui

1. **[`docs/HANDOFF.md`](docs/HANDOFF.md)**: o estado do projeto, as decisões do dono, o plano proposto, os riscos e
   os fatos do ambiente. **Leia primeiro.**
2. [`docs/ESTUDO.md`](docs/ESTUDO.md): regras e visual do Infinite Void, do Malevolent Shrine e do choque, a escala e
   o que cada elemento precisa no Kunftig.
3. [`docs/ESTUDO-TECNICO.md`](docs/ESTUDO-TECNICO.md): como o JJS provavelmente faz por dentro (Roblox/Luau), com
   fato, dedução e prática comum separados.

## Carta branca

O dono deu ao agente deste projeto **carta branca**: imaginação, criatividade e **permissão total para mudar o plano**.
Decida o melhor caminho, a melhor arquitetura e a melhor forma de implementar. Os estudos e o plano são pontos de
partida, não amarras. Respeite as decisões do dono listadas no handoff (seção 3); se achar que uma delas está
atrapalhando, converse com ele.

## Sobre o dono e o ambiente

* **Sempre confira os repositórios do Akashiic** (`list_repos`) antes de responder sobre "o nosso servidor", "o nosso
  ambiente" ou "o nosso projeto". Não presuma a plataforma: a resposta quase sempre está num repositório.
* Responda em **português do Brasil**.
* **Servidor:** Minecraft 1.7.10, Forge 10.13.4.1614 + UniMixins, híbrido **Thermos/Crucible** (Java 21 em produção).
  O modpack é o TEOF (com **Angelica** e GeckoLib Unofficial 1.0.4).
* **[Kunftig](https://github.com/Akashiic/Kunftig)** é o mod (motor) próprio da rede, no cliente e no servidor:
  exibições GeckoLib no mundo, HUD, câmera, teclas, lâminas e efeitos, partículas. Guia: `docs/API.md` no Kunftig.

## Regras

* Trabalhe em branches e abra PRs. Não use force-push.
* O que não foi visto no jogo fica marcado como "a verificar".
* Mudou o plano? Atualize o `docs/HANDOFF.md`.
