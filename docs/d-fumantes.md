# (d) Problema dos Fumantes

Responsável: Felipe Paz Carvalho Batista
Código: [`src/fumantes/`](../src/fumantes/)

## Modelagem

| Elemento | No código |
|---|---|
| **Threads** | 1 `Agente` + 3 `Fumante` (um com tabaco, um com papel, um com fósforo) |
| **Recurso compartilhado** | A mesa (`Mesa.naMesa`): conjunto de ingredientes colocados pelo agente |
| **Regiões críticas** | `Mesa.colocar()` (agente escreve na mesa) e `Mesa.retirar()` (fumante esvazia a mesa) |

**Linha de execução do Agente** (laço por N rodadas): sorteia o ingrediente que vai faltar → coloca os outros
dois na mesa → acorda o fumante que tem o que falta → bloqueia até o cigarro acabar.

**Linha de execução do Fumante** (laço infinito): bloqueia até ser chamado → retira os dois ingredientes →
enrola e fuma por tempo aleatório → desbloqueia o agente.

## Semáforos

| Semáforo | Valor inicial | Papel |
|---|---|---|
| `mutex` | 1 | Exclusão mútua sobre a mesa. Agente e fumante nunca leem/escrevem a mesa ao mesmo tempo |
| `fumante[i]` (um por fumante) | 0 | Os três fumantes começam bloqueados. O agente dá `release` **só** no fumante que tem o ingrediente que falta |
| `agente` | 0 | O agente dá `acquire` depois de servir e fica bloqueado. O fumante dá `release` quando termina de fumar |

## Estratégia contra deadlock

A versão ingênua (`modo ingenuo`) usa um semáforo por ingrediente e deixa os fumantes disputarem: cada fumante
pega um ingrediente de cada vez. Se o agente coloca tabaco + fósforo, o fumante de fósforo pode pegar o tabaco
e ficar esperando papel, que nunca vem — enquanto o fumante de papel, o único que poderia fumar, espera o
tabaco que já foi levado. Há **posse e espera** (segura um, espera outro) e ninguém avança.

A solução elimina a **posse e espera**: nenhum fumante pega ingrediente parcial. Quem sabe qual fumante
deve fumar é o agente (ele sorteou o que falta), então ele acorda diretamente o fumante certo, que retira
os dois ingredientes de uma vez dentro da região crítica. Cada thread fica bloqueada em no máximo um semáforo
por vez e a "vez" passa como um bastão: agente → fumante → agente. Não existe ciclo de espera possível.

## Execução

```powershell
.\rodar.ps1 fumantes ingenuo 10   # trava e imprime quem segura o quê
.\rodar.ps1 fumantes correto 10   # termina, 10 de 10 cigarros
```

## Validação (`TesteFumantes`)

20 execuções × 300 rodadas, com tempos mínimos para maximizar a disputa. Em cada execução verifica:
- terminou dentro do tempo limite (sem deadlock);
- total de cigarros = número de rodadas;
- cigarros de cada fumante = rodadas em que faltava o ingrediente dele (o fumante certo sempre foi acordado);
- a mesa nunca foi servida ocupada nem esvaziada com o conteúdo errado (0 violações);
- no máximo 1 fumante fumando por vez (o agente espera o cigarro acabar).

Controle negativo: a versão ingênua precisa travar.

Última execução (18/10/2026): 21/21 OK.
