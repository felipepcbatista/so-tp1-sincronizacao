## (f)Jantar dos Selvagens

**Responsável:** Petrius Arturo **Código:** `src/selvagens/`

### Modelagem

| Elemento | No código |
|---|---|
| Threads | 1 `Cozinheiro` (laço infinito) + N `Selvagem` (cada um repete o ciclo servir → comer um número fixo de vezes) |
| Recursos compartilhados | O caldeirão (`Caldeirao.porcoes`, com capacidade N porções) |
| Regiões críticas | `Caldeirao.servir()` (o selvagem confere `porcoes`, acorda o cozinheiro se estiver vazio e decrementa) e `Caldeirao.encher()` (o cozinheiro restaura `porcoes` para N) |

Comer **não** é região crítica: não usa nenhum recurso compartilhado, e por isso vários selvagens podem comer ao mesmo tempo, como o enunciado permite.

**Linha de execução do Cozinheiro** (laço infinito): dorme bloqueado em `caldeiraoVazio` → acorda quando um selvagem encontra o caldeirão vazio → enche o caldeirão (restaura as N porções) → avisa que está cheio → volta ao início e dorme de novo.

**Linha de execução do Selvagem** (repete por um número fixo de refeições): entra na região crítica → se o caldeirão estiver vazio, acorda o cozinheiro e espera ele encher → pega uma porção → sai da região crítica → come (fora da região crítica) → volta ao início para a próxima refeição.

### Semáforos

| Semáforo | Valor inicial | Papel |
|---|---|---|
| `mutex` | 1 | Exclusão mútua ao se servir. Dois selvagens nunca conferem/alteram `porcoes` ao mesmo tempo |
| `caldeiraoVazio` | 0 | Aviso "caldeirão vazio". O selvagem faz `release` para acordar o cozinheiro; o cozinheiro faz `acquire` aqui, e é nesse ponto que ele dorme |
| `caldeiraoCheio` | 0 | Aviso "caldeirão cheio". O cozinheiro faz `release` ao terminar de encher; o selvagem faz `acquire` para esperar antes de se servir |

Os dois semáforos de sinalização começam em 0, então quem faz `acquire` neles **bloqueia sem gastar CPU** (não há espera ocupada). Como um semáforo guarda o sinal, nenhum aviso se perde mesmo que o `release` aconteça antes do `acquire`. Os três semáforos atendem às três restrições do enunciado:

- **Selvagens não se servem ao mesmo tempo:** garantido pelo `mutex`.
- **Ninguém se serve de caldeirão vazio:** o selvagem confere `porcoes == 0` dentro do `mutex` e só se serve depois do aviso `caldeiraoCheio`.
- **O cozinheiro só enche quando está vazio:** ele só acorda quando alguém faz `release` em `caldeiraoVazio`, o que só acontece com `porcoes == 0`.

### Estratégia contra deadlock

O selvagem **segura o `mutex` enquanto espera o cozinheiro**, mas o cozinheiro **nunca usa o `mutex`**: ele depende apenas de `caldeiraoVazio` e `caldeiraoCheio`. A cadeia de espera é selvagem → cozinheiro, e nunca volta do cozinheiro para o selvagem. Sem ciclo de espera, o deadlock não pode acontecer (a condição de espera circular é quebrada).

Segurar o `mutex` durante a espera é intencional: impede que outro selvagem veja o caldeirão vazio e acorde o cozinheiro uma segunda vez, ou tente se servir antes do enchimento. Os outros selvagens esperam na fila do `mutex`, e esse custo é aceitável.

O `mutex` é liberado em um bloco `finally`, então nunca fica preso mesmo se ocorrer uma exceção. Também não há inanição: o caldeirão só esvazia depois de N porções e o cozinheiro sempre o enche quando acordado, então o jantar sempre avança.

### Execução

```bash
javac -d out src/selvagens/*.java
java -cp out selvagens.MainSelvagens      # demonstração com log (N=4, 5 selvagens, 3 refeições)
java -cp out selvagens.TesteSelvagens     # 3 testes automáticos de validação
```

Trecho do log da demonstração (a ordem das linhas muda a cada execução, porque quem decide é o escalonador):

```
Selvagem 5 se serviu (restam 0)
Selvagem 3 encontrou o caldeirão vazio e acordou o cozinheiro
Cozinheiro encheu o caldeirão com 4 porções
Selvagem 3 se serviu (restam 3)
...
Fim do jantar. Porções servidas: 15 | enchimentos: 3
```

Os totais finais são sempre os mesmos: 5 selvagens × 3 refeições = 15 porções, e ⌈15 / 4⌉ − 1 = 3 enchimentos (o caldeirão já começa cheio).

### Validação (`TesteSelvagens`)

Em todos os cenários, o teste confere:

- terminou dentro do tempo limite de 30 s (sem deadlock);
- 0 violações registradas (nunca dois selvagens se servindo ao mesmo tempo, nunca servir de caldeirão vazio, cozinheiro só enche quando está vazio);
- porções servidas = selvagens × refeições;
- enchimentos = ⌈total / N⌉ − 1.

Cenários:

- **Cenário normal** — N = 4, 5 selvagens, 6 refeições cada (30 porções, 7 enchimentos). Também exige que mais de um selvagem tenha comido ao mesmo tempo, provando o paralelismo fora da região crítica.
- **Caso extremo** — N = 1, 3 selvagens, 4 refeições (12 porções, 11 enchimentos). O caldeirão esvazia a cada porção, então o cozinheiro é acordado o tempo todo.
- **Cozinheiro ocioso** — N = 100, 3 selvagens, 5 refeições (15 porções, 0 enchimentos). O caldeirão nunca esvazia, então o cozinheiro nunca pode ser acordado.

Resultado: 3/3 testes aprovados, repetido 6 vezes seguidas sem falha.
