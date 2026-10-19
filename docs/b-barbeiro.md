# (b) Problema do Barbeiro Dorminhoco

Responsável: André Almeida Silva
Código: [`src/barbearia/`](../src/barbearia/)

## Modelagem

| Elemento | No código |
|---|---|
| **Threads** | 1 `Barbeiro` (laço infinito) + N `Cliente` (sem laço: cada cliente corta o cabelo uma única vez) |
| **Recursos compartilhados** | A sala de espera (`Barbearia.esperando`, o *waiting* do enunciado) e a cadeira do barbeiro |
| **Regiões críticas** | `Barbearia.entrarNaSala()` (o cliente lê `esperando`, decide se fica e incrementa) e `Barbearia.chamarProximo()` (o barbeiro decrementa `esperando`) |

`esperando` existe porque **não há como ler o valor atual de um semáforo**: é a cópia da contagem de
clientes à espera que o enunciado chama de `waiting`.

**Linha de execução do Barbeiro** (laço infinito): dorme bloqueado em `clientes` → acorda quando alguém
chega → tira um cliente da sala (região crítica) e avisa que está livre → corta o cabelo → avisa que o
corte acabou → espera o cliente levantar → volta ao início (se não houver ninguém, dorme de novo).

**Linha de execução do Cliente** (uma passagem só): chega → entra na região crítica → se a sala estiver
cheia, vai embora sem cortar o cabelo; senão senta, acorda o barbeiro e sai da região crítica → espera a
vez → senta na cadeira do barbeiro → espera o corte terminar → levanta e vai embora.

## Semáforos

| Semáforo | Valor inicial | Papel |
|---|---|---|
| `mutex` | 1 | Exclusão mútua sobre a sala de espera. Dois clientes nunca avaliam/alteram `esperando` ao mesmo tempo |
| `clientes` | 0 | Conta os clientes à espera. O barbeiro faz `acquire` aqui — **é nesse ponto que ele dorme**. O cliente que senta faz `release` e o acorda |
| `barbeiros` | 0 | 0 ou 1 barbeiro ocioso. O barbeiro faz `release` ao chamar o próximo; o cliente faz `acquire` esperando a sua vez |
| `corteFeito` | 0 | Fim do corte: o barbeiro faz `release`, o cliente faz `acquire` para só então levantar da cadeira |
| `clienteSaiu` | 0 | Cadeira desocupada: o cliente faz `release` ao levantar, o barbeiro faz `acquire` antes de chamar o próximo |

Os três primeiros são exatamente os da solução descrita no enunciado (Tanenbaum). `corteFeito` e
`clienteSaiu` são um acréscimo deliberado: na versão mínima, o barbeiro pode terminar um corte e já chamar
o próximo cliente **antes** de o anterior ter levantado da cadeira. Nada quebra na prática, mas a
invariante "no máximo um cliente na cadeira do barbeiro" deixaria de ser verificável. Com esses dois
semáforos o atendimento vira um aperto de mão explícito: barbeiro → cliente → barbeiro.

## Estratégia contra deadlock

A ordem das operações é o que segura a solução: **o cliente libera o `mutex` antes de bloquear à espera
do barbeiro**. Assim, nenhuma thread fica bloqueada em um semáforo segurando outro — não existe posse e
espera, e portanto não há ciclo de espera possível. O `mutex` é segurado apenas por trechos curtos que
não bloqueiam (ler e alterar um inteiro).

Também não há inanição por lotação: o cliente que encontra a sala cheia não fica esperando indefinidamente
— ele desiste e vai embora, exatamente como o enunciado manda.

O repositório traz **dois modos quebrados** como controle negativo, para mostrar o que a solução evita:

- **`sem-mutex` — race condition.** O cliente avalia `esperando < cadeiras` e incrementa `esperando`
  **sem** exclusão mútua. Vários clientes leem o mesmo valor antigo e todos sentam: a sala de espera
  estoura o número de cadeiras, e os incrementos perdidos deixam `esperando` negativo. É a
  *condição de disputa* que o enunciado cita nominalmente. Não trava — simplesmente dá a resposta errada,
  que é o que torna esse tipo de bug perigoso.
- **`mutex-preso` — deadlock.** O cliente faz `acquire` em `barbeiros` **ainda segurando o `mutex`**.
  O barbeiro acorda, tenta pegar o `mutex` para chamar o próximo e trava; o cliente trava esperando o
  barbeiro. Posse e espera fechando um ciclo de duas threads, e a barbearia inteira para com todos os
  outros clientes presos na porta. É exatamente o passo que o enunciado descreve ao dizer que *"quando o
  cliente libera mutex, o barbeiro o pega"*.

## Execução

```powershell
.\rodar.ps1 barbearia correto 12       # termina: 12 clientes entre atendidos e desistentes
.\rodar.ps1 barbearia sem-mutex 12     # race condition: mais clientes na sala do que cadeiras
.\rodar.ps1 barbearia mutex-preso 12   # trava e imprime quem segura o quê
```

Um terceiro argumento opcional muda o número de cadeiras da sala (padrão 3):
`.\rodar.ps1 barbearia correto 12` usa 3 cadeiras; `java -cp out barbearia.MainBarbearia correto 12 1`
usa 1.

O dump de deadlock do modo `mutex-preso` aponta o culpado direto:

```
!!! DEADLOCK: a execução não terminou em 5000 ms. Estado de cada thread:
    Barbeiro         [WAITING] acordou, chamando o próximo da sala
    Cliente-1        [WAITING] SEGURA o mutex e espera o barbeiro
    Cliente-2        [WAITING] entrando, conferindo as cadeiras
    ...
```

## Validação (`TesteBarbearia`)

1. **CORRETO sob alta disputa** — 20 execuções × 200 clientes chegando de uma vez × 3 cadeiras. Em cada uma:
   - terminou dentro do tempo limite (sem deadlock);
   - `atendidos + desistiram = 200` (nenhum cliente se perdeu nem foi atendido duas vezes);
   - cortes feitos pelo barbeiro = clientes atendidos;
   - a sala nunca passou de 3 clientes;
   - nunca houve dois clientes na cadeira do barbeiro ao mesmo tempo;
   - 0 violações registradas.
2. **Barbeiro ocioso** — 8 clientes com chegadas espaçadas: o barbeiro precisa dormir pelo menos uma vez
   (é o "dorminhoco" do enunciado) e todos os 8 são atendidos, sem ninguém ir embora.
3. **Sala lotada** — 60 clientes de uma vez para 2 cadeiras: alguém **tem** que ir embora, e ainda assim a
   sala nunca passa de 2.
4. **Controle negativo `sem-mutex`** — a sala precisa estourar o limite de cadeiras.
5. **Controle negativo `mutex-preso`** — a execução precisa travar.

Última execução (18/10/2026): 24/24 OK, repetido 4 vezes seguidas sem falha.
