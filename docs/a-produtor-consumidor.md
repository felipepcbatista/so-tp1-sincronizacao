# (a) Produtor e Consumidor (Bounded Buffer)

Responsável: Felipe Paz Carvalho Batista
Código: [`src/produtorconsumidor/`](../src/produtorconsumidor/)

O problema tem duas partes no repositório:

| | O que é | Onde |
|---|---|---|
| **Parte I** | O código do enunciado, sem alteração de lógica: compilar, executar e entender o que é impresso | [`src/produtorconsumidor/parte1/`](../src/produtorconsumidor/parte1/) |
| **Parte II** | O mesmo problema com **semáforos**, buffer de N posições e vários produtores e consumidores | [`src/produtorconsumidor/`](../src/produtorconsumidor/) |

---

## Parte I — o código do enunciado

```powershell
.\rodar.ps1 produtor original
```

Saída típica:

```
put 0
get 0
Consumidor #0 get: 0
Produtor #0 put: 0
put 1
Produtor #0 put: 1
get 1
Consumidor #0 get: 1
...
```

**Como funciona.** O `Buffer` é um **monitor**: `get()` e `put()` são `synchronized`, então só uma thread
executa dentro do buffer por vez. O buffer guarda **um único valor** e a flag `cheio`.
- `put` espera (`wait()`) enquanto o buffer está cheio; ao escrever, marca `cheio = true` e acorda os outros (`notifyAll()`).
- `get` espera enquanto o buffer está vazio; ao ler, marca `cheio = false` e acorda os outros.
- A espera fica dentro de um `while`, e não de um `if`: ao acordar, a thread confere a condição de novo.

**O que é impresso e por quê.**
- `put N` e `get N` são impressos **dentro** do monitor, então a ordem deles é sempre a mesma: put 0, get 0,
  put 1, get 1... Com uma posição só, produtor e consumidor alternam estritamente.
- `Produtor #0 put: N` e `Consumidor #0 get: N` são impressos **fora** do monitor, depois que o método retornou.
  Por isso aparecem em ordens variadas: o consumidor pode pegar o valor e imprimir antes de o produtor
  imprimir a própria linha. A ordem muda a cada execução, conforme o escalonador.
- São 5 valores (0 a 4), apesar do comentário "armazena 10 valores": o laço vai de 0 a 4.

---

## Parte II — com semáforos

### Modelagem

| Elemento | No código |
|---|---|
| **Threads** | 2 `Produtor` + 2 `Consumidor` (nos testes, 3 + 3) |
| **Recurso compartilhado** | O buffer circular de 3 posições (`BufferLimitado.dados`) e os índices `entrada` e `saida` |
| **Regiões críticas** | `escrever()` (produtor grava e avança `entrada`) e `ler()` (consumidor lê e avança `saida`) |

**Linha de execução do Produtor** (laço por N itens): produz (tempo aleatório) → espera vaga → entra na região
crítica → grava o item → sai → avisa que tem item novo.

**Linha de execução do Consumidor** (laço por N itens): espera item → entra na região crítica → lê o item →
sai → avisa que liberou vaga → consome (tempo aleatório).

Cada item tem um número único, então o programa sabe se algum item se perdeu ou foi lido duas vezes.

### Semáforos

| Semáforo | Valor inicial | Papel |
|---|---|---|
| `vazios` | N (3) | Conta as posições livres. O produtor dá `acquire` (ocupa uma vaga), o consumidor dá `release` |
| `cheios` | 0 | Conta as posições ocupadas. O consumidor dá `acquire` (pega um item), o produtor dá `release` |
| `mutex` | 1 | Exclusão mútua sobre o vetor e os índices: um de cada vez na região crítica |

`vazios` e `cheios` fazem a **sincronização** (produtor para quando o buffer enche, consumidor para quando
esvazia). O `mutex` faz a **exclusão mútua** (dois produtores não gravam na mesma posição).

### Estratégia contra deadlock: a ordem dos semáforos

```
Produtor: vazios.acquire() → mutex.acquire() → grava → mutex.release() → cheios.release()
Consumidor: cheios.acquire() → mutex.acquire() → lê   → mutex.release() → vazios.release()
```

A regra é **esperar o recurso contável primeiro e pegar o mutex depois**. Assim ninguém dorme segurando o mutex.

O modo `mutex-antes` inverte a ordem: pega o mutex e só então espera vaga/item. Se um consumidor entra com
o buffer vazio, ele **segura o mutex e espera item**, e o produtor, que colocaria o item, fica **esperando o
mutex**. Um espera o outro: deadlock. É a condição de **posse e espera**: segurar um recurso (mutex)
enquanto espera outro (item). A ordem correta elimina essa condição.

### Por que o mutex é necessário: o modo `sem-mutex`

Sem o `mutex`, dois produtores podem ler o mesmo `entrada`, gravar na mesma posição (um item sobrescreve
o outro) e avançar o índice uma vez só. Resultado: um item **se perde** e um consumidor depois **lê uma
posição vazia**. Os semáforos `vazios`/`cheios` sozinhos controlam **quantos** acessam, mas não impedem
que dois acessem **a mesma posição** ao mesmo tempo.

## Execução

```powershell
.\rodar.ps1 produtor correto 6       # termina, 12 de 12 itens consumidos exatamente uma vez
.\rodar.ps1 produtor sem-mutex 6     # termina, mas perde itens e lê posições vazias (condição de corrida)
.\rodar.ps1 produtor mutex-antes 6   # trava: "SEGURA o mutex e espera item" + os outros "esperando o mutex"
```

O log mostra o buffer a cada operação, por exemplo `buffer [ 1  6  5]` (cheio) ou `[__  0 __]`.

## Validação (`TesteProdutorConsumidor`)

1. **Correto sob alta disputa:** 20 execuções × 3 produtores × 3 consumidores × 300 itens, buffer de 5 e sem
   pausas. Em cada uma verifica: terminou, todos os itens consumidos exatamente uma vez, nenhuma leitura de
   posição vazia, ocupação nunca acima da capacidade.
2. **Correto com buffer de 1 posição** (caso extremo).
3. **Correto com a mesma pausa** na região crítica que faz o `sem-mutex` falhar.
4. **`sem-mutex` (controle negativo):** precisa perder ou duplicar itens.
5. **`mutex-antes` (controle negativo):** precisa travar.

Última execução (21/10/2026): 24/24 OK.
