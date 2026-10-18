# (e) Jantar dos Filósofos

Responsável: Felipe Paz Carvalho Batista
Código: [`src/filosofos/`](../src/filosofos/)

## Modelagem

| Elemento | No código |
|---|---|
| **Threads** | 5 `Filosofo` |
| **Recursos compartilhados** | 5 hashis. O filósofo `i` usa o hashi `i` (direita) e o `(i+1) mod 5` (esquerda) |
| **Região crítica** | Comer: só acontece segurando os dois hashis. Cada hashi é um recurso de uso exclusivo |

**Linha de execução do Filósofo** (laço por N refeições): medita → pede lugar ao garçom → pega hashi da
direita → pega hashi da esquerda → come → solta os dois → libera o lugar no garçom.

## Semáforos

| Semáforo | Valor inicial | Papel |
|---|---|---|
| `hashi[i]` (um por hashi) | 1 | Semáforo binário: só um filósofo segura cada hashi |
| `garcom` | 4 (N − 1) | Semáforo contador: no máximo 4 filósofos disputam hashis ao mesmo tempo |

Todos os semáforos são criados com `fair = true` (fila FIFO), para que nenhum filósofo seja preterido
indefinidamente (inanição).

## Estratégia contra deadlock

Na versão ingênua, todos pegam a direita e depois a esquerda. Se os cinco pegam a direita ao mesmo tempo,
cada um segura um hashi e espera o do vizinho: **espera circular** (0 espera 1, 1 espera 2, ..., 4 espera 0).

O garçom quebra a espera circular: com no máximo 4 filósofos na disputa e 5 hashis, pelo princípio da
casa dos pombos pelo menos um deles consegue os dois hashis, come e libera os recursos. O ciclo nunca fecha.

Alternativa conhecida, não usada aqui: ordenar os recursos (cada filósofo pega primeiro o hashi de menor
número). Também quebra a espera circular, mas o garçom deixa a solução explícita em um semáforo.

## Execução

```powershell
.\rodar.ps1 filosofos ingenuo 3   # trava: 0 segura 0 e espera 1 ... 4 segura 4 e espera 0
.\rodar.ps1 filosofos garcom 3    # termina, todos comem 3 de 3
```

## Validação (`TesteFilosofos`)

20 execuções × 5 filósofos × 200 refeições, sem tempo de meditação. Em cada execução verifica:
- terminou dentro do tempo limite (sem deadlock) e todos comeram todas as refeições (sem inanição);
- nenhum hashi foi pego estando com outro filósofo (posse registrada com `compareAndSet`);
- nenhum filósofo comeu ao mesmo tempo que um vizinho;
- no máximo 2 comendo ao mesmo tempo (limite físico com 5 hashis).

Teste extra: o garçom com a mesma pausa entre hashis que faz a versão ingênua travar.
Controle negativo: a versão ingênua precisa travar.

Última execução (18/10/2026): 22/22 OK.
