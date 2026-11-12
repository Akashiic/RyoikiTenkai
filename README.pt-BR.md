# RyoikiTenkai (領域展開)

**Expansão de Domínio** e **Choque de Domínios** para servidores Minecraft **1.7.10** (Thermos/Crucible), desenhados
no cliente pelo mod [Kunftig](https://github.com/Akashiic/Kunftig).

> English version: [README.md](README.md).

## O que é

Um jogador expande o seu domínio: uma faixa estilo mangá atravessa a tela e uma barreira se fecha num raio em volta
dele. Para quem ficou dentro, o mundo vira o interior do domínio (céu, cor, estruturas gigantes), e um efeito garantido
age em todos: paralisar, cortes constantes etc.

Se dois ou mais jogadores expandirem ao mesmo tempo, começa um **choque de domínios**:

* os cenários piscam um sobre o outro;
* um contador de 15 segundos aparece;
* cada acerto no conjurador rival enche a barra de quem acertou;
* vence quem encher primeiro, ou quem tiver mais quando o tempo acabar;
* o domínio do vencedor fica, os outros se estilhaçam, e o empate quebra todos.

O estudo completo (referências, números, visual, como isso vira Minecraft) está em [docs/ESTUDO.md](docs/ESTUDO.md).

## Estado

**Em início.** Primeiro passo: um plugin que usa só o que a API 13 do Kunftig já tem (exibições 3D, HUD, teclas,
efeitos e partículas). Os efeitos de tela (céu, neblina, flash, tremor, câmera) vêm com a API 14 do Kunftig.

## Instalação (quando houver build)

| Onde | O quê |
|---|---|
| Servidor | `RyoikiTenkai-<versão>.jar` em `plugins/`, e o mod Kunftig em `mods/` |
| Cliente | O mod Kunftig (com GeckoLib) e o resource pack do RyoikiTenkai |

## Licença

Ainda não definida.
