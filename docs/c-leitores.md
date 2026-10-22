# (c) Problema dos Leitores e Escritores

Responsável: André Almeida Silva
Código: [`src/leitores/`](../src/leitores/)

## Modelagem

| Elemento | No código |
|---|---|
| **Threads** | N `Leitor` + M `Escritor`, cada um com um laço de K operações |
| **Recurso compartilhado** | O vetor de inteiros `Memoria.m` (o `M` da figura do enunciado) |
| **Região crítica** | O acesso ao vetor. Leitura pode ser **simultânea** entre leitores; escrita é **exclusiva** |

O ponto do problema é que a região crítica não é igual para todo mundo: várias leituras ao mesmo tempo
não se atrapalham, então proibir isso desperdiça paralelismo à toa. Só a escrita precisa de exclusão total.

**Linha de execução do Leitor** (laço por K leituras): pede entrada → lê as posições do vetor →
sai → repete.

**Linha de execução do Escritor** (laço por K escritas): pede acesso exclusivo → escreve o mesmo valor
em todas as posições, uma de cada vez → sai → repete.

O escritor gravar o **mesmo valor em todas as posições** é o que torna o erro visível: num acesso
sincronizado, todo leitor vê o vetor uniforme (`[203, 203, 203, 203]`). Se algum leitor vir
`[203, 203, 101, 101]`, ele pegou o vetor no meio de uma escrita — e o programa registra a violação.

## Semáforos

| Semáforo | Valor inicial | Papel |
|---|---|---|
| `recurso` | 1 | Acesso exclusivo ao vetor. O escritor segura sozinho; os leitores seguram em grupo (só o primeiro faz `acquire`, só o último faz `release`) |
| `mutex` | 1 | Exclusão mútua sobre o contador `leitores` — senão dois leitores se acham "o primeiro" |
| `fila` | 1 | Torniquete de entrada. Impede que leitores novos furem a fila na frente de um escritor que já está esperando |

`recurso` e `fila` são criados com `fair = true` (fila FIFO).

A lógica do "primeiro e último leitor" é o coração da solução:

- o **primeiro** leitor a entrar faz `recurso.acquire()` — tranca os escritores em nome de todo o grupo;
- os leitores seguintes só incrementam o contador e entram direto, **em paralelo**;
- o **último** a sair faz `recurso.release()` — destranca.

## Estratégia contra deadlock e contra inanição

**Deadlock** não ocorre porque nenhuma thread segura um semáforo e fica bloqueada esperando outro
indefinidamente. O `mutex` só protege um `leitores++`, que não bloqueia. O escritor segura `fila` enquanto
espera `recurso`, mas `fila` é sempre liberado logo em seguida e nenhum leitor espera por `fila` segurando
`recurso` — não há ciclo possível.

**Inanição é o risco real deste problema**, e é por isso que o `fila` existe. Na solução clássica com
prioridade de leitor, enquanto houver pelo menos um leitor dentro da área, `recurso` nunca é liberado.
Um fluxo contínuo de leitores mantém o contador sempre acima de zero e **o escritor nunca escreve** —
ele não trava, só nunca chega a vez dele. O torniquete resolve: quando um escritor chega, ele pega o
`fila` e os leitores novos param atrás dele; os que já estavam dentro terminam, o contador zera e o
escritor entra.

Dois controles negativos no código mostram cada falha:

- **`prioridade-leitor` — inanição.** Sem o torniquete. Medindo a maior espera do escritor sob 8 leitores
  insistentes: **228 ms contra 6 ms** da versão justa, ~38x pior. Aumentando a carga de leitura, a espera
  cresce sem limite.
- **`sem-sincronia` — condição de disputa.** Sem semáforo nenhum. O leitor pega o vetor no meio de uma
  escrita e vê posições com valores de escritas diferentes. Na última execução: **328 violações**.
  O programa não trava e não dá erro — só devolve dado corrompido.

## Execução

```powershell
.\rodar.ps1 leitores correto 5             # leitores em paralelo, escrita exclusiva, 0 violações
.\rodar.ps1 leitores prioridade-leitor 8   # escritor esperando muito mais
.\rodar.ps1 leitores sem-sincronia 6       # leituras inconsistentes
```

## Validação (`TesteLeitores`)

1. **CORRETO sob alta disputa** — 20 execuções × 6 leitores × 3 escritores, sem pausa. Em cada uma:
   todas as leituras e escritas aconteceram; nunca dois escritores juntos; nunca leitor e escritor
   juntos; nenhuma leitura inconsistente.
2. **Leitura em paralelo** — verifica que o máximo de leitores simultâneos foi **maior que 1**. Sem isso
   a solução estaria "correta" mas teria jogado fora o objetivo do problema.
3. **Escritor não passa fome** — com 8 leitores insistentes, o escritor completa todas as escritas.
4. **Controle negativo `prioridade-leitor`** — a espera do escritor tem que ser muito maior que a da
   versão justa.
5. **Controle negativo `sem-sincronia`** — leituras inconsistentes têm que aparecer.

Última execução (21/10/2026): 24/24 OK.
