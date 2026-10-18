# TP1 — Comunicação e Sincronização de Processos e Threads

Sistemas Operacionais — Engenharia de Software — PUC Minas
Prof. Diego Silva Caldeira Rocha

## Divisão do grupo

Cada integrante explica no vídeo os problemas pelos quais é responsável.
**A ausência de qualquer integrante nos vídeos zera a nota do grupo.**

| Problema | Responsável | Código | Modelagem | Vídeo |
|---|---|---|---|---|
| (a) Produtor e Consumidor | _a definir_ | | | |
| (b) Barbeiro Dorminhoco | _a definir_ | | | |
| (c) Leitores e Escritores | _a definir_ | | | |
| (d) Fumantes | Felipe Paz | [`src/fumantes/`](src/fumantes/) | [docs/d-fumantes.md](docs/d-fumantes.md) | _pendente_ |
| (e) Jantar dos Filósofos | Felipe Paz | [`src/filosofos/`](src/filosofos/) | [docs/e-filosofos.md](docs/e-filosofos.md) | _pendente_ |
| (f) Jantar dos Selvagens | _a definir_ | | | |
| (g) Três Robôs | _a definir_ | | | |

Checklist de entrega (enunciado):
- [ ] Código-fonte completo de todos os problemas
- [ ] Vídeo de cada integrante cobrindo: modelagem, threads e linhas de execução, recursos compartilhados
      e regiões críticas, semáforos, estratégia contra deadlock, execução e testes
- [ ] Links dos vídeos acessíveis (testar em aba anônima)

## Como contribuir

1. Crie uma branch: `git checkout -b problema-b`
2. Coloque o código em `src/<problema>/` (ex.: `src/barbeiro/`) e a modelagem em `docs/<letra>-<problema>.md`
3. Atualize a sua linha na tabela acima
4. Abra um Pull Request para a `main`

Padrão sugerido, seguido em (d) e (e), que facilita o vídeo:
- uma classe `Main<Problema>` que roda a simulação com log de cada passo;
- uma versão **ingênua** que mostra o problema (deadlock ou condição de corrida) e a versão **correta**;
- uma classe `Teste<Problema>` que roda várias vezes e confere invariantes, não só "rodou sem erro".

As classes `util.Log` (log com tempo e thread) e `util.Execucao` (detecta deadlock por tempo limite)
podem ser reaproveitadas.

## Como compilar e executar

Requer Java 21+. No Windows (PowerShell), a partir da raiz:

```powershell
.\rodar.ps1 fumantes correto 10
.\rodar.ps1 filosofos garcom 3
.\rodar.ps1 testes
```

Em qualquer sistema:

```
javac -encoding UTF-8 -d out src/util/*.java src/fumantes/*.java src/filosofos/*.java
java -cp out fumantes.MainFumantes correto 10
java -cp out filosofos.MainFilosofos garcom 3
```

Ao adicionar um problema novo, inclua o pacote dele no `javac` acima e no `rodar.ps1`.
